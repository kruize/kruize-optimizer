# Optimizer API Reference

Complete API documentation for Kruize Optimizer REST endpoints.

## Table of Contents

- [Base URL](#base-url)
- [API Endpoints](#api-endpoints)
  - [Webhook API](#webhook-api)
  - [Jobs API](#jobs-api)
- [Kruize APIs](#kruize-apis)

## Base URL

```
http://localhost:8080
```

For production deployments, replace with your actual service URL. The OpenAPI specification is available at `/openapi`.

## API Endpoints

### Webhook API

The Kruize Optimizer service provides a webhook endpoint to receive bulk API callbacks from the Kruize service.

#### Receive Webhook

Process incoming webhook events containing bulk API results.

**Endpoint:** `POST /webhook`

**Request Body:**

The webhook receives an array of webhook payload objects containing job summaries and webhook status.

<details>
<summary><b>Example Request</b></summary>

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

</details>

<details>
<summary><b>Request Body Schema</b></summary>

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

</details>

<details>
<summary><b>Example Response</b></summary>

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

</details>

<details>
<summary><b>Example curl</b></summary>

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

</details>

---

#### Receive Config Update

Receive bulk config update notifications from Kruize, allowing the scheduler to adjust per-config timers without requiring a restart.

**Endpoint:** `POST /webhook/config/bulk`

**Request Body:**

A single BulkConfig object with at minimum a `config_name` field.

<details>
<summary><b>Example Request</b></summary>

```json
{
  "config_name": "my-config",
  "cluster_name": "production",
  "datasources": ["prometheus-1"],
  "namespaces": ["default", "app-ns"],
  "labels": {"ibm.runtimes.monitoring.enabled": "true"},
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

</details>

**Response:**

- **200 OK** - Config update processed successfully
- **400 Bad Request** - `config_name` is null or empty
- **500 Internal Server Error** - Processing error

<details>
<summary><b>Example curl</b></summary>

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

</details>

---

### Jobs API

#### Jobs Overview

Returns bulk job statistics tracked by the Optimizer.

**Endpoint:** `GET /jobs/overview`

<details>
<summary><b>Example Response</b></summary>

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

</details>

---

## Kruize APIs

The Optimizer proxies several Kruize Autotune APIs (datasources, metadata profiles, metric profiles, layers, status). For documentation on these APIs, refer to the [Kruize Autotune API documentation](https://github.com/kruize/autotune/tree/master/design).

---

**Previous**: [Configuration Guide](configurables.md) | **Next**: [Design Documentation](design.md) | [Back to docs](README.md)

**Last Updated**: 2026-11-04