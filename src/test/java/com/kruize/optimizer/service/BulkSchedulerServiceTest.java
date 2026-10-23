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
import com.kruize.optimizer.utils.OptimizerConstants.WebhookConstants;
import io.quarkus.arc.ClientProxy;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BulkSchedulerService (config-timer orchestration).
 */
@QuarkusTest
class BulkSchedulerServiceTest {

    @Inject
    BulkSchedulerService bulkSchedulerService;

    private BulkSchedulerService scheduler;

    @InjectMock
    KruizeStateService kruizeStateService;

    @InjectMock
    JobsService jobsService;

    @InjectMock
    ConfigTimerManager configTimerManager;

    @BeforeEach
    void setUp() {
        scheduler = ClientProxy.unwrap(bulkSchedulerService);
        Mockito.reset(kruizeStateService, jobsService, configTimerManager);
        scheduler.completedJobs.clear();
        scheduler.completedJobIdLimit = BulkSchedulerService.DEFAULT_COMPLETED_JOB_ID_LIMIT;
        scheduler.initialized = false;
        doNothing().when(kruizeStateService).refreshStateAndInstallProfiles();
        doNothing().when(configTimerManager).initializeConfigs();
    }

    /**
     * Verifies initialize refreshes state/profiles and starts config timers.
     */
    @Test
    void testInitialize_Success() {
        scheduler.initialize();

        verify(kruizeStateService, times(1)).refreshStateAndInstallProfiles();
        verify(configTimerManager, times(1)).initializeConfigs();
        assertTrue(scheduler.isInitialized());
    }

    /**
     * Verifies initialize failures are swallowed so startup continues.
     */
    @Test
    void testInitialize_ExceptionHandling() {
        doThrow(new RuntimeException("Initialization error"))
                .when(kruizeStateService).refreshStateAndInstallProfiles();

        assertDoesNotThrow(() -> scheduler.initialize());
        verify(kruizeStateService, times(1)).refreshStateAndInstallProfiles();
        verify(configTimerManager, never()).initializeConfigs();
        assertFalse(scheduler.isInitialized());
    }

    /**
     * Verifies config timers are still attempted when refresh succeeds.
     */
    @Test
    void testInitialize_StartsConfigTimers() {
        scheduler.initialize();

        verify(configTimerManager, times(1)).initializeConfigs();
    }

    /**
     * A failure to load configs must not mark the scheduler initialized.
     */
    @Test
    void testInitialize_ConfigFetchFailureLeavesUninitialized() {
        doThrow(new IllegalStateException("Kruize unreachable"))
                .when(configTimerManager).initializeConfigs();

        assertDoesNotThrow(() -> scheduler.initialize());
        assertFalse(scheduler.isInitialized());
    }

    @Test
    void handleWebhookAppliesCompletedJobOnce() {
        WebhookPayload payload = completedJob("job-1");

        scheduler.handleWebhook(List.of(payload));
        scheduler.handleWebhook(List.of(payload));

        verify(jobsService, times(1)).updateExperimentCounters(3, 2, 1);
    }

    @Test
    void handleWebhookDropsOldestJobIdBeyondLimit() {
        scheduler.completedJobIdLimit = 2;

        scheduler.handleWebhook(List.of(completedJob("a")));
        scheduler.handleWebhook(List.of(completedJob("b")));
        scheduler.handleWebhook(List.of(completedJob("b")));
        verify(jobsService, times(2)).updateExperimentCounters(3, 2, 1);

        scheduler.handleWebhook(List.of(completedJob("c")));
        scheduler.handleWebhook(List.of(completedJob("b")));
        verify(jobsService, times(3)).updateExperimentCounters(3, 2, 1);

        scheduler.handleWebhook(List.of(completedJob("a")));
        verify(jobsService, times(4)).updateExperimentCounters(3, 2, 1);
    }

    private static WebhookPayload completedJob(String jobId) {
        WebhookPayload.Summary summary = new WebhookPayload.Summary();
        summary.setJobId(jobId);
        summary.setStatus(WebhookConstants.STATUS_COMPLETED);
        summary.setTotalExperiments(3);
        summary.setProcessedExperiments(2);
        summary.setExistingExperiments(1);
        WebhookPayload payload = new WebhookPayload();
        payload.setSummary(summary);
        return payload;
    }
}
