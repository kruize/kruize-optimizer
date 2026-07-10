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

import com.kruize.optimizer.model.kruize.Datasource;
import com.kruize.optimizer.model.kruize.KruizeProfile;
import com.kruize.optimizer.utils.OptimizerConstants;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Global state service that caches Kruize configuration
 * (datasources, profiles, layers) and provides access to them.
 */
@ApplicationScoped
public class KruizeStateService {

    private static final Logger LOG = Logger.getLogger(KruizeStateService.class);

    @Inject
    DatasourceService datasourceService;

    @Inject
    ProfileService profileService;

    @ConfigProperty(name = "kruize.defaults.datasource")
    String defaultDatasource;

    @ConfigProperty(name = "kruize.defaults.metadata-profile")
    String defaultMetadataProfile;

    @ConfigProperty(name = "kruize.defaults.metric-profile")
    String defaultMetricProfile;

    private List<Datasource> cachedDatasources = new ArrayList<>();
    private List<KruizeProfile> cachedMetadataProfiles = new ArrayList<>();
    private List<KruizeProfile> cachedMetricProfiles = new ArrayList<>();
    private List<KruizeProfile> cachedLayers = new ArrayList<>();
    private LocalDateTime lastChecked;

    /**
     * Refresh the cached state from Kruize
     */
    public synchronized void refreshState() {
        try {
            LOG.info("Refreshing Kruize state cache...");
            
            // Fetch datasources
            cachedDatasources = datasourceService.getDatasources();
            LOG.infof("Cached %d datasources", cachedDatasources.size());

            // Fetch metadata profiles
            cachedMetadataProfiles = profileService.getMetadataProfiles();
            LOG.infof("Cached %d metadata profiles", cachedMetadataProfiles.size());

            // Fetch metric profiles
            cachedMetricProfiles = profileService.getMetricProfiles();
            LOG.infof("Cached %d metric profiles", cachedMetricProfiles.size());

            // Fetch layers
            cachedLayers = profileService.getLayers();
            LOG.infof("Cached %d layers", cachedLayers.size());

            lastChecked = LocalDateTime.now();
            LOG.info("Kruize state cache refreshed successfully");

        } catch (Exception e) {
            LOG.error("Failed to refresh Kruize state cache", e);
        }
    }

    /**
     * Install missing profiles (metadata, metric, layers) from local repository
     */
    public void installMissingProfiles() {
        try {
            LOG.info("Installing missing profiles...");
            
            // Install missing metadata profiles
            List<String> metadataResults = profileService.installMissingProfiles("metadata");
            metadataResults.forEach(result -> LOG.info("Metadata profile: " + result));
            
            // Install missing metric profiles
            List<String> metricResults = profileService.installMissingProfiles("metric");
            metricResults.forEach(result -> LOG.info("Metric profile: " + result));
            
            // Install missing layers
            List<String> layerResults = profileService.installMissingProfiles("layer");
            layerResults.forEach(result -> LOG.info("Layer: " + result));

            // Install missing bulk configs
            List<String> bulkResults = profileService.installMissingProfiles(OptimizerConstants.ProfileType.BULK);
            bulkResults.forEach(result -> LOG.info("Bulk config: " + result));
            
            // Refresh cache after installation
            refreshState();
            
            LOG.info("Missing profiles installation completed");
        } catch (Exception e) {
            LOG.error("Failed to install missing profiles", e);
        }
    }

    /**
     * Common function to refresh state and install missing profiles
     * This is called both on startup and periodically
     */
    public void refreshStateAndInstallProfiles() {
        LOG.info("Refreshing Kruize state and installing missing profiles...");
        
        // Refresh state first
        refreshState();
        
        // Install any missing profiles
        installMissingProfiles();
        
        LOG.info("State refresh and profile installation completed");
    }

    /**
     * Scheduled method to periodically refresh state and install missing profiles
     * The interval is configured via kruize.state.refresh.interval property
     * Delayed to avoid running immediately on startup (manual call handles first run)
     */
    @Scheduled(every = "${kruize.state.refresh.interval:30m}", delayed = "${kruize.state.refresh.interval:30m}")
    public void scheduledStateRefresh() {
        LOG.info("Starting scheduled state refresh and profile installation");
        refreshStateAndInstallProfiles();
    }

    /**
     * Get the default datasource name (falls back to first available if default not found)
     *
     * @return Optional datasource name
     */
    public Optional<String> getDefaultDatasourceName() {
        // Try to find the default datasource first
        Optional<String> defaultDs = cachedDatasources.stream()
                .filter(ds -> defaultDatasource.equals(ds.getName()))
                .map(Datasource::getName)
                .findFirst();
        
        if (defaultDs.isPresent()) {
            return defaultDs;
        }
        
        // Fall back to first available
        return cachedDatasources.stream()
                .findFirst()
                .map(Datasource::getName);
    }

    /**
     * Get all cached datasources
     *
     * @return List of datasources
     */
    public List<Datasource> getCachedDatasources() {
        return new ArrayList<>(cachedDatasources);
    }

    /**
     * Get all cached metadata profiles
     *
     * @return List of metadata profiles
     */
    public List<KruizeProfile> getCachedMetadataProfiles() {
        return new ArrayList<>(cachedMetadataProfiles);
    }

    /**
     * Get the default metadata profile name (falls back to first available if default not found)
     *
     * @return Optional metadata profile name
     */
    public Optional<String> getDefaultMetadataProfileName() {
        // Try to find the default metadata profile first
        Optional<String> defaultProfile = cachedMetadataProfiles.stream()
                .filter(p -> defaultMetadataProfile.equals(p.getName()))
                .map(KruizeProfile::getName)
                .findFirst();
        
        if (defaultProfile.isPresent()) {
            return defaultProfile;
        }
        
        // Fall back to first available
        return cachedMetadataProfiles.stream()
                .findFirst()
                .map(KruizeProfile::getName);
    }

    /**
     * Get the default metric profile name (falls back to first available if default not found)
     *
     * @return Optional metric profile name
     */
    public Optional<String> getDefaultMetricProfileName() {
        // Try to find the default metric profile first
        Optional<String> defaultProfile = cachedMetricProfiles.stream()
                .filter(p -> defaultMetricProfile.equals(p.getName()))
                .map(KruizeProfile::getName)
                .findFirst();
        
        if (defaultProfile.isPresent()) {
            return defaultProfile;
        }
        
        // Fall back to first available
        return cachedMetricProfiles.stream()
                .findFirst()
                .map(KruizeProfile::getName);
    }

    /**
     * Get all cached metric profiles
     *
     * @return List of metric profiles
     */
    public List<KruizeProfile> getCachedMetricProfiles() {
        return new ArrayList<>(cachedMetricProfiles);
    }

    /**
     * Get all cached layers
     *
     * @return List of layers
     */
    public List<KruizeProfile> getCachedLayers() {
        return new ArrayList<>(cachedLayers);
    }

    /**
     * Get the last time the cache was refreshed
     *
     * @return LocalDateTime of last refresh
     */
    public LocalDateTime getLastChecked() {
        return lastChecked;
    }

    /**
     * Check if the cache is empty
     *
     * @return true if cache is empty
     */
    public boolean isCacheEmpty() {
        return cachedDatasources.isEmpty();
    }
}

