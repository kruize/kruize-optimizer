"""Repository cloning and management."""

import logging
from pathlib import Path
from typing import Dict

from utils.git_ops import clone_repo, delete_repo

logger = logging.getLogger("kruize_e2e.setup.repos")


def clone_all_repos(working_dir: str, config) -> Dict[str, str]:
    """Clone all required repositories for the demo.

    Args:
        working_dir: Working directory for repos
        config: Config instance

    Returns:
        Dict mapping repo name to path
    """
    logger.info("Cloning required repositories...")

    working_path = Path(working_dir)
    working_path.mkdir(parents=True, exist_ok=True)

    repos = {}

    # Clone autotune
    autotune_dir = working_path / "autotune"
    clone_repo(config.AUTOTUNE_REPO, str(autotune_dir), config.AUTOTUNE_BRANCH)
    repos["autotune"] = str(autotune_dir)

    # Clone benchmarks
    benchmarks_dir = working_path / "benchmarks"
    clone_repo(config.BENCHMARKS_REPO, str(benchmarks_dir))
    repos["benchmarks"] = str(benchmarks_dir)

    logger.info(f"Cloned {len(repos)} repositories to {working_dir}")
    return repos


def clone_operator_repo(working_dir: str, config) -> str:
    """Clone kruize-operator repository.

    Args:
        working_dir: Working directory
        config: Config instance

    Returns:
        Path to cloned repo
    """
    operator_dir = Path(working_dir) / "kruize-operator"
    clone_repo(config.KRUIZE_OPERATOR_REPO, str(operator_dir), config.KRUIZE_OPERATOR_BRANCH)
    return str(operator_dir)


def clone_optimizer_repo(working_dir: str, config) -> str:
    """Clone kruize-optimizer repository.

    Args:
        working_dir: Working directory
        config: Config instance

    Returns:
        Path to cloned repo
    """
    optimizer_dir = Path(working_dir) / "kruize-optimizer"
    clone_repo(config.KRUIZE_OPTIMIZER_REPO, str(optimizer_dir), config.KRUIZE_OPTIMIZER_BRANCH)
    return str(optimizer_dir)


def delete_all_repos(working_dir: str, repo_names: list) -> None:
    """Delete all cloned repositories.

    Args:
        working_dir: Working directory
        repo_names: List of repo names to delete
    """
    logger.info("Deleting cloned repositories...")

    for repo_name in repo_names:
        repo_path = Path(working_dir) / repo_name
        if repo_path.exists():
            delete_repo(str(repo_path))

    logger.info("Repository cleanup complete")
