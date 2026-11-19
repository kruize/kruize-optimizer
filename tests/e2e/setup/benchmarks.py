"""Benchmark installation utilities."""

import logging
from pathlib import Path
from typing import Dict

logger = logging.getLogger("kruize_e2e.setup.benchmarks")


def install_sysbench(benchmarks_dir: str, namespace: str, kubectl) -> None:
    """Install sysbench benchmark.

    Args:
        benchmarks_dir: Path to benchmarks repository
        namespace: Target namespace
        kubectl: KubectlClient instance
    """
    logger.info(f"Installing sysbench benchmark in namespace {namespace}...")

    # Cleanup old deployment
    kubectl.delete_resource("deployment", "sysbench", namespace)

    # Apply manifest
    sysbench_manifest = Path(benchmarks_dir) / "sysbench" / "manifests" / "sysbench.yaml"
    kubectl.apply_file(str(sysbench_manifest), namespace=namespace)

    logger.info("Sysbench installation complete")


def install_tfb(benchmarks_dir: str, namespace: str, kubectl) -> None:
    """Install TechEmpower (TFB) benchmark.

    Args:
        benchmarks_dir: Path to benchmarks repository
        namespace: Target namespace
        kubectl: KubectlClient instance
    """
    logger.info(f"Installing TFB benchmark in namespace {namespace}...")

    # Cleanup old resources
    kubectl.delete_resource("deployment", "tfb-qrh-sample", namespace)
    kubectl.delete_resource("deployment", "tfb-database", namespace)
    kubectl.delete_resource("service", "tfb-qrh-service", namespace)
    kubectl.delete_resource("service", "tfb-database-service", namespace)
    kubectl.delete_resource("job", "tfb-qrh-load-generator", namespace)

    # Apply manifests
    tfb_manifests = Path(benchmarks_dir) / "techempower" / "manifests" / "kruize-demos"
    kubectl.run(["apply", "-f", str(tfb_manifests)], namespace=namespace)

    logger.info("TFB installation complete")


def label_deployment(kubectl, name: str, labels: Dict[str, str], namespace: str) -> None:
    """Label a deployment.

    Args:
        kubectl: KubectlClient instance
        name: Deployment name
        labels: Labels to apply
        namespace: Namespace
    """
    kubectl.label_resource("deployment", name, labels, namespace, overwrite=True)


def uninstall_sysbench(benchmarks_dir: str, namespace: str, kubectl) -> None:
    """Uninstall sysbench benchmark.

    Args:
        benchmarks_dir: Path to benchmarks repository
        namespace: Namespace
        kubectl: KubectlClient instance
    """
    logger.info("Uninstalling sysbench...")
    kubectl.delete_resource("deployment", "sysbench", namespace)


def uninstall_tfb(benchmarks_dir: str, namespace: str, kubectl) -> None:
    """Uninstall TFB benchmark.

    Args:
        benchmarks_dir: Path to benchmarks repository
        namespace: Namespace
        kubectl: KubectlClient instance
    """
    logger.info("Uninstalling TFB...")
    kubectl.delete_resource("deployment", "tfb-qrh-sample", namespace)
    kubectl.delete_resource("deployment", "tfb-database", namespace)
    kubectl.delete_resource("service", "tfb-qrh-service", namespace)
    kubectl.delete_resource("service", "tfb-database-service", namespace)
