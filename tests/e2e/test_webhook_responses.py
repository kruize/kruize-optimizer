#!/usr/bin/env python3
"""
Quick test script to check actual webhook responses
"""
import requests
import json

BASE_URL = "http://localhost:8080"

def test_response(name, payload, data=None):
    """Test a webhook call and print the response"""
    print(f"\n{'='*60}")
    print(f"Test: {name}")
    print(f"{'='*60}")
    
    if data:
        print(f"Payload (raw): {data}")
        response = requests.post(
            f"{BASE_URL}/webhook",
            data=data,
            headers={'Content-Type': 'application/json'}
        )
    else:
        print(f"Payload (json): {json.dumps(payload, indent=2)}")
        response = requests.post(
            f"{BASE_URL}/webhook",
            json=payload,
            headers={'Content-Type': 'application/json'}
        )
    
    print(f"Status Code: {response.status_code}")
    print(f"Response: {response.text}")
    print(f"Headers: {dict(response.headers)}")

# Test 1: Invalid JSON
test_response("Invalid JSON", None, data="{invalid json}")

# Test 2: Null payload
test_response("Null payload", None, data="null")

# Test 3: Empty array
test_response("Empty array", [])

# Test 4: Missing summary
test_response("Missing summary", [{}])

# Test 5: Null jobId
test_response("Null jobId", [{
    "summary": {
        "jobId": None,
        "status": "COMPLETED",
        "totalExperiments": 10,
        "processedExperiments": 8,
        "existingExperiments": 2
    }
}])

# Test 6: Empty jobId
test_response("Empty jobId", [{
    "summary": {
        "jobId": "",
        "status": "COMPLETED",
        "totalExperiments": 10,
        "processedExperiments": 8,
        "existingExperiments": 2
    }
}])

# Test 7: Whitespace jobId
test_response("Whitespace jobId", [{
    "summary": {
        "jobId": "   ",
        "status": "COMPLETED",
        "totalExperiments": 10,
        "processedExperiments": 8,
        "existingExperiments": 2
    }
}])

# Test 8: Valid payload
test_response("Valid payload", [{
    "summary": {
        "jobId": "test-valid-job-999",
        "status": "COMPLETED",
        "totalExperiments": 5,
        "processedExperiments": 5,
        "existingExperiments": 0
    }
}])

# Test 9: Without Content-Type
print(f"\n{'='*60}")
print(f"Test: Without Content-Type header")
print(f"{'='*60}")
payload = [{
    "summary": {
        "jobId": "test-job-123",
        "status": "COMPLETED",
        "totalExperiments": 10,
        "processedExperiments": 8,
        "existingExperiments": 2
    }
}]
print(f"Payload: {json.dumps(payload, indent=2)}")
response = requests.post(f"{BASE_URL}/webhook", json=payload)
print(f"Status Code: {response.status_code}")
print(f"Response: {response.text}")

# Made with Bob
