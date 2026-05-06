
"""
E2E Test 04: Webhook Negative Test Scenarios

This test module focuses on testing the webhook endpoint with various
invalid/malformed payloads to ensure proper error handling.
"""
import pytest
import requests
import logging
import yaml
import os
import sys

# Add parent directory to path for imports
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from utils.kruize_utils import OptimizerAPIClient

logger = logging.getLogger(__name__)


@pytest.fixture(scope="module")
def config():
    """Load test configuration"""
    config_path = os.path.join(os.path.dirname(__file__), '..', 'config', 'test_config.yaml')
    with open(config_path, 'r') as f:
        return yaml.safe_load(f)


@pytest.fixture(scope="module")
def optimizer_client(config):
    """Create Optimizer API client"""
    # Use the configured optimizer port from config (default 8081)
    optimizer_port = config.get('api', {}).get('optimizer_port', 8081)
    base_url = f"http://localhost:{optimizer_port}"
    return OptimizerAPIClient(base_url)


class TestWebhookNegativeScenarios:
    """Test webhook endpoint with invalid/malformed payloads"""
    
    def test_webhook_invalid_json(self, optimizer_client):
        """
        Test: Send invalid JSON to webhook endpoint
        Expected: 400 Bad Request
        """
        logger.info("Test: Webhook with invalid JSON")
        
        response = requests.post(
            f"{optimizer_client.base_url}/webhook",
            data="{invalid json}",
            headers={'Content-Type': 'application/json'}
        )
        
        assert response.status_code == 400, f"Expected 400, got {response.status_code}"
        logger.info("✓ Invalid JSON rejected with 400")
    
    def test_webhook_null_payload(self, optimizer_client):
        """
        Test: Send null payload to webhook endpoint
        Expected: 400 Bad Request
        """
        logger.info("Test: Webhook with null payload")
        
        response = requests.post(
            f"{optimizer_client.base_url}/webhook",
            data="null",
            headers={'Content-Type': 'application/json'}
        )
        
        assert response.status_code == 400, f"Expected 400, got {response.status_code}"
        logger.info("✓ Null payload rejected with 400")
    
    def test_webhook_empty_array(self, optimizer_client):
        """
        Test: Send empty array to webhook endpoint
        Expected: 400 Bad Request
        """
        logger.info("Test: Webhook with empty array")
        
        response = requests.post(
            f"{optimizer_client.base_url}/webhook",
            json=[],
            headers={'Content-Type': 'application/json'}
        )
        
        assert response.status_code == 400, f"Expected 400, got {response.status_code}"
        logger.info("✓ Empty array rejected with 400")
    
    def test_webhook_missing_summary(self, optimizer_client):
        """
        Test: Send payload without summary field
        Expected: 400 Bad Request
        """
        logger.info("Test: Webhook with missing summary")
        
        payload = [{}]
        response = requests.post(
            f"{optimizer_client.base_url}/webhook",
            json=payload,
            headers={'Content-Type': 'application/json'}
        )
        
        assert response.status_code == 400, f"Expected 400, got {response.status_code}"
        logger.info("✓ Missing summary rejected with 400")
    
    def test_webhook_null_job_id(self, optimizer_client):
        """
        Test: Send payload with null jobID
        Expected: 400 Bad Request
        """
        logger.info("Test: Webhook with null jobID")
        
        payload = [{
            "summary": {
                "jobID": None,
                "status": "COMPLETED",
                "total_experiments": 10,
                "processed_experiments": 8,
                "existing_experiments": 2
            }
        }]
        
        response = requests.post(
            f"{optimizer_client.base_url}/webhook",
            json=payload,
            headers={'Content-Type': 'application/json'}
        )
        
        assert response.status_code == 400, f"Expected 400, got {response.status_code}"
        logger.info("✓ Null jobID rejected with 400")
    
    def test_webhook_empty_job_id(self, optimizer_client):
        """
        Test: Send payload with empty jobID
        Expected: 400 Bad Request
        """
        logger.info("Test: Webhook with empty jobID")
        
        payload = [{
            "summary": {
                "jobID": "",
                "status": "COMPLETED",
                "total_experiments": 10,
                "processed_experiments": 8,
                "existing_experiments": 2
            }
        }]
        
        response = requests.post(
            f"{optimizer_client.base_url}/webhook",
            json=payload,
            headers={'Content-Type': 'application/json'}
        )
        
        assert response.status_code == 400, f"Expected 400, got {response.status_code}"
        logger.info("✓ Empty jobID rejected with 400")
    
    def test_webhook_whitespace_job_id(self, optimizer_client):
        """
        Test: Send payload with whitespace-only jobID
        Expected: 400 Bad Request
        """
        logger.info("Test: Webhook with whitespace jobID")
        
        payload = [{
            "summary": {
                "jobID": "   ",
                "status": "COMPLETED",
                "total_experiments": 10,
                "processed_experiments": 8,
                "existing_experiments": 2
            }
        }]
        
        response = requests.post(
            f"{optimizer_client.base_url}/webhook",
            json=payload,
            headers={'Content-Type': 'application/json'}
        )
        
        assert response.status_code == 400, f"Expected 400, got {response.status_code}"
        logger.info("✓ Whitespace jobID rejected with 400")
    
    def test_webhook_malformed_summary(self, optimizer_client):
        """
        Test: Send payload with malformed summary (string instead of object)
        Expected: 400 Bad Request
        """
        logger.info("Test: Webhook with malformed summary")
        
        payload = [{
            "summary": "this should be an object"
        }]
        
        response = requests.post(
            f"{optimizer_client.base_url}/webhook",
            json=payload,
            headers={'Content-Type': 'application/json'}
        )
        
        assert response.status_code == 400, f"Expected 400, got {response.status_code}"
        logger.info("✓ Malformed summary rejected with 400")
    
    def test_webhook_missing_content_type(self, optimizer_client):
        """
        Test: Send webhook without Content-Type header
        Expected: 400 or 415 (Unsupported Media Type)
        """
        logger.info("Test: Webhook without Content-Type header")
        
        payload = [{
            "summary": {
                "jobID": "test-job-123",
                "status": "COMPLETED",
                "total_experiments": 10,
                "processed_experiments": 8,
                "existing_experiments": 2
            }
        }]
        
        response = requests.post(
            f"{optimizer_client.base_url}/webhook",
            json=payload
            # No Content-Type header
        )
        
        # Accept either 400 or 415 as valid responses
        assert response.status_code in [400, 415], f"Expected 400 or 415, got {response.status_code}"
        logger.info(f"✓ Missing Content-Type handled with {response.status_code}")
    
    def test_webhook_valid_payload_accepted(self, optimizer_client):
        """
        Test: Send valid payload to ensure endpoint works correctly
        Expected: 200 OK
        
        This positive test ensures the endpoint isn't broken and can accept valid requests.
        """
        logger.info("Test: Webhook with valid payload (positive control)")
        
        payload = [{
            "summary": {
                "jobID": "test-valid-job-999",
                "status": "COMPLETED",
                "total_experiments": 5,
                "processed_experiments": 5,
                "existing_experiments": 0
            }
        }]
        
        response = requests.post(
            f"{optimizer_client.base_url}/webhook",
            json=payload,
            headers={'Content-Type': 'application/json'}
        )
        
        assert response.status_code == 200, f"Expected 200, got {response.status_code}"
        logger.info("✓ Valid payload accepted with 200")
    
    def test_webhook_multiple_payloads_one_invalid(self, optimizer_client):
        """
        Test: Send multiple payloads where one is invalid
        Expected: 400 Bad Request (entire request should be rejected)
        """
        logger.info("Test: Webhook with multiple payloads (one invalid)")
        
        payload = [
            {
                "summary": {
                    "jobID": "test-job-valid",
                    "status": "COMPLETED",
                    "total_experiments": 5,
                    "processed_experiments": 5,
                    "existing_experiments": 0
                }
            },
            {
                "summary": {
                    "jobID": None,  # Invalid
                    "status": "COMPLETED",
                    "total_experiments": 3,
                    "processed_experiments": 3,
                    "existing_experiments": 0
                }
            }
        ]
        
        response = requests.post(
            f"{optimizer_client.base_url}/webhook",
            json=payload,
            headers={'Content-Type': 'application/json'}
        )
        
