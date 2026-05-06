"""
Deployment Manager for E2E Tests

Handles complete deployment workflow without external script dependencies:
- Clone required repos (autotune, selected benchmark manifests)
- Create Kind/OpenShift cluster
- Deploy Prometheus
- Deploy in manifest mode or via operator
- Deploy benchmarks (sysbench)
- Wait for all components to be ready
"""
import subprocess
import logging
import time
import os
import shutil
from pathlib import Path
from typing import Optional, Dict, List

logger = logging.getLogger(__name__)


class DeploymentManager:
    """Manages complete E2E test deployment"""
    
    def __init__(self, cluster_type: str, namespace: str, work_dir: Optional[Path] = None):
        self.cluster_type = cluster_type
        self.namespace = namespace
        self.work_dir = work_dir or Path(__file__).resolve().parent.parent / ".repos"
        self.kubectl_cmd = "oc" if cluster_type == "openshift" else "kubectl"
        
        # Repository URLs
        self.autotune_repo = "https://github.com/kruize/autotune.git"
        self.benchmarks_repo = "https://github.com/kruize/benchmarks.git"
        
        # Paths
        self.autotune_dir = self.work_dir / "autotune"
        self.benchmarks_dir = self.work_dir / "benchmarks"
        self.prometheus_script = None
        self.benchmark_manifest_paths = {
            "sysbench": [
                Path("sysbench/manifests/sysbench.yaml"),
            ],
        }
        self.benchmark_deployments = {
            "sysbench": ["sysbench"],
        }
        
    def run_command(self, cmd, check=True, capture_output=True, cwd=None, env=None):
        """Run shell command"""
        logger.debug(f"Running: {' '.join(cmd) if isinstance(cmd, list) else cmd}")
        
        # Use current environment and add any custom env vars
        run_env = os.environ.copy()
        if env:
            run_env.update(env)
        
        if isinstance(cmd, str):
            result = subprocess.run(cmd, shell=True, capture_output=capture_output,
                                  text=True, check=check, cwd=cwd, env=run_env)
        else:
            result = subprocess.run(cmd, capture_output=capture_output, text=True,
                                  check=check, cwd=cwd, env=run_env)
        
        if result.returncode != 0 and check:
            logger.error(f"Command failed: {result.stderr}")
        
        return result
    
    def clone_repositories(self):
        """Clone required repositories"""
        logger.info("Cloning required repositories...")
        self.work_dir.mkdir(parents=True, exist_ok=True)
        
        # Clone autotune (for Prometheus scripts)
        if not self.autotune_dir.exists():
            logger.info(f"Cloning autotune to {self.autotune_dir}")
            self.run_command([
                "git", "clone", "--depth", "1",
                self.autotune_repo,
                str(self.autotune_dir)
            ])
        
        # Clone benchmarks repo metadata only, then sparse checkout the required manifests
        if not self.benchmarks_dir.exists():
            logger.info(f"Cloning selected benchmark manifests to {self.benchmarks_dir}")
            self.run_command([
                "git", "clone", "--depth", "1", "--filter=blob:none", "--sparse",
                self.benchmarks_repo,
                str(self.benchmarks_dir)
            ])
            sparse_paths = sorted({
                str(path.parent) for paths in self.benchmark_manifest_paths.values() for path in paths
            })
            self.run_command(
                ["git", "sparse-checkout", "set", *sparse_paths],
                cwd=self.benchmarks_dir
            )
        
        # Set Prometheus script path
        if self.cluster_type == "kind":
            self.prometheus_script = self.autotune_dir / "scripts" / "prometheus_on_kind.sh"
        elif self.cluster_type == "minikube":
            self.prometheus_script = self.autotune_dir / "scripts" / "prometheus_on_minikube.sh"
        elif self.cluster_type == "openshift":
            self.prometheus_script = self.autotune_dir / "scripts" / "prometheus_on_openshift.sh"
        
        logger.info("Repositories cloned successfully")
    
    def create_kind_cluster(self, cluster_name: str, config_file: Optional[Path] = None):
        """Create Kind cluster"""
        logger.info(f"Creating Kind cluster: {cluster_name}")
        
        cmd = ["kind", "create", "cluster", "--name", cluster_name]
        
        if config_file and config_file.exists():
            cmd.extend(["--config", str(config_file)])
        
        self.run_command(cmd)
        logger.info("Kind cluster created successfully")
    
    def delete_kind_cluster(self, cluster_name: str):
        """Delete Kind cluster"""
        logger.info(f"Deleting Kind cluster: {cluster_name}")
        self.run_command(["kind", "delete", "cluster", "--name", cluster_name], check=False)
    
    def create_namespace(self, namespace: Optional[str] = None):
        """Create namespace"""
        ns = namespace or self.namespace
        logger.info(f"Creating namespace: {ns}")
        
        self.run_command([
            self.kubectl_cmd, "create", "namespace", ns
        ], check=False)  # Don't fail if already exists
    
    def deploy_kruize_manifest_mode(self, kruize_image: Optional[str] = None,
                                     kruize_ui_image: Optional[str] = None,
                                     optimizer_image: Optional[str] = None):
        """Deploy kruize and optimizer in manifest mode"""
        logger.info("Deploying kruize in manifest mode...")
        
        deploy_script = self.autotune_dir / "deploy.sh"
        
        if not deploy_script.exists():
            raise FileNotFoundError(f"deploy.sh script not found: {deploy_script}")
        
        deploy_script.chmod(0o755)
        
        cluster_type_arg = self.cluster_type
        if self.cluster_type == "kind":
            cluster_type_arg = "kind"
        elif self.cluster_type == "minikube":
            cluster_type_arg = "minikube"
        elif self.cluster_type == "openshift":
            cluster_type_arg = "openshift"
        
        cmd = f"bash {deploy_script} -c {cluster_type_arg} -m crc"
        
        if kruize_image:
            cmd += f" -i {kruize_image}"
        if kruize_ui_image:
            cmd += f" -u {kruize_ui_image}"
        
        logger.info("Running kruize manifest deployment")
        result = self.run_command(
            cmd,
            check=False,
            capture_output=True,
            cwd=self.autotune_dir
        )
        
        if result.returncode != 0:
            logger.error(f"Kruize deployment failed with exit code {result.returncode}")
            logger.error(f"STDOUT: {result.stdout}")
            logger.error(f"STDERR: {result.stderr}")
            raise RuntimeError(f"Kruize deployment failed: {result.stderr}")
        
        logger.info("Deploying optimizer manifest from project kustomize files")
        self.deploy_optimizer_manifest(optimizer_image)
        logger.info("Kruize and optimizer deployed successfully in manifest mode")
    
    def deploy_optimizer_manifest(self, optimizer_image: Optional[str] = None):
        """Deploy kruize-optimizer using this project's kustomize files"""
        project_root = Path(__file__).parent.parent.parent.parent
        
        if self.cluster_type == "openshift":
            overlay_dir = project_root / "deployment" / "overlays" / "openshift"
        else:
            overlay_dir = project_root / "deployment" / "overlays" / "kind"
        
        if not overlay_dir.exists():
            raise FileNotFoundError(f"Overlay directory not found: {overlay_dir}")
        
        if optimizer_image:
            logger.info(f"Requested optimizer image override: {optimizer_image}")
        
        self.run_command([
            self.kubectl_cmd, "apply", "-k", str(overlay_dir)
        ])
    
    def deploy_prometheus(self):
        """Deploy Prometheus using autotune scripts"""
        logger.info("Deploying Prometheus...")
        
        if not self.prometheus_script or not self.prometheus_script.exists():
            raise FileNotFoundError(f"Prometheus script not found: {self.prometheus_script}")
        
        # Make script executable
        self.prometheus_script.chmod(0o755)
        
        # Run Prometheus deployment script with -as flags
        # -a = non-interactive mode, -s = start
        logger.info(f"Running Prometheus script: {self.prometheus_script} -as")
        result = self.run_command(
            f"bash {self.prometheus_script} -as",
            check=False,
            capture_output=True,
            cwd=self.prometheus_script.parent
        )
        
        if result.returncode != 0:
            logger.error(f"Prometheus deployment failed with exit code {result.returncode}")
            logger.error(f"STDOUT: {result.stdout}")
            logger.error(f"STDERR: {result.stderr}")
            raise RuntimeError(f"Prometheus deployment failed: {result.stderr}")
        
        logger.info("Prometheus deployed successfully")
    
    def deploy_operator(self, operator_image: Optional[str] = None, 
                       optimizer_image: Optional[str] = None):
        """Deploy kruize-operator using kustomize"""
        logger.info("Deploying kruize-operator...")
        
        # Get project root (3 levels up from this file)
        project_root = Path(__file__).parent.parent.parent.parent
        
        # Determine overlay based on cluster type
        if self.cluster_type == "openshift":
            overlay_dir = project_root / "deployment" / "overlays" / "openshift"
        else:
            overlay_dir = project_root / "deployment" / "overlays" / "kind"
        
        if not overlay_dir.exists():
            raise FileNotFoundError(f"Overlay directory not found: {overlay_dir}")
        
        # Apply kustomize
        logger.info(f"Applying kustomize from: {overlay_dir}")
        self.run_command([
            self.kubectl_cmd, "apply", "-k", str(overlay_dir)
        ])
        
        # Wait for operator to be ready
        logger.info("Waiting for operator to be ready...")
        self.run_command([
            self.kubectl_cmd, "wait", "--for=condition=Available",
            "deployment/kruize-operator",
            "-n", self.namespace,
            "--timeout=300s"
        ])
        
        logger.info("Kruize operator deployed successfully")
    
    def deploy_benchmarks(self, benchmark_name: str, app_namespace: str):
        """Deploy benchmarks (sysbench)"""
        logger.info(f"Deploying benchmark: {benchmark_name}")
        
        manifest_paths = self.benchmark_manifest_paths.get(benchmark_name, [])
        if not manifest_paths:
            logger.warning(f"No benchmark manifests configured for: {benchmark_name}")
            return
        
        resolved_manifests = [self.benchmarks_dir / manifest_path for manifest_path in manifest_paths]
        missing_manifests = [str(manifest) for manifest in resolved_manifests if not manifest.exists()]
        if missing_manifests:
            logger.warning(f"Benchmark manifests not found for {benchmark_name}: {missing_manifests}")
            return
        
        for manifest_file in resolved_manifests:
            logger.info(f"Applying: {manifest_file}")
            self.run_command([
                self.kubectl_cmd, "apply", "-f", str(manifest_file),
                "-n", app_namespace
            ], check=False)
        
        self.wait_for_benchmark_deployments(benchmark_name, app_namespace)
        logger.info(f"Benchmark {benchmark_name} deployed successfully")
    
    def wait_for_benchmark_deployments(self, benchmark_name: str, namespace: str, timeout: int = 300):
        """Wait for benchmark deployments to become available"""
        deployment_names = self.benchmark_deployments.get(benchmark_name, [])
        if not deployment_names:
            logger.info(f"No deployment readiness checks configured for benchmark: {benchmark_name}")
            return
        
        for deployment_name in deployment_names:
            logger.info(
                f"Waiting for benchmark deployment {deployment_name} in namespace {namespace}"
            )
            result = self.run_command([
                self.kubectl_cmd, "wait", "--for=condition=Available",
                f"deployment/{deployment_name}",
                "-n", namespace,
                f"--timeout={timeout}s"
            ], check=False, capture_output=True)
            
            if result.returncode != 0:
                logger.warning(
                    f"Benchmark deployment {deployment_name} was not ready: {result.stderr}"
                )
    
    def wait_for_pod_ready(self, label: str, namespace: Optional[str] = None, timeout: int = 300):
        """Wait for pod to be ready"""
        ns = namespace or self.namespace
        logger.info(f"Waiting for pod with label {label} in namespace {ns}")
        
        self.run_command([
            self.kubectl_cmd, "wait", "--for=condition=Ready",
            "pod", "-l", label,
            "-n", ns,
            f"--timeout={timeout}s"
        ])
    
    def enable_kube_state_metrics_labels(self):
        """Enable kube state metrics labels for Kind/Minikube"""
        if self.cluster_type in ["kind", "minikube"]:
            logger.info("Enabling kube state metrics labels...")
            
            script_path = self.autotune_dir / "scripts" / "enable_kube_state_metrics_labels.sh"
            
            if script_path.exists():
                script_path.chmod(0o755)
                self.run_command(f"bash {script_path}", cwd=script_path.parent, check=False)
    
    def enable_user_workload_monitoring(self):
        """Enable user workload monitoring for OpenShift"""
        if self.cluster_type == "openshift":
            logger.info("Enabling user workload monitoring...")
            
            script_path = self.autotune_dir / "scripts" / "enable_user_workload_monitoring_openshift.sh"
            
            if script_path.exists():
                script_path.chmod(0o755)
                self.run_command(f"bash {script_path}", cwd=script_path.parent, check=False)
    
    def label_workloads(self, deployment_names: List[str], label: str, namespace: str):
        """Add the same label to multiple deployments"""
        for deployment_name in deployment_names:
            self.label_workload(deployment_name, label, namespace)
    
    def label_workload(self, deployment_name: str, label: str, namespace: str):
        """Add label to deployment"""
        logger.info(f"Labeling deployment {deployment_name} with {label}")
        
        # Label the deployment (deployment should exist after wait in deploy_benchmarks)
        result = self.run_command([
            self.kubectl_cmd, "label", "deployment", deployment_name,
            label, "--overwrite",
            "-n", namespace
        ], check=False, capture_output=True)
        
        if result.returncode != 0:
            logger.warning(f"Failed to label deployment {deployment_name}: {result.stderr}")
            logger.warning("Deployment may not exist yet or may not be a deployment resource")
    
    def setup_port_forward(self, service_name: str, local_port: int, 
                          remote_port: int, namespace: Optional[str] = None):
        """Setup port forwarding (returns process)"""
        ns = namespace or self.namespace
        logger.info(f"Setting up port-forward for {service_name}: {local_port}:{remote_port}")
        
        cmd = [
            self.kubectl_cmd, "port-forward",
            f"service/{service_name}",
            f"{local_port}:{remote_port}",
            "-n", ns
        ]
        
        process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        time.sleep(3)  # Give port-forward time to establish
        
        return process
    
    def cleanup(self):
        """Cleanup work directory"""
        if self.work_dir.exists():
            logger.info(f"Cleaning up work directory: {self.work_dir}")
            shutil.rmtree(self.work_dir, ignore_errors=True)

# Made with Bob
