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
package com.kruize.optimizer.utils.constants;

public class KruizeConstants {
    // Private constructor to prevent instantiation
    private KruizeConstants() {}

    public class ApiEndpoints {
        // Private constructor to prevent instantiation
        private ApiEndpoints() {} 

        // datasource APIs
        public static final String LIST_DATASOURCE_ENDPOINT = "/datasources";

        // metadata profile API
        public static final String CREATE_METADATA_PROFILE_ENDPOINT = "/createMetadataProfile";
        public static final String LIST_METADATA_PROFILE_ENDPOINT = "/listMetadataProfiles";
        public static final String UPDATE_METADATA_PROFILE_ENDPOINT = "/updateMetadataProfile";

        // metric profile API
        public static final String CREATE_METRIC_PROFILE_ENDPOINT = "/createMetricProfile";
        public static final String LIST_METRIC_PROFILE_ENDPOINT = "/listMetricProfiles";
        public static final String UPDATE_METRIC_PROFILE_ENDPOINT = "/updateMetricProfile";

        // layers API
        public static final String CREATE_LAYERS_ENDPOINT = "/createLayer";
        public static final String LIST_LAYERS_ENDPOINT = "/listLayers";

        // experiment APIs
        public static final String LIST_EXPERIMENTS_ENDPOINT = "/listExperiments";

        // bulk APIs
        public static final String BULK_ENDPOINT = "/bulk";
        public static final String JOB_ID = "job_id";

    }

}
