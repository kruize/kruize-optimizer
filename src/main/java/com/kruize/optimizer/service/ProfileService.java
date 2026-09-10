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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kruize.optimizer.client.KruizeClient;
import com.kruize.optimizer.exception.KruizeServiceException;
import com.kruize.optimizer.model.kruize.BulkConfig;
import com.kruize.optimizer.model.kruize.KruizeProfile;
import com.kruize.optimizer.utils.OptimizerConstants.MessageConstants;
import com.kruize.optimizer.utils.OptimizerConstants.ProfileType;
import com.kruize.optimizer.utils.OptimizerConstants.ProfilePathConstants;
import jakarta.ws.rs.core.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Service for managing Kruize profiles (metadata, metric, layers)
 */
@ApplicationScoped
public class ProfileService {

    private static final Logger LOG = Logger.getLogger(ProfileService.class);

    @Inject
    @RestClient
    KruizeClient kruizeClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get metadata profiles from Kruize
     * Always fetches with verbose=true to get profile_version
     *
     * @return List of metadata profiles
     */
    public List<KruizeProfile> getMetadataProfiles() {
        try {
            LOG.info("Fetching metadata profiles from Kruize");
            // Always use verbose=true to get profile_version in response
            List<KruizeProfile> profiles = Optional.ofNullable(kruizeClient.getMetadataProfiles(true))
                    .orElse(Collections.emptyList());
            // Set profile type for each profile
            profiles.forEach(p -> p.setProfileType(ProfileType.METADATA));
            return profiles;
        } catch (ClientWebApplicationException e) {
            // Check if this is a "No metadata profiles found" error (400 status)
            // TODO: remove this when issue 1849 is fixed
            if (e.getResponse().getStatus() == 400) {
                try {
                    String responseBody = e.getResponse().readEntity(String.class);
                    if (responseBody != null && responseBody.contains("No metadata profiles found!")) {
                        LOG.info("No metadata profiles found in Kruize, returning empty list");
                        return Collections.emptyList();
                    }
                } catch (Exception ex) {
                    LOG.warn("Failed to read response body", ex);
                }
            }
            LOG.error(MessageConstants.KRUIZE_SERVICE_UNAVAILABLE, e);
            throw new KruizeServiceException(
                MessageConstants.KRUIZE_SERVICE_UNAVAILABLE,
                e,
                Response.Status.SERVICE_UNAVAILABLE.getStatusCode()
            );
        } catch (Exception e) {
            LOG.error(MessageConstants.KRUIZE_SERVICE_UNAVAILABLE, e);
            throw new KruizeServiceException(
                MessageConstants.KRUIZE_SERVICE_UNAVAILABLE,
                e,
                Response.Status.SERVICE_UNAVAILABLE.getStatusCode()
            );
        }
    }

    /**
     * Get metric profiles from Kruize
     * Always fetches with verbose=true to get profile_version
     *
     * @return List of metric profiles
     */
    public List<KruizeProfile> getMetricProfiles() {
        try {
            LOG.info("Fetching metric profiles from Kruize");
            // Always use verbose=true to get profile_version in response
            List<KruizeProfile> profiles = Optional.ofNullable(kruizeClient.getMetricProfiles(true))
                    .orElse(Collections.emptyList());
            // Set profile type for each profile
            profiles.forEach(p -> p.setProfileType(ProfileType.METRIC));
            return profiles;
        } catch (ClientWebApplicationException e) {
            // Check if this is a "No metric profiles found" error (400 status)
            // TODO: remove this when issue 1849 is fixed
            if (e.getResponse().getStatus() == 400) {
                try {
                    String responseBody = e.getResponse().readEntity(String.class);
                    if (responseBody != null && responseBody.contains("No metric profiles found!")) {
                        LOG.info("No metric profiles found in Kruize, returning empty list");
                        return Collections.emptyList();
                    }
                } catch (Exception ex) {
                    LOG.warn("Failed to read response body", ex);
                }
            }
            LOG.error(MessageConstants.KRUIZE_SERVICE_UNAVAILABLE, e);
            throw new KruizeServiceException(
                MessageConstants.KRUIZE_SERVICE_UNAVAILABLE,
                e,
                Response.Status.SERVICE_UNAVAILABLE.getStatusCode()
            );
        } catch (Exception e) {
            LOG.error(MessageConstants.KRUIZE_SERVICE_UNAVAILABLE, e);
            throw new KruizeServiceException(
                MessageConstants.KRUIZE_SERVICE_UNAVAILABLE,
                e,
                Response.Status.SERVICE_UNAVAILABLE.getStatusCode()
            );
        }
    }

    /**
     * Get layers from Kruize
     *
     * @return List of layers
     */
    public List<KruizeProfile> getLayers() {
        try {
            LOG.info("Fetching layers from Kruize");
            List<KruizeProfile> layers = Optional.ofNullable(kruizeClient.getLayers())
                    .orElse(Collections.emptyList());
            // Set profile type for each layer
            layers.forEach(p -> p.setProfileType(ProfileType.LAYER));
            return layers;
        } catch (ClientWebApplicationException e) {
            // Check if this is a "No layers found" error (400 status)
            // TODO: remove this when issue 1849 is fixed
            if (e.getResponse().getStatus() == 400) {
                try {
                    String responseBody = e.getResponse().readEntity(String.class);
                    if (responseBody != null && responseBody.contains("No layers found!")) {
                        LOG.info("No layers found in Kruize, returning empty list");
                        return Collections.emptyList();
                    }
                } catch (Exception ex) {
                    LOG.warn("Failed to read response body", ex);
                }
            }
            LOG.error(MessageConstants.KRUIZE_SERVICE_UNAVAILABLE, e);
            throw new KruizeServiceException(
                MessageConstants.KRUIZE_SERVICE_UNAVAILABLE,
                e,
                Response.Status.SERVICE_UNAVAILABLE.getStatusCode()
            );
        } catch (Exception e) {
            LOG.error(MessageConstants.KRUIZE_SERVICE_UNAVAILABLE, e);
            throw new KruizeServiceException(
                MessageConstants.KRUIZE_SERVICE_UNAVAILABLE,
                e,
                Response.Status.SERVICE_UNAVAILABLE.getStatusCode()
            );
        }
    }

    /**
     * Get bulk configs from Kruize
     *
     * @return List of bulk configs
     */
    public List<KruizeProfile> getBulkConfigs() {
        try {
            LOG.info("Fetching bulk configs from Kruize");
            String response = kruizeClient.getBulkConfigs(null);
            List<BulkConfig> bulkConfigs = objectMapper.readValue(
                    response,
                    new TypeReference<List<BulkConfig>>() {}
            );
            
            // Convert BulkConfig to KruizeProfile
            List<KruizeProfile> profiles = bulkConfigs.stream()
                    .map(bc -> {
                        KruizeProfile kp = new KruizeProfile();
                        kp.setName(bc.getConfigName());
                        kp.setProfileType(ProfileType.BULK);
                        // Note: BulkConfig doesn't have profile_version field in the response
                        return kp;
                    })
                    .collect(Collectors.toList());
            
            return profiles;
            
        } catch (ClientWebApplicationException e) {
            // Check if this is a "No bulk configs found" error (400 status)
            if (e.getResponse().getStatus() == 400) {
                try {
                    String responseBody = e.getResponse().readEntity(String.class);
                    if (responseBody != null && responseBody.contains("No bulk configs found")) {
                        LOG.info("No bulk configs found in Kruize, returning empty list");
                        return Collections.emptyList();
                    }
                } catch (Exception ex) {
                    LOG.warn("Failed to read response body", ex);
                }
            }
            LOG.error(MessageConstants.KRUIZE_SERVICE_UNAVAILABLE, e);
            throw new KruizeServiceException(
                    MessageConstants.KRUIZE_SERVICE_UNAVAILABLE,
                    e,
                    Response.Status.SERVICE_UNAVAILABLE.getStatusCode()
            );
        } catch (Exception e) {
            LOG.error(MessageConstants.KRUIZE_SERVICE_UNAVAILABLE, e);
            throw new KruizeServiceException(
                    MessageConstants.KRUIZE_SERVICE_UNAVAILABLE,
                    e,
                    Response.Status.SERVICE_UNAVAILABLE.getStatusCode()
            );
        }
    }

    /**
     * Install missing profiles from local repository
     *
     * @param profileType type of profile (metadata, metric, layer)
     * @return List of installation results
     */
    public List<String> installMissingProfiles(String profileType) {
        List<String> results = new ArrayList<>();
        
        try {
            // Get installed profiles
            List<KruizeProfile> installedProfiles = getInstalledProfiles(profileType);
            Set<String> installedNames = installedProfiles.stream()
                    .map(KruizeProfile::getName)
                    .collect(Collectors.toSet());

            // Get available profiles from local repository
            Map<String, String> availableProfiles = getAvailableProfilesFromLocal(profileType);

            // Install missing profiles
            for (Map.Entry<String, String> entry : availableProfiles.entrySet()) {
                String profileName = entry.getKey();
                String profileVersion = entry.getValue();
                
                if (!installedNames.contains(profileName)) {
                    try {
                        installProfile(profileType, profileName, profileVersion);
                        results.add("Installed: " + profileName);
                        LOG.info("Successfully installed profile: " + profileName);
                    } catch (Exception e) {
                        String error = "Failed to install " + profileName + ": " + e.getMessage();
                        results.add(error);
                        LOG.error(error, e);
                    }
                } else {
                    results.add("Already installed: " + profileName);
                }
            }

        } catch (Exception e) {
            LOG.error(MessageConstants.ERROR_INSTALLING_PROFILES, e);
            results.add("Error: " + e.getMessage());
        }

        return results;
    }

    /**
     * Install a specific profile
     *
     * @param profileType type of profile
     * @param profileName name of the profile
     * @param profileVersion version of the profile (null for layers)
     */
    private void installProfile(String profileType, String profileName, String profileVersion) {
        try {
            Object profileDefinition = loadProfileFromLocal(profileType, profileName, profileVersion);
            
            switch (profileType) {
                case ProfileType.METADATA:
                    kruizeClient.createMetadataProfile(profileDefinition);
                    break;
                case ProfileType.METRIC:
                    kruizeClient.createMetricProfile(profileDefinition);
                    break;
                case ProfileType.LAYER:
                    kruizeClient.createLayer(profileDefinition);
                    break;
                case ProfileType.BULK:
                    kruizeClient.createBulkConfig(
                            objectMapper.convertValue(profileDefinition, BulkConfig.class));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown profile type: " + profileType);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to install profile: " + profileName, e);
        }
    }

    /**
     * Load profile definition from local resources
     *
     * @param profileType type of profile
     * @param profileName name of the profile
     * @param profileVersion version of the profile (null for layers)
     * @return profile definition as Object
     */
    private Object loadProfileFromLocal(String profileType, String profileName, String profileVersion) {
        try {
            String resourcePath = getResourcePath(profileType, profileName, profileVersion);
            LOG.info("Loading profile from: " + resourcePath);
            
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    throw new RuntimeException(MessageConstants.PROFILE_NOT_FOUND + ": " + resourcePath);
                }
                return objectMapper.readValue(inputStream, Object.class);
            }
        } catch (Exception e) {
            throw new RuntimeException(MessageConstants.ERROR_READING_PROFILE_FILE + ": " + profileName, e);
        }
    }

    /**
     * Get resource path for profile
     *
     * @param profileType type of profile
     * @param profileName name of the profile
     * @param profileVersion version of the profile (null for layers)
     * @return resource path
     */
    private String getResourcePath(String profileType, String profileName, String profileVersion) {
        switch (profileType) {
            case ProfileType.METADATA:
                return ProfilePathConstants.CONFIGS_BASE_PATH + profileVersion +
                       ProfilePathConstants.METADATA_PROFILES_DIR + profileName +
                       ProfilePathConstants.JSON_EXTENSION;
            case ProfileType.METRIC:
                return ProfilePathConstants.CONFIGS_BASE_PATH + profileVersion +
                       ProfilePathConstants.METRIC_PROFILES_DIR + profileName +
                       ProfilePathConstants.JSON_EXTENSION;
            case ProfileType.LAYER:
                return ProfilePathConstants.LAYERS_DIR + profileName +
                       ProfilePathConstants.JSON_EXTENSION;
            case ProfileType.BULK:
                return ProfilePathConstants.CONFIGS_BASE_PATH + profileVersion +
                        ProfilePathConstants.BULK_CONFIGS_DIR + profileName +
                        ProfilePathConstants.JSON_EXTENSION;

            default:
                throw new IllegalArgumentException("Unknown profile type: " + profileType);
        }
    }

    /**
     * Get list of available profiles from local repository by reading configsReferenceIndex.json
     *
     * @param profileType type of profile
     * @return map of profile names to versions (version is null for layers)
     */
    private Map<String, String> getAvailableProfilesFromLocal(String profileType) {
        Map<String, String> profiles = new LinkedHashMap<>();
        
        try {
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(
                    ProfilePathConstants.CONFIGS_INDEX_FILE)) {
                if (inputStream == null) {
                    LOG.warn("configsReferenceIndex.json not found, returning empty list");
                    return profiles;
                }
                
                JsonNode rootNode = objectMapper.readTree(inputStream);
                JsonNode profilesNode = null;
                
                switch (profileType) {
                    case ProfileType.METADATA:
                        profilesNode = rootNode.get(ProfilePathConstants.METADATA_PROFILES_KEY);
                        if (profilesNode != null && profilesNode.isArray()) {
                            for (JsonNode profileNode : profilesNode) {
                                String name = profileNode.get(ProfilePathConstants.NAME_KEY).asText();
                                String version = profileNode.get(ProfilePathConstants.PROFILE_VERSION_KEY).asText();
                                profiles.put(name, version);
                            }
                        }
                        break;
                    case ProfileType.METRIC:
                        profilesNode = rootNode.get(ProfilePathConstants.METRIC_PROFILES_KEY);
                        if (profilesNode != null && profilesNode.isArray()) {
                            for (JsonNode profileNode : profilesNode) {
                                String name = profileNode.get(ProfilePathConstants.NAME_KEY).asText();
                                String version = profileNode.get(ProfilePathConstants.PROFILE_VERSION_KEY).asText();
                                profiles.put(name, version);
                            }
                        }
                        break;
                    case ProfileType.LAYER:
                        profilesNode = rootNode.get(ProfilePathConstants.LAYERS_KEY);
                        if (profilesNode != null && profilesNode.isArray()) {
                            for (JsonNode layerNode : profilesNode) {
                                String name = layerNode.asText();
                                profiles.put(name, null); // Layers don't have versions
                            }
                        }
                        break;
                    case ProfileType.BULK:
                        profilesNode = rootNode.get(ProfilePathConstants.BULK_CONFIGS_KEY);
                        if (profilesNode != null && profilesNode.isArray()) {
                            for (JsonNode profileNode : profilesNode) {
                                String name = profileNode.get(ProfilePathConstants.NAME_KEY).asText();
                                String version = profileNode.get(ProfilePathConstants.PROFILE_VERSION_KEY).asText();
                                profiles.put(name, version);
                            }
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown profile type: " + profileType);
                }
            }
        } catch (Exception e) {
            LOG.error("Error reading configsReferenceIndex.json", e);
        }
        
        return profiles;
    }

    /**
     * Get installed profiles based on type
     *
     * @param profileType type of profile
     * @return list of installed profiles
     */
    private List<KruizeProfile> getInstalledProfiles(String profileType) {
        switch (profileType) {
            case ProfileType.METADATA:
                return getMetadataProfiles();
            case ProfileType.METRIC:
                return getMetricProfiles();
            case ProfileType.LAYER:
                return getLayers();
            case ProfileType.BULK:
                return getBulkConfigs();
            default:
                throw new IllegalArgumentException("Unknown profile type: " + profileType);
        }
    }
}

