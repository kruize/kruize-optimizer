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

/**
 * Model representing a bulk profile from Kruize
 */
public class BulkProfile {

    @JsonProperty("profile_name")
    private String profileName;

    @JsonProperty("clusters")
    private List<ClusterConfig> clusters;

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

    public BulkProfile() {
    }

    public BulkProfile(String profileName, List<ClusterConfig> clusters,
                       RecommendationSettings recommendationSettings, String webhookUrl,
                       Boolean enabled, String createdAt, String updatedAt) {
        this.profileName = profileName;
        this.clusters = clusters;
        this.recommendationSettings = recommendationSettings;
        this.webhookUrl = webhookUrl;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public List<ClusterConfig> getClusters() {
        return clusters;
    }

    public void setClusters(List<ClusterConfig> clusters) {
        this.clusters = clusters;
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

