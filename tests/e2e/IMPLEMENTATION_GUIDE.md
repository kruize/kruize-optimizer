# E2E Test Implementation Guide

## Overview

This guide explains how to implement and run the E2E tests for kruize-optimizer. The tests deploy actual clusters and verify the complete workflow.

## Architecture Decision

**Chosen Approach: Shell Scripts + Python Tests**

### Why This Approach?

1. **Shell Scripts** - Reuse existing deployment logic from kruize-autotune repository
2. **Python Tests** - Better for API testing, log parsing, and complex assertions
3. **Pytest Framework** - Industry standard with excellent reporting

### What We DON'T Use

- **Pure Quarkus Tests** - Cannot deploy actual Kubernetes clusters
- **Pure Shell Scripts** - Difficult to write complex assertions and generate reports

## Implementation Steps

### Step 1: Setup Scripts (Shell)

Create these scripts in `tests/e2e/`:

#### `setup_cluster.sh`
```bash
#!/bin/bash
# Deploy Kind/OpenShift cluster and kruize-operator

CLUSTER_TYPE=${1:-kind}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Clone kruize-autotune repo for deployment scripts
if [ ! -d "kruize-autotune" ]; then
    git clone https://github.com/kruize/autotune.git kruize-autotune
fi

# Use the deployment scripts from kruize-autotune
cd kruize-autotune/deploy

# Deploy based on cluster type
if [ "$CLUSTER_TYPE" == "kind" ]; then
    ./deploy.sh -c kind -f -i <optimizer-image>
elif [ "$CLUSTER_TYPE" == "openshift" ]; then
    ./deploy.sh -c openshift -i <optimizer-image>
fi

# Wait for pods to be ready
kubectl wait --for=condition=Ready pod -l app=kruize-optimizer -n monitoring --timeout=300s
```

#### `teardown_cluster.sh`
```bash
#!/bin/bash
# Cleanup cluster and resources

CLUSTER_TYPE=${1:-kind}

cd kruize-autotune/deploy
./deploy.sh -t -c $CLUSTER_TYPE

# Delete Kind cluster if applicable
if [ "$CLUSTER_TYPE" == "kind" ]; then
    kind delete cluster --name kruize-e2e-test
fi
```

#### `run_e2e_tests.sh`
```bash
#!/bin/bash
# Main test runner

set -e

CLUSTER_TYPE=${1:-kind}
KEEP_CLUSTER=${2:-false}

echo "Setting up cluster..."
./setup_cluster.sh $CLUSTER_TYPE

echo "Running E2E tests..."
pytest tests/ -v --html=test_results/report.html

if [ "$KEEP_CLUSTER" != "true" ]; then
    echo "Cleaning up..."
    ./teardown_cluster.sh $CLUSTER_TYPE
fi
```

### Step 2: Python Test Implementation

The Python tests are already created:

- `test_01_deployment.py` - Verify operator and optimizer deployment
- `test_02_profiles.py` - Verify profile installation
- `test_03_bulk_jobs.py` - Verify bulk job triggering and webhook
- `test_04_webhook.py` - Negative webhook tests (COMPLETED ✓)

### Step 3: Test Execution Flow

```
1. setup_cluster.sh
   ├── Create Kind/OpenShift cluster
   ├── Deploy kruize-operator (using existing scripts)
   ├── Wait for optimizer pod ready
   └── Setup port-forwarding

2. pytest (Python tests)
   ├── test_01: Verify deployment
   │   ├── Check operator pod running
   │   ├── Check optimizer pod running
   │   └── Parse logs for initialization
   │
   ├── test_02: Verify profiles
   │   ├── Call Kruize listMetricProfiles API
   │   ├── Call Kruize listMetadataProfiles API
   │   ├── Call Kruize listLayers API
   │   └── Check optimizer logs for installation messages
   │
   ├── test_03: Verify bulk jobs
   │   ├── Get initial job count
   │   ├── Wait for job trigger (2-3 min)
   │   ├── Verify job count incremented
   │   ├── Wait for webhook callback
   │   └── Verify experiment counters updated
   │
   └── test_04: Webhook negative tests (COMPLETED ✓)
       ├── Invalid JSON → 400
       ├── Null payload → 400
       ├── Empty array → 400
       ├── Missing summary → 400
       ├── Null jobId → 400
       └── Valid payload → 200 (control)

3. teardown_cluster.sh
   └── Cleanup all resources
```

## Key Test Scenarios

### Test 01: Deployment Verification
```python
def test_operator_deployed(cluster_manager):
    # Check operator pod
    assert cluster_manager.wait_for_pod_ready("app=kruize-operator")
    
    # Check optimizer pod
    assert cluster_manager.wait_for_pod_ready("app=kruize-optimizer")
    
    # Get optimizer logs
    pod_name = cluster_manager.get_pod_name("app=kruize-optimizer")
    logs = cluster_manager.get_all_pod_logs(pod_name)
    
    # Verify initialization
    assert "Bulk scheduler initialized" in logs
```

### Test 02: Profile Installation
```python
def test_profiles_installed(kruize_client, cluster_manager):
    # Call Kruize APIs
    metric_profiles = kruize_client.list_metric_profiles()
    assert len(metric_profiles) > 0
    
    metadata_profiles = kruize_client.list_metadata_profiles()
    assert len(metadata_profiles) > 0
    
    layers = kruize_client.list_layers()
    assert len(layers) > 0
    
    # Check optimizer logs
    pod_name = cluster_manager.get_pod_name("app=kruize-optimizer")
    logs = cluster_manager.get_all_pod_logs(pod_name)
    
    assert "Installing metric profile" in logs or "Metric profile installed" in logs
```

### Test 03: Bulk Job Workflow
```python
def test_bulk_job_workflow(optimizer_client, cluster_manager):
    # Get initial state
    initial_jobs = optimizer_client.get_jobs_overview()
    initial_count = initial_jobs.get('jobsTriggered', 0)
    
    # Wait for job trigger (based on schedule)
    assert wait_for_job_trigger(optimizer_client, initial_count, timeout=180)
    
    # Verify in logs
    pod_name = cluster_manager.get_pod_name("app=kruize-optimizer")
    logs = cluster_manager.get_all_pod_logs(pod_name)
    assert "Calling bulk API" in logs
    
    # Wait for webhook callback
    initial_processed = initial_jobs.get('totalExperimentsProcessed', 0)
    assert wait_for_webhook_callback(optimizer_client, initial_processed, timeout=120)
```

### Test 04: Webhook Negative Tests (COMPLETED ✓)
See `tests/e2e/tests/test_04_webhook.py` for complete implementation.

## Running Tests

### Prerequisites
```bash
# Install Python dependencies
cd tests/e2e
pip install -r requirements.txt

# Ensure kubectl/oc and kind are installed
which kubectl
which kind
```

### Run All Tests
```bash
cd tests/e2e
./run_e2e_tests.sh kind
```

### Run Specific Test
```bash
cd tests/e2e
pytest tests/test_04_webhook.py -v
```

### Keep Cluster After Tests (for debugging)
```bash
./run_e2e_tests.sh kind true
```

## Test Output

Tests generate:
- **JUnit XML** - `test_results/junit.xml`
- **HTML Report** - `test_results/report.html`
- **Pod Logs** - `test_results/pod_logs/`
- **Test Logs** - `test_results/test_run_<timestamp>.log`

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
          cd tests/e2e
          pip install -r requirements.txt
      
      - name: Run E2E Tests
        run: |
          cd tests/e2e
          ./run_e2e_tests.sh kind
      
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: tests/e2e/test_results/
```

## Next Steps

1. **Complete test_01_deployment.py** - Deployment verification tests
2. **Complete test_02_profiles.py** - Profile installation tests
3. **Complete test_03_bulk_jobs.py** - Bulk job workflow tests
4. **Integrate with CI/CD** - Add to GitHub Actions
5. **Add more scenarios** - Edge cases, failure scenarios

## Summary

✅ **Completed:**
- E2E test framework structure
- Configuration files
- Utility modules (cluster, kruize, log)
- Webhook negative tests (test_04_webhook.py)
- Documentation

🔄 **In Progress:**
- Deployment tests (test_01)
- Profile tests (test_02)
- Bulk job tests (test_03)

📋 **TODO:**
- Shell scripts (setup/teardown/run)
- Complete remaining Python tests
- CI/CD integration