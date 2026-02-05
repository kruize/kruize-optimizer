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
 * Exception thrown when an invalid parameter is provided to an API endpoint.
 * <p>
 * This exception is raised when request parameters are missing, empty, or
 * contain invalid values that prevent the operation from being executed.
 * </p>
 * 
 * @see com.kruize.optimizer.resource.ClusterScanResource
 */
public class InvalidParameterException extends RuntimeException {

    private final String parameterName;

    /**
     * Constructs a new InvalidParameterException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public InvalidParameterException(String message) {
        super(message);
        this.parameterName = null;
    }

    /**
     * Constructs a new InvalidParameterException with the specified parameter details.
     *
     * @param message the detail message explaining the reason for the exception
     * @param parameterName the name of the invalid parameter
     */
    public InvalidParameterException(String message, String parameterName) {
        super(message);
        this.parameterName = parameterName;
    }

    /**
     * Constructs a new InvalidParameterException with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause the cause of the exception (which is saved for later retrieval by the
     *              {@link #getCause()} method)
     */
    public InvalidParameterException(String message, Throwable cause) {
        super(message, cause);
        this.parameterName = null;
    }

    /**
     * Gets the name of the invalid parameter.
     *
     * @return the parameter name, or null if not specified
     */
    public String getParameterName() {
        return parameterName;
    }
}

// Made with Bob
