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
package com.kruize.optimizer.service;

import com.kruize.optimizer.client.KruizeClient;
import com.kruize.optimizer.util.MockResponseLoader;
import com.kruize.optimizer.utils.OptimizerConstants.BulkSchedulerConstants;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BulkSchedulerService
 */
@QuarkusTest
class BulkSchedulerServiceTest {

    @Inject
    BulkSchedulerService bulkSchedulerService;

    @InjectMock
    @RestClient
    KruizeClient kruizeClient;

    @InjectMock
    KruizeStateService kruizeStateService;

    @InjectMock
    JobsService jobsService;

    @ConfigProperty(name = "kruize.bulk.scheduler.measurement-duration")
    String measurementDuration;

    @ConfigProperty(name = "kruize.webhook.url")
    String webhookUrl;

    @ConfigProperty(name = "kruize.target.labels.json")
    String targetLabelsJson;

    private String mockBulkApiResponse;

    @BeforeEach
    void setUp() throws IOException {
        Mockito.reset(kruizeClient, kruizeStateService, jobsService);

        // Load mock response from JSON file
        mockBulkApiResponse = MockResponseLoader.loadMockResponseAsString("bulk_api_response.json");

        // Mock the initialization methods
        doNothing().when(kruizeStateService).refreshStateAndInstallProfiles();
    }

    /**
     * Test successful scheduled bulk API call
     *
     * Test Description: Verifies that the scheduled bulk API call successfully executes
     * when all required resources (datasource, profiles) are available.
     *
     * Mock Response (from bulk_api_response.json):
     * - Kruize returns: {"job_id": "c0c1ca84-3aaf-450e-909f-a14a3ad6fef4"}
     *
     * Expected Behavior:
     * - Bulk API called with proper payload structure
     * - Payload contains: filter, datasource, metadata_profile, measurement_duration
     * - Jobs counter incremented
     * - Verifies payload values match configuration
     */
    @Test
    void testScheduledBulkApiCall_Success() {
        // Arrange
        when(kruizeStateService.isCacheEmpty()).thenReturn(false);
        when(kruizeStateService.getDefaultDatasourceNames()).thenReturn(List.of("prometheus-1"));
        when(kruizeStateService.getDefaultMetadataProfileName()).thenReturn(Optional.of("cluster-metadata-local-monitoring"));
        when(kruizeStateService.getDefaultMetricProfileName()).thenReturn(Optional.of("resource-optimization-local-monitoring"));
        when(kruizeClient.bulkCreateExperiments(any())).thenReturn(mockBulkApiResponse);
        doNothing().when(jobsService).incrementJobsTriggered();

        // Act
        bulkSchedulerService.initialize();
        bulkSchedulerService.scheduledBulkApiCall();

        // Assert
        verify(kruizeClient, times(1)).bulkCreateExperiments(any());
        verify(jobsService, times(1)).incrementJobsTriggered();

        // Verify the payload structure
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(kruizeClient).bulkCreateExperiments(payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();

        assertNotNull(payload);
        assertTrue(payload.containsKey(BulkSchedulerConstants.FILTER));
        assertTrue(payload.containsKey(BulkSchedulerConstants.DATASOURCES));
        assertFalse(payload.containsKey(BulkSchedulerConstants.DATASOURCE)); // Deprecated field not used when list is present
        assertTrue(payload.containsKey(BulkSchedulerConstants.METADATA_PROFILE));
        assertTrue(payload.containsKey(BulkSchedulerConstants.MEASUREMENT_DURATION));
        assertEquals(List.of("prometheus-1"), payload.get(BulkSchedulerConstants.DATASOURCES));
        assertEquals("cluster-metadata-local-monitoring", payload.get(BulkSchedulerConstants.METADATA_PROFILE));
        assertEquals(measurementDuration, payload.get(BulkSchedulerConstants.MEASUREMENT_DURATION));
    }

    /**
     * Test scheduled bulk API call with multiple datasources
     *
     * Test Description: Verifies that the scheduled bulk API call correctly handles
     * multiple datasources and propagates them in the correct order.
     *
     * Expected Behavior:
     * - Bulk API called with proper payload structure
     * - Payload contains the complete datasource list in order
     * - Deprecated datasource field is NOT present when list is available
     * - Jobs counter incremented
     */
    @Test
    void testScheduledBulkApiCall_MultipleDatasources() {
        // Arrange
        when(kruizeStateService.isCacheEmpty()).thenReturn(false);
        List<String> datasources = List.of("prometheus-1", "prometheus-2");
        when(kruizeStateService.getDefaultDatasourceNames()).thenReturn(datasources);
        when(kruizeStateService.getDefaultMetadataProfileName())
                .thenReturn(Optional.of("cluster-metadata-local-monitoring"));
        when(kruizeStateService.getDefaultMetricProfileName())
                .thenReturn(Optional.of("resource-optimization-local-monitoring"));
        when(kruizeClient.bulkCreateExperiments(any())).thenReturn(mockBulkApiResponse);
        doNothing().when(jobsService).incrementJobsTriggered();

        // Act
        bulkSchedulerService.initialize();
        bulkSchedulerService.scheduledBulkApiCall();

        // Assert
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(kruizeClient).bulkCreateExperiments(payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertNotNull(payload);
        assertTrue(payload.containsKey(BulkSchedulerConstants.FILTER));
        assertTrue(payload.containsKey(BulkSchedulerConstants.DATASOURCES));
        assertFalse(payload.containsKey(BulkSchedulerConstants.DATASOURCE)); // Deprecated field not used when list is present
        assertTrue(payload.containsKey(BulkSchedulerConstants.METADATA_PROFILE));
        assertTrue(payload.containsKey(BulkSchedulerConstants.MEASUREMENT_DURATION));

        // Ensure the full list of datasources is propagated and ordered correctly
        assertEquals(datasources, payload.get(BulkSchedulerConstants.DATASOURCES));
        
        verify(jobsService, times(1)).incrementJobsTriggered();
    }

    /**
     * Test scheduled bulk API call when service is not initialized
     *
     * Test Description: Verifies that the scheduled bulk API call does not execute
     * if the service has not been initialized.
     *
     * Expected Behavior:
     * - Bulk API not called
     * - Jobs counter not incremented
     */
    @Test
    void testScheduledBulkApiCall_NotInitialized() {
        // Act - Call without initialization
        bulkSchedulerService.scheduledBulkApiCall();

        // Assert - Should not call bulk API
        verify(kruizeClient, never()).bulkCreateExperiments(any());
        verify(jobsService, never()).incrementJobsTriggered();
    }

    /**
     * Test scheduled bulk API call when no datasources are available
     *
     * Test Description: Verifies that the scheduled bulk API call does not execute
     * when no datasources are configured in Kruize.
     *
     * Expected Behavior:
     * - Bulk API not called
     * - Jobs counter not incremented
     * - Service logs error about missing datasources (ERROR_NO_DATASOURCES_AVAILABLE)
     */
    @Test
    void testScheduledBulkApiCall_NoDatasource() {
        // Arrange
        when(kruizeStateService.isCacheEmpty()).thenReturn(false);
        when(kruizeStateService.getDefaultDatasourceNames()).thenReturn(Collections.emptyList());

        // Act
        bulkSchedulerService.initialize();
        bulkSchedulerService.scheduledBulkApiCall();

        // Assert
        // - Bulk API not called
        verify(kruizeClient, never()).bulkCreateExperiments(any());
        // - Jobs counter not incremented
        verify(jobsService, never()).incrementJobsTriggered();
    }

    /**
     * Test scheduled bulk API call when no metadata profile is available
     *
     * Test Description: Verifies that the scheduled bulk API call does not execute
     * when no metadata profile is configured in Kruize.
     *
     * Expected Behavior:
     * - Bulk API not called
     * - Jobs counter not incremented
     * - Service logs error about missing metadata profile
     */
    @Test
    void testScheduledBulkApiCall_NoMetadataProfile() {
        // Arrange
        when(kruizeStateService.isCacheEmpty()).thenReturn(false);
        when(kruizeStateService.getDefaultDatasourceName()).thenReturn(Optional.of("prometheus-1"));
        when(kruizeStateService.getDefaultMetadataProfileName()).thenReturn(Optional.empty());

        // Act
        bulkSchedulerService.initialize();
        bulkSchedulerService.scheduledBulkApiCall();

        // Assert - Should not call bulk API
        verify(kruizeClient, never()).bulkCreateExperiments(any());
        verify(jobsService, never()).incrementJobsTriggered();
    }

    /**
     * Test scheduled bulk API call when no metric profile is available
     *
     * Test Description: Verifies that the scheduled bulk API call does not execute
     * when no metric profile is configured in Kruize.
     *
     * Expected Behavior:
     * - Bulk API not called
     * - Jobs counter not incremented
     * - Service logs error about missing metric profile
     */
    @Test
    void testScheduledBulkApiCall_NoMetricProfile() {
        // Arrange
        when(kruizeStateService.isCacheEmpty()).thenReturn(false);
        when(kruizeStateService.getDefaultDatasourceName()).thenReturn(Optional.of("prometheus-1"));
        when(kruizeStateService.getDefaultMetadataProfileName()).thenReturn(Optional.of("cluster-metadata-local-monitoring"));
        when(kruizeStateService.getDefaultMetricProfileName()).thenReturn(Optional.empty());

        // Act
        bulkSchedulerService.initialize();
        bulkSchedulerService.scheduledBulkApiCall();

        // Assert - Should not call bulk API
        verify(kruizeClient, never()).bulkCreateExperiments(any());
        verify(jobsService, never()).incrementJobsTriggered();
    }

    /**
     * Test scheduled bulk API call with empty cache that triggers refresh
     *
     * Test Description: Verifies that when the state cache is empty, the service
     * automatically refreshes the state before making the bulk API call.
     *
     * Expected Behavior:
     * - State cache refresh triggered
     * - Bulk API called successfully after refresh
     * - Jobs counter incremented
     */
    @Test
    void testScheduledBulkApiCall_CacheEmptyRefreshes() {
        // Arrange
        when(kruizeStateService.isCacheEmpty()).thenReturn(true);
        doNothing().when(kruizeStateService).refreshState();
        when(kruizeStateService.getDefaultDatasourceNames()).thenReturn(List.of("prometheus-1"));
        when(kruizeStateService.getDefaultMetadataProfileName()).thenReturn(Optional.of("cluster-metadata-local-monitoring"));
        when(kruizeStateService.getDefaultMetricProfileName()).thenReturn(Optional.of("resource-optimization-local-monitoring"));
        when(kruizeClient.bulkCreateExperiments(any())).thenReturn(mockBulkApiResponse);
        doNothing().when(jobsService).incrementJobsTriggered();

        // Act
        bulkSchedulerService.initialize();
        bulkSchedulerService.scheduledBulkApiCall();

        // Assert
        verify(kruizeStateService, times(1)).refreshState();
        verify(kruizeClient, times(1)).bulkCreateExperiments(any());
        verify(jobsService, times(1)).incrementJobsTriggered();
    }

    /**
     * Test scheduled bulk API call exception handling
     *
     * Test Description: Verifies that when the Kruize bulk API throws an exception,
     * the service handles it gracefully without crashing the scheduler.
     *
     * Expected Behavior:
     * - Exception caught and logged
     * - Jobs counter not incremented (since call failed)
     * - Scheduler continues to run for next iteration
     */
    @Test
    void testScheduledBulkApiCall_ExceptionHandling() {
        // Arrange
        when(kruizeStateService.isCacheEmpty()).thenReturn(false);
        when(kruizeStateService.getDefaultDatasourceNames()).thenReturn(List.of("prometheus-1"));
        when(kruizeStateService.getDefaultMetadataProfileName()).thenReturn(Optional.of("cluster-metadata-local-monitoring"));
        when(kruizeStateService.getDefaultMetricProfileName()).thenReturn(Optional.of("resource-optimization-local-monitoring"));
        when(kruizeClient.bulkCreateExperiments(any())).thenThrow(new RuntimeException("API error"));

        // Act
        bulkSchedulerService.initialize();
        bulkSchedulerService.scheduledBulkApiCall();

        // Assert - Should handle exception gracefully
        verify(kruizeClient, times(1)).bulkCreateExperiments(any());
        verify(jobsService, never()).incrementJobsTriggered();
    }

    /**
     * Test successful service initialization
     *
     * Test Description: Verifies that the BulkSchedulerService initializes correctly
     * by refreshing state and installing missing profiles.
     *
     * Expected Behavior:
     * - KruizeStateService.refreshStateAndInstallProfiles() called once
     * - Service marked as initialized
     */
    @Test
    void testInitialize_Success() {
        // Arrange
        doNothing().when(kruizeStateService).refreshStateAndInstallProfiles();

        // Act
        bulkSchedulerService.initialize();

        // Assert
        verify(kruizeStateService, times(1)).refreshStateAndInstallProfiles();
    }

    /**
     * Test service initialization exception handling
     *
     * Test Description: Verifies that if initialization fails due to an exception,
     * the service handles it gracefully without crashing the application.
     *
     * Expected Behavior:
     * - Exception caught and logged
     * - Application continues to run
     * - Service remains uninitialized (scheduled calls will be skipped)
     */
    @Test
    void testInitialize_ExceptionHandling() {
        // Arrange
        doThrow(new RuntimeException("Initialization error")).when(kruizeStateService).refreshStateAndInstallProfiles();

        // Act - Should not throw exception
        assertDoesNotThrow(() -> bulkSchedulerService.initialize());

        // Assert
        verify(kruizeStateService, times(1)).refreshStateAndInstallProfiles();
    }
}

