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

import com.kruize.optimizer.model.kruize.JobsOverview;
import com.kruize.optimizer.utils.OptimizerConstants.MessageConstants;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Service for managing bulk job statistics
 */
@ApplicationScoped
public class JobsService {

    private static final Logger LOG = Logger.getLogger(JobsService.class);

    // Counters for bulk operations
    private int totalJobsTriggered = 0;
    private int totalExperimentsCreated = 0;
    private int totalExperimentsProcessed = 0;
    private int totalExperimentsUnique = 0;

    // Track jobs per config
    private final java.util.Map<String, Integer> jobsPerConfig = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Increment the total jobs triggered counter
     */
    public synchronized void incrementJobsTriggered() {
        totalJobsTriggered++;
        LOG.debugf(MessageConstants.DEBUG_TOTAL_JOBS_TRIGGERED, totalJobsTriggered);
    }

    /**
     * Increment the total jobs triggered counter for a specific config
     *
     * @param configName Config name
     */
    public synchronized void incrementJobsTriggered(String configName) {
        totalJobsTriggered++;
        jobsPerConfig.merge(configName, 1, Integer::sum);
        LOG.debugf("Total jobs triggered: %d (Config '%s': %d)",
                totalJobsTriggered, configName, jobsPerConfig.get(configName));
    }

    /**
     * Update experiment counters from webhook
     *
     * @param total Total experiments
     * @param processed Processed experiments
     * @param existing Existing experiments
     */
    public synchronized void updateExperimentCounters(int total, int processed, int existing) {
        totalExperimentsCreated += total;
        totalExperimentsProcessed += processed;
        int unique = total - existing;
        totalExperimentsUnique += unique;
        LOG.debugf(MessageConstants.DEBUG_UPDATED_COUNTERS,
                   totalExperimentsCreated, totalExperimentsProcessed, totalExperimentsUnique);
    }

    /**
     * Get jobs overview with all statistics
     *
     * @return JobsOverview object
     */
    public JobsOverview getJobsOverview() {
        return new JobsOverview(
            totalJobsTriggered,
            totalExperimentsCreated,
            totalExperimentsProcessed,
            totalExperimentsUnique
        );
    }

    /**
     * Get total jobs triggered
     *
     * @return total jobs triggered
     */
    public int getTotalJobsTriggered() {
        return totalJobsTriggered;
    }

    /**
     * Get total experiments created
     *
     * @return total experiments created
     */
    public int getTotalExperimentsCreated() {
        return totalExperimentsCreated;
    }

    /**
     * Get total experiments processed
     *
     * @return total experiments processed
     */
    public int getTotalExperimentsProcessed() {
        return totalExperimentsProcessed;
    }

    /**
     * Get total unique experiments
     *
     * @return total unique experiments
     */
    public int getTotalExperimentsUnique() {
        return totalExperimentsUnique;
    }

    /**
     * Get jobs triggered per profile
     *
     * @return Map of profile name to job count
     */
    public java.util.Map<String, Integer> getJobsByProfile() {
        return new java.util.HashMap<>(jobsPerConfig);
    }
}

