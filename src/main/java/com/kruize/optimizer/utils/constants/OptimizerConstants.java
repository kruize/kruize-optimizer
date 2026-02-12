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

public class OptimizerConstants {
    // Private constructor to prevent instantiation
    private OptimizerConstants() {}

    public static final String VERBOSE = "verbose";
    public static final String NAME = "name";
    public static final String EQUALS_STRING = "=";
    public static final String NAMESPACE = "namespace";
    public static final String WORKLOAD_NAME = "workload_name";
    public static final String WORKLOAD_TYPE = "workload_type";

    // target labels
    public static final String DEFAULT_LABEL_KEY = "kruize/autotune";
    public static final String DEFAULT_LABEL_VALUE = "enabled";
    public static final String TARGET_LABELS_CONFIG_PROPERTY_NAME = "kruize.target.labels.json";
    public static final String TARGET_LABELS_LIMIT_CONFIG_PROPERTY_NAME = "kruize.target.labels.limit";
    
    
    public static class ApiEndpoints {
        // Private constructor to prevent instantiation
        private ApiEndpoints() {}

        // params
        public static final String SCAN_ALL_WORKLOADS_PARAM = "scan_all_workloads";

        // API paths
        public static final String SCAN_PATH = "/scan";
        public static final String ENABLE_OPTIMIZATION_PATH = "/enable-optimization";


    }
    
    public static enum WorkloadType {
        DEPLOYMENT("Deployment"),
        STATEFULSET("StatefulSet"),
        REPLICASET("ReplicaSet");

        private final String value;
        WorkloadType(String value) { this.value = value; }
        public String getValue() { return value; }
    }
}
