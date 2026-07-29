"""Test kruize pod status."""

import pytest
from kubernetes import client


@pytest.mark.e2e
@pytest.mark.smoke
class TestKruizePod:
    """Tests for kruize pod."""

    def test_kruize_pod_exists_and_running(self, k8s_client, namespace):
        """Verify that the kruize pod exists and is in Running phase."""
        pods = k8s_client.list_namespaced_pod(
            namespace=namespace,
            label_selector="app=kruize"
        )

        assert len(pods.items) > 0, (
            f"No kruize pods found in namespace '{namespace}'"
        )

        for pod in pods.items:
            assert pod.status.phase == "Running", (
                f"Pod {pod.metadata.name} is in phase '{pod.status.phase}', expected 'Running'"
            )

    def test_kruize_pod_containers_ready(self, k8s_client, namespace):
        """Verify all containers in the kruize pod are ready."""
        pods = k8s_client.list_namespaced_pod(
            namespace=namespace,
            label_selector="app=kruize"
        )

        assert len(pods.items) > 0, "No kruize pods found"

        for pod in pods.items:
            container_statuses = pod.status.container_statuses or []
            assert len(container_statuses) > 0, f"No containers found in pod {pod.metadata.name}"

            for container_status in container_statuses:
                assert container_status.ready, (
                    f"Container '{container_status.name}' in pod '{pod.metadata.name}' is not ready"
                )
