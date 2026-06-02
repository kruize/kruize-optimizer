"""Pytest configuration and fixtures for e2e tests."""

import logging
import sys
import tempfile
from datetime import datetime
from pathlib import Path

import pytest
from kubernetes import client, config as k8s_config

# Add the e2e directory to Python path for absolute imports
sys.path.insert(0, str(Path(__file__).parent))

from config import Config
from setup import benchmarks, cleanup, cluster, kruize, optimizer, prometheus, repos
from setup.port_forward import start_port_forwards
from utils.kubectl import KubectlClient
from utils.wait import sleep_with_progress, wait_for_pod_running

# Configure logging
E2E_DIR = Path(__file__).parent
RESULTS_DIR = E2E_DIR / "results"
RESULTS_DIR.mkdir(exist_ok=True)

LOG_FILE = RESULTS_DIR / f"e2e-test-{datetime.now().strftime('%Y%m%d-%H%M%S')}.log"

# File handler - ALWAYS write detailed logs to file
file_handler = logging.FileHandler(LOG_FILE)
file_handler.setLevel(logging.DEBUG)
file_handler.setFormatter(logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s'))

# Root logger - always logs to file
root_logger = logging.getLogger()
root_logger.setLevel(logging.DEBUG)
root_logger.addHandler(file_handler)

# Our logger
logger = logging.getLogger("kruize_e2e")
logger.setLevel(logging.DEBUG)
logger.propagate = True


def pytest_addoption(parser):
    """Add custom command line options."""
    parser.addoption(
        "--cluster-type",
        default="kind",
        choices=["kind", "minikube", "openshift"],
        help="Cluster type"
    )
    parser.addoption(
        "--install-mode",
        default="manifest",
        choices=["manifest", "operator"],
        help="Install mode (manifest or operator)"
    )
    parser.addoption(
        "--env-setup",
        action="store_true",
        default=False,
        help="Create cluster from scratch (-f flag equivalent)"
    )
    parser.addoption(
        "--app-namespace",
        default="default",
        help="Application namespace for benchmarks"
    )
    parser.addoption(
        "--kruize-image",
        default=None,
        help="Custom kruize image"
    )
    parser.addoption(
        "--kruize-ui-image",
        default=None,
        help="Custom kruize UI image"
    )
    parser.addoption(
        "--kruize-operator-image",
        default=None,
        help="Custom kruize operator image"
    )
    parser.addoption(
        "--kruize-optimizer-image",
        default=None,
        help="Custom kruize optimizer image"
    )
    parser.addoption(
        "--skip-setup",
        action="store_true",
        default=False,
        help="Skip setup, use existing deployment"
    )
    parser.addoption(
        "--skip-teardown",
        action="store_true",
        default=False,
        help="Skip teardown, leave resources running"
    )
    parser.addoption(
        "--working-dir",
        default=None,
        help="Working directory for cloned repos"
    )
    parser.addoption(
        "--optimizer-wait-duration",
        type=int,
        default=None,
        help="Override optimizer wait duration (seconds)"
    )
    parser.addoption(
        "--terminate",
        action="store_true",
        default=False,
        help="Terminate mode: cleanup everything and exit (no tests run)"
    )
    parser.addoption(
        "--verbose-output",
        action="store_true",
        default=False,
        help="Enable verbose output (show all logs on console)"
    )


@pytest.fixture(scope="session")
def config(request):
    """Build Config from CLI args."""
    return Config.from_pytest_config(request.config)


@pytest.fixture(scope="session")
def working_dir(config, tmp_path_factory):
    """Create or use specified working directory."""
    if config.working_dir:
        work_dir = Path(config.working_dir)
        work_dir.mkdir(parents=True, exist_ok=True)
        return str(work_dir)
    else:
        # Default to tests/e2e/cloned_repos/
        work_dir = E2E_DIR / "cloned_repos"
        work_dir.mkdir(exist_ok=True)
        return str(work_dir)


@pytest.fixture(scope="session")
def kubectl(config):
    """Create KubectlClient instance."""
    return KubectlClient(kubectl_cmd=config.kubectl_cmd)


@pytest.fixture(scope="session")
def k8s_client(config):
    """Create Kubernetes Python client."""
    try:
        k8s_config.load_kube_config()
    except Exception as e:
        logger.warning(f"Failed to load kubeconfig: {e}")

    return client.CoreV1Api()


@pytest.fixture(scope="session")
def namespace(config):
    """Return the target namespace."""
    return config.namespace


@pytest.fixture(scope="session", autouse=True)
def setup_environment(config, working_dir, kubectl, request):
    """
    Session-scoped setup/teardown fixture.

    Implements the EXACT flow from optimizer_demo_setup in bash scripts.
    """
    # Console logging - only if --verbose-output is passed
    if request.config.getoption("--verbose-output"):
        console_handler = logging.StreamHandler()
        console_handler.setLevel(logging.DEBUG)
        console_handler.setFormatter(logging.Formatter('%(name)s - %(levelname)s - %(message)s'))
        root_logger.addHandler(console_handler)
        print(f"Verbose mode enabled - detailed logs: console + {LOG_FILE}")
    else:
        print(f"Log file: {LOG_FILE}")

    port_forward_processes = []

    # Terminate mode: cleanup and exit
    if config.terminate:
        print("\n" + "=" * 70)
        print("TERMINATE MODE - Cleaning up all resources")
        print("=" * 70)
        try:
            print("🔄 Running full cleanup...", end=" ", flush=True)
            cleanup.full_cleanup(config, working_dir, kubectl, port_forward_processes)
            print("✅ Done")
            print("=" * 70)
            print("✅ CLEANUP COMPLETE")
            print("=" * 70 + "\n")
        except Exception as e:
            print(f"\n❌ Cleanup failed: {e}")
            print(f"Check detailed logs in: {LOG_FILE}")
        # Exit pytest without running tests
        pytest.exit("Terminate mode: cleanup complete", returncode=0)

    if not config.skip_setup:
        # Clean console output - only step numbers
        print("\n" + "=" * 70)
        print("KRUIZE E2E TEST SETUP")
        print("=" * 70)
        print()

        try:
            total_steps = 16

            # Step 1: Clone repos
            print(f"🔄 [1/{total_steps}] Cloning repositories...", end=" ", flush=True)
            repo_paths = repos.clone_all_repos(working_dir, config)
            autotune_dir = repo_paths["autotune"]
            benchmarks_dir = repo_paths["benchmarks"]
            print("✅")

            # Step 2: Check existing deployment
            print(f"🔄 [2/{total_steps}] Checking existing deployment...", end=" ", flush=True)
            cluster_accessible = kubectl.cluster_info()
            print("✅")

            # Step 3: Pre-test cleanup (if existing resources found)
            if cluster_accessible:
                print(f"🔄 [3/{total_steps}] Pre-test cleanup...", end=" ", flush=True)
                cleanup.pre_test_cleanup(config, working_dir, kubectl)
                print("✅")

            # Step 4 & 5: Environment setup
            if config.env_setup:
                print(f"🔄 [4/{total_steps}] Setting up {config.cluster_type} cluster + prometheus...", end=" ", flush=True)
                if config.cluster_type == "kind":
                    cluster.check_kind_exists()
                    cluster.kind_start(config.KIND_KUBERNETES_VERSION)
                    prometheus.install_prometheus(autotune_dir, config.cluster_type)
                elif config.cluster_type == "openshift":
                    cluster.check_openshift_accessible()
                print("✅")
            else:
                print(f"🔄 [5/{total_steps}] Verifying cluster...", end=" ", flush=True)
                if config.cluster_type == "kind":
                    cluster.check_kind_exists()
                    if not cluster.check_cluster_accessible(config.cluster_type):
                        raise Exception("Kind cluster not running. Use --env-setup")
                elif config.cluster_type == "openshift":
                    cluster.check_openshift_accessible()
                print("✅")

            # Step 6: Install sysbench
            print(f"🔄 [6/{total_steps}] Installing sysbench...", end=" ", flush=True)
            kubectl.create_namespace(config.app_namespace)
            benchmarks.install_sysbench(benchmarks_dir, config.app_namespace, kubectl)
            print("✅")

            # Step 7: Label sysbench
            print(f"🔄 [7/{total_steps}] Labeling sysbench...", end=" ", flush=True)
            benchmarks.label_deployment(
                kubectl, "sysbench", {"kruize/autotune": "enabled"}, config.app_namespace
            )
            print("✅")

            # Step 8: Enable kube state metrics
            print(f"🔄 [8/{total_steps}] Enabling kube state metrics...", end=" ", flush=True)
            prometheus.enable_kube_state_metrics(autotune_dir, config.cluster_type)
            print("✅")

            # Step 9: Install TFB
            print(f"🔄 [9/{total_steps}] Installing TFB benchmark...", end=" ", flush=True)
            benchmarks.install_tfb(benchmarks_dir, config.app_namespace, kubectl)
            benchmarks.label_deployment(
                kubectl, "tfb-qrh-sample", {"kruize/autotune": "enabled"}, config.app_namespace
            )
            print("✅")

            # Step 10: Patch Kruize
            print(f"🔄 [10/{total_steps}] Patching Kruize (local mode)...", end=" ", flush=True)
            kruize.kruize_local_patch(autotune_dir, config.cluster_type)
            print("✅")

            # Step 11: Install Kruize
            print(f"🔄 [11/{total_steps}] Installing Kruize...", end=" ", flush=True)
            kruize.kruize_install(autotune_dir, config.cluster_type, config)
            print("✅")

            # Step 12: Install optimizer
            print(f"🔄 [12/{total_steps}] Installing kruize-optimizer...", end=" ", flush=True)
            optimizer_dir = optimizer.optimizer_install(working_dir, config, kubectl)
            print("✅")

            # Step 13: Verify optimizer pod
            print(f"🔄 [13/{total_steps}] Waiting for optimizer pod...", end=" ", flush=True)
            if not wait_for_pod_running(
                kubectl, config.namespace, "app=kruize-optimizer", max_attempts=60, interval=5
            ):
                raise Exception("Optimizer pod not ready")
            print("✅")

            # Step 14-15: Port forward
            print(f"🔄 [14/{total_steps}] Setting up access...", end=" ", flush=True)
            port_forward_processes = start_port_forwards(kubectl, config.namespace, config)
            print("✅")

            # Step 16: Wait for optimizer
            wait_duration = config.optimizer_wait_duration_resolved
            print(f"🔄 [15/{total_steps}] Waiting {wait_duration}s for experiments...", end=" ", flush=True)
            sleep_with_progress(wait_duration, "")
            print("✅")

            print(f"🔄 [16/{total_steps}] Verifying setup...", end=" ", flush=True)
            print("✅")

            print("\n" + "=" * 70)
            print("✅ SETUP COMPLETE - Running tests")
            print("=" * 70 + "\n")

        except Exception as e:
            print(f"\n❌ SETUP FAILED: {e}")
            print(f"Check detailed logs in: {LOG_FILE}")
            raise

    # Tests run here
    yield

    # Teardown
    if not config.skip_teardown:
        print("\n" + "=" * 70)
        print("RUNNING TEARDOWN")
        print("=" * 70)
        try:
            print("🔄 Cleaning up resources...", end=" ", flush=True)
            cleanup.full_cleanup(config, working_dir, kubectl, port_forward_processes)
            print("✅ Done")
            print("=" * 70)
            print(f"✅ TEARDOWN COMPLETE - Detailed logs: {LOG_FILE}")
            print("=" * 70 + "\n")
        except Exception as e:
            print(f"\n❌ Teardown failed: {e}")
            if request.config.getoption("--verbose-output"):
                print(f"Check detailed logs in: {LOG_FILE}")


@pytest.fixture(scope="session")
def kruize_url(config):
    """Return the Kruize API URL."""
    return config.kruize_url


@pytest.fixture(scope="session")
def kruize_client(kruize_url):
    """Return KruizeAPIClient instance."""
    from utils.http_client import KruizeAPIClient
    return KruizeAPIClient(kruize_url)
