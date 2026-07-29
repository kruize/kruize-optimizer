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

import com.fasterxml.jackson.core.type.TypeReference;
import com.kruize.optimizer.client.KruizeClient;
import com.kruize.optimizer.exception.KruizeServiceException;
import com.kruize.optimizer.model.kruize.KruizeProfile;
import com.kruize.optimizer.util.MockResponseLoader;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProfileService
 */
@QuarkusTest
class ProfileServiceTest extends BaseServiceTest {

    @Inject
    ProfileService profileService;

    @InjectMock
    @RestClient
    KruizeClient kruizeClient;

    private List<KruizeProfile> mockMetadataProfilesList;
    private List<KruizeProfile> mockMetricProfilesList;
    private List<KruizeProfile> mockLayersList;
    private String emptyMetadataProfilesResponse;
    private String emptyMetricProfilesResponse;
    private String emptyLayersResponse;

    @BeforeEach
    void setUp() throws IOException {
        super.setUpCommonMocks();
        Mockito.reset(kruizeClient);

        // Load mock responses from JSON files
        mockMetadataProfilesList = MockResponseLoader.loadMockResponse("metadata_profile_list.json", new TypeReference<List<KruizeProfile>>() {});
        mockMetricProfilesList = MockResponseLoader.loadMockResponse("metric_profile_list.json", new TypeReference<List<KruizeProfile>>() {});
        mockLayersList = MockResponseLoader.loadMockResponse("layer_list.json", new TypeReference<List<KruizeProfile>>() {});
        emptyMetadataProfilesResponse = MockResponseLoader.loadMockResponseAsString("empty_metadata_profile_list.json");
        emptyMetricProfilesResponse = MockResponseLoader.loadMockResponseAsString("empty_metric_profile_list.json");
        emptyLayersResponse = MockResponseLoader.loadMockResponseAsString("empty_layer_list.json");
    }

    /**
     * Test successful retrieval of metadata profiles
     *
     * Test Description: Verifies that getMetadataProfiles() successfully fetches
     * metadata profiles from Kruize client.
     *
     * Expected Output:
     * - Returns list with 1 metadata profile
     * - Profile has correct name
     */
    @Test
    void testGetMetadataProfiles_Success() {
        // Arrange
        when(kruizeClient.getMetadataProfiles(true)).thenReturn(mockMetadataProfilesList);

        // Act
        List<KruizeProfile> profiles = profileService.getMetadataProfiles();

        // Assert
        assertNotNull(profiles);
        assertEquals(1, profiles.size());
        assertEquals("cluster-metadata-local-monitoring", profiles.get(0).getName());
        verify(kruizeClient, times(1)).getMetadataProfiles(true);
    }

    /**
     * Test getMetadataProfiles when no profiles are found
     *
     * Test Description: Verifies that when Kruize returns a 400 error indicating
     * no metadata profiles are found, the service returns an empty list.
     *
     * Expected Output:
     * - Returns empty list
     */
    @Test
    void testGetMetadataProfiles_EmptyList() {
        // Arrange
        Response mockResponse = Response.status(400)
                .entity(emptyMetadataProfilesResponse)
                .build();
        ClientWebApplicationException exception = new ClientWebApplicationException(mockResponse);
        when(kruizeClient.getMetadataProfiles(true)).thenThrow(exception);

        // Act
        List<KruizeProfile> profiles = profileService.getMetadataProfiles();

        // Assert
        assertNotNull(profiles);
        assertTrue(profiles.isEmpty());
        verify(kruizeClient, times(1)).getMetadataProfiles(true);
    }
    /**
     * Test getMetadataProfiles when Kruize returns a non-400 HTTP error
     *
     * Test Description: Verifies that when Kruize returns a 5xx error,
     * the service treats it as a true error and throws KruizeServiceException.
     *
     * Expected Output:
     * - KruizeServiceException is thrown
     */
    @Test
    void testGetMetadataProfiles_Non400ClientWebApplicationException() {
        // Arrange
        Response mockResponse = Response.status(500)
                .entity("Internal Server Error from Kruize")
                .build();
        ClientWebApplicationException exception = new ClientWebApplicationException(mockResponse);
        when(kruizeClient.getMetadataProfiles(true)).thenThrow(exception);

        // Act & Assert
        assertThrows(KruizeServiceException.class, () -> profileService.getMetadataProfiles());
        verify(kruizeClient, times(1)).getMetadataProfiles(true);
    }


    /**
     * Test getMetadataProfiles when service throws exception
     *
     * Test Description: Verifies that when the Kruize client throws an unexpected
     * exception, the service wraps it in a KruizeServiceException.
     *
     * Expected Output:
     * - Throws KruizeServiceException
     */
    @Test
    void testGetMetadataProfiles_ServiceException() {
        // Arrange
        when(kruizeClient.getMetadataProfiles(true)).thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        assertThrows(KruizeServiceException.class, () -> {
            profileService.getMetadataProfiles();
        });
        verify(kruizeClient, times(1)).getMetadataProfiles(true);
    }

    /**
     * Test successful retrieval of metric profiles
     *
     * Test Description: Verifies that getMetricProfiles() successfully fetches
     * metric profiles from Kruize client.
     *
     * Expected Output:
     * - Returns list with 1 metric profile
     * - Profile has correct name
     */
    @Test
    void testGetMetricProfiles_Success() {
        // Arrange
        when(kruizeClient.getMetricProfiles(true)).thenReturn(mockMetricProfilesList);

        // Act
        List<KruizeProfile> profiles = profileService.getMetricProfiles();

        // Assert
        assertNotNull(profiles);
        assertEquals(1, profiles.size());
        assertEquals("resource-optimization-local-monitoring", profiles.get(0).getName());
        verify(kruizeClient, times(1)).getMetricProfiles(true);
    }

    /**
     * Test getMetricProfiles when no profiles are found
     *
     * Test Description: Verifies that when Kruize returns a 400 error indicating
     * no metric profiles are found, the service returns an empty list.
     *
     * Expected Output:
     * - Returns empty list
     */
    @Test
    void testGetMetricProfiles_EmptyList() {
        // Arrange
        Response mockResponse = Response.status(400)
                .entity(emptyMetricProfilesResponse)
                .build();
        ClientWebApplicationException exception = new ClientWebApplicationException(mockResponse);
        when(kruizeClient.getMetricProfiles(true)).thenThrow(exception);

        // Act
        List<KruizeProfile> profiles = profileService.getMetricProfiles();

        // Assert
        assertNotNull(profiles);
        assertTrue(profiles.isEmpty());
        verify(kruizeClient, times(1)).getMetricProfiles(true);
    }

    /**
     * Test getMetricProfiles when service throws exception
     *
     * Test Description: Verifies that when the Kruize client throws an unexpected
     * exception, the service wraps it in a KruizeServiceException.
     *
     * Expected Output:
     * - Throws KruizeServiceException
     */
    @Test
    void testGetMetricProfiles_ServiceException() {
        // Arrange
        when(kruizeClient.getMetricProfiles(true)).thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        assertThrows(KruizeServiceException.class, () -> {
            profileService.getMetricProfiles();
        });
        verify(kruizeClient, times(1)).getMetricProfiles(true);
    }

    /**
     * Test successful retrieval of layers
     *
     * Test Description: Verifies that getLayers() successfully fetches
     * layers from Kruize client.
     *
     * Expected Output:
     * - Returns list with 4 layers
     * - Layers have correct names
     */
    @Test
    void testGetLayers_Success() {
        // Arrange
        when(kruizeClient.getLayers()).thenReturn(mockLayersList);

        // Act
        List<KruizeProfile> layers = profileService.getLayers();

        // Assert
        assertNotNull(layers);
        assertEquals(4, layers.size());
        assertEquals("container", layers.get(0).getName());
        assertEquals("semeru", layers.get(1).getName());
        assertEquals("hotspot", layers.get(2).getName());
        assertEquals("quarkus", layers.get(3).getName());
        verify(kruizeClient, times(1)).getLayers();
    }

    /**
     * Test getLayers when no layers are found
     *
     * Test Description: Verifies that when Kruize returns a 400 error indicating
     * no layers are found, the service returns an empty list.
     *
     * Expected Output:
     * - Returns empty list
     */
    @Test
    void testGetLayers_EmptyList() {
        // Arrange
        Response mockResponse = Response.status(400)
                .entity(emptyLayersResponse)
                .build();
        ClientWebApplicationException exception = new ClientWebApplicationException(mockResponse);
        when(kruizeClient.getLayers()).thenThrow(exception);

        // Act
        List<KruizeProfile> layers = profileService.getLayers();

        // Assert
        assertNotNull(layers);
        assertTrue(layers.isEmpty());
        verify(kruizeClient, times(1)).getLayers();
    }

    /**
     * Test getLayers when service throws exception
     *
     * Test Description: Verifies that when the Kruize client throws an unexpected
     * exception, the service wraps it in a KruizeServiceException.
     *
     * Expected Output:
     * - Throws KruizeServiceException
     */
    @Test
    void testGetLayers_ServiceException() {
        // Arrange
        when(kruizeClient.getLayers()).thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        assertThrows(KruizeServiceException.class, () -> {
            profileService.getLayers();
        });
        verify(kruizeClient, times(1)).getLayers();
    }
}

