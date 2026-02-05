# Unit Tests for Kruize Optimizer - Scan and Enable Optimization APIs

## Overview

This directory contains comprehensive Mockito-based unit tests for the Kruize Optimizer's scan API and enable-optimization API. 

## Test Files

### 1. ClusterScanResourceTest.java 

**Coverage:** 14 test cases

#### Scan API Tests (3 tests)
- Scan cluster with `scanAllWorkloads=true`
- Scan cluster with `scanAllWorkloads=false`
- Handle ClusterScanException and return empty result

#### Enable Optimization API - Namespace Tests (5 tests)
- Label namespace with default labels when no labels provided
- Label namespace with custom labels when labels provided
- Return 400 when namespace is null
- Return 400 when namespace is empty
- Return 404 when namespace not found

#### Enable Optimization API - Workload Tests (6 tests)
- Label Deployment with default labels
- Label StatefulSet with custom labels
- Return 400 when labels are invalid
- Return 404 when workload not found
- Return 400 when InvalidParameterException occurs
- Handle empty labels map by using default labels

**Status: ALL 14 TESTS PASSING**

---

### 2. TargetLabelUtilsTest.java 

**Coverage:** 14 comprehensive test cases

#### Get Default Label Tests (1 test)
- Return default label correctly

#### Label Validation Tests (8 tests)
- Throw exception when validating null labels
- Throw exception when validating empty labels
- Throw exception when label key is null
- Throw exception when label key is empty
- Throw exception when label value is null
- Throw exception when label value is empty
- Throw exception when label key has only whitespace
- Throw exception when label value has only whitespace

#### isLabelInTargetLabels Tests (3 tests)
- Return true when label exists in target labels
- Return false when label does not exist
- Return false when key exists but value is different

#### Get Target Labels Tests (1 test)
- Return unmodifiable target labels map

#### Label Limit Tests (1 test)
- Fallback to default label when label limit is exceeded

**Status: ALL 14 TESTS PASSING**

---

### 3. TestDataFactory.java 

**Purpose:** Provides reusable test data and helper methods for creating resources.

---

## Running the Tests

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=ClusterScanResourceTest

mvn test -Dtest=TargetLabelUtilsTest
```

---