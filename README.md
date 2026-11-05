# Kruize Optimizer

> ### Tag It | Forget It | Optimize It 

A lightweight Quarkus‑based microservice that automates configuration, experiment creation, profile validation, and health orchestration for Kruize Autotune, simplifying the experience for SREs and Platform Engineers.


## Prerequisites

- **Java 25** - Required to build the Docker image and run the application

## Deployment

### Deploy to Kind (local development)

```bash
kubectl apply -k deployment/overlays/kind
```

This deploys to the `monitoring` namespace.

### Deploy to OpenShift

```bash
kubectl apply -k deployment/overlays/openshift
```

This deploys to the `openshift-tuning` namespace.

For detailed installation instructions, see the [Installation Guide](docs/installation.md).

## Documentation

For detailed documentation, see the [docs/](docs/README.md) directory covering installation, configuration, API reference, architecture, and contributing guidelines.

