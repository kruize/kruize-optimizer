"""Kruize installation utilities (manifest mode)."""

import logging
import time
from pathlib import Path

from utils.file_ops import replace_in_file, read_version_from_pom
from utils.git_ops import checkout_branch
from utils.process import run_script

logger = logging.getLogger("kruize_e2e.setup.kruize")


def kruize_local_patch(autotune_dir: str, cluster_type: str) -> None:
    """Patch Kruize manifests to enable local mode.

    Args:
        autotune_dir: Path to autotune repository
        cluster_type: Cluster type
    """
    logger.info("Patching Kruize manifests for local mode...")

    manifest_base = Path(autotune_dir) / "manifests" / "crc" / "default-db-included-installation"

    if cluster_type == "kind":
        manifest_file = manifest_base / "minikube" / "kruize-crc-minikube.yaml"
    elif cluster_type == "openshift":
        manifest_file = manifest_base / "openshift" / "kruize-crc-openshift.yaml"
    else:
        logger.warning(f"Kruize local patch not supported for {cluster_type}")
        return

    replace_in_file(str(manifest_file), r'"local": "false"', '"local": "true"')
    logger.info("Kruize local mode patch applied")


def kruize_install(autotune_dir: str, cluster_type: str, config) -> None:
    """Install Kruize using deploy.sh (manifest mode).

    Args:
        autotune_dir: Path to autotune repository
        cluster_type: Cluster type
        config: Config instance
    """
    logger.info("Installing Kruize (manifest mode)...")

    # Checkout mvp_demo branch
    checkout_branch(autotune_dir, "mvp_demo")

    # Read version from pom.xml
    pom_path = Path(autotune_dir) / "pom.xml"
    version = read_version_from_pom(str(pom_path))
    logger.info(f"Kruize version: {version}")

    # Build image argument
    kruize_image = config.kruize_image or f"{config.KRUIZE_DOCKER_REPO}:{version}"
    deploy_cluster_type = config.cluster_type_for_deploy

    # Terminate existing installation (non-fatal - may not exist in fresh cluster)
    logger.info("Attempting to terminate existing Kruize installation (if any)...")
    deploy_script = Path(autotune_dir) / "deploy.sh"
    try:
        run_script(
            str(deploy_script),
            args=["-c", deploy_cluster_type, "-m", config.TARGET, "-t"],
            cwd=autotune_dir,
            timeout=300,
        )
        time.sleep(5)
    except Exception as e:
        logger.debug(f"Terminate returned error (expected if nothing installed): {e}")
        # Continue - this is expected in a fresh cluster

    # Install Kruize
    logger.info(f"Installing Kruize with image {kruize_image}...")
    args = ["-c", deploy_cluster_type, "-i", kruize_image, "-m", config.TARGET]

    if config.kruize_ui_image:
        args.extend(["-u", config.kruize_ui_image])

    run_script(str(deploy_script), args=args, cwd=autotune_dir, timeout=600)

    # Wait for sync with Prometheus
    logger.info("Waiting 40s for Kruize to sync with Prometheus...")
    time.sleep(40)

    logger.info("Kruize installation complete")


def kruize_uninstall(autotune_dir: str, cluster_type: str, config) -> None:
    """Uninstall Kruize using deploy.sh.

    Args:
        autotune_dir: Path to autotune repository
        cluster_type: Cluster type
        config: Config instance
    """
    logger.info("Uninstalling Kruize...")

    deploy_script = Path(autotune_dir) / "deploy.sh"
    deploy_cluster_type = config.cluster_type_for_deploy

    run_script(
        str(deploy_script),
        args=["-c", deploy_cluster_type, "-m", config.TARGET, "-t"],
        cwd=autotune_dir,
        timeout=300,
    )

    logger.info("Kruize uninstallation complete")
