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
package com.kruize.optimizer.exceptions.targetLabelsProcessing;

/**
 * Exception thrown when the number of target labels exceeds the configured limit.
 * This exception is raised when attempting to configure more target labels than
 * the system allows. The limit is typically configured via the
 * {@code kruize.target.labels.limit} property.
 * 
 * @see com.kruize.optimizer.utils.TargetLabelUtils
 */
public class TargetLabelLimitExceededException extends RuntimeException {

    private final int actualCount;
    private final int limit;

    /**
     * Constructs a new TargetLabelLimitExceededException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public TargetLabelLimitExceededException(String message) {
        super(message);
        this.actualCount = -1;
        this.limit = -1;
    }

    /**
     * Constructs a new TargetLabelLimitExceededException with the specified detail message,
     * actual count, and limit.
     *
     * @param message the detail message explaining the reason for the exception
     * @param actualCount the actual number of labels that were attempted
     * @param limit the maximum allowed number of labels
     */
    public TargetLabelLimitExceededException(String message, int actualCount, int limit) {
        super(message);
        this.actualCount = actualCount;
        this.limit = limit;
    }
}


