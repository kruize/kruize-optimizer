# Webhook Test Fixes - Field Name Corrections

## Issue
The webhook tests in [`test_04_webhook.py`](tests/test_04_webhook.py) were failing because they used incorrect field names in the JSON payload.

## Root Cause Analysis

### Incorrect Field Names (Before Fix)
```json
{
  "summary": {
    "jobId": "test-123",              // ❌ Wrong - should be jobID
    "status": "COMPLETED",
    "totalExperiments": 5,            // ❌ Wrong - should be total_experiments
    "processedExperiments": 5,        // ❌ Wrong - should be processed_experiments
    "existingExperiments": 0          // ❌ Wrong - should be existing_experiments
  }
}
```

### Correct Field Names (After Fix)
```json
{
  "summary": {
    "jobID": "test-123",              // ✅ Correct - capital ID
    "status": "COMPLETED",
    "total_experiments": 5,           // ✅ Correct - snake_case
    "processed_experiments": 5,       // ✅ Correct - snake_case
    "existing_experiments": 0         // ✅ Correct - snake_case
  }
}
```

## Source of Truth

The correct field names are defined in [`OptimizerConstants.java`](../../src/main/java/com/kruize/optimizer/utils/OptimizerConstants.java):

### WebhookConstants (Line 184-198)
```java
public static final class WebhookConstants {
    public static final String SUMMARY = "summary";
    public static final String WEBHOOK = "webhook";
    public static final String JOB_ID = "jobID";        // ← Note: capital ID
    public static final String STATUS = "status";
}
```

### JobsConstants (Line 169-181)
```java
public static final class JobsConstants {
    public static final String JOBS_TRIGGERED = "jobs_triggered";
    public static final String TOTAL_EXPERIMENTS = "total_experiments";           // ← snake_case
    public static final String PROCESSED_EXPERIMENTS = "processed_experiments";   // ← snake_case
    public static final String UNIQUE_EXPERIMENTS = "unique_experiments";
    public static final String EXISTING_EXPERIMENTS = "existing_experiments";     // ← snake_case
}
```

## Testing Results

### Port Configuration
- Optimizer is accessible on port **9090** (not 8080 or 8081)
- Updated fixture to use: `http://localhost:9090`

### Validation Results

| Test Case | Payload | Expected | Actual | Status |
|-----------|---------|----------|--------|--------|
| Empty array | `[]` | 400 | 400 | ✅ Pass |
| Null payload | `null` | 400 | 400 | ✅ Pass |
| Missing summary | `[{}]` | 400 | 400 | ✅ Pass |
| Null jobID | `jobID: null` | 400 | 400 | ✅ Pass |
| Empty jobID | `jobID: ""` | 400 | 400 | ✅ Pass |
| Whitespace jobID | `jobID: "   "` | 400 | 400 | ✅ Pass |
| Valid payload | Correct fields | 200 | 200 | ✅ Pass |

### Error Messages
The API returns clear validation messages:
- Empty/null payload: `"Invalid webhook payload: payload cannot be null or empty"`
- Missing summary: `"Invalid webhook payload: summary is required"`
- Invalid jobID: `"Invalid webhook payload: jobID is required and cannot be empty"`

## Changes Made

### 1. Updated Port Configuration
```python
@pytest.fixture(scope="module")
def optimizer_client(config):
    """Create Optimizer API client"""
    # Use port 9090 for optimizer (port-forwarded)
    base_url = "http://localhost:9090"
    return OptimizerAPIClient(base_url)
```

### 2. Fixed All Field Names
Changed all occurrences of:
- `jobId` → `jobID`
- `totalExperiments` → `total_experiments`
- `processedExperiments` → `processed_experiments`
- `existingExperiments` → `existing_experiments`

## Verification Commands

Test the webhook endpoint manually:

```bash
# Test 1: Valid payload (should return 200)
curl -X POST http://localhost:9090/webhook \
  -H "Content-Type: application/json" \
  -d '[{"summary":{"jobID":"test-123","status":"COMPLETED","total_experiments":5,"processed_experiments":5,"existing_experiments":0}}]'

# Test 2: Empty array (should return 400)
curl -X POST http://localhost:9090/webhook \
  -H "Content-Type: application/json" \
  -d '[]'

# Test 3: Missing jobID (should return 400)
curl -X POST http://localhost:9090/webhook \
  -H "Content-Type: application/json" \
  -d '[{"summary":{"status":"COMPLETED"}}]'
```

## Running the Tests

```bash
cd tests/e2e
pytest tests/test_04_webhook.py -v -s
```

Expected output:
```
test_webhook_invalid_json PASSED
test_webhook_null_payload PASSED
test_webhook_empty_array PASSED
test_webhook_missing_summary PASSED
test_webhook_null_job_id PASSED
test_webhook_empty_job_id PASSED
test_webhook_whitespace_job_id PASSED
test_webhook_malformed_summary PASSED
test_webhook_missing_content_type PASSED
test_webhook_valid_payload_accepted PASSED
test_webhook_multiple_payloads_one_invalid PASSED
```

## Key Takeaways

1. **Always check the source code** for exact field names - don't assume camelCase or snake_case
2. **Field name casing matters**: `jobId` ≠ `jobID`
3. **Use constants from the codebase** as the source of truth
4. **Test against the actual running service** to verify field names
5. **Port forwarding**: Remember to use the correct port (9090 in this case)

## Related Files
- Test file: [`tests/e2e/tests/test_04_webhook.py`](tests/test_04_webhook.py)
- Constants: [`src/main/java/com/kruize/optimizer/utils/OptimizerConstants.java`](../../src/main/java/com/kruize/optimizer/utils/OptimizerConstants.java)
- Model: [`src/main/java/com/kruize/optimizer/model/WebhookPayload.java`](../../src/main/java/com/kruize/optimizer/model/WebhookPayload.java)
- Resource: [`src/main/java/com/kruize/optimizer/resource/WebhookResource.java`](../../src/main/java/com/kruize/optimizer/resource/WebhookResource.java)