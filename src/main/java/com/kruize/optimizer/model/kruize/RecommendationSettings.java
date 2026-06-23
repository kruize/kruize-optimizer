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
 * Model representing recommendation settings in a bulk profile
 */
public class RecommendationSettings {

    @JsonProperty("scheduling")
    private String scheduling;

    @JsonProperty("terms")
    private List<String> terms;

    @JsonProperty("models")
    private List<String> models;

    @JsonProperty("measurement_duration")
    private String measurementDuration;

    public RecommendationSettings() {
    }

    public RecommendationSettings(String scheduling, List<String> terms, List<String> models, String measurementDuration) {
        this.scheduling = scheduling;
        this.terms = terms;
        this.models = models;
        this.measurementDuration = measurementDuration;
    }

    public String getScheduling() {
        return scheduling;
    }

    public void setScheduling(String scheduling) {
        this.scheduling = scheduling;
    }

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    public List<String> getModels() {
        return models;
    }

    public void setModels(List<String> models) {
        this.models = models;
    }

    public String getMeasurementDuration() {
        return measurementDuration;
    }

    public void setMeasurementDuration(String measurementDuration) {
        this.measurementDuration = measurementDuration;
    }
}
