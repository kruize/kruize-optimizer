"""Wait and polling utilities."""

import logging
import time
from typing import Callable

logger = logging.getLogger("kruize_e2e.wait")


def wait_for_pod_running(
    kubectl,
    namespace: str,
    label_selector: str,
    max_attempts: int = 12,
    interval: int = 5,
) -> bool:
    """Wait for pods with label to be in Running state.

    Args:
        kubectl: KubectlClient instance
        namespace: Namespace
        label_selector: Label selector
        max_attempts: Maximum polling attempts
        interval: Interval between attempts in seconds

    Returns:
        True if pod is running
    """
    logger.info(f"Waiting for pod with label '{label_selector}' in namespace '{namespace}'")

    for attempt in range(1, max_attempts + 1):
        try:
            pods = kubectl.get_pods(namespace, label_selector)
            if pods:
                running_pods = [p for p in pods if p.get('status', {}).get('phase') == 'Running']
                if running_pods:
                    logger.info(f"Pod is running after {attempt} attempts")
                    return True

            if attempt < max_attempts:
                logger.debug(f"Attempt {attempt}/{max_attempts}: Pod not running yet, waiting {interval}s...")
                time.sleep(interval)
        except Exception as e:
            logger.warning(f"Error checking pod status: {e}")
            if attempt < max_attempts:
                time.sleep(interval)

    logger.error(f"Pod did not become running after {max_attempts} attempts")
    return False


def wait_for_condition(
    check_fn: Callable[[], bool],
    timeout: int,
    interval: int = 5,
    message: str = "",
) -> bool:
    """Generic polling utility.

    Args:
        check_fn: Function that returns True when condition is met
        timeout: Total timeout in seconds
        interval: Interval between checks
        message: Log message

    Returns:
        True if condition was met
    """
    if message:
        logger.info(f"Waiting for: {message}")

    start_time = time.time()
    while time.time() - start_time < timeout:
        try:
            if check_fn():
                return True
        except Exception as e:
            logger.debug(f"Check failed: {e}")

        time.sleep(interval)

    logger.error(f"Condition not met after {timeout}s timeout")
    return False


def sleep_with_progress(seconds: int, message: str = "") -> None:
    """Sleep with progress logging.

    Args:
        seconds: Seconds to sleep
        message: Message to log
    """
    if message:
        logger.info(f"{message} ({seconds}s)")
    else:
        logger.info(f"Sleeping for {seconds}s...")

    time.sleep(seconds)
