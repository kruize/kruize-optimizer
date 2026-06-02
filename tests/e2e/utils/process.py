"""Process utilities for running subprocess commands."""

import logging
import subprocess
from pathlib import Path
from typing import List, Optional

logger = logging.getLogger("kruize_e2e.process")


class ProcessError(Exception):
    """Exception raised for process failures."""
    pass


def run_command(
    cmd: List[str],
    cwd: Optional[str] = None,
    timeout: int = 300,
    check: bool = True,
    env: Optional[dict] = None,
) -> subprocess.CompletedProcess:
    """Run a command via subprocess with logging.

    Args:
        cmd: Command and arguments as list
        cwd: Working directory
        timeout: Timeout in seconds
        check: Raise exception on non-zero exit
        env: Environment variables

    Returns:
        CompletedProcess instance

    Raises:
        ProcessError: If check=True and command fails
    """
    cmd_str = " ".join(cmd)
    logger.info("=" * 80)
    logger.info(f"COMMAND: {cmd_str}")
    if cwd:
        logger.info(f"CWD: {cwd}")
    logger.info("=" * 80)

    try:
        result = subprocess.run(
            cmd,
            cwd=cwd,
            timeout=timeout,
            check=False,
            capture_output=True,
            text=True,
            env=env,
        )

        if result.stdout:
            logger.info(f"STDOUT:\n{result.stdout}")
        if result.stderr:
            logger.info(f"STDERR:\n{result.stderr}")

        logger.info(f"EXIT CODE: {result.returncode}")
        logger.info("=" * 80)

        if check and result.returncode != 0:
            raise ProcessError(
                f"Command failed with exit code {result.returncode}: {cmd_str}\n"
                f"stdout: {result.stdout}\n"
                f"stderr: {result.stderr}"
            )

        return result

    except subprocess.TimeoutExpired as e:
        logger.error(f"Command timed out after {timeout}s: {cmd_str}")
        raise ProcessError(f"Command timed out: {cmd_str}") from e


def run_script(
    script_path: str,
    args: Optional[List[str]] = None,
    cwd: Optional[str] = None,
    timeout: int = 600,
) -> subprocess.CompletedProcess:
    """Run a bash script with arguments.

    Args:
        script_path: Path to the script
        args: Script arguments
        cwd: Working directory
        timeout: Timeout in seconds

    Returns:
        CompletedProcess instance
    """
    script = Path(script_path)
    if not script.exists():
        raise FileNotFoundError(f"Script not found: {script_path}")

    cmd = ["/bin/bash", str(script)]
    if args:
        cmd.extend(args)

    logger.info(f"Running script: {script.name} {' '.join(args or [])}")
    return run_command(cmd, cwd=cwd, timeout=timeout)


def kill_process_by_port(port: int) -> None:
    """Kill processes using the specified port.

    Args:
        port: Port number
    """
    logger.debug(f"Attempting to kill processes on port {port}")

    try:
        # Use lsof to find PIDs using the port
        result = run_command(
            ["lsof", "-ti", f":{port}"],
            check=False,
        )

        if result.returncode == 0 and result.stdout.strip():
            pids = result.stdout.strip().split("\n")
            for pid in pids:
                pid = pid.strip()
                if pid:
                    logger.info(f"Killing process {pid} on port {port}")
                    run_command(["kill", pid], check=False)
    except Exception as e:
        logger.warning(f"Failed to kill processes on port {port}: {e}")
