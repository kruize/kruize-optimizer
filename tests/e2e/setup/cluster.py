"""Cluster management utilities."""

import logging
from pathlib import Path

from utils.process import run_command, ProcessError

logger = logging.getLogger("kruize_e2e.setup.cluster")


def check_cluster_accessible(cluster_type: str) -> bool:
    """Check if cluster is accessible.

    Args:
        cluster_type: Cluster type (kind/minikube/openshift)

    Returns:
        True if accessible
    """
    cmd = "oc" if cluster_type == "openshift" else "kubectl"
    try:
        run_command([cmd, "cluster-info"], timeout=5, check=True)
        return True
    except (ProcessError, Exception) as e:
        logger.warning(f"Cluster not accessible: {e}")
        return False


def check_kind_exists() -> None:
    """Check if kind is installed.

    Raises:
        ProcessError: If kind is not available
    """
    logger.info("Checking if kind is installed...")
    try:
        run_command(["kind", "version"], check=True)
        logger.info("kind is available")
    except ProcessError:
        raise ProcessError("kind is not installed. Please install kind and try again!")


def kind_start(kubernetes_version: str) -> None:
    """Start a new kind cluster.

    Args:
        kubernetes_version: Kubernetes version (e.g., "v1.28.0")
    """
    logger.info("Deleting existing kind clusters...")
    run_command(["kind", "delete", "clusters", "--all"], check=False)

    logger.info(f"Creating new kind cluster with Kubernetes {kubernetes_version}...")
    image = f"kindest/node:{kubernetes_version}"
    run_command(["kind", "create", "cluster", "--image", image], check=True)

    logger.info("Setting kubectl context...")
    run_command(["kubectl", "cluster-info", "--context", "kind-kind"], check=True)
    run_command(["kubectl", "config", "use-context", "kind-kind"], check=True)

    logger.info("Kind cluster is ready")


def kind_delete() -> None:
    """Delete kind cluster."""
    logger.info("Deleting kind clusters...")
    run_command(["kind", "delete", "clusters", "--all"], check=False)


def check_openshift_accessible() -> None:
    """Check if OpenShift cluster is accessible.

    Raises:
        ProcessError: If cluster is not accessible
    """
    logger.info("Checking OpenShift cluster accessibility...")
    try:
        run_command(["oc", "cluster-info"], timeout=5, check=True)
        logger.info("OpenShift cluster is accessible")
    except ProcessError:
        raise ProcessError("OpenShift cluster is not accessible. Please ensure you are logged in!")
