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
 * Exception thrown when a requested Kubernetes resource is not found.
 * <p>
 * This exception is raised when attempting to access or modify a Kubernetes
 * resource (such as a namespace, deployment, stateful set, or replica set)
 * that does not exist in the cluster.
 * </p>
 * 
 * @see com.kruize.optimizer.resource.ClusterScanResource
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String resourceName;

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceType = null;
        this.resourceName = null;
    }

    /**
     * Constructs a new ResourceNotFoundException with the specified resource details.
     *
     * @param message the detail message explaining the reason for the exception
     * @param resourceType the type of resource that was not found (e.g., "Deployment", "Namespace")
     * @param resourceName the name of the resource that was not found
     */
    public ResourceNotFoundException(String message, String resourceType, String resourceName) {
        super(message);
        this.resourceType = resourceType;
        this.resourceName = resourceName;
    }

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause the cause of the exception (which is saved for later retrieval by the
     *              {@link #getCause()} method)
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.resourceType = null;
        this.resourceName = null;
    }

    /**
     * Gets the type of resource that was not found.
     *
     * @return the resource type, or null if not specified
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Gets the name of the resource that was not found.
     *
     * @return the resource name, or null if not specified
     */
    public String getResourceName() {
        return resourceName;
    }
}

// Made with Bob
