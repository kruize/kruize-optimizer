# E2E Test Framework for kruize-optimizer

Python-based e2e test framework that replicates the exact behavior of the bash demo scripts (`optimizer_demo.sh`, `common.sh`, `common_helper.sh`) with pure Python replacements for sed/awk/grep.

## Features

- ✅ **Exact flow replication** - Matches bash demo scripts step-by-step
- ✅ **Pure Python** - Replaces sed/awk/grep with Python (YAML parsing, regex, etc.)
- ✅ **Dual install modes** - Manifest mode (default) and operator mode
- ✅ **Multi-cluster support** - kind and OpenShift
- ✅ **Proper cleanup** - Pre-test cleanup with `-f` flag, post-test teardown
- ✅ **Runtime repo cloning** - Clones autotune, benchmarks, kruize-operator dynamically

## Installation

### Create and activate virtual environment

```bash
cd tests/e2e

# Create virtual environment
python3 -m venv venv

# Activate virtual environment
# On Linux/macOS:
source venv/bin/activate

# On Windows:
# venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
```

**Note**: Always activate the virtual environment before running tests.

## Quick Start

### Run with existing kind cluster

```bash
pytest tests/ --cluster-type kind --install-mode manifest
```

**Note**: 
- Console shows **only** clean step progress by default (e.g., "[1/16] Cloning... ✅")
- Detailed logs (kubectl output, script output) are **ALWAYS** written to `tests/e2e/results/e2e-test-<timestamp>.log`
- Pass `--verbose-output` to also show detailed logs on console
- Cloned repositories are stored in `tests/e2e/cloned_repos/`

### Full setup from scratch (creates kind cluster + everything)

```bash
pytest tests/ --cluster-type kind --install-mode manifest --env-setup
```

### OpenShift

```bash
pytest tests/ --cluster-type openshift --install-mode manifest
```

## Command Line Options

| Option | Default | Description |
|--------|---------|-------------|
| `--cluster-type` | `kind` | Cluster type: `kind`, `minikube`, `openshift` |
| `--install-mode` | `manifest` | Install mode: `manifest` (default), `operator` |
| `--env-setup` | `False` | Create cluster from scratch (equiv to `-f` flag) |
| `--app-namespace` | `default` | Namespace for benchmarks |
| `--kruize-image` | (auto) | Custom kruize image |
| `--kruize-ui-image` | (auto) | Custom kruize UI image |
| `--kruize-operator-image` | (auto) | Custom kruize operator image |
| `--kruize-optimizer-image` | `quay.io/kruize/kruize-optimizer:0.0.1` | Custom optimizer image |
| `--skip-setup` | `False` | Use existing deployment, skip setup |
| `--skip-teardown` | `False` | Leave resources running after tests |
| `--working-dir` | (temp) | Working directory for cloned repos |
| `--optimizer-wait-duration` | `120` | Wait time for optimizer (seconds) |

## Usage Examples

### Skip setup (use existing deployment)

```bash
pytest tests/ --skip-setup --cluster-type kind
```

### Skip teardown (leave resources for debugging)

```bash
pytest tests/ --skip-teardown
```

### Custom images

```bash
pytest tests/ \
  --kruize-image quay.io/myrepo/autotune:dev \
  --kruize-optimizer-image quay.io/myrepo/optimizer:dev
```

### Run specific test categories

```bash
# Only smoke tests (pod health checks)
pytest tests/ -m smoke --cluster-type kind

# Only API tests (profiles, datasources, experiments)
pytest tests/ -m api --cluster-type kind

# Run both smoke and API tests
pytest tests/ -m "smoke or api" --cluster-type kind
```

### Verbose output (console logging)

```bash
# Enable verbose mode to show detailed logs on BOTH console and file
pytest tests/ --verbose-output

# Without verbose, detailed logs only go to file (console shows only clean step progress)
pytest tests/  # Clean console UX (recommended)
```

### Terminate mode (cleanup only)

```bash
# Cleanup everything and exit (no tests run)
pytest tests/ --terminate
```

### Custom working directory and wait duration

```bash
# Use custom directory for cloned repos (default: tests/e2e/cloned_repos)
pytest tests/ \
  --working-dir /tmp/e2e-workdir \
  --optimizer-wait-duration 180
```

## Setup Flow (Manifest Mode)

The framework replicates the exact `optimizer_demo_setup` flow:

1. **Clone repos** - autotune (mvp_demo), benchmarks
2. **Check existing deployment** - operator, kruize, optimizer pods
3. **Pre-test cleanup** - if resources exist
4. **Environment setup** - kind cluster + prometheus (if `--env-setup`)
5. **Install sysbench** - benchmark in app namespace
6. **Label sysbench** - `kruize/autotune=enabled`
7. **Enable kube state metrics** - via autotune scripts
8. **Install TFB** - benchmark + label
9. **Kruize local patch** - set `"local": "true"` in manifests
10. **Install Kruize** - via `deploy.sh` from autotune
11. **Install kruize-optimizer** - via kustomize
12. **Verify optimizer pod** - running status
13. **Port forward** - (kind only)
14. **Wait for optimizer** - 120s for auto-experiment creation

## Tests

The framework implements tests in the following order:

### 1. `test_kruize_pod.py` - Pod Health (2 tests)
- ✅ Kruize pod exists and is Running
- ✅ All kruize containers are ready

### 2. `test_optimizer_pod.py` - Pod Health (2 tests)
- ✅ Optimizer pod exists and is Running
- ✅ All optimizer containers are ready

### 3. `test_kruize_api.py` - API Validation (4 tests)
- ✅ Metric profiles loaded (validates `resource-optimization-local-monitoring` from configsReferenceIndex.json)
- ✅ Metadata profiles loaded (validates `cluster-metadata-local-monitoring` from configsReferenceIndex.json)
- ✅ Datasource configured correctly (kind: `prometheus-1`, openshift: `thanos-1`)
- ✅ Sysbench experiment auto-created (verifies optimizer auto-discovery of labeled workloads)

**Total: 8 tests** (2 smoke + 4 API validation)

## Directory Structure

```
tests/e2e/
├── conftest.py          # Pytest fixtures, session-scoped setup
├── pytest.ini           # Pytest configuration
├── requirements.txt     # Python dependencies
├── README.md            # This file
├── config/
│   └── settings.py      # Configuration dataclass
├── utils/
│   ├── kubectl.py       # Kubectl/oc wrapper
│   ├── git_ops.py       # Git clone/checkout
│   ├── process.py       # Subprocess helpers
│   ├── file_ops.py      # Python sed/awk replacements
│   ├── wait.py          # Polling utilities
│   └── http_client.py   # Kruize API client
├── setup/
│   ├── cluster.py       # Cluster lifecycle
│   ├── repos.py         # Repository cloning
│   ├── prometheus.py    # Prometheus install
│   ├── benchmarks.py    # Benchmark install/uninstall
│   ├── kruize.py        # Kruize install (manifest mode)
│   ├── optimizer.py     # Optimizer install
│   ├── operator.py      # Operator mode (TODO)
│   ├── port_forward.py  # Port forwarding
│   └── cleanup.py       # Cleanup orchestration
├── tests/
│   ├── test_kruize_pod.py     # Kruize pod tests
│   └── test_optimizer_pod.py  # Optimizer pod tests
├── cloned_repos/        # Working directory for cloned repos (auto-created)
│   ├── autotune/
│   ├── benchmarks/
│   ├── kruize-operator/
│   └── kruize-optimizer/
└── results/             # Test results and logs (auto-created)
    └── e2e-test-<timestamp>.log
```

**Note**: The `cloned_repos/` and `results/` directories are auto-created and gitignored.

## Python Replacements for Bash Tools

| Bash | Python Module | Function |
|------|---------------|----------|
| `sed -i 's/.../.../` | `file_ops.py` | `replace_in_file()` |
| `sed` (YAML patching) | `file_ops.py` | `patch_yaml_file()` (uses PyYAML) |
| `grep \| awk` (pom.xml) | `file_ops.py` | `read_version_from_pom()` |
| `grep \| awk` (Makefile) | `file_ops.py` | `read_version_from_makefile()` |
| `sed` (deployment image) | `file_ops.py` | `update_deployment_image()` |
| `awk` (CR block removal) | `file_ops.py` | `remove_cr_blocks()` |
| `kubectl` | `kubectl.py` | `KubectlClient` class |
| `git clone` | `git_ops.py` | `clone_repo()` |
| `curl` (Kruize API) | `http_client.py` | `KruizeAPIClient` class |

## Troubleshooting

### Port conflicts (kind)

If ports 8080/8081 are in use, the framework will fail. Kill any processes using these ports:

```bash
lsof -ti :8080 | xargs kill
lsof -ti :8081 | xargs kill
```

### Cluster not accessible

Ensure your cluster is running:

```bash
# For kind
kind get clusters

# For OpenShift
oc cluster-info
```

### Cleanup failed

Manually clean up resources:

```bash
# Delete kind cluster
kind delete clusters --all

# Or for specific namespace
kubectl delete namespace monitoring --force --grace-period=0

# Clean up working directories
rm -rf tests/e2e/cloned_repos/
rm -rf tests/e2e/results/
```

## Known Limitations

1. **Operator mode**: Not yet implemented (manifest mode is default and working)
2. **Timing sensitivity**: The 120s optimizer wait is a workaround, may need adjustment
3. **macOS/Linux differences**: Python eliminates most sed/awk portability issues

## Adding New Tests

1. Create a new test file in `tests/`
2. Use the fixtures provided by `conftest.py`:
   - `config` - Configuration object
   - `kubectl` - KubectlClient instance
   - `k8s_client` - Kubernetes Python client
   - `namespace` - Target namespace
   - `kruize_url` - Kruize API URL

Example:

```python
import pytest

@pytest.mark.e2e
def test_my_feature(k8s_client, namespace):
    pods = k8s_client.list_namespaced_pod(namespace=namespace)
    assert len(pods.items) > 0
```

## Contributing

When modifying the framework:

1. **Preserve exact flow** - Match bash script behavior step-by-step
2. **No subprocess for file ops** - Use Python (PyYAML, regex, etc.)
3. **Log everything** - Use `logger.info()` for key steps
4. **Update this README** - Document new options/tests

## References

- Bash scripts: `tmp/kruize-demos/monitoring/local_monitoring/`
- Kruize deployment: `deployment/overlays/{kind|openshift}/`
- Experiment templates: `tmp/kruize-demos/monitoring/local_monitoring/experiments/`
