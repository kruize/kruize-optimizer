# Kruize Optimizer E2E Tests

Self-contained end-to-end test framework for Kruize Optimizer that handles complete deployment and testing workflow.

## Overview

This E2E test framework:
- ✅ Clones required repositories into [`tests/e2e/.repos`](tests/e2e/.repos)
- ✅ Uses sparse benchmark checkout for sysbench manifests only
- ✅ Creates Kubernetes cluster (Kind/OpenShift)
- ✅ Deploys Prometheus for monitoring
- ✅ Enables cluster monitoring immediately after Prometheus installation
- ✅ Deploys sysbench workload
- ✅ Deploys using operator mode or manifest mode
- ✅ Runs comprehensive E2E tests
- ✅ Cleans up all resources

**No external script dependencies** - everything is handled in Python!

## Architecture

```
tests/e2e/
├── run_e2e_tests.py          # Main test runner
├── config/
│   ├── test_config.yaml      # Test configuration
│   └── kind-config.yaml      # Kind cluster config
├── utils/
│   ├── deployment_manager.py # Deployment orchestration
│   ├── cluster_utils.py      # Kubernetes operations
│   ├── kruize_utils.py       # API clients
│   └── log_utils.py          # Log parsing
└── tests/
    ├── test_01_complete_workflow.py  # 10 tests
    ├── test_02_profiles.py           # 10 tests
    ├── test_03_bulk_jobs.py          # 11 tests
    └── test_04_webhook.py            # 11 tests
```

## Prerequisites

### System Requirements
- Python 3.8+
- Docker
- kubectl
- Kind (for Kind cluster) or OpenShift CLI (for OpenShift)
- Git

### Python Dependencies
```bash
pip install -r requirements.txt
```

Required packages:
- pytest
- pytest-html
- requests
- pyyaml
- kubernetes

## Quick Start

### Run E2E Tests on Kind Cluster (Operator Mode)
```bash
cd tests/e2e
python run_e2e_tests.py --cluster-type kind --mode operator
```

### Run E2E Tests on OpenShift (Operator Mode)
```bash
python run_e2e_tests.py --cluster-type openshift --mode operator
```

### Skip Cleanup (for debugging)
```bash
python run_e2e_tests.py --cluster-type kind --mode operator --skip-cleanup
```

## Configuration

Edit [`tests/e2e/config/test_config.yaml`](tests/e2e/config/test_config.yaml) to customize:

```yaml
cluster:
  type: kind
  name: kruize-e2e-test
  namespace: monitoring

workload:
  name: test-sysbench
  namespace: default
  image: quay.io/kruize/sysbench:latest

images:
  kruize_operator: quay.io/kruize/kruize-operator:latest
  kruize_optimizer: quay.io/kruize/kruize-optimizer:0.0.1
  kruize: quay.io/kruize/autotune_operator:latest
  kruize_ui: quay.io/kruize/kruize-ui:latest
```

## Test Suites

### 1. Complete Workflow Tests (`test_01_complete_workflow.py`)
Tests the entire deployment and initialization workflow:
- ✅ Cluster accessibility
- ✅ Namespace creation
- ✅ Operator deployment
- ✅ Database initialization
- ✅ Kruize service availability
- ✅ Optimizer service availability
- ✅ Benchmark deployment
- ✅ Health checks
- ✅ Service endpoints
- ✅ Pod status verification

### 2. Profile Tests (`test_02_profiles.py`)
Tests profile installation and verification:
- ✅ Metric profile installation
- ✅ Metadata profile installation
- ✅ Layer installation
- ✅ Profile listing via API
- ✅ Profile verification in logs
- ✅ Default profiles loaded
- ✅ Custom profiles support

### 3. Bulk Job Tests (`test_03_bulk_jobs.py`)
Tests bulk job triggering and webhook workflow:
- ✅ Bulk job triggering
- ✅ Webhook callback handling
- ✅ Experiment auto-creation
- ✅ Workload monitoring
- ✅ Job status tracking
- ✅ Experiment validation
- ✅ Recommendation generation
- ✅ End-to-end workflow

### 4. Webhook Tests (`test_04_webhook.py`)
Tests webhook negative scenarios:
- ✅ Invalid JSON payload
- ✅ Null payload
- ✅ Missing required fields
- ✅ Invalid data types
- ✅ Empty arrays
- ✅ Malformed requests
- ✅ Error handling
- ✅ Response validation

## Deployment Modes

### Operator Mode (Default)
Deploys using kruize-operator which manages all components:
- Kruize database (PostgreSQL)
- Kruize service
- Kruize optimizer
- Kruize UI

```bash
python run_e2e_tests.py --mode operator
```

### Manifest Mode
Deploys Kruize via the autotune manifest flow and deploys optimizer separately using this project's kustomize files:
```bash
python run_e2e_tests.py --mode manifest
```

## Cluster Types

### Kind (Default)
Local Kubernetes cluster using Docker:
```bash
python run_e2e_tests.py --cluster-type kind
```

### OpenShift
Red Hat OpenShift cluster:
```bash
python run_e2e_tests.py --cluster-type openshift
```

### Minikube
Local Kubernetes cluster using VM:
```bash
python run_e2e_tests.py --cluster-type minikube
```

## Workflow

The E2E test runner follows this workflow:

1. **Setup Phase**
   - Clone autotune repository into [`tests/e2e/.repos`](tests/e2e/.repos)
   - Sparse checkout only the sysbench benchmark manifests into [`tests/e2e/.repos`](tests/e2e/.repos)
   - Create Kubernetes cluster (Kind/OpenShift)
   - Create namespaces

2. **Deployment Phase**
   - Deploy Prometheus monitoring
   - Enable monitoring immediately after Prometheus installation
   - Deploy sysbench workload
   - Label sysbench for auto-experiment creation
   - Deploy via operator mode, or deploy Kruize plus optimizer in manifest mode
   - Wait for all required pods to be ready

3. **Port Forward Phase** (Kind only)
   - Setup port-forward for kruize service (8080)
   - Setup port-forward for optimizer service (8081)

4. **Wait Phase**
   - Wait for optimizer to create experiments (configurable, default 120s)

5. **Test Phase**
   - Run pytest test suites
   - Generate HTML test report

6. **Cleanup Phase**
   - Terminate port-forward processes
   - Delete Kubernetes cluster (Kind)
   - Remove cloned repositories
   - Clean up temporary files

## Test Reports

After running tests, an HTML report is generated:
```
test-report-{cluster_type}-{mode}.html
```

Example:
- `test-report-kind-operator.html`
- `test-report-openshift-operator.html`

## Debugging

### View Logs
```bash
# Kruize logs
kubectl logs -l app=kruize -n monitoring

# Optimizer logs
kubectl logs -l app=kruize-optimizer -n monitoring

# Operator logs
kubectl logs deployment/kruize-operator -n monitoring
```

### Skip Cleanup
Keep cluster running after tests for debugging:
```bash
python run_e2e_tests.py --skip-cleanup
```

### Run Specific Tests
```bash
# Run only workflow tests
pytest tests/test_01_complete_workflow.py -v

# Run only webhook tests
pytest tests/test_04_webhook.py -v

# Run specific test
pytest tests/test_01_complete_workflow.py::test_cluster_accessible -v
```

## Troubleshooting

### Cluster Creation Fails
```bash
# Check Docker is running
docker ps

# Check Kind is installed
kind version

# Delete existing cluster
kind delete cluster --name kruize-test
```

### Prometheus Deployment Fails
```bash
# Check Prometheus pods
kubectl get pods -n monitoring

# Check Prometheus logs
kubectl logs -l app=prometheus -n monitoring
```

### Operator Deployment Fails
```bash
# Check operator logs
kubectl logs deployment/kruize-operator -n monitoring

# Check CRDs
kubectl get crd kruizes.kruize.io

# Check operator status
kubectl get deployment kruize-operator -n monitoring
```

### Port Forward Issues (Kind)
```bash
# Kill existing port-forwards
pkill -f "kubectl port-forward"

# Manually setup port-forward
kubectl port-forward service/kruize 8080:8080 -n monitoring
```

### Tests Fail
```bash
# Check all pods are running
kubectl get pods -n monitoring
kubectl get pods -n default

# Check services
kubectl get svc -n monitoring

# Check logs
kubectl logs -l app=kruize-optimizer -n monitoring --tail=100
```

## CI/CD Integration

### GitHub Actions Example
```yaml
name: E2E Tests

on: [push, pull_request]

jobs:
  e2e-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.9'
      
      - name: Install dependencies
        run: |
          pip install -r tests/e2e/requirements.txt
      
      - name: Run E2E tests
        run: |
          cd tests/e2e
          python run_e2e_tests.py --cluster-type kind --mode operator
      
      - name: Upload test report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-report
          path: tests/e2e/test-report-*.html
```

## Development

### Adding New Tests

1. Create test file in `tests/` directory
2. Use pytest fixtures for setup/teardown
3. Use utility classes from `utils/` for common operations
4. Follow naming convention: `test_XX_description.py`

Example:
```python
import pytest
from utils.kruize_utils import OptimizerAPIClient

def test_new_feature(optimizer_client):
    """Test new optimizer feature"""
    response = optimizer_client.get_status()
    assert response.status_code == 200
```

### Adding New Deployment Steps

Edit `utils/deployment_manager.py` to add new deployment logic:
```python
def deploy_new_component(self):
    """Deploy new component"""
    logger.info("Deploying new component...")
    # Add deployment logic
```

## Contributing

1. Fork the repository
2. Create feature branch
3. Add tests for new features
4. Ensure all tests pass
5. Submit pull request

## License

Apache License 2.0

## Support

For issues and questions:
- GitHub Issues: https://github.com/kruize/kruize-optimizer/issues
- Slack: #kruize on Kubernetes Slack
