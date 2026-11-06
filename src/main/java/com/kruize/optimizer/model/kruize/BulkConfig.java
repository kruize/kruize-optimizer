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
 * Model representing a bulk config from Kruize
 */
public class BulkConfig {

    @JsonProperty("config_name")
    private String configName;

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

    @JsonProperty("performance_profile")
    private String performanceProfile;

    @JsonProperty("trial_settings")
    private TrialSettings trialSettings;

    @JsonProperty("recommendation_settings")
    private RecommendationSettings recommendationSettings;

    @JsonProperty("webhook_url")
    private String webhookUrl;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    public BulkConfig() {
    }

    public BulkConfig(String configName, String clusterName, List<String> datasources,
                      List<String> namespaces, Map<String, String> labels,
                      List<String> experimentTypes, String metadataProfile,
                      String performanceProfile, TrialSettings trialSettings,
                      RecommendationSettings recommendationSettings, String webhookUrl,
                      Boolean enabled, String createdAt, String updatedAt) {
        this.configName = configName;
        this.clusterName = clusterName;
        this.datasources = datasources;
        this.namespaces = namespaces;
        this.labels = labels;
        this.experimentTypes = experimentTypes;
        this.metadataProfile = metadataProfile;
        this.performanceProfile = performanceProfile;
        this.trialSettings = trialSettings;
        this.recommendationSettings = recommendationSettings;
        this.webhookUrl = webhookUrl;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
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

    public String getPerformanceProfile() {
        return performanceProfile;
    }

    public void setPerformanceProfile(String performanceProfile) {
        this.performanceProfile = performanceProfile;
    }

    public TrialSettings getTrialSettings() {
        return trialSettings;
    }

    public void setTrialSettings(TrialSettings trialSettings) {
        this.trialSettings = trialSettings;
    }

    public RecommendationSettings getRecommendationSettings() {
        return recommendationSettings;
    }

    public void setRecommendationSettings(RecommendationSettings recommendationSettings) {
        this.recommendationSettings = recommendationSettings;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
