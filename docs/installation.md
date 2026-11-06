# Installation Guide

This guide provides detailed instructions for installing and deploying Kruize Optimizer in various environments.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Important Notes](#important-notes)
- [Installation Methods](#installation-methods)
  - [Kind Installation](#kind-installation)
  - [OpenShift Installation](#openshift-installation)
  - [Local Development Setup](#local-development-setup)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### System Requirements

- **Java**: Java 25 or higher (for building Kruize Optimizer)
- **Maven**: 3.6.0 or higher (for building from source)
- **Container Runtime**: Docker 20.10+ or Docker-compatible runtimes (Podman, containerd, etc.) for containerized deployment
- **Kubernetes**: 1.20+ or OpenShift 4.8+ (for cluster deployment)

### Required Tools

```bash
# Verify Java installation
java -version

# Verify Maven installation
mvn -version

# Verify container runtime installation
# For Docker:
docker --version

# For Podman (Docker-compatible alternative):
podman --version

# Verify kubectl installation (for Kubernetes)
kubectl version --client
```

## Important Notes

### Kruize Dependency

**IMPORTANT**: Kruize Optimizer requires Kruize to be already running in your cluster.

**Recommended Installation Methods:**

1. **Using Kruize Operator** (Version >= 0.0.5)
   - The Kruize Operator automatically installs the Optimizer
   - No need to install Optimizer separately
   - Repository: https://github.com/kruize/kruize-operator/

2. **Using Autotune**
   - Alternative installation method
   - Repository: https://github.com/kruize/autotune/

## Installation Methods

### Kind Installation

#### Using Kustomize

1. Deploy using the Kind overlay:
```bash
kubectl apply -k deployment/overlays/kind/
```

2. Verify the deployment:
```bash
kubectl get pods -l app=kruize-optimizer -n monitoring
kubectl get svc kruize-optimizer -n monitoring
```

#### Exposing the API Endpoint (Optional)

If you want to access the API from outside the cluster:

```bash
# Port forward to access locally
kubectl port-forward -n monitoring svc/kruize-optimizer 8080:8080

# Verify the API is accessible by checking the OpenAPI specification
curl http://localhost:8080/openapi
# Or access it in your browser at http://localhost:8080/openapi
```

> **Note**: Some browsers may download the OpenAPI specification as a file instead of rendering it inline. This is normal behavior and indicates the endpoint is working correctly. We will add support for the health and readiness check endpoints in the upcoming release.

### OpenShift Installation

1. Log in to your OpenShift cluster:
```bash
oc login <cluster-url>
```

2. Deploy using the OpenShift overlay:
```bash
oc apply -k deployment/overlays/openshift/
```

3. Verify the deployment:
```bash
oc get pods -l app=kruize-optimizer -n openshift-tuning
oc get svc kruize-optimizer -n openshift-tuning
```

#### Exposing the API Endpoint (Optional)

If you want to expose the API endpoint externally:

```bash
# Create a route to expose the service
oc expose svc/kruize-optimizer -n openshift-tuning

# Get the route URL
oc get route kruize-optimizer -n openshift-tuning
```

The route URL will be displayed. Verify the API is accessible:

```bash
# Verify using the OpenAPI endpoint
curl http://<route-url>/openapi
# Or access it in your browser at http://<route-url>/openapi
```

### Local Development Setup

#### Building from Source

1. Clone the repository:
```bash
git clone https://github.com/kruize/kruize-optimizer.git
cd kruize-optimizer
```

2. Build the project:
```bash
# Using Maven wrapper (recommended)
./mvnw clean package

# Or using system Maven
mvn clean package
```

3. Configure Kruize URL:

Before running locally, update the Kruize URL in [`src/main/resources/application.yml`](../src/main/resources/application.yml):

```yaml
# Update this section with your Kruize instance URL
# Use different port-mapping for Kruize pod, as 8080 will be used by optimizer
kruize:
  url: http://localhost:9090  # Change to your Kruize URL
```

See the [Configuration Guide](configurables.md) for more details.

4. Run in development mode:
```bash
./mvnw quarkus:dev
```

The application will start on `http://localhost:8080` with hot reload enabled.

For running tests, see the [Contributing Guide](contributing.md).

## Troubleshooting

### Common Issues

#### Port Already in Use

If port 8080 is already in use, you can change it using environment variables.

**For Kubernetes/OpenShift deployments:**

Edit the deployment to change the `QUARKUS_HTTP_PORT` environment variable:

```yaml
env:
- name: QUARKUS_HTTP_PORT
  value: "9090"  # Change to your desired port
```

See [Environment Variables](configurables.md#environment-variables) for more details.

**For local development:**

```bash
# Set environment variable before running
export QUARKUS_HTTP_PORT=9090
./mvnw quarkus:dev
```

**To kill process using port 8080:**

```bash
# Find process using port 8080
lsof -i :8080

# Kill the process (replace PID with actual process ID)
kill -9 <PID>

# Or use a one-liner
kill -9 $(lsof -t -i:8080)
```

#### Pod Not Starting

**Check pod logs:**

```bash
# For Kubernetes
kubectl logs -l app=kruize-optimizer -n monitoring

# For OpenShift
oc logs -l app=kruize-optimizer -n openshift-tuning
```

**Describe pod for events:**

```bash
# For Kubernetes
kubectl describe pod -l app=kruize-optimizer -n monitoring

# For OpenShift
oc describe pod -l app=kruize-optimizer -n openshift-tuning
```

#### Connection to Kruize Failed

Ensure Kruize is running and accessible:

```bash
# Check if Kruize service is available
kubectl get svc -n monitoring
# Check if Kruize pod is running
kubectl get pods -n monitoring

# For OpenShift
oc get svc -n openshift-tuning
# Check if Kruize pod is running
kubectl get pods -n openshift-tuning
```

Update the Kruize URL in your configuration if needed. See [Configurables.md](configurables.md).

### Getting Help

If you encounter issues not covered here:

1. Check the [GitHub Issues](https://github.com/kruize/kruize-optimizer/issues)
2. Review application logs
3. Consult the [Configuration Guide](configurables.md)
4. Open a new issue with detailed information

---

**Next**: [Configuration Guide](configurables.md) | [Back to docs](README.md)

**Last Updated**: 2026-11-04