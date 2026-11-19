"""Prometheus installation utilities."""

import logging
from pathlib import Path

from utils.process import run_script

logger = logging.getLogger("kruize_e2e.setup.prometheus")


def install_prometheus(autotune_dir: str, cluster_type: str) -> None:
    """Install Prometheus using autotune scripts.

    Args:
        autotune_dir: Path to autotune repository
        cluster_type: Cluster type (kind/minikube/openshift)
    """
    logger.info(f"Installing Prometheus for {cluster_type}...")

    if cluster_type == "minikube":
        script = Path(autotune_dir) / "scripts" / "prometheus_on_minikube.sh"
    elif cluster_type == "kind":
        script = Path(autotune_dir) / "scripts" / "prometheus_on_kind.sh"
    else:
        logger.warning(f"Prometheus installation not supported for {cluster_type}")
        return

    run_script(str(script), args=["-as"], cwd=autotune_dir, timeout=600)
    logger.info("Prometheus installation complete")


def enable_kube_state_metrics(autotune_dir: str, cluster_type: str) -> None:
    """Enable kube state metrics labels.

    Args:
        autotune_dir: Path to autotune repository
        cluster_type: Cluster type
    """
    logger.info(f"Enabling kube state metrics for {cluster_type}...")

    if cluster_type in ["kind", "minikube"]:
        script = Path(autotune_dir) / "scripts" / "enable_kube_state_metrics_labels.sh"
    elif cluster_type == "openshift":
        script = Path(autotune_dir) / "scripts" / "enable_user_workload_monitoring_openshift.sh"
    else:
        logger.warning(f"Kube state metrics not supported for {cluster_type}")
        return

    run_script(str(script), cwd=autotune_dir, timeout=300)
    logger.info("Kube state metrics enabled")
