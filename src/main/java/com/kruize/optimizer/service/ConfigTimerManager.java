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
import com.kruize.optimizer.model.kruize.BulkConfig;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Manages individual timers for each bulk config
 */
@ApplicationScoped
public class ConfigTimerManager {

    private static final Logger LOG = Logger.getLogger(ConfigTimerManager.class);

    @Inject
    BulkConfigService bulkConfigService;

    @Inject
    @RestClient
    KruizeClient kruizeClient;

    @Inject
    JobsService jobsService;

    // Map of config name to scheduled future
    private final Map<String, ScheduledFuture<?>> configTimers = new ConcurrentHashMap<>();

    // Scheduler for executing timers
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    /**
     * Schedule a timer for a config (executes immediately, then on interval)
     *
     * @param config Bulk config to schedule
     */
    public void scheduleConfig(BulkConfig config) {
        scheduleConfig(config, 0);
    }

    /**
     * Schedule a timer for a config with a custom initial delay
     *
     * @param config Bulk config to schedule
     * @param initialDelayMillis delay before first execution
     */
    public void scheduleConfig(BulkConfig config, long initialDelayMillis) {
        String configName = config.getConfigName();

        // Cancel existing timer if any
        cancelConfigTimer(configName);

        if (config.getRecommendationSettings() == null
                || config.getRecommendationSettings().getScheduling() == null
                || config.getRecommendationSettings().getScheduling().isBlank()) {
            LOG.errorf("Cannot schedule config '%s': recommendation_settings.scheduling is missing",
                    configName);
            return;
        }

        // Parse scheduling interval
        Duration interval = bulkConfigService.parseScheduling(
                config.getRecommendationSettings().getScheduling()
        );

        long delay = Math.max(0, initialDelayMillis);
        LOG.infof("Scheduling config '%s' with interval: %s (initial delay: %dms)",
                configName, interval, delay);

        // Catch Throwable so scheduleAtFixedRate does not permanently cancel the timer
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        executeConfigJob(config);
                    } catch (Throwable t) {
                        LOG.errorf(t,
                                "Unexpected error executing bulk job for config '%s'; timer will continue",
                                config.getConfigName());
                    }
                },
                delay,
                interval.toMillis(),
                TimeUnit.MILLISECONDS
        );

        configTimers.put(configName, future);
    }

    /**
     * Update config timer (called from webhook)
     *
     * @param updatedConfig Updated bulk config
     */
    public void updateConfigTimer(BulkConfig updatedConfig) {
        String configName = updatedConfig.getConfigName();

        if (updatedConfig.getEnabled() == null || !updatedConfig.getEnabled()) {
            // Config disabled - cancel timer
            LOG.infof("Config '%s' disabled, canceling timer", configName);
            cancelConfigTimer(configName);
            return;
        }

        // Get current timer
        ScheduledFuture<?> currentTimer = configTimers.get(configName);

        if (currentTimer == null) {
            // No existing timer - schedule new one
            LOG.infof("No existing timer for '%s', scheduling new", configName);
            scheduleConfig(updatedConfig);
            return;
        }

        if (updatedConfig.getRecommendationSettings() == null
                || updatedConfig.getRecommendationSettings().getScheduling() == null
                || updatedConfig.getRecommendationSettings().getScheduling().isBlank()) {
            LOG.errorf("Cannot update timer for config '%s': recommendation_settings.scheduling is missing",
                    configName);
            return;
        }

        // Calculate new interval
        Duration newInterval = bulkConfigService.parseScheduling(
                updatedConfig.getRecommendationSettings().getScheduling()
        );

        // Get time until next execution (may be negative if currently running)
        long delayMillis = Math.max(0, currentTimer.getDelay(TimeUnit.MILLISECONDS));

        cancelConfigTimer(configName);

        if (delayMillis > newInterval.toMillis()) {
            // Remaining wait exceeds new interval - run soon, then use new period
            LOG.infof("Config '%s' interval shortened (remaining %dms > new %dms), executing immediately",
                    configName, delayMillis, newInterval.toMillis());
            scheduleConfig(updatedConfig, 0);
        } else {
            // Preserve remaining delay before applying the new period
            LOG.infof("Config '%s' interval changed to %s, rescheduling with remaining delay %dms",
                    configName, newInterval, delayMillis);
            scheduleConfig(updatedConfig, delayMillis);
        }
    }

    /**
     * Cancel timer for a config
     *
     * @param configName Config name
     */
    public void cancelConfigTimer(String configName) {
        ScheduledFuture<?> future = configTimers.remove(configName);
        if (future != null) {
            future.cancel(false);
            LOG.infof("Canceled timer for config '%s'", configName);
        }
    }

    /**
     * Execute bulk job for a config
     *
     * @param config Bulk config
     */
    private void executeConfigJob(BulkConfig config) {
        try {
            LOG.infof("Executing bulk job for config: %s", config.getConfigName());

            // Convert config to bulk job
            Map<String, Object> bulkJob = bulkConfigService.convertConfigToBulkJob(config);

            // Call bulk API
            String response = kruizeClient.bulkCreateExperiments(bulkJob);

            LOG.infof("Bulk job created for config '%s': %s",
                    config.getConfigName(), response);

            // Track job with config name
            jobsService.incrementJobsTriggered(config.getConfigName());

        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute bulk job for config '%s'",
                    config.getConfigName());
        }
    }

    /**
     * Initialize all configs at startup
     */
    public void initializeConfigs() {
        try {
            LOG.info("Initializing config timers...");

            List<BulkConfig> configs = bulkConfigService.getEnabledConfigs();

            LOG.infof("Found %d enabled configs", configs.size());

            for (BulkConfig config : configs) {
                scheduleConfig(config);
            }

            LOG.info("Config timers initialized successfully");

        } catch (Exception e) {
            LOG.error("Failed to initialize config timers", e);
        }
    }

    /**
     * Shutdown all timers
     */
    @PreDestroy
    public void shutdown() {
        LOG.info("Shutting down config timers...");
        configTimers.values().forEach(f -> f.cancel(false));
        configTimers.clear();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOG.info("Config timers shut down successfully");
    }

    /**
     * Get count of active timers
     *
     * @return Number of active config timers
     */
    public int getActiveTimerCount() {
        return configTimers.size();
    }
}