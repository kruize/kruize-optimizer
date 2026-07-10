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
import com.kruize.optimizer.model.kruize.BulkConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for managing bulk configs and converting them to bulk job requests
 */
@ApplicationScoped
public class BulkConfigService {

    private static final Logger LOG = Logger.getLogger(BulkConfigService.class);

    @Inject
    @RestClient
    KruizeClient kruizeClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "kruize.webhook.url")
    String webhookUrl;

    // Regex pattern for parsing scheduling strings like "24h", "15min", "2d"
    private static final Pattern SCHEDULING_PATTERN =
            Pattern.compile("(\\d+)\\s*(h|hr|hrs|hour|hours|m|min|mins|minute|minutes|d|day|days)");

    /**
     * Fetch all enabled configs from Kruize
     *
     * @return List of enabled bulk configs
     */
    public List<BulkConfig> getEnabledConfigs() {
        try {
            String response = kruizeClient.getBulkConfigs();
            List<BulkConfig> allConfigs = objectMapper.readValue(
                    response,
                    new TypeReference<List<BulkConfig>>() {}
            );

            List<BulkConfig> enabledConfigs = allConfigs.stream()
                    .filter(c -> c.getEnabled() != null && c.getEnabled())
                    .collect(Collectors.toList());

            LOG.infof("Fetched %d enabled configs out of %d total configs",
                    enabledConfigs.size(), allConfigs.size());

            return enabledConfigs;

        } catch (Exception e) {
            LOG.error("Failed to fetch bulk configs from Kruize", e);
            return Collections.emptyList();
        }
    }

    /**
     * Parse scheduling string to Duration
     * Examples: "24h" -> 24 hours, "15min" -> 15 minutes, "2d" -> 2 days
     *
     * @param scheduling Scheduling string
     * @return Duration object
     */
    public Duration parseScheduling(String scheduling) {
        if (scheduling == null || scheduling.trim().isEmpty()) {
            throw new IllegalArgumentException("Scheduling string cannot be null or empty");
        }

        Matcher matcher = SCHEDULING_PATTERN.matcher(scheduling.trim().toLowerCase());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid scheduling format: " + scheduling);
        }

        int value = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2);

        switch (unit) {
            case "h":
            case "hr":
            case "hrs":
            case "hour":
            case "hours":
                return Duration.ofHours(value);
            case "m":
            case "min":
            case "mins":
            case "minute":
            case "minutes":
                return Duration.ofMinutes(value);
            case "d":
            case "day":
            case "days":
                return Duration.ofDays(value);
            default:
                throw new IllegalArgumentException("Unsupported time unit: " + unit);
        }
    }

    /**
     * Convert bulk config to bulk job request
     *
     * Expected bulk API format:
     * {
     *   "filter": {
     *     "include": {
     *       "labels": {"key": "value"}
     *     }
     *   },
     *   "datasource": "datasource-name",
     *   "metadata_profile": "profile-name",
     *   "measurement_duration": "15min",
     *   "webhook": {
     *     "url": "http://..."
     *   }
     * }
     *
     * @param config Bulk config
     * @return Bulk job request as Map
     */
    public Map<String, Object> convertConfigToBulkJob(BulkConfig config) {
        Map<String, Object> bulkJob = new HashMap<>();

        // Build filter with labels
        if (config.getLabels() != null && !config.getLabels().isEmpty()) {
            Map<String, Object> filter = new HashMap<>();
            Map<String, Object> include = new HashMap<>();
            include.put("labels", config.getLabels());
            filter.put("include", include);
            bulkJob.put("filter", filter);
        }

        // Add datasource (use first datasource)
        if (config.getDatasources() != null && !config.getDatasources().isEmpty()) {
            bulkJob.put("datasource", config.getDatasources().get(0));
        }

        // Add metadata profile
        if (config.getMetadataProfile() != null && !config.getMetadataProfile().isEmpty()) {
            bulkJob.put("metadata_profile", config.getMetadataProfile());
        }

        // Add measurement duration from trial settings
        if (config.getTrialSettings() != null) {
            String measurementDuration = config.getTrialSettings().getMeasurementDuration();
            if (measurementDuration != null && !measurementDuration.isEmpty()) {
                bulkJob.put("measurement_duration", measurementDuration);
            }
        }

        // Add experiment_types from config
        if (config.getExperimentTypes() != null && !config.getExperimentTypes().isEmpty()) {
            bulkJob.put("experiment_types", config.getExperimentTypes());
        }

        // Add cluster_name from config
        if (config.getClusterName() != null && !config.getClusterName().isEmpty()) {
            bulkJob.put("cluster_name", config.getClusterName());
        }

        // Add model_settings from recommendation settings
        if (config.getRecommendationSettings() != null &&
                config.getRecommendationSettings().getModels() != null &&
                !config.getRecommendationSettings().getModels().isEmpty()) {
            Map<String, Object> modelSettings = new HashMap<>();
            modelSettings.put("models", config.getRecommendationSettings().getModels());
            bulkJob.put("model_settings", modelSettings);
        }

        // Add term_settings from recommendation settings
        if (config.getRecommendationSettings() != null &&
                config.getRecommendationSettings().getTerms() != null &&
                !config.getRecommendationSettings().getTerms().isEmpty()) {
            Map<String, Object> termSettings = new HashMap<>();
            termSettings.put("terms", config.getRecommendationSettings().getTerms());
            bulkJob.put("term_settings", termSettings);
        }

        // Add webhook URL if present
        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            Map<String, String> webhook = new HashMap<>();
            webhook.put("url", webhookUrl);
            bulkJob.put("webhook", webhook);
        }

        // Log the complete bulk job JSON
        try {
            String jsonPayload = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(bulkJob);
            LOG.infof("Converted config '%s' to bulk job:\n%s",
                    config.getConfigName(), jsonPayload);
        } catch (Exception e) {
            LOG.warnf(e, "Failed to serialize bulk job for config '%s'",
                    config.getConfigName());
        }

        LOG.debugf("Converted config '%s' to bulk job request: %s",
                config.getConfigName(), bulkJob);
        return bulkJob;
    }
}