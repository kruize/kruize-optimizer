# Quick Start Guide - Kruize Optimizer E2E Tests

Get started with E2E testing in 5 minutes!

## Prerequisites Check

```bash
# Check Python version (need 3.8+)
python --version

# Check Docker
docker ps

# Check kubectl
kubectl version --client

# Check Kind
kind version

# Check Git
git --version
```

## Installation

```bash
# 1. Navigate to E2E test directory
cd tests/e2e

# 2. Install Python dependencies
pip install -r requirements.txt
```

## Run Tests

### Option 1: Kind Cluster (Recommended for local testing)
```bash
python run_e2e_tests.py --cluster-type kind --mode operator
```

This will:
- ✅ Create a Kind cluster
- ✅ Clone autotune and benchmarks repos
- ✅ Deploy Prometheus
- ✅ Deploy kruize-operator
- ✅ Deploy sysbench and tfb benchmarks
- ✅ Run 42 E2E tests
- ✅ Generate HTML test report
- ✅ Clean up everything

**Expected Duration:** 10-15 minutes

### Option 2: OpenShift Cluster
```bash
# Make sure you're logged into OpenShift
oc login

# Run tests
python run_e2e_tests.py --cluster-type openshift --mode operator
```

### Option 3: Keep Cluster for Debugging
```bash
python run_e2e_tests.py --cluster-type kind --mode operator --skip-cleanup
```

## What Gets Tested?

### ✅ Complete Workflow (10 tests)
- Cluster setup and accessibility
- Operator deployment
- Database initialization
- Service availability
- Benchmark deployment

### ✅ Profiles (10 tests)
- Metric profile installation
- Metadata profile installation
- Layer installation
- Profile verification

### ✅ Bulk Jobs (11 tests)
- Job triggering
- Webhook callbacks
- Experiment auto-creation
- Recommendation generation

### ✅ Webhooks (11 tests)
- Invalid payloads
- Missing fields
- Error handling
- Response validation

**Total: 42 tests**

## View Results

After tests complete, open the HTML report:
```bash
open test-report-kind-operator.html
```

## Common Issues

### Issue: Docker not running
```bash
# Start Docker Desktop or Docker daemon
```

### Issue: Kind cluster already exists
```bash
# Delete existing cluster
kind delete cluster --name kruize-test

# Run tests again
python run_e2e_tests.py --cluster-type kind --mode operator
```

### Issue: Port already in use
```bash
# Kill existing port-forwards
pkill -f "kubectl port-forward"

# Run tests again
python run_e2e_tests.py --cluster-type kind --mode operator
```

### Issue: Tests fail
```bash
# Keep cluster running for debugging
python run_e2e_tests.py --cluster-type kind --mode operator --skip-cleanup

# Check logs
kubectl logs -l app=kruize-optimizer -n monitoring --tail=100

# Check pods
kubectl get pods -n monitoring
kubectl get pods -n default
```

## Next Steps

- Read [README.md](README.md) for detailed documentation
- Customize [config/test_config.yaml](config/test_config.yaml)
- Add your own tests in `tests/` directory
- Check [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md) for architecture details

## Quick Commands

```bash
# Run only specific test file
pytest tests/test_01_complete_workflow.py -v

# Run specific test
pytest tests/test_01_complete_workflow.py::test_cluster_accessible -v

# Run with more verbose output
pytest tests/ -vv

# Run and stop on first failure
pytest tests/ -x

# Run tests matching pattern
pytest tests/ -k "webhook" -v
```

## Configuration

Edit `config/test_config.yaml` to customize:

```yaml
# Change cluster name
kind_cluster_name: my-test-cluster

# Change namespace
namespace: my-namespace

# Use custom images
operator_image: quay.io/myorg/kruize-operator:dev
optimizer_image: quay.io/myorg/kruize-optimizer:dev

# Adjust wait times
optimizer_wait_duration: 180  # 3 minutes

# Skip TFB deployment
deploy_tfb: false
```

## Support

- 📖 Full docs: [README.md](README.md)
- 🐛 Issues: https://github.com/kruize/kruize-optimizer/issues
- 💬 Slack: #kruize on Kubernetes Slack