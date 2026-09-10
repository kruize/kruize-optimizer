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

import com.kruize.optimizer.model.WebhookPayload;
import com.kruize.optimizer.utils.OptimizerConstants.MessageConstants;
import com.kruize.optimizer.utils.OptimizerConstants.WebhookConstants;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Orchestrates bulk-config scheduling: installs/refreshes Kruize state,
 * starts per-config timers, and handles bulk-job completion webhooks.
 */
@ApplicationScoped
public class BulkSchedulerService {

    private static final Logger LOG = Logger.getLogger(BulkSchedulerService.class);

    @Inject
    KruizeStateService kruizeStateService;

    @Inject
    JobsService jobsService;

    @Inject
    ConfigTimerManager configTimerManager;

    private final Set<String> completedJobs = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private volatile boolean initialized = false;

    /**
     * Initialize by refreshing state, installing missing profiles/configs,
     * and starting timers for enabled bulk configs.
     */
    public void initialize() {
        try {
            LOG.info(MessageConstants.INFO_INITIALIZING_BULK_SCHEDULER);

            kruizeStateService.refreshStateAndInstallProfiles();

            LOG.info("Initializing config-based bulk timers...");
            configTimerManager.initializeConfigs();

            initialized = true;
            LOG.info(MessageConstants.INFO_BULK_SCHEDULER_INITIALIZED);
        } catch (Exception e) {
            LOG.error(MessageConstants.ERROR_FAILED_TO_INITIALIZE_BULK_SCHEDULER, e);
        }
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

                    jobsService.updateExperimentCounters(total, processed, existing);

                    LOG.infof(MessageConstants.INFO_JOB_COMPLETED,
                            jobId, total, processed, existing);
                } else {
                    LOG.infof(MessageConstants.INFO_JOB_STATUS, jobId, status);
                }
            }
        }
    }

    /**
     * @return whether initialize() completed successfully
     */
    public boolean isInitialized() {
        return initialized;
    }
}
