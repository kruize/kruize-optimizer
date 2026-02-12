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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Request model for enabling autotune optimization on Kubernetes resources.
 * 
 * This model represents the payload for the enable autotune endpoint, allowing
 * users to specify which resources to label and with which labels.
 *
 */
public class EnableOptimizationRequest {

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("workloadName")
    private String workloadName;

    @JsonProperty("workloadType")
    private String workloadType;

    @JsonProperty("labels")
    private Map<String, String> labels;

    /**
     * Default constructor for JSON deserialization.
     */
    public EnableOptimizationRequest() {
    }

    /**
     * @param namespace the namespace containing the resource
     * @param workloadName the name of the workload (optional)
     * @param workloadType the type of workload (optional)
     * @param labels the labels to apply (optional, uses default if not provided)
     */
    public EnableOptimizationRequest(String namespace, String workloadName, String workloadType, Map<String, String> labels) {
        this.namespace = namespace;
        this.workloadName = workloadName;
        this.workloadType = workloadType;
        this.labels = labels;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getWorkloadName() {
        return workloadName;
    }

    public void setWorkloadName(String workloadName) {
        this.workloadName = workloadName;
    }

    public String getWorkloadType() {
        return workloadType;
    }

    public void setWorkloadType(String workloadType) {
        this.workloadType = workloadType;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    @Override
    public String toString() {
        return "EnableOptimizationRequest{" +
                "namespace='" + namespace + '\'' +
                ", workloadName='" + workloadName + '\'' +
                ", workloadType='" + workloadType + '\'' +
                ", labels=" + labels +
                '}';
    }
}
