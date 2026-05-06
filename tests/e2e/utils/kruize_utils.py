"""
Kruize API utility functions for E2E tests
"""
import requests
import logging
import time
from typing import Dict, List, Optional, Any

logger = logging.getLogger(__name__)


class KruizeAPIClient:
    """Client for interacting with Kruize APIs"""
    
    def __init__(self, base_url: str, timeout: int = 30):
        self.base_url = base_url.rstrip('/')
        self.timeout = timeout
        self.session = requests.Session()
    
    def _make_request(self, method: str, endpoint: str, **kwargs) -> requests.Response:
        """Make HTTP request to Kruize API"""
        url = f"{self.base_url}{endpoint}"
        logger.debug(f"{method} {url}")
        
        try:
            response = self.session.request(method, url, timeout=self.timeout, **kwargs)
            logger.debug(f"Response status: {response.status_code}")
            return response
        except requests.exceptions.RequestException as e:
            logger.error(f"Request failed: {e}")
            raise
    
    def list_datasources(self) -> Dict:
        """Call listDatasources API"""
        response = self._make_request('GET', '/datasources')
        return response.json() if response.status_code == 200 else {}
    
    def list_metric_profiles(self) -> List[Dict]:
        """Call listMetricProfiles API"""
        response = self._make_request('GET', '/listMetricProfiles')
        return response.json() if response.status_code == 200 else []
    
    def list_metadata_profiles(self) -> List[Dict]:
        """Call listMetadataProfiles API"""
        response = self._make_request('GET', '/listMetadataProfiles')
        return response.json() if response.status_code == 200 else []
    
    def list_layers(self) -> List[Dict]:
        """Call listLayers API"""
        response = self._make_request('GET', '/listLayers')
        return response.json() if response.status_code == 200 else []
    
    def health_check(self) -> bool:
        """Check if Kruize service is healthy"""
        try:
            response = self._make_request('GET', '/q/health/live')
            return response.status_code == 200
        except:
            return False
    
    def wait_for_service(self, max_retries: int = 30, retry_interval: int = 2) -> bool:
        """Wait for Kruize service to be available"""
        logger.info(f"Waiting for Kruize service at {self.base_url}")
        
        for attempt in range(max_retries):
            try:
                if self.health_check():
                    logger.info("Kruize service is available")
                    return True
            except:
                pass
            
            logger.debug(f"Attempt {attempt + 1}/{max_retries}: Service not ready yet")
            time.sleep(retry_interval)
        
        logger.error(f"Kruize service did not become available after {max_retries} attempts")
        return False


class OptimizerAPIClient:
    """Client for interacting with Optimizer APIs"""
    
    def __init__(self, base_url: str, timeout: int = 30):
        self.base_url = base_url.rstrip('/')
        self.timeout = timeout
        self.session = requests.Session()
    
    def _make_request(self, method: str, endpoint: str, **kwargs) -> requests.Response:
        """Make HTTP request to Optimizer API"""
        url = f"{self.base_url}{endpoint}"
        logger.debug(f"{method} {url}")
        
        try:
            response = self.session.request(method, url, timeout=self.timeout, **kwargs)
            logger.debug(f"Response status: {response.status_code}")
            return response
        except requests.exceptions.RequestException as e:
            logger.error(f"Request failed: {e}")
            raise
    
    def get_status(self) -> Dict:
        """Get optimizer status"""
        response = self._make_request('GET', '/status')
        return response.json() if response.status_code == 200 else {}
    
    def get_jobs_overview(self) -> Dict:
        """Get jobs overview"""
        response = self._make_request('GET', '/jobs')
        return response.json() if response.status_code == 200 else {}
    
    def send_webhook(self, payload: List[Dict]) -> requests.Response:
        """Send webhook payload to optimizer"""
        return self._make_request('POST', '/webhook', json=payload, headers={'Content-Type': 'application/json'})
    
    def health_check(self) -> bool:
        """Check if Optimizer service is healthy"""
        try:
            response = self._make_request('GET', '/q/health/live')
            return response.status_code == 200
        except:
            return False
    
    def wait_for_service(self, max_retries: int = 30, retry_interval: int = 2) -> bool:
        """Wait for Optimizer service to be available"""
        logger.info(f"Waiting for Optimizer service at {self.base_url}")
        
        for attempt in range(max_retries):
            try:
                if self.health_check():
                    logger.info("Optimizer service is available")
                    return True
            except:
                pass
            
            logger.debug(f"Attempt {attempt + 1}/{max_retries}: Service not ready yet")
            time.sleep(retry_interval)
        
        logger.error(f"Optimizer service did not become available after {max_retries} attempts")
        return False


def verify_profiles_installed(kruize_client: KruizeAPIClient) -> Dict[str, bool]:
    """Verify that all required profiles are installed"""
    results = {
        'metric_profiles': False,
        'metadata_profiles': False,
        'layers': False
    }
    
    try:
        # Check metric profiles
        metric_profiles = kruize_client.list_metric_profiles()
        if metric_profiles and len(metric_profiles) > 0:
            logger.info(f"Found {len(metric_profiles)} metric profile(s)")
            results['metric_profiles'] = True
        else:
            logger.warning("No metric profiles found")
        
        # Check metadata profiles
        metadata_profiles = kruize_client.list_metadata_profiles()
        if metadata_profiles and len(metadata_profiles) > 0:
            logger.info(f"Found {len(metadata_profiles)} metadata profile(s)")
            results['metadata_profiles'] = True
        else:
            logger.warning("No metadata profiles found")
        
        # Check layers
        layers = kruize_client.list_layers()
        if layers and len(layers) > 0:
            logger.info(f"Found {len(layers)} layer(s)")
            results['layers'] = True
        else:
            logger.warning("No layers found")
    
    except Exception as e:
        logger.error(f"Error verifying profiles: {e}")
    
    return results


def wait_for_job_trigger(optimizer_client: OptimizerAPIClient, initial_count: int, timeout: int = 180) -> bool:
    """Wait for a new bulk job to be triggered"""
    logger.info(f"Waiting for job trigger (initial count: {initial_count}, timeout: {timeout}s)")
    
    start_time = time.time()
    while time.time() - start_time < timeout:
        try:
            jobs_overview = optimizer_client.get_jobs_overview()
            current_count = jobs_overview.get('jobsTriggered', 0)
            
            if current_count > initial_count:
                logger.info(f"New job triggered! Count: {initial_count} -> {current_count}")
                return True
            
            logger.debug(f"Jobs triggered: {current_count} (waiting for > {initial_count})")
        except Exception as e:
            logger.debug(f"Error checking job status: {e}")
        
        time.sleep(5)
    
    logger.error(f"No new job triggered within {timeout}s")
    return False


def wait_for_webhook_callback(optimizer_client: OptimizerAPIClient, initial_processed: int, timeout: int = 120) -> bool:
    """Wait for webhook callback to be received"""
    logger.info(f"Waiting for webhook callback (initial processed: {initial_processed}, timeout: {timeout}s)")
    
    start_time = time.time()
    while time.time() - start_time < timeout:
        try:
            jobs_overview = optimizer_client.get_jobs_overview()
            current_processed = jobs_overview.get('totalExperimentsProcessed', 0)
            
            if current_processed > initial_processed:
                logger.info(f"Webhook received! Processed: {initial_processed} -> {current_processed}")
                return True
            
            logger.debug(f"Experiments processed: {current_processed} (waiting for > {initial_processed})")
        except Exception as e:
            logger.debug(f"Error checking webhook status: {e}")
        
        time.sleep(5)
    
    logger.error(f"No webhook callback received within {timeout}s")
    return False
