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
- [Examples](#examples)

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

**Note**: The Kruize Optimizer service primarily provides the webhook endpoint for receiving bulk API callbacks. For complete API documentation of Kruize endpoints (datasources, jobs, profiles, layers, status), please refer to the Kruize service documentation. (https://github.com/kruize/autotune/tree/master/design). All other endpoints currently exposed in optimizer will be removed in next release. 

---

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

## Support

For API support and questions:
- GitHub Issues: [kruize/kruize-optimizer/issues](https://github.com/kruize/kruize-optimizer/issues)
- Documentation: [docs/README.md](README.md)

---

**Last Updated**: 2026-05-05