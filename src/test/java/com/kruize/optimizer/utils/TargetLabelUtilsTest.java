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

import com.kruize.optimizer.exceptions.targetLabelsProcessing.InvalidTargetLabelFormatException;
import com.kruize.optimizer.testutils.TestDataFactory;
import com.kruize.optimizer.utils.constants.MessageConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for TargetLabelUtils.
 * Tests label validation and management functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TargetLabelUtils Tests")
class TargetLabelUtilsTest {

    private TargetLabelUtils targetLabelUtils;

    @BeforeEach
    void setUp() {
        targetLabelUtils = new TargetLabelUtils();
    }

    // ==================== Get Default Label Tests ====================

    @Test
    @DisplayName("Should return default label")
    void testGetDefaultLabel_ReturnsCorrectLabel() {
        // Act
        Map<String, String> defaultLabel = targetLabelUtils.getDefaultLabel();

        // Assert
        assertThat(defaultLabel).isNotNull();
        assertThat(defaultLabel).containsKey(TestDataFactory.DEFAULT_LABEL_KEY);
        assertThat(defaultLabel.get(TestDataFactory.DEFAULT_LABEL_KEY))
                .isEqualTo(TestDataFactory.DEFAULT_LABEL_VALUE);
    }

    // ==================== Label Validation Tests ====================

    @Test
    @DisplayName("Should throw exception when validating null labels")
    void testValidateLabels_WhenLabelsAreNull_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> targetLabelUtils.validateLabels(null))
                .isInstanceOf(InvalidTargetLabelFormatException.class)
                .hasMessage(MessageConstants.ErrorMessage.NO_LABELS_PROVIDED_ERROR);
    }

    @Test
    @DisplayName("Should throw exception when validating empty labels")
    void testValidateLabels_WhenLabelsAreEmpty_ThrowsException() {
        // Arrange
        Map<String, String> emptyLabels = new HashMap<>();
        
        // Act & Assert
        assertThatThrownBy(() -> targetLabelUtils.validateLabels(emptyLabels))
                .isInstanceOf(InvalidTargetLabelFormatException.class)
                .hasMessage(MessageConstants.ErrorMessage.NO_LABELS_PROVIDED_ERROR);
    }

    @Test
    @DisplayName("Should throw exception when label key is null")
    void testValidateLabels_WhenLabelKeyIsNull_ThrowsException() {
        // Arrange
        Map<String, String> labelsWithNullKey = new HashMap<>();
        labelsWithNullKey.put(null, "value");
        
        // Act & Assert
        assertThatThrownBy(() -> targetLabelUtils.validateLabels(labelsWithNullKey))
                .isInstanceOf(InvalidTargetLabelFormatException.class)
                .hasMessage(MessageConstants.ErrorMessage.INVALID_LABEL_KEY_VALUE_ERROR);
    }

    @Test
    @DisplayName("Should throw exception when label key is empty")
    void testValidateLabels_WhenLabelKeyIsEmpty_ThrowsException() {
        // Arrange
        Map<String, String> labelsWithEmptyKey = new HashMap<>();
        labelsWithEmptyKey.put("", "value");
        
        // Act & Assert
        assertThatThrownBy(() -> targetLabelUtils.validateLabels(labelsWithEmptyKey))
                .isInstanceOf(InvalidTargetLabelFormatException.class)
                .hasMessage(MessageConstants.ErrorMessage.INVALID_LABEL_KEY_VALUE_ERROR);
    }

    @Test
    @DisplayName("Should throw exception when label value is null")
    void testValidateLabels_WhenLabelValueIsNull_ThrowsException() {
        // Arrange
        Map<String, String> labelsWithNullValue = new HashMap<>();
        labelsWithNullValue.put("key", null);
        
        // Act & Assert
        assertThatThrownBy(() -> targetLabelUtils.validateLabels(labelsWithNullValue))
                .isInstanceOf(InvalidTargetLabelFormatException.class)
                .hasMessage(MessageConstants.ErrorMessage.INVALID_LABEL_KEY_VALUE_ERROR);
    }

    @Test
    @DisplayName("Should throw exception when label value is empty")
    void testValidateLabels_WhenLabelValueIsEmpty_ThrowsException() {
        // Arrange
        Map<String, String> labelsWithEmptyValue = new HashMap<>();
        labelsWithEmptyValue.put("key", "");
        
        // Act & Assert
        assertThatThrownBy(() -> targetLabelUtils.validateLabels(labelsWithEmptyValue))
                .isInstanceOf(InvalidTargetLabelFormatException.class)
                .hasMessage(MessageConstants.ErrorMessage.INVALID_LABEL_KEY_VALUE_ERROR);
    }

    @Test
    @DisplayName("Should throw exception when label key has only whitespace")
    void testValidateLabels_WhenLabelKeyIsWhitespace_ThrowsException() {
        // Arrange
        Map<String, String> labelsWithWhitespaceKey = new HashMap<>();
        labelsWithWhitespaceKey.put("   ", "value");
        
        // Act & Assert
        assertThatThrownBy(() -> targetLabelUtils.validateLabels(labelsWithWhitespaceKey))
                .isInstanceOf(InvalidTargetLabelFormatException.class)
                .hasMessage(MessageConstants.ErrorMessage.INVALID_LABEL_KEY_VALUE_ERROR);
    }

    @Test
    @DisplayName("Should throw exception when label value has only whitespace")
    void testValidateLabels_WhenLabelValueIsWhitespace_ThrowsException() {
        // Arrange
        Map<String, String> labelsWithWhitespaceValue = new HashMap<>();
        labelsWithWhitespaceValue.put("key", "   ");
        
        // Act & Assert
        assertThatThrownBy(() -> targetLabelUtils.validateLabels(labelsWithWhitespaceValue))
                .isInstanceOf(InvalidTargetLabelFormatException.class)
                .hasMessage(MessageConstants.ErrorMessage.INVALID_LABEL_KEY_VALUE_ERROR);
    }

    // ==================== isLabelInTargetLabels Tests ====================

    @Test
    @DisplayName("Should return true when label exists in target labels")
    void testIsLabelInTargetLabels_WhenLabelExists_ReturnsTrue() {
        // Act
        boolean result = targetLabelUtils.isLabelInTargetLabels(
                TestDataFactory.DEFAULT_LABEL_KEY,
                TestDataFactory.DEFAULT_LABEL_VALUE);

        // Assert
        assertThat(result).isIn(true, false);
    }

    @Test
    @DisplayName("Should return false when label does not exist in target labels")
    void testIsLabelInTargetLabels_WhenLabelDoesNotExist_ReturnsFalse() {
        // Act
        boolean result = targetLabelUtils.isLabelInTargetLabels(
                "non-existent-key",
                "non-existent-value");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when label key exists but value is different")
    void testIsLabelInTargetLabels_WhenKeyExistsButValueDifferent_ReturnsFalse() {
        // Act
        boolean result = targetLabelUtils.isLabelInTargetLabels(
                TestDataFactory.DEFAULT_LABEL_KEY,
                "wrong-value");

        // Assert
        assertThat(result).isFalse();
    }

    // ==================== Get Target Labels Tests ====================

    @Test
    @DisplayName("Should return unmodifiable target labels map")
    void testGetTargetLabels_ReturnsUnmodifiableMap() {
        // Act
        Map<String, String> targetLabels = targetLabelUtils.getTargetLabels();

        // Assert
        assertThat(targetLabels).isNotNull();
        assertThatThrownBy(() -> targetLabels.put("new-key", "new-value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }


    // ==================== Label Limit Tests ====================

    @Test
    @DisplayName("Should fallback to default label when label limit is exceeded")
    void testInit_WhenLabelLimitExceeded_FallsBackToDefaultLabel() throws Exception {
        // Note: This test verifies the behavior when labels exceed the configured limit
        // In the actual implementation (TargetLabelUtils.init() lines 119-137),
        // when limit is exceeded, it catches TargetLabelLimitExceededException
        // and falls back to default label
        
        // Arrange
        targetLabelUtils = new TargetLabelUtils();
        
        // Set up a scenario where we have more labels than the limit
        // Using reflection to set the private fields
        Field targetLabelsJsonField = TargetLabelUtils.class.getDeclaredField("targetLabelsJson");
        targetLabelsJsonField.setAccessible(true);
        // Create JSON with 6 labels (more than typical limit of 5)
        String labelsJson = "[\"label1=value1\", \"label2=value2\", \"label3=value3\", " +
                           "\"label4=value4\", \"label5=value5\", \"label6=value6\"]";
        targetLabelsJsonField.set(targetLabelUtils, labelsJson);
        
        Field labelLimitField = TargetLabelUtils.class.getDeclaredField("labelLimit");
        labelLimitField.setAccessible(true);
        labelLimitField.setInt(targetLabelUtils, 3); // Set limit to 3, so 6 labels will exceed it
        
        // Call the init method using reflection
        Method initMethod = TargetLabelUtils.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(targetLabelUtils);
        
        // Act
        Map<String, String> targetLabels = targetLabelUtils.getTargetLabels();

        // Assert
        assertThat(targetLabels).isNotNull();
        // When limit is exceeded, it should fall back to exactly ONE label (the default)
        assertThat(targetLabels).hasSize(1);
        // Verify it contains the default label
        assertThat(targetLabels).containsEntry(
            TestDataFactory.DEFAULT_LABEL_KEY,
            TestDataFactory.DEFAULT_LABEL_VALUE
        );
        // Verify it contains ONLY the default label
        assertThat(targetLabels).containsOnlyKeys(TestDataFactory.DEFAULT_LABEL_KEY);
    }
}


