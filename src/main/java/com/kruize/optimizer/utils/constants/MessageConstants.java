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

public class MessageConstants {
    // Private constructor to prevent instantiation
    private MessageConstants() {}
    
    public class WarningMesaage {
        private WarningMesaage() {}

        public static final String NO_TARGET_LABELS_WARNING = "No target labels are configured. Using default label 'kruize/autotune=enabled'";
        public static final String LABEL_LIMIT_EXCEEDED_WARNING = "Label count %d exceeds limit %d.  Using default label 'kruize/autotune=enabled' to scan the workloads.";
    }

    public class ErrorMessage {
        private ErrorMessage() {}

        public static final String INVALID_JSON_FORMAT_ERROR = "Invalid JSON format for kruize.target.labels.json. Must be an Array.";
        public static final String JSON_PARSE_ERROR = "Failed to parse kruize.target.labels.json. Using default label 'kruize/autotune=enabled' to scan the workloads";
        public static final String LOADED_TARGET_LABELS_INFO = "Loaded Target Labels: %s";
        public static final String INVALID_LABEL_FORMAT_EXCEPTION_MESSAGE = "Invalid label format: '%s'. Expected format is 'key=value'.";
        public static final String LABEL_LIMIT_EXCEEDED_EXCEPTION_MESSAGE = "Target label count (%d) exceeds the configured limit (%d). Using default label 'kruize/autotune=enabled' to scan the workloads";
        
        public static final String NAMESPACE_REQUIRED_ERROR = "Namespace parameter is required";
        public static final String NAMESPACE_NOT_FOUND_ERROR = "Namespace not found: %s";
        public static final String DEPLOYMENT_NOT_FOUND_ERROR = "Deployment not found: %s";
        public static final String STATEFULSET_NOT_FOUND_ERROR = "StatefulSet not found: %s";
        public static final String REPLICASET_NOT_FOUND_ERROR = "ReplicaSet not found: %s";
        public static final String WORKLOAD_NOT_FOUND_ERROR = "%s not found: %s";
        public static final String CLUSTER_SCAN_ERROR = "Error scanning cluster: %s";
        public static final String RESOURCE_LABELING_ERROR = "Error labeling resource: %s";
        public static final String INVALID_WORKLOAD_TYPE_ERROR = "Invalid workload type: %s. Supported types are: Deployment, StatefulSet, ReplicaSet";
        
        public static final String LABEL_NOT_IN_TARGET_LABELS_ERROR = "Label '%s=%s' is not in the configured target labels. Available labels: %s";
        public static final String INVALID_LABEL_KEY_VALUE_ERROR = "Invalid label: key and value cannot be null or empty";
        public static final String NO_LABELS_PROVIDED_ERROR = "No labels provided in the request";
    }

    public class InfoMessage {
        private InfoMessage() {}

        public static final String SCANNING_CLUSTER_INFO = "Scanning cluster for workloads";
    }

    public class SuccessMessage {
        private SuccessMessage() {}

        public static final String STARTUP_MESSAGE = "Kruize Optimizer Service is STARTED!";

        public static final String LOADED_TARGET_LABELS_INFO = "Loaded Target Labels: %s";

        public static final String NAMESPACE_LABELED_SUCCESS = "Namespace %s labeled successfully";
        public static final String DEPLOYMENT_LABELED_SUCCESS = "Deployment %s labeled successfully";
        public static final String STATEFULSET_LABELED_SUCCESS = "StatefulSet %s labeled successfully";
        public static final String REPLICASET_LABELED_SUCCESS = "ReplicaSet %s labeled successfully";
        public static final String WORKLOAD_LABELED_SUCCESS = "Successfully labeled %s '%s' in namespace '%s' with labels: %s";
        public static final String SCAN_COMPLETED_INFO = "Cluster scan completed. Found %d namespaces and %d workloads";

    }
}
