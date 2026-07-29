"""Cleanup orchestration utilities."""

import logging
import time

from setup import benchmarks, cluster, kruize, operator, optimizer, repos
from setup.port_forward import kill_all_port_forwards

logger = logging.getLogger("kruize_e2e.setup.cleanup")


def pre_test_cleanup(config, working_dir: str, kubectl, port_forward_processes=None) -> None:
    """Cleanup before tests (if existing resources found).

    Args:
        config: Config instance
        working_dir: Working directory
        kubectl: KubectlClient instance
        port_forward_processes: List of port forward processes
    """
    logger.info("Running pre-test cleanup...")

    # Kill port forwards (kind only)
    if config.cluster_type == "kind" and port_forward_processes:
        kill_all_port_forwards(port_forward_processes)

    # Check for existing operator deployment
    operator_deployment = kubectl.get_deployment("kruize-operator", config.namespace)

    # Check for existing kruize/optimizer pods
    kruize_pods = kubectl.get_pods(config.namespace, "app=kruize")
    optimizer_pods = kubectl.get_pods(config.namespace, "app=kruize-optimizer")

    if operator_deployment:
        logger.info("Found existing operator deployment, cleaning up...")
        operator.kruize_operator_cleanup(config.namespace, config.cluster_type, kubectl)

    if kruize_pods:
        logger.info("Found existing kruize pods, uninstalling...")
        autotune_dir = f"{working_dir}/autotune"
        kruize.kruize_uninstall(autotune_dir, config.cluster_type, config)

    if optimizer_pods:
        logger.info("Found existing optimizer pods, uninstalling...")
        optimizer_dir = f"{working_dir}/kruize-optimizer"
        optimizer.optimizer_uninstall(optimizer_dir, config, kubectl)

    if operator_deployment or kruize_pods or optimizer_pods:
        logger.info("Waiting 10s for resources to be removed...")
        time.sleep(10)


def full_cleanup(config, working_dir: str, kubectl, port_forward_processes=None) -> None:
    """Full cleanup after tests.

    Args:
        config: Config instance
        working_dir: Working directory
        kubectl: KubectlClient instance
        port_forward_processes: List of port forward processes
    """
    logger.info("Running full cleanup...")

    # Operator cleanup (if operator mode)
    if config.install_mode == "operator":
        operator.kruize_operator_cleanup(config.namespace, config.cluster_type, kubectl)

    # Kruize uninstall
    autotune_dir = f"{working_dir}/autotune"
    try:
        kruize.kruize_uninstall(autotune_dir, config.cluster_type, config)
    except Exception as e:
        logger.warning(f"Kruize uninstall failed: {e}")

    # Optimizer uninstall
    optimizer_dir = f"{working_dir}/kruize-optimizer"
    try:
        optimizer.optimizer_uninstall(optimizer_dir, config, kubectl)
    except Exception as e:
        logger.warning(f"Optimizer uninstall failed: {e}")

    # Benchmark uninstall
    benchmarks_dir = f"{working_dir}/benchmarks"
    try:
        benchmarks.uninstall_sysbench(benchmarks_dir, config.app_namespace, kubectl)
        benchmarks.uninstall_tfb(benchmarks_dir, config.app_namespace, kubectl)
    except Exception as e:
        logger.warning(f"Benchmark uninstall failed: {e}")

    # Delete namespace (if not default)
    if config.app_namespace != "default":
        kubectl.delete_namespace(config.app_namespace)

    # Kill port forwards and delete cluster (kind only, if env_setup was used)
    if config.cluster_type == "kind":
        if port_forward_processes:
            kill_all_port_forwards(port_forward_processes)

        if config.env_setup:
            cluster.kind_delete()

    # Delete cloned repos
    repos.delete_all_repos(working_dir, ["autotune", "benchmarks", "kruize-operator", "kruize-optimizer"])

    logger.info("Full cleanup complete")
