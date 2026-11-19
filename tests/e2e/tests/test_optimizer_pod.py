"""Test kruize-optimizer pod status."""

import pytest
from kubernetes import client


@pytest.mark.e2e
@pytest.mark.smoke
class TestOptimizerPod:
    """Tests for kruize-optimizer pod."""

    def test_optimizer_pod_exists_and_running(self, k8s_client, namespace):
        """Verify that the kruize-optimizer pod exists and is in Running phase."""
        pods = k8s_client.list_namespaced_pod(
            namespace=namespace,
            label_selector="app=kruize-optimizer"
        )

        assert len(pods.items) > 0, (
            f"No kruize-optimizer pods found in namespace '{namespace}'"
        )

        for pod in pods.items:
            assert pod.status.phase == "Running", (
                f"Pod {pod.metadata.name} is in phase '{pod.status.phase}', expected 'Running'"
            )

    def test_optimizer_pod_containers_ready(self, k8s_client, namespace):
        """Verify all containers in the optimizer pod are ready."""
        pods = k8s_client.list_namespaced_pod(
            namespace=namespace,
            label_selector="app=kruize-optimizer"
        )

        assert len(pods.items) > 0, "No kruize-optimizer pods found"

        for pod in pods.items:
            container_statuses = pod.status.container_statuses or []
            assert len(container_statuses) > 0, f"No containers found in pod {pod.metadata.name}"

            for container_status in container_statuses:
                assert container_status.ready, (
                    f"Container '{container_status.name}' in pod '{pod.metadata.name}' is not ready"
                )
