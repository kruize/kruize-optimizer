# Configuration Guide

This guide describes all configurable options available in Kruize Optimizer and how to customize them for your environment.

## Table of Contents

- [Overview](#overview)
- [Application Configuration](#application-configuration)
- [Profile Configuration](#profile-configuration)
- [Layer Configuration](#layer-configuration)
- [Deployment Configuration](#deployment-configuration)
- [Environment Variables](#environment-variables)
- [Configuration Best Practices](#configuration-best-practices)

## Overview

Kruize Optimizer uses a combination of configuration files, environment variables, and runtime profiles to provide flexible customization options. Configuration can be modified at different levels:

1. **Application Level**: Core application settings
2. **Profile Level**: Metadata and metric profiles
3. **Layer Level**: Optimization layer configurations
4. **Deployment Level**: Kubernetes/OpenShift deployment settings

## Application Configuration

### Main Configuration File

The primary configuration file is located at [`src/main/resources/application.yml`](../src/main/resources/application.yml).

#### Key Configuration Sections

```yaml
# HTTP Server Configuration
quarkus:
  application:
    name: kruize-optimizer
    
  # Swagger UI Configuration
  swagger-ui:
    path: /swagger-ui
    always-include: ${ENABLE_SWAGGER:false}
    
  # Logging Configuration
  log:
    console:
      json:
        enabled: false
        pretty-print: false

# Kruize Optimizer Configuration
kruize:
  # URL of the Kruize Autotune service
  url: ${KRUIZE_URL:http://kruize:8080}
  
  state:
    refresh:
      interval: ${KRUIZE_STATE_REFRESH_INTERVAL:60m}
  
  bulk:
    scheduler:
      interval: ${KRUIZE_BULK_SCHEDULER_INTERVAL:15m}
      startup-delay: ${KRUIZE_BULK_SCHEDULER_STARTUP_DELAY:1m}
      measurement-duration: ${KRUIZE_BULK_MEASUREMENT_DURATION:15min}
  
  webhook:
    url: ${KRUIZE_WEBHOOK_URL:http://kruize-optimizer:8080/webhook}
  
  target:
    labels:
      limit: ${KRUIZE_TARGET_LABEL_LIMIT:1}
      json: ${KRUIZE_TARGET_LABELS}
  
  defaults:
    datasource: ${KRUIZE_DEFAULT_DATASOURCE:prometheus-1}
    metadata-profile: ${KRUIZE_DEFAULT_METADATA_PROFILE:cluster-metadata-local-monitoring}
    metric-profile: ${KRUIZE_DEFAULT_METRIC_PROFILE:resource-optimization-local-monitoring}
```

### Modifying Application Configuration

#### 1. Edit application.yml

```yaml
# Example: Change Kruize URL
kruize:
  url: http://kruize-service.monitoring:8080
```

#### 2. Using Environment Variables

```bash
# Override Kruize URL
export KRUIZE_URL=http://kruize-service.monitoring:8080

# Override bulk scheduler interval
export KRUIZE_BULK_SCHEDULER_INTERVAL=30m
```

## Profile Configuration

### Important Note

**After updating profiles, you must rebuild the Docker image** as profiles are bundled into the application at build time.

```bash
# Rebuild the image after profile changes
./build_and_push.sh -p false
```

See the [Build Guide](build.md) for detailed build instructions.

### Metadata Profiles

Metadata profiles define cluster and application metadata structure.

**Location**: `src/main/resources/configs/v1.0/metadata-profiles/`

**Design Documentation**: https://github.com/kruize/autotune/tree/master/design

#### Creating Custom Metadata Profiles

1. Create a new JSON file in the metadata-profiles directory
2. Follow the schema structure from existing profiles
3. Update the [`configsReferenceIndex.json`](../src/main/resources/configs/configsReferenceIndex.json) file
4. Rebuild the Docker image

### Metric Profiles

Metric profiles specify which metrics to collect and analyze.

**Location**: `src/main/resources/configs/v1.0/metric-profiles/`

**Design Documentation**: https://github.com/kruize/autotune/tree/master/design

#### Creating Custom Metric Profiles

1. Create a new JSON file in the metric-profiles directory
2. Follow the schema structure from existing profiles
3. Update the [`configsReferenceIndex.json`](../src/main/resources/configs/configsReferenceIndex.json) file
4. Rebuild the Docker image

## Layer Configuration

Layer configurations define parameters for different layers (container, JVM, framework).

**Location**: `src/main/resources/configs/layers/`

**Design Documentation**: https://github.com/kruize/autotune/tree/master/design

### Creating Custom Layers

1. Create a new JSON file in the layers directory
2. Follow the schema structure from existing layers
3. Update the [`configsReferenceIndex.json`](../src/main/resources/configs/configsReferenceIndex.json) file
4. Rebuild the Docker image

## Deployment Configuration

### Kubernetes Deployment

**Base Configuration**: `deployment/base/deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kruize-optimizer
spec:
  replicas: 1                    # Number of replicas
  selector:
    matchLabels:
      app: kruize-optimizer
  template:
    metadata:
      labels:
        app: kruize-optimizer
    spec:
      containers:
      - name: kruize-optimizer
        image: quay.io/kruize/kruize-optimizer:0.0.1
        ports:
        - containerPort: 8080
        env:
        - name: KRUIZE_URL
          value: "http://kruize:8080"
        # See Environment Variables section for all available options
```

### Kustomize Overlays

Overlays allow environment-specific customizations without modifying the base deployment.

#### Kind Overlay

**File**: `deployment/overlays/kind/kustomization.yaml`

The Kind overlay sets the namespace to `monitoring` and configures the default datasource for Prometheus:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: monitoring           # Deploy to monitoring namespace

resources:
- ../../base

patches:
  - patch: |-
      apiVersion: apps/v1
      kind: Deployment
      metadata:
        name: kruize-optimizer
      spec:
        template:
          spec:
            containers:
              - name: kruize-optimizer
                env:
                  - name: KRUIZE_DEFAULT_DATASOURCE
                    value: "prometheus-1"
    target:
      kind: Deployment
      name: kruize-optimizer
```

#### OpenShift Overlay

**File**: `deployment/overlays/openshift/kustomization.yaml`

The OpenShift overlay sets the namespace to `openshift-tuning` and configures the default datasource for Thanos:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: openshift-tuning     # Deploy to openshift-tuning namespace

resources:
- ../../base

patches:
  - patch: |-
      apiVersion: apps/v1
      kind: Deployment
      metadata:
        name: kruize-optimizer
      spec:
        template:
          spec:
            containers:
              - name: kruize-optimizer
                env:
                  - name: KRUIZE_DEFAULT_DATASOURCE
                    value: "thanos-1"
    target:
      kind: Deployment
      name: kruize-optimizer
```

**Key Differences:**
- **Kind**: Uses `prometheus-1` as default datasource, deploys to `monitoring` namespace
- **OpenShift**: Uses `thanos-1` as default datasource, deploys to `openshift-tuning` namespace

## Environment Variables

### Core Quarkus Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `QUARKUS_HTTP_PORT` | 8080 | HTTP server port |
| `QUARKUS_HTTP_HOST` | 0.0.0.0 | HTTP bind address |
| `QUARKUS_LOG_LEVEL` | INFO | Logging level (TRACE, DEBUG, INFO, WARN, ERROR) |
| `ENABLE_SWAGGER` | false | Enable Swagger UI |

### Kruize Optimizer Environment Variables

All environment variables used in Kruize Optimizer deployment:

| Variable | Default | Description |
|----------|---------|-------------|
| `KRUIZE_URL` | http://kruize:8080 | URL of the Kruize Autotune service |
| `KRUIZE_STATE_REFRESH_INTERVAL` | 60m | Interval to refresh state and install missing profiles |
| `KRUIZE_BULK_SCHEDULER_INTERVAL` | 15m | Interval to call bulk API (cron format or duration) |
| `KRUIZE_BULK_SCHEDULER_STARTUP_DELAY` | 1m | Startup delay before first bulk API call |
| `KRUIZE_BULK_MEASUREMENT_DURATION` | 15min | Measurement duration for bulk API calls |
| `KRUIZE_WEBHOOK_URL` | http://kruize-optimizer:8080/webhook | Webhook URL for bulk API callbacks |
| `KRUIZE_TARGET_LABEL_LIMIT` | 1 | Max number of labels to process |
| `KRUIZE_TARGET_LABELS` | {"kruize/autotune": "enabled"} | Labels as JSON String (Object or Array of "key=value") |
| `KRUIZE_DEFAULT_DATASOURCE` | prometheus-1 | Default datasource name |
| `KRUIZE_DEFAULT_METADATA_PROFILE` | cluster-metadata-local-monitoring | Default metadata profile name |
| `KRUIZE_DEFAULT_METRIC_PROFILE` | resource-optimization-local-monitoring | Default metric profile name |

### Setting Environment Variables

#### In Deployment YAML

```yaml
env:
- name: KRUIZE_URL
  value: "http://kruize-service.monitoring:8080"
- name: KRUIZE_BULK_SCHEDULER_INTERVAL
  value: "30m"
- name: KRUIZE_TARGET_LABELS
  value: '{"app": "myapp", "env": "production"}'
```


## Configuration Best Practices

1. **Use Environment Variables**: For environment-specific values like URLs and intervals
2. **Version Control**: Keep configuration files in version control
3. **Documentation**: Document custom configurations and changes
4. **Testing**: Test configuration changes in non-production environments first
5. **Validation**: Validate configuration before deployment
6. **Rebuild Images**: Remember to rebuild Docker images after modifying profiles or layers

## Configuration Validation

### Validate YAML Files

```bash
# Validate Kubernetes manifests
kubectl apply --dry-run=client -f deployment/base/deployment.yaml

# Validate with kustomize
kubectl kustomize deployment/overlays/kind/
```

---

**Last Updated**: 2026-05-05