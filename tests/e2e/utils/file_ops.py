"""File operation utilities - Python replacements for sed/awk/grep."""

import logging
import re
from pathlib import Path
from typing import List

import yaml

logger = logging.getLogger("kruize_e2e.file_ops")


def replace_in_file(filepath: str, pattern: str, replacement: str) -> None:
    """Replace pattern in file (replaces sed).

    Args:
        filepath: Path to file
        pattern: Pattern to search (can be regex)
        replacement: Replacement string
    """
    path = Path(filepath)
    if not path.exists():
        raise FileNotFoundError(f"File not found: {filepath}")

    content = path.read_text()
    new_content = re.sub(pattern, replacement, content)

    if content != new_content:
        logger.info(f"Patching {filepath}: {pattern} -> {replacement}")
        path.write_text(new_content)
    else:
        logger.debug(f"No changes needed in {filepath}")


def patch_yaml_file(filepath: str, patches: dict) -> None:
    """Patch YAML file with dictionary of changes.

    Args:
        filepath: Path to YAML file
        patches: Dict of key paths to new values
    """
    path = Path(filepath)
    if not path.exists():
        raise FileNotFoundError(f"File not found: {filepath}")

    with open(path, 'r') as f:
        data = yaml.safe_load(f)

    for key_path, value in patches.items():
        keys = key_path.split('.')
        current = data
        for key in keys[:-1]:
            if key not in current:
                current[key] = {}
            current = current[key]
        current[keys[-1]] = value
        logger.info(f"Patching {filepath}: {key_path} = {value}")

    with open(path, 'w') as f:
        yaml.safe_dump(data, f, default_flow_style=False)


def read_version_from_pom(pom_path: str) -> str:
    """Read version from Maven pom.xml (replaces grep | awk).

    Args:
        pom_path: Path to pom.xml

    Returns:
        Version string
    """
    path = Path(pom_path)
    if not path.exists():
        raise FileNotFoundError(f"pom.xml not found: {pom_path}")

    content = path.read_text()

    # Find <artifactId>autotune</artifactId> followed by <version>X.Y.Z</version>
    match = re.search(
        r'<artifactId>autotune</artifactId>\s*<version>([^<]+)</version>',
        content,
        re.DOTALL
    )
    if match:
        version = match.group(1).strip()
        logger.debug(f"Found autotune version in pom.xml: {version}")
        return version

    raise ValueError("Could not find autotune version in pom.xml")


def read_version_from_makefile(makefile_path: str) -> str:
    """Read VERSION from Makefile (replaces grep | awk).

    Args:
        makefile_path: Path to Makefile

    Returns:
        Version string
    """
    path = Path(makefile_path)
    if not path.exists():
        raise FileNotFoundError(f"Makefile not found: {makefile_path}")

    content = path.read_text()

    # Find VERSION ?= X.Y.Z or VERSION = X.Y.Z
    match = re.search(r'^VERSION\s*\??=\s*(.+)$', content, re.MULTILINE)
    if match:
        version = match.group(1).strip()
        logger.debug(f"Found VERSION in Makefile: {version}")
        return version

    raise ValueError("Could not find VERSION in Makefile")


def update_deployment_image(deployment_yaml_path: str, new_image: str) -> None:
    """Update image in deployment YAML (replaces sed).

    Args:
        deployment_yaml_path: Path to deployment.yaml
        new_image: New image string
    """
    path = Path(deployment_yaml_path)
    if not path.exists():
        raise FileNotFoundError(f"Deployment file not found: {deployment_yaml_path}")

    with open(path, 'r') as f:
        docs = list(yaml.safe_load_all(f))

    modified = False
    for doc in docs:
        if doc and doc.get('kind') == 'Deployment':
            containers = doc.get('spec', {}).get('template', {}).get('spec', {}).get('containers', [])
            for container in containers:
                if 'image' in container:
                    old_image = container['image']
                    container['image'] = new_image
                    logger.info(f"Updated image: {old_image} -> {new_image}")
                    modified = True

    if modified:
        with open(path, 'w') as f:
            yaml.safe_dump_all(docs, f, default_flow_style=False)


def remove_cr_blocks(cr_file: str, blocks: List[str]) -> None:
    """Remove optional CR resource blocks from YAML (replaces awk script).

    Args:
        cr_file: Path to CR YAML file
        blocks: List of top-level block names to remove
    """
    path = Path(cr_file)
    if not path.exists():
        logger.warning(f"CR file not found: {cr_file}, skipping")
        return

    logger.info(f"Removing optional CR blocks from {cr_file}: {blocks}")

    with open(path, 'r') as f:
        lines = f.readlines()

    output_lines = []
    skip = False
    current_block = None

    for line in lines:
        # Check if this is a top-level spec child block
        match = re.match(r'^  ([a-zA-Z0-9_-]+):$', line)
        if match:
            block_name = match.group(1)
            if block_name in blocks:
                skip = True
                current_block = block_name
                logger.debug(f"Skipping block: {block_name}")
                continue
            else:
                skip = False
                current_block = None

        if not skip:
            output_lines.append(line)

    # Create backup
    path.rename(f"{cr_file}.bak")

    # Write modified content
    with open(path, 'w') as f:
        f.writelines(output_lines)

    logger.info(f"Removed blocks successfully, backup at {cr_file}.bak")
