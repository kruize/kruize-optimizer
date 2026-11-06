"""Kruize-optimizer installation utilities."""

import logging
from pathlib import Path

from utils.file_ops import update_deployment_image
from setup.repos import clone_optimizer_repo

logger = logging.getLogger("kruize_e2e.setup.optimizer")


def optimizer_install(working_dir: str, config, kubectl) -> str:
    """Install kruize-optimizer (manifest mode).

    Args:
        working_dir: Working directory
        config: Config instance
        kubectl: KubectlClient instance

    Returns:
        Path to cloned optimizer repo
    """
    logger.info("Installing kruize-optimizer...")

    # Clone kruize-optimizer repo
    optimizer_dir = clone_optimizer_repo(working_dir, config)

    # Update image if custom image provided
    if config.kruize_optimizer_image:
        deployment_yaml = Path(optimizer_dir) / "deployment" / "base" / "deployment.yaml"
        update_deployment_image(str(deployment_yaml), config.kruize_optimizer_image)

    # Determine overlay based on cluster type
    overlay = "kind" if config.cluster_type == "kind" else "openshift"
    overlay_path = Path(optimizer_dir) / "deployment" / "overlays" / overlay

    # Apply kustomize overlay
    kubectl.apply_kustomize(str(overlay_path))

    # Wait for pod to be ready using wait utility (more reliable)
    from utils.wait import wait_for_pod_running
    import time

    logger.info("Waiting for kruize-optimizer pod to be ready...")
    time.sleep(5)  # Give k8s time to create the pod

    if not wait_for_pod_running(
        kubectl,
        config.namespace,
        "app=kruize-optimizer",
        max_attempts=60,  # 60 * 5 = 300s
        interval=5,
    ):
        raise Exception("Kruize-optimizer pod did not become ready within 300s")

    logger.info("Kruize-optimizer installation complete")
    return optimizer_dir


def optimizer_uninstall(optimizer_dir: str, config, kubectl) -> None:
    """Uninstall kruize-optimizer.

    Args:
        optimizer_dir: Path to optimizer repository
        config: Config instance
        kubectl: KubectlClient instance
    """
    logger.info("Uninstalling kruize-optimizer...")

    overlay = "kind" if config.cluster_type == "kind" else "openshift"
    overlay_path = Path(optimizer_dir) / "deployment" / "overlays" / overlay

    if overlay_path.exists():
        kubectl.delete_kustomize(str(overlay_path))

    logger.info("Kruize-optimizer uninstallation complete")
