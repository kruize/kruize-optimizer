# Build Guide

This guide explains how to build and push Docker images for Kruize Optimizer.

## Table of Contents

- [Overview](#overview)
- [Build Script](#build-script)
- [Build Options](#build-options)
- [Examples](#examples)

## Overview

Kruize Optimizer provides a convenient build script ([`build_and_push.sh`](../build_and_push.sh)) that simplifies building and pushing Docker images. The script uses Quarkus Jib extension for efficient container image creation.


### Configure Registry Credentials

For pushing to container registries:

```bash
# For Quay.io
docker login quay.io

# For Docker Hub
docker login

# For other registries
docker login <registry-url>
```

## Build Script

The build script is located at the root of the repository: [`build_and_push.sh`](../build_and_push.sh)

### Basic Usage

```bash
./build_and_push.sh [OPTIONS]
```

### Script Options

| Option | Description | Default |
|--------|-------------|---------|
| `-i IMAGE_NAME` | Full image name (registry/repository:tag) | quay.io/kruize/optimizer:0.1 |
| `-t TAG` | Image tag (used if -i not provided) | 0.1 |
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

Build and push with a specific tag:

```bash
./build_and_push.sh -t 1.0.0
```

This creates: `quay.io/kruize/optimizer:1.0.0`

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

## Examples

### Example 1: Local Development Build

Build locally for testing without pushing:

```bash
./build_and_push.sh -t dev -p false -l linux/amd64
```

### Example 2: Release Build

Build and push a release version:

```bash
./build_and_push.sh -t 1.0.0
```

### Example 3: Custom Registry

Build and push to a custom registry:

```bash
./build_and_push.sh -i myregistry.com/kruize/optimizer:latest
```

### Example 4: Build Only for Testing

Build without pushing to test changes:

```bash
./build_and_push.sh -b true -p false
```


## Manual Build

If you prefer to build manually without the script:

### Using Maven with Quarkus Jib

```bash
# Build only
./mvnw clean package \
  -Dquarkus.container-image.build=true \
  -Dquarkus.container-image.push=false

# Build and push
./mvnw clean package \
  -Dquarkus.container-image.build=true \
  -Dquarkus.container-image.image=quay.io/kruize/optimizer:1.0.0 \
  -Dquarkus.container-image.push=true \
  -Dquarkus.jib.platforms=linux/amd64,linux/arm64
```

### Using Docker Directly

```bash
# Build JVM image
docker build -f src/main/docker/Dockerfile.jvm -t kruize-optimizer:latest .

# Build native image (requires GraalVM)
docker build -f src/main/docker/Dockerfile.native -t kruize-optimizer:native .
```


---

**Last Updated**: 2026-05-05