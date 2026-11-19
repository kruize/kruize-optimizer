"""Git operations utilities."""

import logging
import shutil
from pathlib import Path
from typing import Optional

from utils.process import run_command, ProcessError

logger = logging.getLogger("kruize_e2e.git_ops")


def clone_repo(url: str, dest: str, branch: Optional[str] = None) -> None:
    """Clone a git repository, trying SSH first then falling back to HTTPS.

    Args:
        url: Repository URL (HTTPS format)
        dest: Destination directory
        branch: Branch to checkout (optional)

    Raises:
        ProcessError: If both SSH and HTTPS clone fail
    """
    dest_path = Path(dest)

    if dest_path.exists():
        logger.info(f"Repository already exists at {dest}, skipping clone")
        return

    # Try SSH first (convert HTTPS URL to SSH)
    ssh_url = url.replace("https://github.com/", "git@github.com:")

    try:
        logger.info(f"Attempting to clone {ssh_url} (SSH)")
        cmd = ["git", "clone", ssh_url, str(dest)]
        if branch:
            cmd.extend(["-b", branch])
        run_command(cmd, check=True)
        logger.info(f"Successfully cloned via SSH to {dest}")
        return
    except ProcessError:
        logger.warning("SSH clone failed, falling back to HTTPS")

    # Fall back to HTTPS
    try:
        logger.info(f"Attempting to clone {url} (HTTPS)")
        cmd = ["git", "clone", url, str(dest)]
        if branch:
            cmd.extend(["-b", branch])
        run_command(cmd, check=True)
        logger.info(f"Successfully cloned via HTTPS to {dest}")
    except ProcessError as e:
        raise ProcessError(f"Failed to clone repository {url}: {e}") from e


def checkout_branch(repo_dir: str, branch: str) -> None:
    """Checkout a specific branch in a git repository.

    Args:
        repo_dir: Repository directory
        branch: Branch name
    """
    logger.info(f"Checking out branch '{branch}' in {repo_dir}")
    try:
        run_command(["git", "checkout", branch], cwd=repo_dir, check=True)
    except ProcessError as e:
        raise ProcessError(f"Failed to checkout branch {branch}: {e}") from e


def delete_repo(repo_dir: str) -> None:
    """Delete a repository directory.

    Args:
        repo_dir: Repository directory path
    """
    repo_path = Path(repo_dir)
    if repo_path.exists():
        logger.info(f"Deleting repository at {repo_dir}")
        shutil.rmtree(repo_path)
