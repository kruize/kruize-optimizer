"""Kubectl/oc wrapper utilities."""

import json
import logging
import subprocess
from typing import Dict, List, Optional

from utils.process import run_command, ProcessError

logger = logging.getLogger("kruize_e2e.kubectl")


class KubectlError(Exception):
    """Exception raised for kubectl operation failures."""
    pass


class KubectlClient:
    """Wrapper around kubectl/oc commands."""

    def __init__(self, kubectl_cmd: str = "kubectl"):
        """Initialize KubectlClient.

        Args:
            kubectl_cmd: Command to use (kubectl or oc)
        """
        self.kubectl_cmd = kubectl_cmd

    def run(
        self,
        args: List[str],
        namespace: Optional[str] = None,
        check: bool = True,
    ) -> subprocess.CompletedProcess:
        """Run kubectl/oc command.

        Args:
            args: Command arguments
            namespace: Namespace for namespaced operations
            check: Raise exception on failure

        Returns:
            CompletedProcess instance
        """
        cmd = [self.kubectl_cmd]
        if namespace:
            cmd.extend(["-n", namespace])
        cmd.extend(args)

        try:
            return run_command(cmd, check=check)
        except ProcessError as e:
            if check:
                raise KubectlError(str(e)) from e
            raise

    def cluster_info(self, timeout: int = 5) -> bool:
        """Check if cluster is accessible.

        Args:
            timeout: Timeout in seconds

        Returns:
            True if cluster is accessible
        """
        try:
            run_command([self.kubectl_cmd, "cluster-info"], timeout=timeout, check=True)
            return True
        except (ProcessError, Exception) as e:
            logger.warning(f"Cluster not accessible: {e}")
            return False

    def get_pods(
        self, namespace: str, label_selector: Optional[str] = None
    ) -> List[Dict]:
        """Get pods in a namespace.

        Args:
            namespace: Namespace
            label_selector: Label selector (e.g., "app=kruize")

        Returns:
            List of pod dictionaries
        """
        args = ["get", "pods", "-o", "json"]
        if label_selector:
            args.extend(["-l", label_selector])

        result = self.run(args, namespace=namespace)
        data = json.loads(result.stdout)
        return data.get("items", [])

    def get_deployment(self, name: str, namespace: str) -> Optional[Dict]:
        """Get a deployment by name.

        Args:
            name: Deployment name
            namespace: Namespace

        Returns:
            Deployment dict or None if not found
        """
        result = self.run(
            ["get", "deployment", name, "-o", "json"],
            namespace=namespace,
            check=False,
        )
        if result.returncode != 0:
            return None
        return json.loads(result.stdout)

    def apply_file(self, file_path: str, namespace: Optional[str] = None) -> None:
        """Apply a YAML/JSON file.

        Args:
            file_path: Path to manifest file
            namespace: Namespace (optional)
        """
        logger.info(f"Applying {file_path}")
        self.run(["apply", "-f", file_path], namespace=namespace)

    def apply_kustomize(self, overlay_path: str) -> None:
        """Apply kustomize overlay.

        Args:
            overlay_path: Path to kustomize overlay directory
        """
        logger.info(f"Applying kustomize overlay {overlay_path}")
        run_command([self.kubectl_cmd, "apply", "-k", overlay_path])

    def delete_kustomize(self, overlay_path: str) -> None:
        """Delete resources from kustomize overlay.

        Args:
            overlay_path: Path to kustomize overlay directory
        """
        logger.info(f"Deleting kustomize overlay {overlay_path}")
        run_command(
            [self.kubectl_cmd, "delete", "-k", overlay_path, "--ignore-not-found=true"],
            check=False,
        )

    def create_namespace(self, namespace: str) -> None:
        """Create a namespace if it doesn't exist.

        Args:
            namespace: Namespace name
        """
        result = self.run(["get", "namespace", namespace], check=False)
        if result.returncode == 0:
            logger.info(f"Namespace {namespace} already exists")
        else:
            logger.info(f"Creating namespace {namespace}")
            self.run(["create", "namespace", namespace])

    def delete_namespace(self, namespace: str) -> None:
        """Delete a namespace.

        Args:
            namespace: Namespace name
        """
        logger.info(f"Deleting namespace {namespace}")
        self.run(["delete", "namespace", namespace, "--ignore-not-found"], check=False)

    def label_resource(
        self,
        resource_type: str,
        name: str,
        labels: Dict[str, str],
        namespace: str,
        overwrite: bool = True,
    ) -> None:
        """Label a resource.

        Args:
            resource_type: Resource type (deployment, pod, etc.)
            name: Resource name
            labels: Labels as dict
            namespace: Namespace
            overwrite: Overwrite existing labels
        """
        label_str = " ".join([f"{k}={v}" for k, v in labels.items()])
        args = ["label", resource_type, name, label_str]
        if overwrite:
            args.append("--overwrite")

        logger.info(f"Labeling {resource_type}/{name} with {label_str}")
        self.run(args, namespace=namespace)

    def wait_for_condition(
        self,
        resource: str,
        condition: str,
        namespace: str,
        timeout: int = 300,
    ) -> None:
        """Wait for a resource condition.

        Args:
            resource: Resource (e.g., "pod", "deployment kruize")
            condition: Condition (e.g., "Ready")
            namespace: Namespace
            timeout: Timeout in seconds
        """
        logger.info(f"Waiting for {resource} condition={condition} in {namespace}")
        # Split resource into parts for proper command syntax
        resource_parts = resource.split()
        cmd = ["wait", f"--for=condition={condition}"] + resource_parts + [f"--timeout={timeout}s"]
        self.run(cmd, namespace=namespace)

    def delete_resource(
        self,
        resource_type: str,
        name: str,
        namespace: str,
        ignore_not_found: bool = True,
    ) -> None:
        """Delete a resource.

        Args:
            resource_type: Resource type
            name: Resource name
            namespace: Namespace
            ignore_not_found: Don't fail if resource doesn't exist
        """
        args = ["delete", resource_type, name]
        if ignore_not_found:
            args.append("--ignore-not-found")

        logger.info(f"Deleting {resource_type}/{name}")
        self.run(args, namespace=namespace, check=False)
