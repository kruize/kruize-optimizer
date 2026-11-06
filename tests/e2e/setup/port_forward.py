"""Port forwarding utilities."""

import logging
import subprocess
from typing import List

logger = logging.getLogger("kruize_e2e.setup.port_forward")


def start_port_forwards(kubectl, namespace: str, config) -> List[subprocess.Popen]:
    """Start port-forwards for Kruize services (kind only).

    Args:
        kubectl: KubectlClient instance
        namespace: Namespace
        config: Config instance

    Returns:
        List of Popen processes
    """
    if config.cluster_type != "kind":
        logger.debug("Port forwarding only needed for kind clusters")
        return []

    processes = []

    logger.info("Starting port forwards...")

    # Kruize service
    try:
        cmd = [kubectl.kubectl_cmd, "-n", namespace, "port-forward", "svc/kruize", f"{config.KRUIZE_PORT}:8080"]
        proc = subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        processes.append(proc)
        logger.info(f"Port forward: kruize -> localhost:{config.KRUIZE_PORT}")
    except Exception as e:
        logger.warning(f"Failed to start port forward for kruize: {e}")

    # Kruize UI service
    try:
        cmd = [kubectl.kubectl_cmd, "-n", namespace, "port-forward", "svc/kruize-ui-nginx-service", f"{config.KRUIZE_UI_PORT}:8080"]
        proc = subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        processes.append(proc)
        logger.info(f"Port forward: kruize-ui -> localhost:{config.KRUIZE_UI_PORT}")
    except Exception as e:
        logger.warning(f"Failed to start port forward for kruize-ui: {e}")

    return processes


def kill_all_port_forwards(processes: List[subprocess.Popen]) -> None:
    """Kill all port forward processes.

    Args:
        processes: List of Popen processes
    """
    logger.info("Killing port forward processes...")
    for proc in processes:
        try:
            proc.terminate()
            proc.wait(timeout=5)
        except Exception as e:
            logger.warning(f"Failed to terminate process: {e}")
            try:
                proc.kill()
            except:
                pass
