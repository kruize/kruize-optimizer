"""Tests for Kruize API endpoints and data validation."""

import pytest

from utils.http_client import KruizeAPIClient


@pytest.fixture(scope="class")
def kruize_client(kruize_url):
    """Create KruizeAPIClient instance."""
    return KruizeAPIClient(kruize_url)


@pytest.mark.e2e
@pytest.mark.api
class TestKruizeAPI:
    """Test Kruize API endpoints."""

    @pytest.mark.api
    def test_metric_profiles_present(self, kruize_client, config):
        """Test that required metric profiles are loaded.

        Validates that the resource-optimization-local-monitoring profile
        from configsReferenceIndex.json is available via API.
        """
        profiles = kruize_client.list_metric_profiles()

        assert isinstance(profiles, list), "listMetricProfiles should return a list"
        assert len(profiles) > 0, "No metric profiles found"

        # Check for the expected profile from configsReferenceIndex.json
        profile_names = [p.get('profile_name') or p.get('name') for p in profiles]
        assert "resource-optimization-local-monitoring" in profile_names, \
            f"Expected metric profile 'resource-optimization-local-monitoring' not found. Found: {profile_names}"

    @pytest.mark.api
    def test_metadata_profiles_present(self, kruize_client, config):
        """Test that required metadata profiles are loaded.

        Validates that the cluster-metadata-local-monitoring profile
        from configsReferenceIndex.json is available via API.
        """
        profiles = kruize_client.list_metadata_profiles()

        assert isinstance(profiles, list), "listMetadataProfiles should return a list"
        assert len(profiles) > 0, "No metadata profiles found"

        # Check for the expected profile from configsReferenceIndex.json
        profile_names = [p.get('profile_name') or p.get('name') for p in profiles]
        assert "cluster-metadata-local-monitoring" in profile_names, \
            f"Expected metadata profile 'cluster-metadata-local-monitoring' not found. Found: {profile_names}"

    @pytest.mark.api
    def test_datasource_present(self, kruize_client, config):
        """Test that the correct datasource is configured.

        For kind clusters: prometheus-1
        For OpenShift clusters: thanos-1
        """
        datasources = kruize_client.list_datasources()

        assert isinstance(datasources, list), "listDatasources should return a list"
        assert len(datasources) > 0, "No datasources found"

        datasource_names = [ds.get('datasource_name') or ds.get('name') for ds in datasources]

        # Check cluster-specific datasource
        if config.cluster_type == "kind":
            expected_datasource = "prometheus-1"
        elif config.cluster_type == "openshift":
            expected_datasource = "thanos-1"
        else:
            expected_datasource = "prometheus-1"  # default

        assert expected_datasource in datasource_names, \
            f"Expected datasource '{expected_datasource}' for cluster type '{config.cluster_type}' not found. Found: {datasource_names}"

    @pytest.mark.api
    def test_sysbench_experiment_present(self, kruize_client, config):
        """Test that sysbench experiment was auto-created by optimizer.

        After the optimizer starts and waits for OPTIMIZER_WAIT_DURATION,
        it should have auto-created experiments for labeled workloads.
        """
        experiments = kruize_client.list_experiments()

        assert isinstance(experiments, list), "listExperiments should return a list"

        # Find sysbench experiment
        # The experiment name format is: datasource|cluster_name|namespace|workload(type)|container
        # e.g., prometheus-1|default|default|sysbench(deployment)|sysbench
        sysbench_experiments = [
            exp for exp in experiments
            if any(
                ko.get('name') == 'sysbench'
                for ko in exp.get('kubernetes_objects', [])
            )
        ]

        assert len(sysbench_experiments) > 0, \
            f"No experiments found for sysbench workload. Total experiments: {len(experiments)}"

        # Verify the experiment has the expected structure
        sysbench_exp = sysbench_experiments[0]
        assert 'experiment_name' in sysbench_exp, "Experiment missing 'experiment_name' field"
        assert 'kubernetes_objects' in sysbench_exp, "Experiment missing 'kubernetes_objects' field"

        # Verify kubernetes object details
        k8s_obj = sysbench_exp['kubernetes_objects'][0]
        assert k8s_obj['name'] == 'sysbench', f"Expected workload name 'sysbench', got '{k8s_obj['name']}'"
        assert k8s_obj['namespace'] == config.app_namespace, \
            f"Expected namespace '{config.app_namespace}', got '{k8s_obj['namespace']}'"
