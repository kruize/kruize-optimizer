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
package com.kruize.optimizer.model.kruize;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Model representing cluster configuration in a bulk profile
 */
public class ClusterConfig {

    @JsonProperty("cluster_name")
    private String clusterName;

    @JsonProperty("datasources")
    private List<String> datasources;

    @JsonProperty("namespaces")
    private List<String> namespaces;

    @JsonProperty("labels")
    private Map<String, String> labels;

    @JsonProperty("experiment_types")
    private List<String> experimentTypes;

    @JsonProperty("metadata_profile")
    private String metadataProfile;

    public ClusterConfig() {
    }

    public ClusterConfig(String clusterName, List<String> datasources, List<String> namespaces,
                         Map<String, String> labels, List<String> experimentTypes, String metadataProfile) {
        this.clusterName = clusterName;
        this.datasources = datasources;
        this.namespaces = namespaces;
        this.labels = labels;
        this.experimentTypes = experimentTypes;
        this.metadataProfile = metadataProfile;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public List<String> getDatasources() {
        return datasources;
    }

    public void setDatasources(List<String> datasources) {
        this.datasources = datasources;
    }

    public List<String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(List<String> namespaces) {
        this.namespaces = namespaces;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public List<String> getExperimentTypes() {
        return experimentTypes;
    }

    public void setExperimentTypes(List<String> experimentTypes) {
        this.experimentTypes = experimentTypes;
    }

    public String getMetadataProfile() {
        return metadataProfile;
    }

    public void setMetadataProfile(String metadataProfile) {
        this.metadataProfile = metadataProfile;
    }
}
