"""
Cluster utility functions for E2E tests
"""
import subprocess
import time
import logging
from typing import Optional, Dict, List

logger = logging.getLogger(__name__)


class ClusterManager:
    """Manages Kubernetes cluster operations for E2E tests"""
    
    def __init__(self, cluster_type: str, cluster_name: str, namespace: str):
        self.cluster_type = cluster_type
        self.cluster_name = cluster_name
        self.namespace = namespace
        self.kubectl_cmd = "oc" if cluster_type == "openshift" else "kubectl"
        
    def run_command(self, cmd: List[str], check: bool = True, capture_output: bool = True) -> subprocess.CompletedProcess:
        """Run a shell command"""
        logger.debug(f"Running command: {' '.join(cmd)}")
        result = subprocess.run(cmd, capture_output=capture_output, text=True, check=check)
        if result.returncode != 0 and check:
            logger.error(f"Command failed: {result.stderr}")
        return result
    
    def create_kind_cluster(self, config_file: str) -> bool:
        """Create a Kind cluster"""
        try:
            logger.info(f"Creating Kind cluster: {self.cluster_name}")
            self.run_command(["kind", "create", "cluster", "--name", self.cluster_name, "--config", config_file])
            logger.info("Kind cluster created successfully")
            return True
        except subprocess.CalledProcessError as e:
            logger.error(f"Failed to create Kind cluster: {e}")
            return False
    
    def delete_kind_cluster(self) -> bool:
        """Delete a Kind cluster"""
        try:
            logger.info(f"Deleting Kind cluster: {self.cluster_name}")
            self.run_command(["kind", "delete", "cluster", "--name", self.cluster_name])
            logger.info("Kind cluster deleted successfully")
            return True
        except subprocess.CalledProcessError as e:
            logger.error(f"Failed to delete Kind cluster: {e}")
            return False
    
    def create_namespace(self, namespace: Optional[str] = None) -> bool:
        """Create a namespace"""
        ns = namespace or self.namespace
        try:
            logger.info(f"Creating namespace: {ns}")
            self.run_command([self.kubectl_cmd, "create", "namespace", ns])
            return True
        except subprocess.CalledProcessError:
            logger.warning(f"Namespace {ns} may already exist")
            return True
    
    def delete_namespace(self, namespace: Optional[str] = None) -> bool:
        """Delete a namespace"""
        ns = namespace or self.namespace
        try:
            logger.info(f"Deleting namespace: {ns}")
            self.run_command([self.kubectl_cmd, "delete", "namespace", ns, "--ignore-not-found=true"])
            return True
        except subprocess.CalledProcessError as e:
            logger.error(f"Failed to delete namespace: {e}")
            return False
    
    def wait_for_pod_ready(self, pod_label: str, namespace: Optional[str] = None, timeout: int = 300) -> bool:
        """Wait for pod to be ready"""
        ns = namespace or self.namespace
        logger.info(f"Waiting for pod with label {pod_label} in namespace {ns} to be ready (timeout: {timeout}s)")
        
        try:
            cmd = [
                self.kubectl_cmd, "wait", "--for=condition=Ready",
                f"pod", "-l", pod_label,
                "-n", ns,
                f"--timeout={timeout}s"
            ]
            self.run_command(cmd)
            logger.info(f"Pod with label {pod_label} is ready")
            return True
        except subprocess.CalledProcessError as e:
            logger.error(f"Pod did not become ready within {timeout}s: {e}")
            return False
    
    def get_pod_name(self, pod_label: str, namespace: Optional[str] = None) -> Optional[str]:
        """Get pod name by label"""
        ns = namespace or self.namespace
        try:
            result = self.run_command([
                self.kubectl_cmd, "get", "pod",
                "-l", pod_label,
                "-n", ns,
                "-o", "jsonpath={.items[0].metadata.name}"
            ])
            pod_name = result.stdout.strip()
            return pod_name if pod_name else None
        except subprocess.CalledProcessError:
            return None
    
    def get_pod_logs(self, pod_name: str, namespace: Optional[str] = None, tail: int = 100) -> str:
        """Get pod logs"""
        ns = namespace or self.namespace
        try:
            result = self.run_command([
                self.kubectl_cmd, "logs",
                pod_name,
                "-n", ns,
                f"--tail={tail}"
            ])
            return result.stdout
        except subprocess.CalledProcessError as e:
            logger.error(f"Failed to get logs for pod {pod_name}: {e}")
            return ""
    
    def get_all_pod_logs(self, pod_name: str, namespace: Optional[str] = None) -> str:
        """Get all pod logs"""
        ns = namespace or self.namespace
        try:
            result = self.run_command([
                self.kubectl_cmd, "logs",
                pod_name,
                "-n", ns
            ])
            return result.stdout
        except subprocess.CalledProcessError as e:
            logger.error(f"Failed to get logs for pod {pod_name}: {e}")
            return ""
    
    def apply_manifest(self, manifest_file: str) -> bool:
        """Apply a Kubernetes manifest"""
        try:
            logger.info(f"Applying manifest: {manifest_file}")
            self.run_command([self.kubectl_cmd, "apply", "-f", manifest_file])
            return True
        except subprocess.CalledProcessError as e:
            logger.error(f"Failed to apply manifest: {e}")
            return False
    
    def apply_kustomize(self, kustomize_dir: str) -> bool:
        """Apply kustomize directory"""
        try:
            logger.info(f"Applying kustomize: {kustomize_dir}")
            self.run_command([self.kubectl_cmd, "apply", "-k", kustomize_dir])
            return True
        except subprocess.CalledProcessError as e:
            logger.error(f"Failed to apply kustomize: {e}")
            return False
    
    def delete_kustomize(self, kustomize_dir: str) -> bool:
        """Delete resources from kustomize directory"""
        try:
            logger.info(f"Deleting kustomize: {kustomize_dir}")
            self.run_command([self.kubectl_cmd, "delete", "-k", kustomize_dir, "--ignore-not-found=true"])
            return True
        except subprocess.CalledProcessError as e:
            logger.error(f"Failed to delete kustomize: {e}")
            return False
    
    def port_forward(self, service_name: str, local_port: int, remote_port: int, namespace: Optional[str] = None) -> subprocess.Popen:
        """Start port forwarding (returns process handle)"""
        ns = namespace or self.namespace
        logger.info(f"Starting port-forward for {service_name}: {local_port}:{remote_port}")
        
        cmd = [
            self.kubectl_cmd, "port-forward",
            f"service/{service_name}",
            f"{local_port}:{remote_port}",
            "-n", ns
        ]
        
        process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        time.sleep(2)  # Give port-forward time to establish
        return process
    
    def get_service_url(self, service_name: str, namespace: Optional[str] = None) -> Optional[str]:
        """Get service URL (for OpenShift routes or NodePort services)"""
        ns = namespace or self.namespace
        
        if self.cluster_type == "openshift":
            try:
                result = self.run_command([
                    "oc", "get", "route", service_name,
                    "-n", ns,
                    "-o", "jsonpath={.spec.host}"
                ])
                host = result.stdout.strip()
                return f"http://{host}" if host else None
            except subprocess.CalledProcessError:
                return None
        else:
            # For Kind, we use localhost with NodePort
            return f"http://localhost:8080"
    
    def check_deployment_ready(self, deployment_name: str, namespace: Optional[str] = None, timeout: int = 300) -> bool:
        """Check if deployment is ready"""
        ns = namespace or self.namespace
        logger.info(f"Checking if deployment {deployment_name} is ready")
        
        try:
            cmd = [
                self.kubectl_cmd, "wait", "--for=condition=Available",
                f"deployment/{deployment_name}",
                "-n", ns,
                f"--timeout={timeout}s"
            ]
            self.run_command(cmd)
            logger.info(f"Deployment {deployment_name} is ready")
            return True
        except subprocess.CalledProcessError as e:
            logger.error(f"Deployment did not become ready within {timeout}s: {e}")
            return False

