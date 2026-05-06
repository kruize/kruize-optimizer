#!/usr/bin/env python3
"""
E2E Test Runner for Kruize Optimizer

Self-contained test runner that:
1. Clones required repos under `tests/e2e`
2. Creates Kind/OpenShift cluster
3. Deploys Prometheus
4. Deploys via operator or manifest mode
5. Deploys benchmarks (sysbench)
6. Runs E2E tests
7. Cleans up resources

Usage:
    python run_e2e_tests.py --cluster-type kind --mode operator
    python run_e2e_tests.py --cluster-type openshift --mode manifest
"""
import argparse
import logging
import sys
import time
from pathlib import Path

import pytest
import yaml

from utils.deployment_manager import DeploymentManager
from utils.cluster_utils import ClusterManager
from utils.kruize_utils import KruizeAPIClient, OptimizerAPIClient

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class E2ETestRunner:
    """Main E2E test runner"""
    
    def __init__(self, cluster_type: str, mode: str, config_file: Path):
        self.cluster_type = cluster_type
        self.mode = mode  # 'operator' or 'manifest'
        self.config = self.load_config(config_file)
        
        # Setup paths
        self.test_dir = Path(__file__).parent
        self.project_root = self.test_dir.parent.parent
        
        # Initialize managers
        namespace = self.config.get('cluster', {}).get('namespace', 'monitoring')
        work_dir = self.test_dir / ".repos"
        self.deployment_mgr = DeploymentManager(cluster_type, namespace, work_dir)
        self.cluster_mgr = None
        
        # Port forward processes
        self.port_forward_processes = []
        
    def load_config(self, config_file: Path) -> dict:
        """Load test configuration"""
        with open(config_file) as f:
            return yaml.safe_load(f)
    
    def setup_cluster(self):
        """Setup Kubernetes cluster"""
        logger.info(f"Setting up {self.cluster_type} cluster...")
        
        if self.cluster_type == "kind":
            cluster_name = self.config.get('cluster', {}).get('name', 'kruize-test')
            kind_config = self.test_dir / "config" / "kind-config.yaml"
            
            # Delete existing cluster if any
            self.deployment_mgr.delete_kind_cluster(cluster_name)
            
            # Create new cluster
            self.deployment_mgr.create_kind_cluster(cluster_name, kind_config)
            
        elif self.cluster_type == "openshift":
            logger.info("Using existing OpenShift cluster")
            # Assume OpenShift cluster is already running
        
        else:
            raise ValueError(f"Unsupported cluster type: {self.cluster_type}")
        
        # Initialize cluster manager
        cluster_name = self.config.get('cluster', {}).get('name', 'kruize-test')
        namespace = self.config.get('cluster', {}).get('namespace', 'monitoring')
        self.cluster_mgr = ClusterManager(self.cluster_type, cluster_name, namespace)
        
        logger.info("Cluster setup complete")
    
    def deploy_components(self):
        """Deploy all required components"""
        logger.info("Deploying components...")
        
        # Clone repositories
        self.deployment_mgr.clone_repositories()
        
        namespace = self.config.get('cluster', {}).get('namespace', 'monitoring')
        app_namespace = self.config.get('workload', {}).get('namespace', 'default')
        
        logger.info(f"Creating namespace: {namespace}")
        self.deployment_mgr.create_namespace(namespace)
        
        if app_namespace != namespace:
            logger.info(f"Creating namespace: {app_namespace}")
            self.deployment_mgr.create_namespace(app_namespace)
        
        # Deploy Prometheus first
        self.deployment_mgr.deploy_prometheus()
        
        # Wait for Prometheus to be ready
        logger.info("Waiting for Prometheus to be ready...")
        time.sleep(30)
        
        # Enable cluster monitoring immediately after Prometheus installation
        if self.cluster_type in ["kind", "minikube"]:
            self.deployment_mgr.enable_kube_state_metrics_labels()
        elif self.cluster_type == "openshift":
            self.deployment_mgr.enable_user_workload_monitoring()
        
        # Deploy benchmarks before Kruize so workloads already exist
        self.deploy_benchmarks()
        
        # Deploy kruize-operator or kruize
        if self.mode == "operator":
            self.deploy_operator_mode()
        else:
            self.deploy_manifest_mode()
        
        logger.info("All components deployed successfully")
    
    def deploy_operator_mode(self):
        """Deploy using operator"""
        logger.info("Deploying in operator mode...")
        
        operator_image = self.config.get('images', {}).get('kruize_operator')
        optimizer_image = self.config.get('images', {}).get('kruize_optimizer')
        
        self.deployment_mgr.deploy_operator(operator_image, optimizer_image)
        
        # Wait for all operator-managed pods
        namespace = self.config.get('cluster', {}).get('namespace', 'monitoring')
        
        logger.info("Waiting for kruize-db pod...")
        self.deployment_mgr.wait_for_pod_ready("app=kruize-db", namespace)
        
        logger.info("Waiting for kruize pod...")
        self.deployment_mgr.wait_for_pod_ready("app=kruize", namespace)
        
        logger.info("Waiting for kruize-optimizer pod...")
        self.deployment_mgr.wait_for_pod_ready("app=kruize-optimizer", namespace)
        
        logger.info("Waiting for kruize-ui pod...")
        self.deployment_mgr.wait_for_pod_ready("app=kruize-ui-nginx", namespace)
        
        logger.info("Operator mode deployment complete")
    
    def deploy_manifest_mode(self):
        """Deploy using manifests (without operator)"""
        logger.info("Deploying in manifest mode...")
        
        kruize_image = self.config.get('images', {}).get('kruize')
        kruize_ui_image = self.config.get('images', {}).get('kruize_ui')
        optimizer_image = self.config.get('images', {}).get('kruize_optimizer')
        
        self.deployment_mgr.deploy_kruize_manifest_mode(
            kruize_image,
            kruize_ui_image,
            optimizer_image
        )
        
        namespace = self.config.get('cluster', {}).get('namespace', 'monitoring')
        
        logger.info("Waiting for kruize pod...")
        self.deployment_mgr.wait_for_pod_ready("app=kruize", namespace)
        
        logger.info("Waiting for kruize-db pod...")
        self.deployment_mgr.wait_for_pod_ready("app=kruize-db", namespace)
        
        logger.info("Waiting for kruize-optimizer pod...")
        self.deployment_mgr.wait_for_pod_ready("app=kruize-optimizer", namespace)
        
        logger.info("Manifest mode deployment complete")
    
    def deploy_benchmarks(self):
        """Deploy benchmark workloads"""
        logger.info("Deploying benchmarks...")
        
        app_namespace = self.config.get('workload', {}).get('namespace', 'default')
        
        logger.info("Deploying sysbench...")
        self.deployment_mgr.deploy_benchmarks("sysbench", app_namespace)
        self.deployment_mgr.label_workloads(
            ["sysbench"],
            "kruize/autotune=enabled",
            app_namespace
        )
        
        logger.info("Benchmarks deployed successfully")
    
    def setup_port_forwards(self):
        """Setup port forwarding for services"""
        if self.cluster_type != "kind":
            logger.info("Port forwarding not needed for non-Kind clusters")
            return
        
        logger.info("Setting up port forwards...")
        
        namespace = self.config.get('cluster', {}).get('namespace', 'monitoring')
        
        # Port forward kruize service
        kruize_port = self.config.get('kruize_port', 8080)
        process = self.deployment_mgr.setup_port_forward(
            "kruize", kruize_port, 8080, namespace
        )
        self.port_forward_processes.append(process)
        
        # Port forward optimizer service
        optimizer_port = self.config.get('optimizer_port', 8081)
        process = self.deployment_mgr.setup_port_forward(
            "kruize-optimizer", optimizer_port, 8080, namespace
        )
        self.port_forward_processes.append(process)
        
        logger.info("Port forwards established")
    
    def run_tests(self):
        """Run pytest tests"""
        logger.info("Running E2E tests...")
        
        # Set environment variables for tests
        import os
        os.environ['CLUSTER_TYPE'] = self.cluster_type
        os.environ['DEPLOYMENT_MODE'] = self.mode
        os.environ['KRUIZE_URL'] = f"localhost:{self.config.get('kruize_port', 8080)}"
        os.environ['OPTIMIZER_URL'] = f"localhost:{self.config.get('optimizer_port', 8081)}"
        
        # Run pytest
        test_dir = self.test_dir / "tests"
        pytest_args = [
            str(test_dir),
            "-v",
            "--tb=short",
            f"--html=test-report-{self.cluster_type}-{self.mode}.html",
            "--self-contained-html"
        ]
        
        result = pytest.main(pytest_args)
        
        return result
    
    def cleanup(self):
        """Cleanup resources"""
        logger.info("Cleaning up resources...")
        
        # Kill port forward processes
        for process in self.port_forward_processes:
            try:
                process.terminate()
                process.wait(timeout=5)
            except Exception as e:
                logger.warning(f"Error terminating port-forward: {e}")
        
        # Delete cluster if Kind
        if self.cluster_type == "kind":
            cluster_name = self.config.get('kind_cluster_name', 'kruize-test')
            self.deployment_mgr.delete_kind_cluster(cluster_name)
        
        # Cleanup work directory
        self.deployment_mgr.cleanup()
        
        logger.info("Cleanup complete")
    
    def run(self):
        """Main execution flow"""
        try:
            logger.info("=" * 60)
            logger.info("Starting Kruize Optimizer E2E Tests")
            logger.info(f"Cluster Type: {self.cluster_type}")
            logger.info(f"Deployment Mode: {self.mode}")
            logger.info("=" * 60)
            
            # Setup cluster
            self.setup_cluster()
            
            # Deploy components
            self.deploy_components()
            
            # Setup port forwards
            self.setup_port_forwards()
            
            # Wait for optimizer to create experiments
            wait_time = self.config.get('optimizer_wait_duration', 120)
            logger.info(f"Waiting {wait_time}s for optimizer to create experiments...")
            time.sleep(wait_time)
            
            # Run tests
            result = self.run_tests()
            
            logger.info("=" * 60)
            if result == 0:
                logger.info("E2E Tests PASSED")
            else:
                logger.error("E2E Tests FAILED")
            logger.info("=" * 60)
            
            return result
            
        except Exception as e:
            logger.error(f"E2E test execution failed: {e}", exc_info=True)
            return 1
        
        finally:
            # Always cleanup
            if not self.config.get('skip_cleanup', False):
                self.cleanup()


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(
        description="Run Kruize Optimizer E2E Tests"
    )
    
    parser.add_argument(
        '--cluster-type',
        choices=['kind', 'openshift', 'minikube'],
        default='kind',
        help='Kubernetes cluster type'
    )
    
    parser.add_argument(
        '--mode',
        choices=['operator', 'manifest'],
        default='operator',
        help='Deployment mode'
    )
    
    parser.add_argument(
        '--config',
        type=Path,
        default=Path(__file__).parent / "config" / "test_config.yaml",
        help='Test configuration file'
    )
    
    parser.add_argument(
        '--skip-cleanup',
        action='store_true',
        help='Skip cleanup after tests'
    )
    
    args = parser.parse_args()
    
    # Load config and update with CLI args
    runner = E2ETestRunner(args.cluster_type, args.mode, args.config)
    
    if args.skip_cleanup:
        runner.config['skip_cleanup'] = True
    
    # Run tests
    result = runner.run()
    
    sys.exit(result)


if __name__ == "__main__":
    main()

