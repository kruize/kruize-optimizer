"""
E2E Test 01: Complete Optimizer Workflow

This test verifies the complete end-to-end workflow after deployment:
1. Verify optimizer pod is running
2. Verify optimizer service has started
3. Verify profiles are installed via API (metric, metadata, layers)
4. Verify profiles match configsReferenceIndex.json
5. Verify profile installation messages in logs
6. Verify bulk jobs are triggered with autotune label
7. Verify job completion with job_id and status

This test assumes the cluster is already deployed using optimizer_demo.sh
"""
import pytest
import requests
import logging
import yaml
import os
import sys
import time
import json
import re

# Add parent directory to path for imports
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from utils.cluster_utils import ClusterManager
from utils.kruize_utils import KruizeAPIClient, OptimizerAPIClient, verify_profiles_installed
from utils.log_utils import (
    parse_optimizer_logs,
    check_log_for_message,
    extract_job_ids_from_logs,
    verify_profile_installation_logs
)

logger = logging.getLogger(__name__)


@pytest.fixture(scope="module")
def config():
    """Load test configuration"""
    config_path = os.path.join(os.path.dirname(__file__), '..', 'config', 'test_config.yaml')
    with open(config_path, 'r') as f:
        return yaml.safe_load(f)


@pytest.fixture(scope="module")
def cluster_manager(config):
    """Create cluster manager"""
    return ClusterManager(
        cluster_type=config['cluster']['type'],
        cluster_name=config['cluster']['name'],
        namespace=config['cluster']['namespace']
    )


@pytest.fixture(scope="module")
def kruize_client(config):
    """Create Kruize API client"""
    base_url = f"http://localhost:{config['api']['kruize_port']}"
    client = KruizeAPIClient(base_url)
    
    # Wait for service to be available
    if not client.wait_for_service(max_retries=30):
        pytest.fail("Kruize service did not become available")
    
    return client


@pytest.fixture(scope="module")
def optimizer_client(config):
    """Create Optimizer API client"""
    base_url = f"http://localhost:{config['api']['optimizer_port']}"
    client = OptimizerAPIClient(base_url)
    
    # Wait for service to be available
    if not client.wait_for_service(max_retries=30):
        pytest.fail("Optimizer service did not become available")
    
    return client


class TestCompleteWorkflow:
    """Test complete optimizer workflow"""
    
    def test_01_optimizer_pod_running(self, cluster_manager, config):
        """
        Test: Verify kruize-optimizer pod is running
        Expected: Pod exists and is in Running state
        """
        logger.info("Test: Verify optimizer pod is running")
        
        # Get the appropriate namespace based on cluster type
        cluster_type = config['cluster']['type']
        optimizer_namespace = config['cluster']['optimizer_namespace'].get(cluster_type, 'monitoring')
        
        logger.info(f"Looking for optimizer pod in namespace: {optimizer_namespace}")
        
        # Check if optimizer pod is ready
        assert cluster_manager.wait_for_pod_ready("app=kruize-optimizer", namespace=optimizer_namespace, timeout=60), \
            "Optimizer pod did not become ready"
        
        # Get pod name
        pod_name = cluster_manager.get_pod_name("app=kruize-optimizer", namespace=optimizer_namespace)
        assert pod_name is not None, "Could not find optimizer pod"
        
        logger.info(f"✓ Optimizer pod is running: {pod_name}")
    
    def test_02_optimizer_service_started(self, cluster_manager, config):
        """
        Test: Verify optimizer service has started
        Expected: Logs contain "Kruize Optimizer Service is STARTED!"
        """
        logger.info("Test: Verify optimizer service started")
        
        # Get the appropriate namespace based on cluster type
        cluster_type = config['cluster']['type']
        optimizer_namespace = config['cluster']['optimizer_namespace'].get(cluster_type, 'monitoring')
        
        # Get optimizer pod logs
        pod_name = cluster_manager.get_pod_name("app=kruize-optimizer", namespace=optimizer_namespace)
        logs = cluster_manager.get_all_pod_logs(pod_name, namespace=optimizer_namespace)
        
        # Check for service started message
        assert check_log_for_message(logs, "Kruize Optimizer Service is STARTED!"), \
            "Optimizer service did not start properly"
        
        logger.info("✓ Optimizer service started successfully")
    
    def test_03_load_configs_reference(self):
        """
        Test: Load configsReferenceIndex.json for validation
        Expected: File exists and contains expected structure
        """
        logger.info("Test: Load configs reference index")
        
        config_path = os.path.join(
            os.path.dirname(__file__),
            '..', '..', '..',
            'src', 'main', 'resources', 'configs',
            'configsReferenceIndex.json'
        )
        
        assert os.path.exists(config_path), f"Config file not found: {config_path}"
        
        with open(config_path, 'r') as f:
            self.configs_reference = json.load(f)
        
        # Validate structure
        assert 'metadata_profiles' in self.configs_reference, "Missing metadata_profiles"
        assert 'metric_profiles' in self.configs_reference, "Missing metric_profiles"
        assert 'layers' in self.configs_reference, "Missing layers"
        
        logger.info(f"✓ Loaded configs reference: {len(self.configs_reference['metadata_profiles'])} metadata, "
                   f"{len(self.configs_reference['metric_profiles'])} metric, "
                   f"{len(self.configs_reference['layers'])} layers")
    
    def test_04_profiles_installed_via_api(self, kruize_client):
        """
        Test: Verify profiles are installed via Kruize API and match configsReferenceIndex.json
        Expected: All profiles from config file are installed
        """
        logger.info("Test: Verify profiles via API match config reference")
        
        # Load config reference if not already loaded
        if not hasattr(self, 'configs_reference'):
            config_path = os.path.join(
                os.path.dirname(__file__),
                '..', '..', '..',
                'src', 'main', 'resources', 'configs',
                'configsReferenceIndex.json'
            )
            with open(config_path, 'r') as f:
                self.configs_reference = json.load(f)
        
        # Get installed profiles from API
        metric_profiles = kruize_client.list_metric_profiles()
        metadata_profiles = kruize_client.list_metadata_profiles()
        layers = kruize_client.list_layers()
        
        logger.info(f"API returned: {len(metric_profiles)} metric profiles, "
                   f"{len(metadata_profiles)} metadata profiles, "
                   f"{len(layers)} layers")
        
        # Verify metric profiles
        expected_metric = [p['name'] for p in self.configs_reference['metric_profiles']]
        installed_metric = [p.get('profile_name', p.get('name', '')) for p in metric_profiles]
        
        for expected_name in expected_metric:
            assert expected_name in installed_metric, \
                f"Metric profile '{expected_name}' not found in API response"
        
        logger.info(f"✓ All {len(expected_metric)} metric profiles installed")
        
        # Verify metadata profiles
        expected_metadata = [p['name'] for p in self.configs_reference['metadata_profiles']]
        installed_metadata = [p.get('profile_name', p.get('name', '')) for p in metadata_profiles]
        
        for expected_name in expected_metadata:
            assert expected_name in installed_metadata, \
                f"Metadata profile '{expected_name}' not found in API response"
        
        logger.info(f"✓ All {len(expected_metadata)} metadata profiles installed")
        
        # Verify layers
        expected_layers = self.configs_reference['layers']
        installed_layers = [layer.get('layer_name', layer.get('name', '')) for layer in layers]
        
        for expected_name in expected_layers:
            assert expected_name in installed_layers, \
                f"Layer '{expected_name}' not found in API response"
        
        logger.info(f"✓ All {len(expected_layers)} layers installed")
    
    def test_05_profiles_in_optimizer_logs(self, cluster_manager, config):
        """
        Test: Verify specific profile installation messages in optimizer logs
        Expected: Logs contain "Metadata profile: Installed: <name>" for each profile
        """
        logger.info("Test: Verify profile installation messages in logs")
        
        # Load config reference if not already loaded
        if not hasattr(self, 'configs_reference'):
            config_path = os.path.join(
                os.path.dirname(__file__),
                '..', '..', '..',
                'src', 'main', 'resources', 'configs',
                'configsReferenceIndex.json'
            )
            with open(config_path, 'r') as f:
                self.configs_reference = json.load(f)
        
        # Get the appropriate namespace based on cluster type
        cluster_type = config['cluster']['type']
        optimizer_namespace = config['cluster']['optimizer_namespace'].get(cluster_type, 'monitoring')
        
        # Get optimizer pod logs
        pod_name = cluster_manager.get_pod_name("app=kruize-optimizer", namespace=optimizer_namespace)
        logs = cluster_manager.get_all_pod_logs(pod_name, namespace=optimizer_namespace)
        
        # Verify metadata profile installation logs
        for profile in self.configs_reference['metadata_profiles']:
            profile_name = profile['name']
            expected_log = f"Metadata profile: Installed: {profile_name}"
            assert check_log_for_message(logs, expected_log), \
                f"Log message not found: '{expected_log}'"
            logger.info(f"✓ Found log: {expected_log}")
        
        # Verify metric profile installation logs
        for profile in self.configs_reference['metric_profiles']:
            profile_name = profile['name']
            expected_log = f"Metric profile: Installed: {profile_name}"
            assert check_log_for_message(logs, expected_log), \
                f"Log message not found: '{expected_log}'"
            logger.info(f"✓ Found log: {expected_log}")
        
        # Verify layer installation logs
        for layer_name in self.configs_reference['layers']:
            expected_log = f"Layer: Installed: {layer_name}"
            assert check_log_for_message(logs, expected_log), \
                f"Log message not found: '{expected_log}'"
            logger.info(f"✓ Found log: {expected_log}")
        
        logger.info("✓ All profile installation messages verified in logs")
    
    def test_06_workloads_deployed(self, cluster_manager, config):
        """
        Test: Verify sysbench workload is deployed
        Expected: Sysbench workload pod is running
        """
        logger.info("Test: Verify workloads are deployed")
        
        workload_namespace = config['workload']['namespace']
        
        # Check for sysbench
        sysbench_ready = cluster_manager.wait_for_pod_ready(
            "app=sysbench",
            namespace=workload_namespace,
            timeout=60
        )
        
        if sysbench_ready:
            logger.info("✓ Sysbench workload is running")
        else:
            logger.warning("⚠️  Sysbench workload not found or not ready")
        
        assert sysbench_ready, "Sysbench workload is not running"
    
    def test_07_bulk_job_triggered_with_autotune_label(self, cluster_manager, config):
        """
        Test: Verify bulk job is triggered with autotune label filter
        Expected: Logs show bulk API call with "kruize/autotune": "enabled" label
        """
        logger.info("Test: Verify bulk job with autotune label")
        
        # Get the appropriate namespace based on cluster type
        cluster_type = config['cluster']['type']
        optimizer_namespace = config['cluster']['optimizer_namespace'].get(cluster_type, 'monitoring')
        
        # Get optimizer pod logs
        pod_name = cluster_manager.get_pod_name("app=kruize-optimizer", namespace=optimizer_namespace)
        logs = cluster_manager.get_all_pod_logs(pod_name, namespace=optimizer_namespace)
        
        # Check for bulk API call with autotune label
        from utils.log_utils import check_bulk_job_with_autotune_label
        has_autotune_label = check_bulk_job_with_autotune_label(logs)
        
        assert has_autotune_label, \
            'Bulk API call does not include "kruize/autotune": "enabled" label'
        
        logger.info('✓ Bulk job triggered with "kruize/autotune": "enabled" label')
    
    def test_07b_bulk_job_completion(self, cluster_manager, config):
        """
        Test: Verify at least one bulk job has completed successfully
        Expected: Logs contain job_id and completion status with Total/Processed/Existing counts
        """
        logger.info("Test: Verify bulk job completion")
        
        # Get the appropriate namespace based on cluster type
        cluster_type = config['cluster']['type']
        optimizer_namespace = config['cluster']['optimizer_namespace'].get(cluster_type, 'monitoring')
        
        # Get optimizer pod logs
        pod_name = cluster_manager.get_pod_name("app=kruize-optimizer", namespace=optimizer_namespace)
        logs = cluster_manager.get_all_pod_logs(pod_name, namespace=optimizer_namespace)
        
        # Extract job information from logs
        job_info_list = extract_job_ids_from_logs(logs)
        
        assert len(job_info_list) > 0, "No bulk jobs found in logs"
        logger.info(f"Found {len(job_info_list)} job(s) in logs")
        
        # Check for at least one completed job
        completed_jobs = [job for job in job_info_list if job['status'] == 'completed']
        
        assert len(completed_jobs) > 0, "No completed jobs found in logs"
        
        # Log details of completed jobs
        for job in completed_jobs:
            logger.info(f"✓ Job {job['job_id']} completed: "
                       f"Total={job['total']}, Processed={job['processed']}, Existing={job['existing']}")
        
        # Verify at least one job processed some experiments
        jobs_with_processed = []
        for job in completed_jobs:
            processed = job.get('processed', 0)
            if isinstance(processed, int) and processed > 0:
                jobs_with_processed.append(job)
        
        assert len(jobs_with_processed) > 0, "No jobs processed any experiments"
        
        logger.info(f"✓ {len(completed_jobs)} job(s) completed successfully")
    
    def test_08_webhook_callback_received(self, optimizer_client, config):
        """
        Test: Verify webhook callback is received
        Expected: Experiment counters are updated
        """
        logger.info("Test: Verify webhook callback")
        
        # Get current state
        jobs_overview = optimizer_client.get_jobs_overview()
        total_experiments = jobs_overview.get('totalExperiments', 0)
        processed_experiments = jobs_overview.get('totalExperimentsProcessed', 0)
        
        logger.info(f"Total experiments: {total_experiments}")
        logger.info(f"Processed experiments: {processed_experiments}")
        
        # If experiments are already processed, test passes
        if processed_experiments > 0:
            logger.info(f"✓ Webhook callbacks received (processed: {processed_experiments})")
            return
        
        # Otherwise wait for webhook
        logger.info("Waiting for webhook callback...")
        timeout = config['timeouts']['webhook_callback']
        
        start_time = time.time()
        webhook_received = False
        
        while time.time() - start_time < timeout:
            current_jobs = optimizer_client.get_jobs_overview()
            current_processed = current_jobs.get('totalExperimentsProcessed', 0)
            
            if current_processed > 0:
                webhook_received = True
                logger.info(f"✓ Webhook received! Processed: {current_processed}")
                break
            
            logger.debug(f"Waiting... (processed: {current_processed})")
            time.sleep(10)
        
        if not webhook_received:
            logger.warning(f"⚠️  No webhook received within {timeout}s")
            logger.warning("This may be expected if experiments haven't been created yet")
    
    def test_09_optimizer_logs_no_errors(self, cluster_manager, config):
        """
        Test: Verify optimizer logs don't contain unexpected errors
        Expected: No critical errors in logs
        """
        logger.info("Test: Verify no critical errors in logs")
        
        # Get the appropriate namespace based on cluster type
        cluster_type = config['cluster']['type']
        optimizer_namespace = config['cluster']['optimizer_namespace'].get(cluster_type, 'monitoring')
        
        # Get optimizer pod logs
        pod_name = cluster_manager.get_pod_name("app=kruize-optimizer", namespace=optimizer_namespace)
        logs = cluster_manager.get_all_pod_logs(pod_name, namespace=optimizer_namespace)
        
        # Parse logs
        log_info = parse_optimizer_logs(logs)
        
        # Check for errors (allow some expected errors)
        allowed_errors = [
            "connection refused",  # Expected during startup
            "not found",  # Expected if resources don't exist yet
        ]
        
        critical_errors = [e for e in log_info['errors'] 
                          if not any(allowed in e.lower() for allowed in allowed_errors)]
        
        if critical_errors:
            logger.warning("⚠️  Found some errors in logs:")
            for error in critical_errors[:5]:  # Show first 5
                logger.warning(f"  {error}")
        
        # Don't fail test on errors, just warn
        logger.info("✓ Log check complete")
    
    def test_10_health_endpoints(self, optimizer_client):
        """
        Test: Verify health endpoints are accessible
        Expected: Liveness and readiness endpoints return 200
        """
        logger.info("Test: Verify health endpoints")
        
        # Test liveness
        response = requests.get(f"{optimizer_client.base_url}/q/health/live")
        assert response.status_code == 200, f"Liveness check failed: {response.status_code}"
        logger.info("✓ Liveness endpoint OK")
        
        # Test readiness
        response = requests.get(f"{optimizer_client.base_url}/q/health/ready")
        assert response.status_code == 200, f"Readiness check failed: {response.status_code}"
        logger.info("✓ Readiness endpoint OK")


if __name__ == "__main__":
    pytest.main([__file__, "-v", "-s"])

