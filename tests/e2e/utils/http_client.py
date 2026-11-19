"""HTTP client utilities for Kruize API."""

import logging
from typing import Dict, List, Optional

import requests

logger = logging.getLogger("kruize_e2e.http_client")


class KruizeAPIClient:
    """Client for Kruize API interactions."""

    def __init__(self, base_url: str):
        """Initialize API client.

        Args:
            base_url: Base URL (e.g., "127.0.0.1:8080" or "http://127.0.0.1:8080")
        """
        # Ensure base URL has http:// prefix
        if not base_url.startswith("http"):
            base_url = f"http://{base_url}"

        self.base_url = base_url.rstrip('/')
        logger.info(f"Initialized Kruize API client with base URL: {self.base_url}")

    def list_experiments(self, experiment_name: Optional[str] = None) -> List[Dict]:
        """List experiments.

        Args:
            experiment_name: Optional experiment name to filter

        Returns:
            List of experiment dicts
        """
        url = f"{self.base_url}/listExperiments"
        params = {}
        if experiment_name:
            params['experiment_name'] = experiment_name

        logger.debug(f"GET {url} params={params}")
        response = requests.get(url, params=params, timeout=30)
        response.raise_for_status()
        return response.json()

    def list_recommendations(self, experiment_name: str) -> List[Dict]:
        """List recommendations for an experiment.

        Args:
            experiment_name: Experiment name

        Returns:
            List of recommendation dicts
        """
        url = f"{self.base_url}/listRecommendations"
        params = {'experiment_name': experiment_name}

        logger.debug(f"GET {url} params={params}")
        response = requests.get(url, params=params, timeout=30)
        response.raise_for_status()
        return response.json()

    def list_metric_profiles(self, profile_name: Optional[str] = None) -> List[Dict]:
        """List metric profiles.

        Args:
            profile_name: Optional profile name to filter

        Returns:
            List of metric profile dicts
        """
        url = f"{self.base_url}/listMetricProfiles"
        params = {}
        if profile_name:
            params['profile_name'] = profile_name

        logger.debug(f"GET {url} params={params}")
        response = requests.get(url, params=params, timeout=30)
        response.raise_for_status()
        return response.json()

    def list_metadata_profiles(self, profile_name: Optional[str] = None) -> List[Dict]:
        """List metadata profiles.

        Args:
            profile_name: Optional profile name to filter

        Returns:
            List of metadata profile dicts
        """
        url = f"{self.base_url}/listMetadataProfiles"
        params = {}
        if profile_name:
            params['profile_name'] = profile_name

        logger.debug(f"GET {url} params={params}")
        response = requests.get(url, params=params, timeout=30)
        response.raise_for_status()
        return response.json()

    def list_datasources(self, datasource_name: Optional[str] = None) -> List[Dict]:
        """List datasources.

        Args:
            datasource_name: Optional datasource name to filter

        Returns:
            List of datasource dicts
        """
        url = f"{self.base_url}/listDatasources"
        params = {}
        if datasource_name:
            params['datasource_name'] = datasource_name

        logger.debug(f"GET {url} params={params}")
        response = requests.get(url, params=params, timeout=30)
        response.raise_for_status()
        return response.json()

    def health_check(self) -> bool:
        """Check if Kruize is responding.

        Returns:
            True if healthy
        """
        try:
            url = f"{self.base_url}/listExperiments"
            response = requests.get(url, timeout=10)
            return response.status_code == 200
        except Exception as e:
            logger.warning(f"Health check failed: {e}")
            return False
