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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kruize.optimizer.exceptions.targetLabelsProcessing.InvalidTargetLabelFormatException;
import com.kruize.optimizer.exceptions.targetLabelsProcessing.TargetLabelLimitExceededException;
import com.kruize.optimizer.utils.constants.MessageConstants;
import com.kruize.optimizer.utils.constants.OptimizerConstants;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.*;

/**
 * Utility class for managing target labels used in workload scanning.
 * This class is responsible for loading, parsing, and validating target labels
 * from configuration. Target labels are used to identify which Kubernetes workloads
 * should be scanned and optimized by Kruize. Labels must be in the format "key=value".
 * 
 * The target labels are configured via the {@code kruize.target.labels.json} property,
 * and the maximum number of labels is controlled by {@code kruize.target.labels.limit}.
 * If no labels are configured, a default label "kruize/autotune=enabled" is used.
 *
 * @see InvalidTargetLabelFormatException
 * @see TargetLabelLimitExceededException
 */
@ApplicationScoped
public class TargetLabelUtils {
    private static final Logger LOG = Logger.getLogger(TargetLabelUtils.class);

    @ConfigProperty(name = OptimizerConstants.TARGET_LABELS_CONFIG_PROPERTY_NAME)
    String targetLabelsJson;

    @ConfigProperty(name = OptimizerConstants.TARGET_LABELS_LIMIT_CONFIG_PROPERTY_NAME)
    int labelLimit;

    private Map<String, String> targetLabels = new HashMap<>();

    /**
     * Initializes the target labels from configuration.
     * 
     * It parses the target labels JSON configuration, validates the format, applies the label
     * limit, and sets up the target labels map. If parsing fails or no labels are
     * configured, it falls back to the default label "kruize/autotune=enabled".
     * 
     */
    @PostConstruct
    void init() {
        // Checking if no target labels are present in configuration
        if (targetLabelsJson == null || targetLabelsJson.isBlank()) {
            LOG.warn(MessageConstants.WarningMessage.NO_TARGET_LABELS_WARNING);
            // Fallback to default if no valid labels were parsed
            targetLabels.put(OptimizerConstants.DEFAULT_LABEL_KEY, OptimizerConstants.DEFAULT_LABEL_VALUE);
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(targetLabelsJson);
            if (rootNode.isArray()) {
                List<String> labelList = mapper.convertValue(rootNode, new TypeReference<List<String>>() {
                });
                for (String label : labelList) {
                    try {
                        String[] parts = label.split(OptimizerConstants.EQUALS_STRING, 2);
                        if (parts.length == 2) {
                            targetLabels.put(parts[0].trim(), parts[1].trim());
                        } else {
                            String errorMessage = String.format(
                                MessageConstants.ErrorMessage.INVALID_LABEL_FORMAT_EXCEPTION_MESSAGE,
                                label
                            );
                            throw new InvalidTargetLabelFormatException(errorMessage);
                        }
                    } catch (InvalidTargetLabelFormatException e) {
                        // Log error and continue processing other labels
                        LOG.error(e.getMessage());
                    }
                }
            } else {
                try {
                    throw new InvalidTargetLabelFormatException(
                        MessageConstants.ErrorMessage.INVALID_JSON_FORMAT_ERROR
                    );
                } catch (InvalidTargetLabelFormatException e) {
                    LOG.error(e.getMessage());
                }
            }
        } catch (JsonProcessingException e) {
            LOG.error(MessageConstants.ErrorMessage.JSON_PARSE_ERROR, e);
        }
        
        // Fallback to default if no valid labels were parsed
        if (targetLabels.isEmpty()) {
            LOG.warn(MessageConstants.WarningMessage.NO_TARGET_LABELS_WARNING);
            targetLabels.put(OptimizerConstants.DEFAULT_LABEL_KEY, OptimizerConstants.DEFAULT_LABEL_VALUE);
        }

        // Check if target labels are within the limit of maximum labels
        try {
            if (targetLabels.size() > labelLimit) {
                String exceptionMessage = String.format(
                    MessageConstants.ErrorMessage.LABEL_LIMIT_EXCEEDED_EXCEPTION_MESSAGE,
                    targetLabels.size(),
                    labelLimit
                );
                throw new TargetLabelLimitExceededException(exceptionMessage, targetLabels.size(), labelLimit);
            }
        } catch (TargetLabelLimitExceededException e) {
            // Log error and truncate to limit
            LOG.error(e.getMessage());
            
            // Fallback to default 
            Map<String, String> limited = new LinkedHashMap<>();
            limited.put(OptimizerConstants.DEFAULT_LABEL_KEY, OptimizerConstants.DEFAULT_LABEL_VALUE);
            targetLabels = limited;
        }

        LOG.infof(MessageConstants.SuccessMessage.LOADED_TARGET_LABELS_INFO, targetLabels);
    }

    /**
     * Gets the configured target labels as an unmodifiable map.
     * The returned map contains key-value pairs representing the target labels
     * that should be used for workload scanning. The map is unmodifiable to
     * prevent external modifications.
     *
     * @return an unmodifiable map of target labels (key-value pairs)
     */
    public Map<String, String> getTargetLabels() {
        return Collections.unmodifiableMap(targetLabels);
    }

    /**
     * Validates if the provided labels are present in the configured target labels.
     * <p>
     * This method checks if all provided labels (key-value pairs) exist in the
     * configured target labels. If any label is not found, an InvalidTargetLabelFormatException
     * is thrown with details about which label is invalid.
     * </p>
     *
     * @param labelsToValidate the labels to validate against target labels
     * @throws InvalidTargetLabelFormatException if any label is not in target labels
     */
    public void validateLabels(Map<String, String> labelsToValidate) throws InvalidTargetLabelFormatException {
        if (labelsToValidate == null || labelsToValidate.isEmpty()) {
            throw new InvalidTargetLabelFormatException(MessageConstants.ErrorMessage.NO_LABELS_PROVIDED_ERROR);
        }

        for (Map.Entry<String, String> entry : labelsToValidate.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Validate key and value are not null or empty
            if (key == null || key.trim().isEmpty() || value == null || value.trim().isEmpty()) {
                throw new InvalidTargetLabelFormatException(MessageConstants.ErrorMessage.INVALID_LABEL_KEY_VALUE_ERROR);
            }

            // Check if the label exists in target labels
            if (!isLabelInTargetLabels(key, value)) {
                String errorMessage = String.format(
                    MessageConstants.ErrorMessage.LABEL_NOT_IN_TARGET_LABELS_ERROR,
                    key,
                    value,
                    targetLabels.toString()
                );
                throw new InvalidTargetLabelFormatException(errorMessage);
            }
        }
    }

    /**
     * Checks if a specific label (key-value pair) exists in the target labels.
     *
     * @param key the label key
     * @param value the label value
     * @return true if the label exists in target labels, false otherwise
     */
    public boolean isLabelInTargetLabels(String key, String value) {
        return value.equals(targetLabels.get(key));
    }

    /**
     * Gets the default label as a map.
     * <p>
     * Returns a map containing the default label (kruize/autotune=enabled).
     * This is useful when no custom labels are provided.
     * </p>
     *
     * @return a map containing the default label
     */
    public Map<String, String> getDefaultLabel() {
        Map<String, String> defaultLabel = new HashMap<>();
        defaultLabel.put(OptimizerConstants.DEFAULT_LABEL_KEY, OptimizerConstants.DEFAULT_LABEL_VALUE);
        return defaultLabel;
    }
}

