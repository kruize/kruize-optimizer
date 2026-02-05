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
package com.kruize.optimizer.model;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents the result of a cluster scan operation.
 * This class encapsulates information about namespaces and workloads discovered
 * during a Kubernetes cluster scan. It provides structured data about the cluster's
 * resources and their optimization status.
 */
public class ClusterScanResult {
    private List<NamespaceInfo> namespaces = new ArrayList<>();
    private List<WorkloadInfo> workloads = new ArrayList<>();

    /**
     * Gets the list of namespaces discovered in the cluster scan.
     *
     * @return a list of {@link NamespaceInfo} objects representing the namespaces
     */
    public List<NamespaceInfo> getNamespaces() {
        return namespaces;
    }

    /**
     * Sets the list of namespaces discovered in the cluster scan.
     *
     * @param namespaces a list of {@link NamespaceInfo} objects to set
     */
    public void setNamespaces(List<NamespaceInfo> namespaces) {
        this.namespaces = namespaces;
    }

    /**
     * Gets the list of workloads discovered in the cluster scan.
     *
     * @return a list of {@link WorkloadInfo} objects representing the workloads
     */
    public List<WorkloadInfo> getWorkloads() {
        return workloads;
    }

    /**
     * Sets the list of workloads discovered in the cluster scan.
     *
     * @param workloads a list of {@link WorkloadInfo} objects to set
     */
    public void setWorkloads(List<WorkloadInfo> workloads) {
        this.workloads = workloads;
    }

    /**
     * Represents information about a Kubernetes namespace.
     * This class contains details about a namespace including its name 
     * and whether it has been optimized by Kruize.
     */
    public static class NamespaceInfo {
        private String name;
        private boolean kruizeOptimized;

        /**
         * Default constructor for NamespaceInfo.
         */
        public NamespaceInfo() {
        }

        /**
         * Constructs a NamespaceInfo with the specified parameters.
         *
         * @param name the name of the namespace
         * @param kruizeOptimized whether this namespace is optimized by Kruize
         */
        public NamespaceInfo(String name, boolean kruizeOptimized) {
            this.name = name;
            this.kruizeOptimized = kruizeOptimized;
        }

        /**
         * Gets the name of the namespace.
         *
         * @return the namespace name
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the name of the namespace.
         *
         * @param name the namespace name to set
         */
        public void setName(String name) {
            this.name = name;
        }


        /**
         * Checks if this namespace is optimized by Kruize.
         *
         * @return true if the namespace is Kruize optimized, false otherwise
         */
        public boolean isKruizeOptimized() {
            return kruizeOptimized;
        }

        /**
         * Sets whether this namespace is optimized by Kruize.
         *
         * @param kruizeOptimized true if the namespace is Kruize optimized, false otherwise
         */
        public void setKruizeOptimized(boolean kruizeOptimized) {
            this.kruizeOptimized = kruizeOptimized;
        }
    }

    /**
     * Represents information about a Kubernetes workload.
     * <p>
     * This class contains details about a workload including its name, namespace,
     * type (e.g., Deployment, StatefulSet, ReplicaSet), optimization status,
     * containers, and labels.
     * </p>
     */
    public static class WorkloadInfo {
        private String name;
        private String namespace;
        private String type; // Deployment, StatefulSet, ReplicaSet
        private boolean kruizeOptimized;
        private List<ContainerInfo> containers = new ArrayList<>();
        private java.util.Map<String, String> labels;

        /**
         * Default constructor for WorkloadInfo.
         */
        public WorkloadInfo() {
        }

        /**
         * Constructs a WorkloadInfo with the specified parameters.
         *
         * @param name the name of the workload
         * @param namespace the namespace containing this workload
         * @param type the type of workload (e.g., Deployment, StatefulSet, ReplicaSet)
         * @param kruizeOptimized whether this workload is optimized by Kruize
         * @param containers the list of containers in this workload
         * @param labels the labels associated with this workload
         */
        public WorkloadInfo(String name, String namespace, String type, boolean kruizeOptimized,
                List<ContainerInfo> containers, java.util.Map<String, String> labels) {
            this.name = name;
            this.namespace = namespace;
            this.type = type;
            this.kruizeOptimized = kruizeOptimized;
            this.containers = containers;
            this.labels = labels;
        }

        /**
         * Gets the name of the workload.
         *
         * @return the workload name
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the name of the workload.
         *
         * @param name the workload name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Gets the namespace containing this workload.
         *
         * @return the namespace name
         */
        public String getNamespace() {
            return namespace;
        }

        /**
         * Sets the namespace containing this workload.
         *
         * @param namespace the namespace name to set
         */
        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        /**
         * Gets the type of the workload.
         *
         * @return the workload type (e.g., Deployment, StatefulSet, ReplicaSet)
         */
        public String getType() {
            return type;
        }

        /**
         * Sets the type of the workload.
         *
         * @param type the workload type to set (e.g., Deployment, StatefulSet, ReplicaSet)
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * Checks if this workload is optimized by Kruize.
         *
         * @return true if the workload is Kruize optimized, false otherwise
         */
        public boolean isKruizeOptimized() {
            return kruizeOptimized;
        }

        /**
         * Sets whether this workload is optimized by Kruize.
         *
         * @param kruizeOptimized true if the workload is Kruize optimized, false otherwise
         */
        public void setKruizeOptimized(boolean kruizeOptimized) {
            this.kruizeOptimized = kruizeOptimized;
        }

        /**
         * Gets the list of containers in this workload.
         *
         * @return a list of {@link ContainerInfo} objects
         */
        public List<ContainerInfo> getContainers() {
            return containers;
        }

        /**
         * Sets the list of containers in this workload.
         *
         * @param containers a list of {@link ContainerInfo} objects to set
         */
        public void setContainers(List<ContainerInfo> containers) {
            this.containers = containers;
        }

        /**
         * Gets the labels associated with this workload.
         *
         * @return a map of label key-value pairs
         */
        public java.util.Map<String, String> getLabels() {
            return labels;
        }

        /**
         * Sets the labels associated with this workload.
         *
         * @param labels a map of label key-value pairs to set
         */
        public void setLabels(java.util.Map<String, String> labels) {
            this.labels = labels;
        }
    }

    /**
     * Represents information about a container within a workload.
     * <p>
     * This class contains basic details about a container including its name
     * and the image it uses.
     * </p>
     */
    public static class ContainerInfo {
        private String name;
        private String image;

        /**
         * Default constructor for ContainerInfo.
         */
        public ContainerInfo() {
        }

        /**
         * Constructs a ContainerInfo with the specified parameters.
         *
         * @param name the name of the container
         * @param image the container image
         */
        public ContainerInfo(String name, String image) {
            this.name = name;
            this.image = image;
        }

        /**
         * Gets the name of the container.
         *
         * @return the container name
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the name of the container.
         *
         * @param name the container name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Gets the container image.
         *
         * @return the container image
         */
        public String getImage() {
            return image;
        }

        /**
         * Sets the container image.
         *
         * @param image the container image to set
         */
        public void setImage(String image) {
            this.image = image;
        }
    }
}

