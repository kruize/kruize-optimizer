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
import com.kruize.optimizer.exception.KruizeServiceException;
import com.kruize.optimizer.model.api.DatasourceListResponse;
import com.kruize.optimizer.model.kruize.Datasource;
import com.kruize.optimizer.util.MockResponseLoader;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DatasourceService
 */
@QuarkusTest
class DatasourceServiceTest extends BaseServiceTest {

    @Inject
    DatasourceService datasourceService;

    @InjectMock
    @RestClient
    KruizeClient kruizeClient;

    private DatasourceListResponse mockDatasourceResponse;
    private DatasourceListResponse emptyDatasourceResponse;

    @BeforeEach
    void setUp() throws IOException {
        super.setUpCommonMocks();
        Mockito.reset(kruizeClient);

        // Load mock responses from JSON files
        mockDatasourceResponse = MockResponseLoader.loadMockResponse("datasource_list.json", DatasourceListResponse.class);
        emptyDatasourceResponse = MockResponseLoader.loadMockResponse("empty_datasource_list.json", DatasourceListResponse.class);
    }

    /**
     * Test successful retrieval of datasources
     *
     * Test Description: Verifies that getDatasources() successfully fetches datasources
     * from Kruize client and returns them.
     *
     * Mock Response (from datasource_list.json):
     * - Contains 1 datasource: prometheus-1
     *
     * Expected Output:
     * - Returns list with 1 datasource
     * - Datasource has correct name, provider, and URL
     */
    @Test
    void testGetDatasources_Success() {
        // Arrange
        when(kruizeClient.getDatasources()).thenReturn(mockDatasourceResponse);

        // Act
        List<Datasource> datasources = datasourceService.getDatasources();

        // Assert
        assertNotNull(datasources);
        assertEquals(1, datasources.size());
        assertEquals("prometheus-1", datasources.get(0).getName());
        assertEquals("prometheus", datasources.get(0).getProvider());
        assertEquals("http://prometheus-k8s.monitoring.svc.cluster.local:9090", datasources.get(0).getUrl());
        verify(kruizeClient, times(1)).getDatasources();
    }

    /**
     * Test getDatasources when no datasources are available
     *
     * Test Description: Verifies that when Kruize returns an empty datasources list,
     * the service returns an empty list.
     *
     * Mock Response (from empty_datasource_list.json):
     * - Kruize returns: {"version": "v1.0", "datasources": []}
     *
     * Expected Output:
     * - Returns empty list
     */
    @Test
    void testGetDatasources_EmptyList() {
        // Arrange
        when(kruizeClient.getDatasources()).thenReturn(emptyDatasourceResponse);

        // Act
        List<Datasource> datasources = datasourceService.getDatasources();

        // Assert
        assertNotNull(datasources);
        assertTrue(datasources.isEmpty());
        verify(kruizeClient, times(1)).getDatasources();
    }

    /**
     * Test getDatasources when Kruize client throws exception
     *
     * Test Description: Verifies that when the Kruize client throws an exception,
     * the service wraps it in a KruizeServiceException.
     *
     * Expected Output:
     * - Throws KruizeServiceException
     * - Exception has SERVICE_UNAVAILABLE status code
     */
    @Test
    void testGetDatasources_ServiceException() {
        // Arrange
        when(kruizeClient.getDatasources()).thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        KruizeServiceException exception = assertThrows(KruizeServiceException.class, () -> {
            datasourceService.getDatasources();
        });

        assertEquals(503, exception.getStatusCode());
        verify(kruizeClient, times(1)).getDatasources();
    }

    /**
     * Test isKruizeAvailable when service is available
     *
     * Test Description: Verifies that isKruizeAvailable() returns true
     * when Kruize service responds successfully.
     *
     * Expected Output:
     * - Returns true
     */
    @Test
    void testIsKruizeAvailable_Success() {
        // Arrange
        when(kruizeClient.getDatasources()).thenReturn(mockDatasourceResponse);

        // Act
        boolean available = datasourceService.isKruizeAvailable();

        // Assert
        assertTrue(available);
        verify(kruizeClient, times(1)).getDatasources();
    }

    /**
     * Test isKruizeAvailable when service is unavailable
     *
     * Test Description: Verifies that isKruizeAvailable() returns false
     * when Kruize service throws an exception.
     *
     * Expected Output:
     * - Returns false
     */
    @Test
    void testIsKruizeAvailable_ServiceUnavailable() {
        // Arrange
        when(kruizeClient.getDatasources()).thenThrow(new RuntimeException("Service error"));

        // Act
        boolean available = datasourceService.isKruizeAvailable();

        // Assert
        assertFalse(available);
        verify(kruizeClient, times(1)).getDatasources();
    }
}

