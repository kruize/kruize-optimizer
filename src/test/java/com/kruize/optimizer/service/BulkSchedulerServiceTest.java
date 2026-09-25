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

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BulkSchedulerService (config-timer orchestration).
 */
@QuarkusTest
class BulkSchedulerServiceTest {

    @Inject
    BulkSchedulerService bulkSchedulerService;

    @InjectMock
    KruizeStateService kruizeStateService;

    @InjectMock
    JobsService jobsService;

    @InjectMock
    ConfigTimerManager configTimerManager;

    @BeforeEach
    void setUp() {
        Mockito.reset(kruizeStateService, jobsService, configTimerManager);
        doNothing().when(kruizeStateService).refreshStateAndInstallProfiles();
        doNothing().when(configTimerManager).initializeConfigs();
    }

    /**
     * Verifies initialize refreshes state/profiles and starts config timers.
     */
    @Test
    void testInitialize_Success() {
        bulkSchedulerService.initialize();

        verify(kruizeStateService, times(1)).refreshStateAndInstallProfiles();
        verify(configTimerManager, times(1)).initializeConfigs();
        assertTrue(bulkSchedulerService.isInitialized());
    }

    /**
     * Verifies initialize failures are swallowed so startup continues.
     */
    @Test
    void testInitialize_ExceptionHandling() {
        doThrow(new RuntimeException("Initialization error"))
                .when(kruizeStateService).refreshStateAndInstallProfiles();

        assertDoesNotThrow(() -> bulkSchedulerService.initialize());
        verify(kruizeStateService, times(1)).refreshStateAndInstallProfiles();
        verify(configTimerManager, never()).initializeConfigs();
        assertFalse(bulkSchedulerService.isInitialized());
    }

    /**
     * Verifies config timers are still attempted when refresh succeeds.
     */
    @Test
    void testInitialize_StartsConfigTimers() {
        bulkSchedulerService.initialize();

        verify(configTimerManager, times(1)).initializeConfigs();
    }
}
