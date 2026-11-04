# Configuration Guide

This guide describes all configurable options available in Kruize Optimizer and how to customize them for your environment.

## Table of Contents

- [Overview](#overview)
- [Application Configuration](#application-configuration)
- [Profile Configuration](#profile-configuration)
- [Bulk Config Configuration](#bulk-config-configuration)
- [Environment Variables](#environment-variables)

## Overview

Kruize Optimizer uses a combination of configuration files, environment variables, and runtime profiles to provide flexible customization options. Configuration can be modified at different levels:

1. **Application Level**: Core application settings
2. **Profile Level**: Metadata and metric profiles

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

The Optimizer bundles default metadata and metric profiles that are automatically installed into Kruize on startup if missing.

- **Metadata Profiles**: `src/main/resources/configs/v1.0/metadata-profiles/`
- **Metric Profiles**: `src/main/resources/configs/v1.0/metric-profiles/`

For profile schema and design details, refer to the [Kruize Autotune documentation](https://github.com/kruize/autotune/tree/master/design).


## Bulk Config Configuration

Bulk job scheduling is **config-driven**. Each `BulkConfig` object stored in Kruize defines its own scheduling interval, target namespaces, labels, and experiment settings. The Optimizer fetches enabled configs on startup and schedules per-config timers automatically.

For bulk config fields, API details, and usage, refer to the [Kruize Autotune documentation](https://github.com/kruize/autotune/tree/master/design).

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
```

---

**Previous**: [Installation Guide](installation.md) | **Next**: [API Reference](optimizerAPI.md) | [Back to docs](README.md)

**Last Updated**: 2026-11-04