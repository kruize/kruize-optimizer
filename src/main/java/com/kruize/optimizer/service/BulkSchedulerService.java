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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kruize.optimizer.client.KruizeClient;
import com.kruize.optimizer.model.WebhookPayload;
import com.kruize.optimizer.utils.OptimizerConstants.MessageConstants;
import com.kruize.optimizer.utils.OptimizerConstants.BulkSchedulerConstants;
import com.kruize.optimizer.utils.OptimizerConstants.WebhookConstants;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.*;

/**
 * Service that schedules bulk API calls at configurable intervals
 * with a configurable target label.
 */
@ApplicationScoped
public class BulkSchedulerService {

    private static final Logger LOG = Logger.getLogger(BulkSchedulerService.class);

    @Inject
    @RestClient
    KruizeClient kruizeClient;

    @Inject
    KruizeStateService kruizeStateService;

    @Inject
    JobsService jobsService;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "kruize.bulk.scheduler.measurement-duration")
    String measurementDuration;

    @ConfigProperty(name = "kruize.webhook.url")
    String webhookUrl;
    
    @ConfigProperty(name = "kruize.target.labels.json", defaultValue = "{\"kruize/autotune\": \"enabled\"}")
    String targetLabelsJson;

    @ConfigProperty(name = "kruize.bulk.scheduler.startup-delay", defaultValue = "1m")
    String startupDelay;

    private final Set<String> completedJobs = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private volatile boolean initialized = false;

    /**
     * Initialize the bulk scheduler by refreshing state and installing missing profiles
     */
    public void initialize() {
        try {
            LOG.info(MessageConstants.INFO_INITIALIZING_BULK_SCHEDULER);
            
            // Use common function to refresh state and install missing profiles
            kruizeStateService.refreshStateAndInstallProfiles();
            
            initialized = true;
            LOG.info(MessageConstants.INFO_BULK_SCHEDULER_INITIALIZED);
        } catch (Exception e) {
            LOG.error(MessageConstants.ERROR_FAILED_TO_INITIALIZE_BULK_SCHEDULER, e);
        }
    }

    /**
     * Scheduled method that calls the bulk API at the configured interval.
     * The interval is configured via kruize.bulk.scheduler.interval property.
     * Waits for initialization to complete before executing.
     */
    @Scheduled(every = "${kruize.bulk.scheduler.interval:5m}", delayed = "${kruize.bulk.scheduler.startup-delay:1m}")
    public void scheduledBulkApiCall() {
        if (!initialized) {
            LOG.debug(MessageConstants.INFO_BULK_SCHEDULER_NOT_INITIALIZED);
            return;
        }

        LOG.infof(MessageConstants.INFO_STARTING_SCHEDULED_BULK_API_CALL, targetLabelsJson);

        try {
            // Parse target labels from JSON
            Map<String, String> targetLabels = parseTargetLabels();
            if (targetLabels.isEmpty()) {
                LOG.error(MessageConstants.ERROR_NO_VALID_TARGET_LABELS);
                return;
            }

            // Check if state cache is empty, refresh if needed
            if (kruizeStateService.isCacheEmpty()) {
                LOG.debug(MessageConstants.INFO_KRUIZE_STATE_CACHE_EMPTY);
                kruizeStateService.refreshState();
            }

            // Get datasources from global state
            List<String> datasourceNames = kruizeStateService.getDefaultDatasourceNames();
            if (datasourceNames.isEmpty()) {
                LOG.error(MessageConstants.ERROR_NO_DATASOURCES_AVAILABLE);
                return;
            }

            // Get metadata profile from global state
            Optional<String> metadataProfileName = kruizeStateService.getDefaultMetadataProfileName();
            if (!metadataProfileName.isPresent()) {
                LOG.error(MessageConstants.ERROR_NO_METADATA_PROFILE_AVAILABLE);
                return;
            }

            // Get metric profile from global state
            Optional<String> metricProfileName = kruizeStateService.getDefaultMetricProfileName();
            if (!metricProfileName.isPresent()) {
                LOG.error(MessageConstants.ERROR_NO_METRIC_PROFILE_AVAILABLE);
                return;
            }

            // Construct the bulk API payload
            Map<String, Object> payload = buildBulkPayload(
                    targetLabels,
                    datasourceNames,
                    metadataProfileName.get(),
                    metricProfileName.get()
            );

            // Log the exact JSON payload before calling the bulk API
            try {
                String jsonPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
                LOG.infof(MessageConstants.INFO_CALLING_BULK_API_WITH_PAYLOAD, jsonPayload);
            } catch (Exception e) {
                LOG.warnf(e, MessageConstants.WARN_FAILED_TO_SERIALIZE_PAYLOAD);
            }

            // Call the bulk API
            String response = kruizeClient.bulkCreateExperiments(payload);
            LOG.infof(MessageConstants.INFO_BULK_API_CALL_SUCCESSFUL, response);

            // Increment job counter in global state
            jobsService.incrementJobsTriggered();

        } catch (Exception e) {
            LOG.errorf(e, MessageConstants.ERROR_FAILED_TO_EXECUTE_SCHEDULED_BULK_API_CALL);
        }
    }

    /**
     * Parse target labels from JSON configuration
     *
     * @return Map of label key-value pairs
     */
    private Map<String, String> parseTargetLabels() {
        Map<String, String> labels = new HashMap<>();
        try {
            // Simple JSON parsing for {"key": "value"} format
            String json = targetLabelsJson.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1);
                String[] pairs = json.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replaceAll("\"", "");
                        String value = keyValue[1].trim().replaceAll("\"", "");
                        labels.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            LOG.errorf(e, MessageConstants.ERROR_FAILED_TO_PARSE_TARGET_LABELS, targetLabelsJson);
        }
        return labels;
    }

    /**
     * Builds the payload for the bulk API call.
     *
     * @param targetLabels       The target labels to filter workloads
     * @param datasources        The list of datasource names
     * @param metadataProfile    The metadata profile name
     * @param metricProfile      The metric profile name
     * @return The bulk API payload as a Map
     */
    private Map<String, Object> buildBulkPayload(Map<String, String> targetLabels,
                                                   List<String> datasources, String metadataProfile, String metricProfile) {
        Map<String, Object> payload = new HashMap<>();

        // Create filter with the target labels
        Map<String, Object> filter = new HashMap<>();
        Map<String, Object> include = new HashMap<>();
        
        // Add label filter
        include.put(BulkSchedulerConstants.LABELS, targetLabels);

        filter.put(BulkSchedulerConstants.INCLUDE, include);
        payload.put(BulkSchedulerConstants.FILTER, filter);

        // Add datasources based on availability:
        // - If list is present, use the new 'datasources' field (supports multiple datasources)
        // - If list is absent/empty, fall back to deprecated 'datasource' field (single datasource)
        if (!datasources.isEmpty()) {
            payload.put(BulkSchedulerConstants.DATASOURCES, datasources);
        } else {
            // This case should not occur as we check for empty datasources earlier,
            // but kept for completeness
            payload.put(BulkSchedulerConstants.DATASOURCE, null);
        }

        // Add metadata profile from global state
        payload.put(BulkSchedulerConstants.METADATA_PROFILE, metadataProfile);

        // Add measurement duration
        payload.put(BulkSchedulerConstants.MEASUREMENT_DURATION, measurementDuration);

        // Add webhook URL
        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            Map<String, String> webhook = new HashMap<>();
            webhook.put(BulkSchedulerConstants.URL, webhookUrl);
            payload.put(BulkSchedulerConstants.WEBHOOK_KEY, webhook);
        }

        LOG.debugf(MessageConstants.DEBUG_BUILT_BULK_PAYLOAD, payload);
        return payload;
    }

    /**
     * Handle webhook callbacks from Kruize bulk API
     *
     * @param payloads List of webhook payloads
     */
    public void handleWebhook(List<WebhookPayload> payloads) {
        for (WebhookPayload payload : payloads) {
            if (payload.getSummary() != null) {
                WebhookPayload.Summary summary = payload.getSummary();
                String jobId = summary.getJobId();
                String status = summary.getStatus();
                LOG.debugf(MessageConstants.INFO_RECEIVED_WEBHOOK_FOR_JOB, jobId, status);

                if (WebhookConstants.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                    if (!completedJobs.add(jobId)) {
                        LOG.infof(MessageConstants.INFO_JOB_ALREADY_PROCESSED, jobId);
                        continue;
                    }

                    int total = summary.getTotalExperiments();
                    int processed = summary.getProcessedExperiments();
                    int existing = summary.getExistingExperiments();

                    // Update experiment counters in global state
                    jobsService.updateExperimentCounters(total, processed, existing);

                    LOG.infof(MessageConstants.INFO_JOB_COMPLETED,
                            jobId, total, processed, existing);
                } else {
                    LOG.infof(MessageConstants.INFO_JOB_STATUS, jobId, status);
                }
            }
        }
    }
}

