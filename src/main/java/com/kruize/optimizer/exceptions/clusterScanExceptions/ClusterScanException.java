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
package com.kruize.optimizer.exceptions.clusterScanExceptions;

/**
 * Exception thrown when an error occurs during cluster scanning operations.
 * <p>
 * This exception is raised when the system encounters issues while scanning
 * Kubernetes clusters for namespaces, deployments, stateful sets, or replica sets.
 * Common scenarios include network failures, authentication issues, or API errors.
 * </p>
 * 
 * @see com.kruize.optimizer.resource.ClusterScanResource
 */
public class ClusterScanException extends RuntimeException {

    /**
     * Constructs a new ClusterScanException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public ClusterScanException(String message) {
        super(message);
    }

    /**
     * Constructs a new ClusterScanException with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause the cause of the exception (which is saved for later retrieval by the
     *              {@link #getCause()} method)
     */
    public ClusterScanException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new ClusterScanException with the specified cause.
     *
     * @param cause the cause of the exception (which is saved for later retrieval by the
     *              {@link #getCause()} method)
     */
    public ClusterScanException(Throwable cause) {
        super(cause);
    }
}

// Made with Bob
