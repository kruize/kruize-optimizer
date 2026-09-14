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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Model representing a Kruize datasource
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Datasource {

    private String name;
    private String provider;
    private String url;

    @JsonProperty("service_name")
    private String serviceName;

    private String namespace;

    private List<String> clusters;

    public Datasource() {
    }

    public Datasource(String name, String provider, String url) {
        this.name = name;
        this.provider = provider;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public List<String> getClusters() {
        return clusters;
    }

    public void setClusters(List<String> clusters) {
        this.clusters = clusters;
    }

    @Override
    public String toString() {
        return "Datasource{" +
                "name='" + name + '\'' +
                ", provider='" + provider + '\'' +
                ", url='" + url + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", namespace='" + namespace + '\'' +
                ", clusters=" + clusters +
                '}';
    }
}

