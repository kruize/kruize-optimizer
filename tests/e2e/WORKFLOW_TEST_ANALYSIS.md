# Kruize Optimizer Complete Workflow Test Analysis

## Overview
This document provides a comprehensive analysis of the Kruize Optimizer workflow based on log analysis and the enhanced E2E test implementation.

## Log Analysis Summary

### Startup Sequence
From the provided logs, the optimizer follows this initialization sequence:

1. **Service Startup** (08:40:07)
   - Quarkus application starts
   - Kruize Optimizer Service is STARTED!

2. **Bulk Scheduler Initialization** (08:40:07)
   - Bulk scheduler service initializes
   - Kruize state service refreshes

3. **Profile Installation** (08:40:08 - 08:40:09)
   - Metadata profiles installed: `cluster-metadata-local-monitoring`
   - Metric profiles installed: `resource-optimization-local-monitoring`
   - Layers installed: `container`, `semeru`, `hotspot`, `quarkus`

4. **Bulk Job Execution** (08:41:07, 08:56:07, 09:11:07)
   - Jobs triggered every 15 minutes
   - Each job includes filter: `{"kruize/autotune": "enabled"}`
   - Jobs complete with experiment counts

### Key Log Patterns

#### Profile Installation
```
Metadata profile: Installed: cluster-metadata-local-monitoring
Metric profile: Installed: resource-optimization-local-monitoring
Layer: Installed: container
Layer: Installed: semeru
Layer: Installed: hotspot
Layer: Installed: quarkus
```

#### Bulk Job Triggering
```
Starting scheduled bulk API call with target labels: {"kruize/autotune": "enabled"}
Calling bulk API with payload:
{
  "filter" : {
    "include" : {
      "labels" : {
        "kruize/autotune" : "enabled"
      }
    }
  },
  "webhook" : {
    "url" : "http://kruize-optimizer:8080/webhook"
  },
  "datasource" : "prometheus-1",
  "metadata_profile" : "cluster-metadata-local-monitoring",
  "measurement_duration" : "15min"
}
```

#### Job Completion
```
Bulk API call successful. Response: {"job_id":"36d458cc-f6b6-4b3a-af96-db0df8a953b1"}
Job 36d458cc-f6b6-4b3a-af96-db0df8a953b1 completed. Total: 25, Processed: 25, Existing: 0
```

## Enhanced Test Implementation

### Test Structure

The enhanced [`test_01_complete_workflow.py`](tests/test_01_complete_workflow.py) now includes:

#### Test 01: Optimizer Pod Running
- Verifies the kruize-optimizer pod is in Running state
- Checks pod readiness

#### Test 02: Optimizer Service Started
- Validates log message: "Kruize Optimizer Service is STARTED!"
- Confirms successful service initialization

#### Test 03: Load Configs Reference
- Loads [`configsReferenceIndex.json`](../../../src/main/resources/configs/configsReferenceIndex.json)
- Validates file structure (metadata_profiles, metric_profiles, layers)

#### Test 04: Profiles Installed via API
- Calls Kruize APIs:
  - `/listMetricProfiles`
  - `/listMetadataProfiles`
  - `/listLayers`
- Validates each profile from configsReferenceIndex.json is installed
- **Dual Validation**: API response + config file reference

#### Test 05: Profiles in Optimizer Logs
- Searches for specific installation messages:
  - `Metadata profile: Installed: <name>`
  - `Metric profile: Installed: <name>`
  - `Layer: Installed: <name>`
- **Dual Validation**: Log messages + config file reference

#### Test 06: Workloads Deployed
- Verifies sysbench workload is running
- Checks for labeled pods

#### Test 07: Bulk Job with Autotune Label
- Validates bulk API payload contains: `"kruize/autotune": "enabled"`
- Confirms label-based filtering is active

#### Test 07b: Bulk Job Completion
- Extracts job IDs from logs
- Validates job completion messages
- Verifies job statistics (Total, Processed, Existing)
- Ensures at least one job processed experiments

### New Utility Functions

Added to [`log_utils.py`](utils/log_utils.py):

#### `extract_job_ids_from_logs(logs: str)`
Extracts job information including:
- job_id (UUID format)
- status (triggered/completed)
- total, processed, existing counts

#### `verify_profile_installation_logs(logs: str, expected_profiles: Dict)`
Validates profile installation messages against expected profiles from config.

#### `check_bulk_job_with_autotune_label(logs: str)`
Checks if bulk API calls include the autotune label filter.

## Workflow Validation Checklist

### ✅ Complete Workflow Requirements

1. **Optimizer Pod Running**
   - Pod exists in correct namespace
   - Pod is in Ready state

2. **Optimizer Service Started**
   - Service initialization message in logs
   - Health endpoints responding

3. **Profiles Installed**
   - **API Validation**: All profiles from configsReferenceIndex.json present
   - **Log Validation**: Installation messages for each profile
   - **Dual Assert**: Both API and logs must confirm installation

4. **Profile-Config Alignment**
   - Metadata profiles match config
   - Metric profiles match config
   - Layers match config

5. **Bulk Jobs with Autotune Label**
   - Jobs triggered with label filter
   - Payload includes: `"kruize/autotune": "enabled"`

6. **Job Completion**
   - Job IDs logged
   - Completion status logged
   - Experiment counts logged (Total, Processed, Existing)
   - At least one job processes experiments

## Expected Log Patterns

### Successful Workflow
```
1. Kruize Optimizer Service is STARTED!
2. Metadata profile: Installed: cluster-metadata-local-monitoring
3. Metric profile: Installed: resource-optimization-local-monitoring
4. Layer: Installed: container
5. Layer: Installed: semeru
6. Layer: Installed: hotspot
7. Layer: Installed: quarkus
8. Starting scheduled bulk API call with target labels: {"kruize/autotune": "enabled"}
9. Bulk API call successful. Response: {"job_id":"<uuid>"}
10. Job <uuid> completed. Total: X, Processed: Y, Existing: Z
```

## Configuration Files

### configsReferenceIndex.json
Location: `src/main/resources/configs/configsReferenceIndex.json`

Structure:
```json
{
  "metadata_profiles": [
    {
      "name": "cluster-metadata-local-monitoring",
      "profile_version": "v1.0"
    }
  ],
  "metric_profiles": [
    {
      "name": "resource-optimization-local-monitoring",
      "profile_version": "v1.0"
    }
  ],
  "layers": [
    "container",
    "semeru",
    "hotspot",
    "quarkus"
  ]
}
```

## Test Execution

### Running the Tests
```bash
cd tests/e2e
python -m pytest tests/test_01_complete_workflow.py -v -s
```

### Expected Output
```
test_01_optimizer_pod_running PASSED
test_02_optimizer_service_started PASSED
test_03_load_configs_reference PASSED
test_04_profiles_installed_via_api PASSED
test_05_profiles_in_optimizer_logs PASSED
test_06_workloads_deployed PASSED
test_07_bulk_job_triggered_with_autotune_label PASSED
test_07b_bulk_job_completion PASSED
```

## Key Insights

1. **Dual Validation Strategy**: Tests validate both API responses AND log messages for critical operations
2. **Config-Driven Testing**: Uses configsReferenceIndex.json as source of truth
3. **Job Lifecycle Tracking**: Monitors jobs from trigger through completion
4. **Label-Based Filtering**: Confirms autotune label is used for workload selection
5. **Comprehensive Coverage**: Tests cover initialization, configuration, and runtime behavior

## Troubleshooting

### Common Issues

1. **No profiles found in API**
   - Check if optimizer service started successfully
   - Verify profile installation logs

2. **No bulk jobs triggered**
   - Check scheduler configuration
   - Verify workloads have autotune label

3. **Jobs triggered but not completed**
   - Check webhook endpoint accessibility
   - Verify datasource connectivity

## References

- Test Implementation: [`tests/e2e/tests/test_01_complete_workflow.py`](tests/test_01_complete_workflow.py)
- Utility Functions: [`tests/e2e/utils/log_utils.py`](utils/log_utils.py)
- Config Reference: [`src/main/resources/configs/configsReferenceIndex.json`](../../../src/main/resources/configs/configsReferenceIndex.json)