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
package com.kruize.optimizer.utils;

public final class OptimizerConstants {

    private OptimizerConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // contains constants related to Kruize application
    public static final class KruizeClientConstants {

        private KruizeClientConstants() {
            throw new UnsupportedOperationException("Utility class");
        }

        // query params and other general constants
        public static final String VERBOSE = "verbose";
        public static final String NAME = "name";

        // list APIs
        public static final String LIST_DATASOURCE_ENDPOINT = "/datasources";
        public static final String LIST_METADATA_PROFILE_ENDPOINT = "/listMetadataProfiles";
        public static final String LIST_METRIC_PROFILE_ENDPOINT = "/listMetricProfiles";
        public static final String LIST_LAYERS_ENDPOINT = "/listLayers";
        public static final String LIST_EXPERIMENTS_ENDPOINT = "/listExperiments";

        // create APIs
        public static final String CREATE_METADATA_PROFILE_ENDPOINT = "/createMetadataProfile";
        public static final String CREATE_METRIC_PROFILE_ENDPOINT = "/createMetricProfile";
        public static final String CREATE_LAYERS_ENDPOINT = "/createLayer";

        // update APIs
        public static final String UPDATE_METADATA_PROFILE_ENDPOINT = "/updateMetadataProfile";
        public static final String UPDATE_METRIC_PROFILE_ENDPOINT = "/updateMetricProfile";

        // bulk APIs
        public static final String BULK_ENDPOINT = "/bulk";
        public static final String JOB_ID = "job_id";

        // bulk config APIs
        public static final String BULK_CONFIGS_ENDPOINT = "/bulkConfigs";
        public static final String CONFIG_NAME = "config_name";
    }

    // contains constants for Optimizer Service API endpoints
    public static final class OptimizerApiConstants {

        private OptimizerApiConstants() {
            throw new UnsupportedOperationException("Utility class");
        }

        // Base path
        public static final String KRUIZE_BASE_PATH = "/kruize";

        // Common endpoint suffixes (reusable across resources)
        public static final String LIST_PATH = "/list";
        public static final String INSTALL_PATH = "/install";

        // Datasource endpoints
        public static final String DATASOURCES_PATH = "/datasources";

        // Metadata Profile endpoints
        public static final String METADATA_PROFILES_PATH = "/metadataProfiles";

        // Metric Profile endpoints
        public static final String METRIC_PROFILES_PATH = "/metricProfiles";

        // Layer endpoints
        public static final String LAYERS_PATH = "/layers";

        // Status endpoint
        public static final String STATUS_PATH = "/status";

        // Jobs endpoints
        public static final String JOBS_PATH = "/jobs";
        public static final String JOBS_OVERVIEW_PATH = "/overview";

        // Webhook endpoint
        public static final String WEBHOOK_PATH = "/webhook";
    }

    // contains generic constants related to Optimizer application
    public static final class GenericOptimizerConstants {

        private GenericOptimizerConstants() {
            throw new UnsupportedOperationException("Utility class");
        }

        public static final String STARTUP_MESSAGE = "Kruize Optimizer Service is STARTED!";
    }

    // contains message constants (info, error, warn log messages)
    public static final class MessageConstants {

        private MessageConstants() {
            throw new UnsupportedOperationException("Utility class");
        }

        // Success messages
        public static final String PROFILES_INSTALLED_SUCCESS = "Profiles installed successfully";
        public static final String DATASOURCES_FETCHED_SUCCESS = "Datasources fetched successfully";
        public static final String PROFILES_FETCHED_SUCCESS = "Profiles fetched successfully";
        public static final String JOBS_OVERVIEW_FETCHED_SUCCESS = "Jobs overview fetched successfully";

        // Error messages
        public static final String ERROR_FETCHING_DATASOURCES = "Error fetching datasources from Kruize";
        public static final String ERROR_FETCHING_PROFILES = "Error fetching profiles from Kruize";
        public static final String ERROR_INSTALLING_PROFILES = "Error installing profiles";
        public static final String ERROR_READING_PROFILE_FILE = "Error reading profile file";
        public static final String ERROR_INVALID_PROFILE_FORMAT = "Invalid profile format";
        public static final String KRUIZE_SERVICE_UNAVAILABLE = "Kruize service is unavailable";
        public static final String ERROR_FETCHING_JOBS_OVERVIEW = "Error fetching jobs overview";
        public static final String ERROR_PROCESSING_WEBHOOK = "Error processing webhook";
        public static final String ERROR_PROCESSING_WEBHOOK_WITH_MESSAGE = "Error processing webhook: %s";
        public static final String ERROR_INVALID_WEBHOOK_PAYLOAD_NULL_OR_EMPTY = "Invalid webhook payload: payload is null or empty";
        public static final String ERROR_INVALID_WEBHOOK_PAYLOAD_MISSING_SUMMARY = "Invalid webhook payload: missing summary";
        public static final String ERROR_INVALID_WEBHOOK_PAYLOAD_MISSING_JOB_ID = "Invalid webhook payload: missing or empty jobId";
        public static final String VALIDATION_ERROR_PAYLOAD_NULL_OR_EMPTY = "Invalid webhook payload: payload cannot be null or empty";
        public static final String VALIDATION_ERROR_SUMMARY_REQUIRED = "Invalid webhook payload: summary is required";
        public static final String VALIDATION_ERROR_JOB_ID_REQUIRED = "Invalid webhook payload: jobId is required and cannot be empty";
        public static final String ERROR_FAILED_TO_INITIALIZE_BULK_SCHEDULER = "Failed to initialize bulk scheduler";
        public static final String ERROR_NO_VALID_TARGET_LABELS = "No valid target labels found. Cannot proceed with bulk API call.";
        public static final String ERROR_NO_DATASOURCE_AVAILABLE = "No datasource available in Kruize. Cannot proceed with bulk API call.";
        public static final String ERROR_NO_METADATA_PROFILE_AVAILABLE = "No metadata profile available in Kruize. Cannot proceed with bulk API call.";
        public static final String ERROR_NO_METRIC_PROFILE_AVAILABLE = "No metric profile available in Kruize. Cannot proceed with bulk API call.";
        public static final String ERROR_FAILED_TO_PARSE_TARGET_LABELS = "Failed to parse target labels JSON: %s";
        public static final String ERROR_FAILED_TO_EXECUTE_SCHEDULED_BULK_API_CALL = "Failed to execute scheduled bulk API call";

        // Info messages
        public static final String NO_DATASOURCES_FOUND = "No datasources found";
        public static final String NO_PROFILES_FOUND = "No profiles found";
        public static final String PROFILE_ALREADY_EXISTS = "Profile already exists";
        public static final String PROFILE_NOT_FOUND = "Profile not found in local repository";
        public static final String INFO_FETCHING_JOBS_OVERVIEW = "Fetching jobs overview";
        public static final String INFO_INITIALIZING_BULK_SCHEDULER = "Initializing bulk scheduler...";
        public static final String INFO_BULK_SCHEDULER_INITIALIZED = "Bulk scheduler initialized successfully";
        public static final String INFO_BULK_SCHEDULER_NOT_INITIALIZED = "Bulk scheduler not yet initialized. Skipping this run.";
        public static final String INFO_STARTING_SCHEDULED_BULK_API_CALL = "Starting scheduled bulk API call with target labels: %s";
        public static final String INFO_KRUIZE_STATE_CACHE_EMPTY = "Kruize state cache is empty, refreshing...";
        public static final String INFO_CALLING_BULK_API_WITH_PAYLOAD = "Calling bulk API with payload:\n%s";
        public static final String INFO_BULK_API_CALL_SUCCESSFUL = "Bulk API call successful. Response: %s";
        public static final String INFO_RECEIVED_WEBHOOK = "Received webhook with %d payload(s)";
        public static final String INFO_RECEIVED_WEBHOOK_FOR_JOB = "Received webhook for Job %s with status %s";
        public static final String INFO_JOB_ALREADY_PROCESSED = "Job %s already processed. Skipping.";
        public static final String INFO_JOB_COMPLETED = "Job %s completed. Total: %d, Processed: %d, Existing: %d";
        public static final String INFO_JOB_STATUS = "Job %s status is %s";
        
        // Warning messages
        public static final String WARN_FAILED_TO_SERIALIZE_PAYLOAD = "Failed to serialize payload to JSON for logging";
        
        // Debug messages
        public static final String DEBUG_TOTAL_JOBS_TRIGGERED = "Total jobs triggered: %d";
        public static final String DEBUG_UPDATED_COUNTERS = "Updated counters - Created: %d, Processed: %d, Unique: %d";
        public static final String DEBUG_BUILT_BULK_PAYLOAD = "Built bulk payload: %s";
    }

    // contains constants for Jobs API
    public static final class JobsConstants {

        private JobsConstants() {
            throw new UnsupportedOperationException("Utility class");
        }

        // JSON property names
        public static final String JOBS_TRIGGERED = "jobs_triggered";
        public static final String TOTAL_EXPERIMENTS = "total_experiments";
        public static final String PROCESSED_EXPERIMENTS = "processed_experiments";
        public static final String UNIQUE_EXPERIMENTS = "unique_experiments";
        public static final String EXISTING_EXPERIMENTS = "existing_experiments";
    }

    // contains constants for Webhook API
    public static final class WebhookConstants {

        private WebhookConstants() {
            throw new UnsupportedOperationException("Utility class");
        }

        // JSON property names
        public static final String SUMMARY = "summary";
        public static final String WEBHOOK = "webhook";
        public static final String JOB_ID = "jobID";
        public static final String STATUS = "status";

        // Status values
        public static final String STATUS_COMPLETED = "COMPLETED";
    }

    // contains constants for Bulk Scheduler Service
    public static final class BulkSchedulerConstants {

        private BulkSchedulerConstants() {
            throw new UnsupportedOperationException("Utility class");
        }

        // JSON payload keys
        public static final String FILTER = "filter";
        public static final String INCLUDE = "include";
        public static final String LABELS = "labels";
        public static final String DATASOURCE = "datasource";
        public static final String METADATA_PROFILE = "metadata_profile";
        public static final String MEASUREMENT_DURATION = "measurement_duration";
        public static final String WEBHOOK_KEY = "webhook";
        public static final String URL = "url";
    }

    // contains profile type constants
    public static final class ProfileType {

        private ProfileType() {
            throw new UnsupportedOperationException("Utility class");
        }

        public static final String METADATA = "metadata";
        public static final String METRIC = "metric";
        public static final String LAYER = "layer";
        public static final String BULK = "bulk_config";
    }

    // contains profile path constants
    public static final class ProfilePathConstants {

        private ProfilePathConstants() {
            throw new UnsupportedOperationException("Utility class");
        }

        public static final String CONFIGS_BASE_PATH = "configs/";
        public static final String CONFIGS_INDEX_FILE = "configs/configsReferenceIndex.json";
        public static final String METADATA_PROFILES_DIR = "/metadata-profiles/";
        public static final String METRIC_PROFILES_DIR = "/metric-profiles/";
        public static final String BULK_CONFIGS_DIR = "/bulk-configs/";
        public static final String LAYERS_DIR = "configs/layers/";
        public static final String JSON_EXTENSION = ".json";
        
        // JSON field names in configsReferenceIndex.json
        public static final String METADATA_PROFILES_KEY = "metadata_profiles";
        public static final String METRIC_PROFILES_KEY = "metric_profiles";
        public static final String BULK_CONFIGS_KEY = "bulk_configs";
        public static final String LAYERS_KEY = "layers";
        public static final String NAME_KEY = "name";
        public static final String PROFILE_VERSION_KEY = "profile_version";
    }
}
