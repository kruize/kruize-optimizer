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
import com.kruize.optimizer.model.kruize.RecommendationSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for per-config timer scheduling.
 */
class ConfigTimerManagerTest {

    private ConfigTimerManager configTimerManager;
    private BulkConfigService bulkConfigService;
    private KruizeClient kruizeClient;

    @BeforeEach
    void setUp() {
        configTimerManager = new ConfigTimerManager();
        bulkConfigService = spy(new BulkConfigService());
        kruizeClient = mock(KruizeClient.class);
        configTimerManager.bulkConfigService = bulkConfigService;
        configTimerManager.kruizeClient = kruizeClient;
        configTimerManager.jobsService = mock(JobsService.class);
        doReturn(new HashMap<String, Object>()).when(bulkConfigService).convertConfigToBulkJob(any());
        when(kruizeClient.bulkCreateExperiments(any())).thenReturn("ok");
    }

    @AfterEach
    void tearDown() {
        configTimerManager.shutdown();
    }

    @Test
    void initializeConfigsContinuesAfterInvalidScheduling() {
        doReturn(List.of(
                config("good-before", "1h", true),
                config("malformed", "not-a-duration", true),
                config("zero", "0h", true),
                config("good-after", "2h", true)
        )).when(bulkConfigService).getEnabledConfigs();

        configTimerManager.initializeConfigs();

        assertEquals(2, configTimerManager.getActiveTimerCount());
    }

    @Test
    void parseSchedulingRejectsNonPositiveInterval() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bulkConfigService.parseScheduling("0min"));
        assertTrue(error.getMessage().contains("positive"));
    }

    @Test
    void updateConfigTimerCancelsWhenSchedulingIsRemoved() {
        BulkConfig original = config("daily", "1h", true);
        configTimerManager.scheduleConfig(original, TimeUnit.HOURS.toMillis(1));
        assertEquals(1, configTimerManager.getActiveTimerCount());

        BulkConfig blankScheduling = config("daily", "  ", true);
        configTimerManager.updateConfigTimer(blankScheduling);
        assertEquals(0, configTimerManager.getActiveTimerCount());

        configTimerManager.scheduleConfig(original, TimeUnit.HOURS.toMillis(1));
        BulkConfig nullScheduling = config("daily", "1h", true);
        nullScheduling.setRecommendationSettings(
                new RecommendationSettings(null, List.of("short_term"), List.of("cost")));
        configTimerManager.updateConfigTimer(nullScheduling);
        assertEquals(0, configTimerManager.getActiveTimerCount());

        configTimerManager.scheduleConfig(original, TimeUnit.HOURS.toMillis(1));
        BulkConfig missingSettings = config("daily", "1h", true);
        missingSettings.setRecommendationSettings(null);
        configTimerManager.updateConfigTimer(missingSettings);
        assertEquals(0, configTimerManager.getActiveTimerCount());
        verify(kruizeClient, never()).bulkCreateExperiments(any());
    }

    @Test
    void updateConfigTimerKeepsExistingTimerWhenSchedulingIsInvalid() {
        BulkConfig original = config("daily", "1h", true);
        configTimerManager.scheduleConfig(original, TimeUnit.HOURS.toMillis(1));
        assertEquals(1, configTimerManager.getActiveTimerCount());

        BulkConfig broken = config("daily", "0h", true);
        configTimerManager.updateConfigTimer(broken);

        assertEquals(1, configTimerManager.getActiveTimerCount());
        verify(kruizeClient, never()).bulkCreateExperiments(any());
    }

    @Test
    void updateConfigTimerPreservesRemainingDelay() {
        BulkConfig original = config("daily", "1h", true);
        configTimerManager.scheduleConfig(original, TimeUnit.HOURS.toMillis(1));

        BulkConfig lengthened = config("daily", "2h", true);
        configTimerManager.updateConfigTimer(lengthened);

        assertEquals(1, configTimerManager.getActiveTimerCount());
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
        verify(kruizeClient, never()).bulkCreateExperiments(any());
    }

    private static BulkConfig config(String name, String scheduling, boolean enabled) {
        BulkConfig config = new BulkConfig();
        config.setConfigName(name);
        config.setEnabled(enabled);
        config.setRecommendationSettings(new RecommendationSettings(scheduling, List.of("short_term"), List.of("cost")));
        return config;
    }
}
