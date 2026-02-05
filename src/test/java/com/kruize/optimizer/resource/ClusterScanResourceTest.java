/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.kruize.optimizer.resource;

import com.kruize.optimizer.exceptions.clusterScanExceptions.ClusterScanException;
import com.kruize.optimizer.exceptions.clusterScanExceptions.InvalidParameterException;
import com.kruize.optimizer.exceptions.clusterScanExceptions.ResourceNotFoundException;
import com.kruize.optimizer.exceptions.targetLabelsProcessing.InvalidTargetLabelFormatException;
import com.kruize.optimizer.model.ClusterScanResult;
import com.kruize.optimizer.model.EnableOptimizationRequest;
import com.kruize.optimizer.service.WorkloadLabelService;
import com.kruize.optimizer.service.WorkloadScanService;
import com.kruize.optimizer.testutils.TestDataFactory;
import com.kruize.optimizer.utils.TargetLabelUtils;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClusterScanResource.
 * Tests both the scan API and enable-optimization API endpoints.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClusterScanResource Tests")
class ClusterScanResourceTest {

    @Mock
    private WorkloadScanService scanService;

    @Mock
    private WorkloadLabelService labelService;

    @Mock
    private TargetLabelUtils targetLabelUtils;

    @InjectMocks
    private ClusterScanResource resource;

    private ClusterScanResult mockScanResult;
    private Map<String, String> defaultLabels;
    private Map<String, String> customLabels;

    @BeforeEach
    void setUp() {
        mockScanResult = TestDataFactory.createClusterScanResult(3, 5);
        defaultLabels = TestDataFactory.createDefaultLabels();
        customLabels = TestDataFactory.createCustomLabels();
    }

    // ==================== Scan API Tests ====================

    @Test
    @DisplayName("Should scan cluster with scanAllWorkloads=true")
    void testScanCluster_WithScanAllWorkloads_ReturnsAllWorkloads() {
        // Arrange
        when(scanService.scanCluster(true)).thenReturn(mockScanResult);

        // Act
        ClusterScanResult result = resource.scan(true);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getNamespaces()).hasSize(3);
        assertThat(result.getWorkloads()).hasSize(5);
        verify(scanService, times(1)).scanCluster(true);
    }

    @Test
    @DisplayName("Should scan cluster with scanAllWorkloads=false")
    void testScanCluster_WithoutScanAllWorkloads_ReturnsLabeledWorkloads() {
        // Arrange
        when(scanService.scanCluster(false)).thenReturn(mockScanResult);

        // Act
        ClusterScanResult result = resource.scan(false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getNamespaces()).isNotEmpty();
        assertThat(result.getWorkloads()).isNotEmpty();
        verify(scanService, times(1)).scanCluster(false);
    }

    @Test
    @DisplayName("Should return empty result when ClusterScanException occurs")
    void testScanCluster_WhenExceptionOccurs_ReturnsEmptyResult() {
        // Arrange
        when(scanService.scanCluster(anyBoolean()))
                .thenThrow(new ClusterScanException("Cluster scan failed"));

        // Act
        ClusterScanResult result = resource.scan(true);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getNamespaces()).isEmpty();
        assertThat(result.getWorkloads()).isEmpty();
        verify(scanService, times(1)).scanCluster(true);
    }

    // ==================== Enable Optimization API - Namespace Tests ====================

    @Test
    @DisplayName("Should label namespace with default labels when no labels provided")
    void testEnableAutotune_LabelNamespace_WithDefaultLabels_Success() {
        // Arrange
        EnableOptimizationRequest request = TestDataFactory.createNamespaceLabelRequest(
                TestDataFactory.TEST_NAMESPACE, null);
        when(targetLabelUtils.getDefaultLabel()).thenReturn(defaultLabels);
        doNothing().when(labelService).labelNamespace(anyString(), anyMap());

        // Act
        Response response = resource.enableAutotune(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity().toString()).contains("message");
        verify(targetLabelUtils, times(1)).getDefaultLabel();
        verify(labelService, times(1)).labelNamespace(TestDataFactory.TEST_NAMESPACE, defaultLabels);
        verify(targetLabelUtils, never()).validateLabels(any());
    }

    @Test
    @DisplayName("Should label namespace with custom labels when labels provided")
    void testEnableAutotune_LabelNamespace_WithCustomLabels_Success() {
        // Arrange
        EnableOptimizationRequest request = TestDataFactory.createNamespaceLabelRequest(
                TestDataFactory.TEST_NAMESPACE, customLabels);
        doNothing().when(targetLabelUtils).validateLabels(customLabels);
        doNothing().when(labelService).labelNamespace(anyString(), anyMap());

        // Act
        Response response = resource.enableAutotune(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        verify(targetLabelUtils, times(1)).validateLabels(customLabels);
        verify(labelService, times(1)).labelNamespace(TestDataFactory.TEST_NAMESPACE, customLabels);
        verify(targetLabelUtils, never()).getDefaultLabel();
    }

    @Test
    @DisplayName("Should return 400 when namespace is null")
    void testEnableAutotune_WhenNamespaceIsNull_ReturnsBadRequest() {
        // Arrange
        EnableOptimizationRequest request = new EnableOptimizationRequest();
        request.setNamespace(null);

        // Act
        Response response = resource.enableAutotune(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(response.getEntity().toString()).contains("error");
        verify(labelService, never()).labelNamespace(anyString(), anyMap());
    }

    @Test
    @DisplayName("Should return 400 when namespace is empty")
    void testEnableAutotune_WhenNamespaceIsEmpty_ReturnsBadRequest() {
        // Arrange
        EnableOptimizationRequest request = TestDataFactory.createNamespaceLabelRequest("", null);

        // Act
        Response response = resource.enableAutotune(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(response.getEntity().toString()).contains("error");
        verify(labelService, never()).labelNamespace(anyString(), anyMap());
    }

    @Test
    @DisplayName("Should return 404 when namespace not found")
    void testEnableAutotune_WhenNamespaceNotFound_ReturnsNotFound() {
        // Arrange
        EnableOptimizationRequest request = TestDataFactory.createNamespaceLabelRequest(
                TestDataFactory.TEST_NAMESPACE, null);
        when(targetLabelUtils.getDefaultLabel()).thenReturn(defaultLabels);
        doThrow(new ResourceNotFoundException("Namespace not found", "Namespace", TestDataFactory.TEST_NAMESPACE))
                .when(labelService).labelNamespace(anyString(), anyMap());

        // Act
        Response response = resource.enableAutotune(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        assertThat(response.getEntity().toString()).contains("error");
        verify(labelService, times(1)).labelNamespace(anyString(), anyMap());
    }

    // ==================== Enable Optimization API - Workload Tests ====================

    @Test
    @DisplayName("Should label Deployment with default labels")
    void testEnableAutotune_LabelDeployment_WithDefaultLabels_Success() {
        // Arrange
        EnableOptimizationRequest request = TestDataFactory.createWorkloadLabelRequest(
                TestDataFactory.TEST_NAMESPACE,
                TestDataFactory.TEST_DEPLOYMENT_NAME,
                "Deployment",
                null);
        when(targetLabelUtils.getDefaultLabel()).thenReturn(defaultLabels);
        doNothing().when(labelService).labelWorkload(anyString(), anyString(), anyString(), anyMap());

        // Act
        Response response = resource.enableAutotune(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity().toString()).contains("message");
        verify(labelService, times(1)).labelWorkload(
                TestDataFactory.TEST_NAMESPACE,
                TestDataFactory.TEST_DEPLOYMENT_NAME,
                "Deployment",
                defaultLabels);
    }

    @Test
    @DisplayName("Should label StatefulSet with custom labels")
    void testEnableAutotune_LabelStatefulSet_WithCustomLabels_Success() {
        // Arrange
        EnableOptimizationRequest request = TestDataFactory.createWorkloadLabelRequest(
                TestDataFactory.TEST_NAMESPACE,
                TestDataFactory.TEST_STATEFULSET_NAME,
                "StatefulSet",
                customLabels);
        doNothing().when(targetLabelUtils).validateLabels(customLabels);
        doNothing().when(labelService).labelWorkload(anyString(), anyString(), anyString(), anyMap());

        // Act
        Response response = resource.enableAutotune(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        verify(targetLabelUtils, times(1)).validateLabels(customLabels);
        verify(labelService, times(1)).labelWorkload(
                TestDataFactory.TEST_NAMESPACE,
                TestDataFactory.TEST_STATEFULSET_NAME,
                "StatefulSet",
                customLabels);
    }

    @Test
    @DisplayName("Should return 400 when labels are invalid")
    void testEnableAutotune_WhenLabelsAreInvalid_ReturnsBadRequest() {
        // Arrange
        Map<String, String> invalidLabels = new HashMap<>();
        invalidLabels.put("invalid-key", "invalid-value");
        EnableOptimizationRequest request = TestDataFactory.createWorkloadLabelRequest(
                TestDataFactory.TEST_NAMESPACE,
                TestDataFactory.TEST_DEPLOYMENT_NAME,
                "Deployment",
                invalidLabels);
        doThrow(new InvalidTargetLabelFormatException("Invalid label format"))
                .when(targetLabelUtils).validateLabels(invalidLabels);

        // Act
        Response response = resource.enableAutotune(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(response.getEntity().toString()).contains("error");
        verify(targetLabelUtils, times(1)).validateLabels(invalidLabels);
        verify(labelService, never()).labelWorkload(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("Should return 404 when workload not found")
    void testEnableAutotune_WhenWorkloadNotFound_ReturnsNotFound() {
        // Arrange
        EnableOptimizationRequest request = TestDataFactory.createWorkloadLabelRequest(
                TestDataFactory.TEST_NAMESPACE,
                TestDataFactory.TEST_DEPLOYMENT_NAME,
                "Deployment",
                null);
        when(targetLabelUtils.getDefaultLabel()).thenReturn(defaultLabels);
        doThrow(new ResourceNotFoundException("Deployment not found", "Deployment", TestDataFactory.TEST_DEPLOYMENT_NAME))
                .when(labelService).labelWorkload(anyString(), anyString(), anyString(), anyMap());

        // Act
        Response response = resource.enableAutotune(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        assertThat(response.getEntity().toString()).contains("error");
        verify(labelService, times(1)).labelWorkload(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("Should return 400 when InvalidParameterException occurs")
    void testEnableAutotune_WhenInvalidParameterException_ReturnsBadRequest() {
        // Arrange
        EnableOptimizationRequest request = TestDataFactory.createWorkloadLabelRequest(
                TestDataFactory.TEST_NAMESPACE,
                TestDataFactory.TEST_DEPLOYMENT_NAME,
                "InvalidType",
                null);
        when(targetLabelUtils.getDefaultLabel()).thenReturn(defaultLabels);
        doThrow(new InvalidParameterException("Invalid workload type", "workloadType"))
                .when(labelService).labelWorkload(anyString(), anyString(), anyString(), anyMap());

        // Act
        Response response = resource.enableAutotune(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(response.getEntity().toString()).contains("error");
    }

    @Test
    @DisplayName("Should handle empty labels map by using default labels")
    void testEnableAutotune_WithEmptyLabelsMap_UsesDefaultLabels() {
        // Arrange
        EnableOptimizationRequest request = TestDataFactory.createNamespaceLabelRequest(
                TestDataFactory.TEST_NAMESPACE, new HashMap<>());
        when(targetLabelUtils.getDefaultLabel()).thenReturn(defaultLabels);
        doNothing().when(labelService).labelNamespace(anyString(), anyMap());

        // Act
        Response response = resource.enableAutotune(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        verify(targetLabelUtils, times(1)).getDefaultLabel();
        verify(labelService, times(1)).labelNamespace(TestDataFactory.TEST_NAMESPACE, defaultLabels);
    }
}

