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
import com.kruize.optimizer.model.kruize.BulkProfile;
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
 * Manages individual timers for each bulk profile
 */
@ApplicationScoped
public class ProfileTimerManager {

    private static final Logger LOG = Logger.getLogger(ProfileTimerManager.class);

    @Inject
    BulkProfileService bulkProfileService;

    @Inject
    @RestClient
    KruizeClient kruizeClient;

    @Inject
    JobsService jobsService;

    // Map of profile name to scheduled future
    private final Map<String, ScheduledFuture<?>> profileTimers = new ConcurrentHashMap<>();

    // Scheduler for executing timers
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    /**
     * Schedule a timer for a profile
     *
     * @param profile Bulk profile to schedule
     */
    public void scheduleProfile(BulkProfile profile) {
        String profileName = profile.getProfileName();

        // Cancel existing timer if any
        cancelProfileTimer(profileName);

        // Parse scheduling interval
        Duration interval = bulkProfileService.parseScheduling(
            profile.getRecommendationSettings().getScheduling()
        );

        LOG.infof("Scheduling profile '%s' with interval: %s", profileName, interval);

        // Schedule recurring task
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            () -> executeProfileJob(profile),
            0,  // Initial delay = 0 (execute immediately)
            interval.toMillis(),
            TimeUnit.MILLISECONDS
        );

        profileTimers.put(profileName, future);
    }

    /**
     * Update profile timer (called from webhook)
     *
     * @param updatedProfile Updated bulk profile
     */
    public void updateProfileTimer(BulkProfile updatedProfile) {
        String profileName = updatedProfile.getProfileName();

        if (updatedProfile.getEnabled() == null || !updatedProfile.getEnabled()) {
            // Profile disabled - cancel timer
            LOG.infof("Profile '%s' disabled, canceling timer", profileName);
            cancelProfileTimer(profileName);
            return;
        }

        // Get current timer
        ScheduledFuture<?> currentTimer = profileTimers.get(profileName);

        if (currentTimer == null) {
            // No existing timer - schedule new one
            LOG.infof("No existing timer for '%s', scheduling new", profileName);
            scheduleProfile(updatedProfile);
            return;
        }

        // Calculate new interval
        Duration newInterval = bulkProfileService.parseScheduling(
            updatedProfile.getRecommendationSettings().getScheduling()
        );

        // Get time until next execution
        long delayMillis = currentTimer.getDelay(TimeUnit.MILLISECONDS);

        if (delayMillis > newInterval.toMillis()) {
            // New interval is shorter - execute immediately and reschedule
            LOG.infof("Profile '%s' interval shortened from %dms to %dms, executing immediately",
                profileName, delayMillis, newInterval.toMillis());
            cancelProfileTimer(profileName);
            scheduleProfile(updatedProfile);
        } else {
            // New interval is longer or similar - just reschedule for next run
            LOG.infof("Profile '%s' interval changed to %s, rescheduling",
                profileName, newInterval);
            cancelProfileTimer(profileName);
            scheduleProfile(updatedProfile);
        }
    }

    /**
     * Cancel timer for a profile
     *
     * @param profileName Profile name
     */
    public void cancelProfileTimer(String profileName) {
        ScheduledFuture<?> future = profileTimers.remove(profileName);
        if (future != null) {
            future.cancel(false);
            LOG.infof("Canceled timer for profile '%s'", profileName);
        }
    }

    /**
     * Execute bulk job for a profile
     *
     * @param profile Bulk profile
     */
    private void executeProfileJob(BulkProfile profile) {
        try {
            LOG.infof("Executing bulk job for profile: %s", profile.getProfileName());

            // Convert profile to bulk job
            Map<String, Object> bulkJob = bulkProfileService.convertProfileToBulkJob(profile);

            // Call bulk API
            String response = kruizeClient.bulkCreateExperiments(bulkJob);

            LOG.infof("Bulk job created for profile '%s': %s",
                profile.getProfileName(), response);

            // Track job with profile name
            jobsService.incrementJobsTriggered(profile.getProfileName());

        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute bulk job for profile '%s'",
                profile.getProfileName());
        }
    }

    /**
     * Initialize all profiles at startup
     */
    public void initializeProfiles() {
        try {
            LOG.info("Initializing profile timers...");

            List<BulkProfile> profiles = bulkProfileService.getEnabledProfiles();

            LOG.infof("Found %d enabled profiles", profiles.size());

            for (BulkProfile profile : profiles) {
                scheduleProfile(profile);
            }

            LOG.info("Profile timers initialized successfully");

        } catch (Exception e) {
            LOG.error("Failed to initialize profile timers", e);
        }
    }

    /**
     * Shutdown all timers
     */
    @PreDestroy
    public void shutdown() {
        LOG.info("Shutting down profile timers...");
        profileTimers.values().forEach(f -> f.cancel(false));
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOG.info("Profile timers shut down successfully");
    }

    /**
     * Get count of active timers
     *
     * @return Number of active profile timers
     */
    public int getActiveTimerCount() {
        return profileTimers.size();
    }
}

