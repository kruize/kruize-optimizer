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
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import static org.mockito.Mockito.doNothing;

/**
 * Base test class for service tests that provides common mock initialization
 * for BulkSchedulerService and KruizeStateService.
 * 
 * This centralizes the repeated mocking pattern to prevent startup connections
 * to real Kruize instances during testing.
 */
public abstract class BaseServiceTest {

    @InjectMock
    protected BulkSchedulerService bulkSchedulerService;

    @InjectMock
    protected KruizeStateService kruizeStateService;

    /**
     * Initialize common mocks before each test.
     * Subclasses should call super.setUpCommonMocks() in their @BeforeEach method.
     */
    @BeforeEach
    protected void setUpCommonMocks() {
        Mockito.reset(bulkSchedulerService, kruizeStateService);
        
        // Mock the initialization to prevent startup from connecting to real Kruize
        doNothing().when(bulkSchedulerService).initialize();
        doNothing().when(kruizeStateService).refreshStateAndInstallProfiles();
    }
}

// Made with Bob
