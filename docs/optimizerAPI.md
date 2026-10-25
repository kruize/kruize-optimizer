# Optimizer API Reference

Complete API documentation for Kruize Optimizer REST endpoints.

## Table of Contents

- [Base URL](#base-url)
- [API Documentation Tools](#api-documentation-tools)
- [Authentication](#authentication)
- [Response Format](#response-format)
- [Error Handling](#error-handling)
- [API Endpoints](#api-endpoints)
  - [Webhook API](#webhook-api)
  - [Datasource API](#datasource-api)
  - [Jobs API](#jobs-api)
  - [Metadata Profile API](#metadata-profile-api)
  - [Metric Profile API](#metric-profile-api)
  - [Layer API](#layer-api)
  - [Status API](#status-api)

## Base URL

```
http://localhost:8080
```

For production deployments, replace with your actual service URL.

## API Documentation Tools

Kruize Optimizer provides interactive API documentation through OpenAPI and Swagger UI.

### OpenAPI Specification

The OpenAPI specification is available at:

```
http://localhost:8080/openapi
```

This endpoint provides the complete API specification in OpenAPI 3.0 format, which can be used with various API tools and clients.

**Example:**

```bash
# View OpenAPI specification
curl http://localhost:8080/openapi

# Save to file
curl http://localhost:8080/openapi > kruize-optimizer-openapi.json
```

### Swagger UI (Development Mode)

Swagger UI provides an interactive interface to explore and test the API. It is available in development mode.

#### Enabling Swagger UI

**Development Mode (Automatic)**

When running in development mode, Swagger UI is automatically available:

```bash
./mvnw quarkus:dev
```

Access Swagger UI at: `http://localhost:8080/swagger-ui`

#### Using Swagger UI

Swagger UI provides:
- **Interactive API Explorer**: Test endpoints directly from the browser
- **Request/Response Examples**: See example payloads for each endpoint
- **Schema Documentation**: View data models and their properties
- **Try It Out**: Execute API calls with custom parameters

### Dev UI (Development Mode Only)

In development mode, Quarkus provides a comprehensive Dev UI:

```
http://localhost:8080/q/dev
```

The Dev UI includes:
- **API Explorer**: Browse and test all endpoints
- **Configuration Editor**: View and modify configuration
- **Metrics**: View application metrics
- **Build Information**: See build details and dependencies

## Authentication

Currently, the API does not require authentication.

## Response Format

All API responses follow a consistent JSON format:

### Success Response

```json
{
  "status": "success",
  "message": "Operation completed successfully",
  "data": {
    // Response data
  }
}
```

### Error Response

```json
{
  "status": "error",
  "message": "Error description",
  "code": "ERROR_CODE",
  "details": {
    // Additional error details
  }
}
```

## Error Handling

### HTTP Status Codes

- `200 OK` - Request succeeded
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request parameters
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

## API Endpoints

### Webhook API

The Kruize Optimizer service provides a webhook endpoint to receive bulk API callbacks from the Kruize service.

#### Receive Webhook

Process incoming webhook events containing bulk API results.

**Endpoint:** `POST /webhook`

**Request Body:**

The webhook receives an array of webhook payload objects containing job summaries and webhook status.

```json
[
  {
    "summary": {
      "jobID": "job-12345",
      "status": "completed",
      "total_experiments": 10,
      "processed_experiments": 10,
      "existing_experiments": 0
    },
    "webhook": {
      "status": "success"
    }
  }
]
```

**Request Body Schema:**

```json
{
  "type": "array",
  "items": {
    "type": "object",
    "properties": {
      "summary": {
        "type": "object",
        "properties": {
          "jobID": {
            "type": "string",
            "description": "Unique identifier for the job"
          },
          "status": {
            "type": "string",
            "description": "Status of the job (e.g., completed, failed)"
          },
          "total_experiments": {
            "type": "integer",
            "description": "Total number of experiments in the job"
          },
          "processed_experiments": {
            "type": "integer",
            "description": "Number of experiments successfully processed"
          },
          "existing_experiments": {
            "type": "integer",
            "description": "Number of experiments that already existed"
          }
        }
      },
      "webhook": {
        "type": "object",
        "properties": {
          "status": {
            "type": "string",
            "description": "Status of the webhook delivery"
          }
        }
      }
    }
  }
}
```

**Response:**

**Success (200 OK):**

```json
{
  "status": "success",
  "message": "Webhook processed successfully"
}
```

**Error (400 Bad Request):**

```json
{
  "status": "error",
  "message": "Invalid webhook payload",
  "details": "Missing required field: summary"
}
```

**Example:**

```bash
curl -X POST http://localhost:8080/webhook \
  -H "Content-Type: application/json" \
  -d '[
    {
      "summary": {
        "jobID": "job-12345",
        "status": "completed",
        "total_experiments": 10,
        "processed_experiments": 10,
        "existing_experiments": 0
      },
      "webhook": {
        "status": "success"
      }
    }
  ]'
```

**Use Case:**

This webhook endpoint is automatically called by the Kruize service when bulk API operations complete. It allows the Optimizer service to:
- Track bulk operation progress
- Update internal state based on job results
- Handle experiment processing results
- Monitor webhook delivery status

**Configuration:**

The webhook URL is configured via the `KRUIZE_WEBHOOK_URL` environment variable (default: `http://kruize-optimizer:8080/webhook`). See the [Configuration Guide](configurables.md#environment-variables) for details.

---

#### Receive Config Update

Receive bulk config update notifications from Kruize, allowing the scheduler to adjust per-config timers without requiring a restart.

**Endpoint:** `POST /webhook/config/bulk`

**Request Body:**

A single BulkConfig object with at minimum a `config_name` field.

```json
{
  "config_name": "my-config",
  "cluster_name": "production",
  "datasources": ["prometheus-1"],
  "namespaces": ["default", "app-ns"],
  "labels": {"kruize/autotune": "enabled"},
  "experiment_types": ["container", "namespace"],
  "metadata_profile": "cluster-metadata-local-monitoring",
  "recommendation_settings": {
    "scheduling": "24h",
    "terms": ["short_term"],
    "models": ["cost"]
  },
  "trial_settings": {
    "measurement_duration": "15min"
  },
  "enabled": true
}
```

**Response:**

- **200 OK** - Config update processed successfully
- **400 Bad Request** - `config_name` is null or empty
- **500 Internal Server Error** - Processing error

**Behavior:**

- If the config is **enabled** and has a valid `scheduling` value, the timer is created or updated
- If the config is **disabled** (`enabled: false`), any existing timer for that config is canceled
- If the scheduling interval is shortened, the next job executes immediately

**Example:**

```bash
curl -X POST http://localhost:8080/webhook/config/bulk \
  -H "Content-Type: application/json" \
  -d '{
    "config_name": "my-config",
    "recommendation_settings": {
      "scheduling": "12h"
    },
    "enabled": true
  }'
```

---

### Datasource API

#### List Datasources

Retrieves the list of configured datasources from Kruize.

**Endpoint:** `GET /kruize/datasources/list`

**Response (200 OK):**

```json
[
  {
    "name": "prometheus-1",
    "provider": "prometheus",
    "url": "http://prometheus:9090",
    "service_name": "prometheus",
    "namespace": "monitoring"
  }
]
```

**Example:**

```bash
curl http://localhost:8080/kruize/datasources/list
```

---

### Jobs API

#### Jobs Overview

Returns bulk job statistics tracked by the Optimizer.

**Endpoint:** `GET /jobs/overview`

**Response (200 OK):**

```json
{
  "jobs_triggered": 42,
  "total_experiments": 500,
  "processed_experiments": 480,
  "unique_experiments": 120
}
```

**Example:**

```bash
curl http://localhost:8080/jobs/overview
```

---

### Metadata Profile API

#### List Metadata Profiles

Retrieves metadata profiles from Kruize.

**Endpoint:** `GET /kruize/metadataProfiles/list`

**Response (200 OK):**

```json
[
  {
    "name": "cluster-metadata-local-monitoring",
    "profile_version": 1.0
  }
]
```

**Example:**

```bash
curl http://localhost:8080/kruize/metadataProfiles/list
```

#### Install Metadata Profiles

Installs missing metadata profiles from the Optimizer's local config into Kruize.

**Endpoint:** `POST /kruize/metadataProfiles/install`

**Response (200 OK):**

```json
{
  "status": "success",
  "message": "Missing metadata profiles installed"
}
```

---

### Metric Profile API

#### List Metric Profiles

Retrieves metric profiles from Kruize.

**Endpoint:** `GET /kruize/metricProfiles/list`

**Response (200 OK):**

```json
[
  {
    "name": "resource-optimization-local-monitoring",
    "profile_version": 1.0
  }
]
```

**Example:**

```bash
curl http://localhost:8080/kruize/metricProfiles/list
```

#### Install Metric Profiles

Installs missing metric profiles from the Optimizer's local config into Kruize.

**Endpoint:** `POST /kruize/metricProfiles/install`

**Response (200 OK):**

```json
{
  "status": "success",
  "message": "Missing metric profiles installed"
}
```

---

### Layer API

#### List Layers

Retrieves layer configurations from Kruize.

**Endpoint:** `GET /kruize/layers/list`

**Response (200 OK):**

```json
[
  {
    "name": "container"
  },
  {
    "name": "hotspot"
  }
]
```

**Example:**

```bash
curl http://localhost:8080/kruize/layers/list
```

#### Install Layers

Installs missing layer configurations from the Optimizer's local config into Kruize.

**Endpoint:** `POST /kruize/layers/install`

**Response (200 OK):**

```json
{
  "status": "success",
  "message": "Missing layers installed"
}
```

---

### Status API

#### System Status

Returns the aggregated system status including datasources, profiles, layers, and alerts.

**Endpoint:** `GET /kruize/status`

**Response (200 OK):**

```json
{
  "datasources": {
    "count": 1,
    "available": true
  },
  "profiles": {
    "metadata": 1,
    "metric": 1,
    "layers": 4
  },
  "alerts": []
}
```

**Example:**

```bash
curl http://localhost:8080/kruize/status
```

---

## Support

For API support and questions:
- GitHub Issues: [kruize/kruize-optimizer/issues](https://github.com/kruize/kruize-optimizer/issues)
- Documentation: [docs/README.md](README.md)

---

**Last Updated**: 2026-10-25