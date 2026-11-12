# Build Guide

This guide explains how to build and push Docker images for Kruize Optimizer.

## Table of Contents

- [Overview](#overview)
- [Build Script](#build-script)
- [Build Options](#build-options)

## Overview

Kruize Optimizer provides a convenient build script ([`build_and_push.sh`](../build_and_push.sh)) that simplifies building and pushing Docker images. The script uses Quarkus Jib extension for efficient container image creation.

## Build Script

### Basic Usage

```bash
./build_and_push.sh [OPTIONS]
```

### Script Options

| Option | Description | Default |
|--------|-------------|---------|
| `-i IMAGE_NAME` | Full image name (registry/repository:tag) | quay.io/kruize/kruize-optimizer:&lt;project version&gt; |
| `-t TAG` | Image tag (used if -i not provided) | Project version from `pom.xml` |
| `-b BUILD` | Build image (true/false) | true |
| `-p PUSH` | Push image (true/false) | true |
| `-l PLATFORMS` | Target platforms | linux/amd64,linux/arm64 |
| `-h` | Show help message | - |

**Note**: Use either `-i` for full image name OR `-t` for tag with default registry/repo.

## Build Options

### 1. Build Only (No Push)

Build the image locally without pushing to a registry:

```bash
./build_and_push.sh -p false
```

### 2. Build and Push with Custom Tag

Build and push with a specific tag (run `docker login <registry-url>` first):

```bash
./build_and_push.sh -t 1.0.0
```

This creates: `quay.io/kruize/kruize-optimizer:1.0.0`

### 3. Build and Push with Full Image Name

Specify the complete image name including registry:

```bash
./build_and_push.sh -i quay.io/myuser/kruize-optimizer:1.0.0
```

### 4. Build for Specific Platform

Build for a single platform (faster):

```bash
./build_and_push.sh -l linux/amd64 -p false
```

### 5. Multi-Platform Build

Build for multiple platforms (default):

```bash
./build_and_push.sh -l linux/amd64,linux/arm64
```

For more options, run `./build_and_push.sh -h`.

---

**Previous**: [Design Documentation](design.md) | **Next**: [Contributing Guide](contributing.md) | [Back to docs](README.md)

**Last Updated**: 2026-11-04