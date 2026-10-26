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
    name: kruize-optimizer-service
    version: 1.0.0

  # OpenAPI Configuration
  smallrye-openapi:
    path: /openapi

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

  # REST Client Configuration
  rest-client:
    "com.kruize.optimizer.client.KruizeClient":
      url: ${kruize.url}

# Kruize Optimizer Configuration
kruize:
  # URL of the Kruize Autotune service
  url: ${KRUIZE_URL:http://kruize:8080}

  # Startup initialization (set to false to skip auto-initialization)
  startup:
    initialize: true

  state:
    refresh:
      # Interval to refresh state and install missing profiles
      interval: ${KRUIZE_STATE_REFRESH_INTERVAL:60m}

  webhook:
    # Webhook URL for bulk API callbacks
    url: ${KRUIZE_WEBHOOK_URL:http://kruize-optimizer:8080/webhook}

  target:
    labels:
      # Max number of labels to process
      limit: ${KRUIZE_TARGET_LABEL_LIMIT:1}
      # Pass labels as a JSON String (Object or Array of "key=value")
      json: ${KRUIZE_TARGET_LABELS}

  defaults:
    # Default datasource name
    datasource: ${KRUIZE_DEFAULT_DATASOURCE:prometheus-1}
    # Default metadata profile name
    metadata-profile: ${KRUIZE_DEFAULT_METADATA_PROFILE:cluster-metadata-local-monitoring}
    # Default metric profile name
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

# Override state refresh interval
export KRUIZE_STATE_REFRESH_INTERVAL=30m
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

## Bulk Config Configuration

Bulk job scheduling is **config-driven** rather than using global environment variables. Each `BulkConfig` object stored in Kruize defines its own scheduling interval, target namespaces, labels, and experiment settings.

### How It Works

1. On startup, `ConfigTimerManager` fetches all enabled `BulkConfig` objects from Kruize via `BulkConfigService`
2. A per-config timer is created based on each config's `recommendation_settings.scheduling` value
3. When a timer fires, `BulkConfigService` converts the config into a bulk job request and submits it to Kruize
4. Configs can be updated at runtime via the `POST /webhook/config/bulk` endpoint without requiring a restart

### Local Bulk Config Profiles

Default bulk config profiles are bundled with the application:

**Location**: `src/main/resources/configs/v1.0/bulk-configs/`

These are registered in [`configsReferenceIndex.json`](../src/main/resources/configs/configsReferenceIndex.json) under the `bulk_configs` section and are automatically installed into Kruize on startup if missing.

The `recommendation_settings.scheduling` field in each bulk config profile supports human-readable duration strings:

| Format  | Examples                    |
|---------|-----------------------------|
| Hours   | `24h`, `1hr`, `2hours`      |
| Minutes | `15m`, `30min`, `45minutes` |
| Days    | `1d`, `2day`, `7days`       |

### BulkConfig Fields

| Field                     | Type     | Description                                               |
|---------------------------|----------|-----------------------------------------------------------|
| `config_name`             | string   | Unique name for the config                                |
| `cluster_name`            | string   | Target cluster name                                       |
| `datasources`             | string[] | List of datasource names                                  |
| `namespaces`              | string[] | Target namespaces to filter                               |
| `labels`                  | object   | Label key-value pairs to filter workloads                 |
| `experiment_types`        | string[] | Experiment types (e.g., "container", "namespace")         |
| `metadata_profile`        | string   | Metadata profile name                                     |
| `performance_profile`     | string   | Performance profile name                                  |
| `trial_settings`          | object   | Trial settings (`measurement_duration`)                   |
| `recommendation_settings` | object   | Recommendation settings (`scheduling`, `terms`, `models`) |
| `webhook_url`             | string   | Webhook URL override                                      |
| `enabled`                 | boolean  | Whether the config is active                              |

## Deployment Configuration

The deployment is managed via Kustomize with environment-specific overlays:

- **Base**: [`deployment/base/deployment.yaml`](../deployment/base/deployment.yaml)
- **Kind overlay**: [`deployment/overlays/kind/`](../deployment/overlays/kind/) — deploys to `monitoring` namespace, uses `prometheus-1` datasource
- **OpenShift overlay**: [`deployment/overlays/openshift/`](../deployment/overlays/openshift/) — deploys to `openshift-tuning` namespace, uses `thanos-1` datasource

See the [Installation Guide](installation.md) for deployment instructions.

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
| `KRUIZE_STARTUP_INITIALIZE` | true | Enable/disable automatic initialization on startup |
| `KRUIZE_STATE_REFRESH_INTERVAL` | 60m | Interval to refresh state and install missing profiles |
| `KRUIZE_WEBHOOK_URL` | http://kruize-optimizer:8080/webhook | Webhook URL for bulk API callbacks |
| `KRUIZE_TARGET_LABEL_LIMIT` | 1 | Max number of labels to process |
| `KRUIZE_TARGET_LABELS` | {"ibm.runtimes.monitoring.enabled": "true"} | Labels as JSON String (Object or Array of "key=value") |
| `KRUIZE_DEFAULT_DATASOURCE` | prometheus-1 | Default datasource name |
| `KRUIZE_DEFAULT_METADATA_PROFILE` | cluster-metadata-local-monitoring | Default metadata profile name |
| `KRUIZE_DEFAULT_METRIC_PROFILE` | resource-optimization-local-monitoring | Default metric profile name |

### Setting Environment Variables

#### In Deployment YAML

```yaml
env:
- name: KRUIZE_URL
  value: "http://kruize-service.monitoring:8080"
- name: KRUIZE_STATE_REFRESH_INTERVAL
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

**Last Updated**: 2026-10-25