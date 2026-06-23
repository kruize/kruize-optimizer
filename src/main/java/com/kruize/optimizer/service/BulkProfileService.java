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
import com.kruize.optimizer.model.kruize.BulkProfile;
import com.kruize.optimizer.model.kruize.ClusterConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for managing bulk profiles and converting them to bulk job requests
 */
@ApplicationScoped
public class BulkProfileService {

    private static final Logger LOG = Logger.getLogger(BulkProfileService.class);

    @Inject
    @RestClient
    KruizeClient kruizeClient;

    @Inject
    ObjectMapper objectMapper;

    // Regex pattern for parsing scheduling strings like "24h", "15min", "2d"
    private static final Pattern SCHEDULING_PATTERN = 
        Pattern.compile("(\\d+)\\s*(h|hr|hrs|hour|hours|m|min|mins|minute|minutes|d|day|days)");

    /**
     * Fetch all enabled profiles from Kruize
     *
     * @return List of enabled bulk profiles
     */
    public List<BulkProfile> getEnabledProfiles() {
        try {
            String response = kruizeClient.getBulkProfiles();
            List<BulkProfile> allProfiles = objectMapper.readValue(
                response, 
                new TypeReference<List<BulkProfile>>() {}
            );
            
            List<BulkProfile> enabledProfiles = allProfiles.stream()
                .filter(p -> p.getEnabled() != null && p.getEnabled())
                .collect(Collectors.toList());
            
            LOG.infof("Fetched %d enabled profiles out of %d total profiles", 
                enabledProfiles.size(), allProfiles.size());
            
            return enabledProfiles;
            
        } catch (Exception e) {
            LOG.error("Failed to fetch bulk profiles from Kruize", e);
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
     * Convert bulk profile to bulk job request
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
     * @param profile Bulk profile
     * @return Bulk job request as Map
     */
    public Map<String, Object> convertProfileToBulkJob(BulkProfile profile) {
        Map<String, Object> bulkJob = new HashMap<>();

        // Get the first cluster config (assuming single cluster for now)
        if (profile.getClusters() == null || profile.getClusters().isEmpty()) {
            throw new IllegalArgumentException("Profile must have at least one cluster configuration");
        }
        
        ClusterConfig cluster = profile.getClusters().get(0);

        // Build filter with labels
        if (cluster.getLabels() != null && !cluster.getLabels().isEmpty()) {
            Map<String, Object> filter = new HashMap<>();
            Map<String, Object> include = new HashMap<>();
            include.put("labels", cluster.getLabels());
            filter.put("include", include);
            bulkJob.put("filter", filter);
        }

        // Add datasource (use first datasource from cluster)
        if (cluster.getDatasources() != null && !cluster.getDatasources().isEmpty()) {
            bulkJob.put("datasource", cluster.getDatasources().get(0));
        }

        // Add metadata profile from cluster
        if (cluster.getMetadataProfile() != null && !cluster.getMetadataProfile().isEmpty()) {
            bulkJob.put("metadata_profile", cluster.getMetadataProfile());
        }

        // Add measurement duration from recommendation settings
        String measurementDuration = profile.getRecommendationSettings().getMeasurementDuration();
        if (measurementDuration != null && !measurementDuration.isEmpty()) {
            bulkJob.put("measurement_duration", measurementDuration);
        }

        // Add webhook URL if present
        if (profile.getWebhookUrl() != null && !profile.getWebhookUrl().isEmpty()) {
            Map<String, String> webhook = new HashMap<>();
            webhook.put("url", profile.getWebhookUrl());
            bulkJob.put("webhook", webhook);
        }

        LOG.debugf("Converted profile '%s' to bulk job request: %s",
                   profile.getProfileName(), bulkJob);
        
        return bulkJob;
    }
}

