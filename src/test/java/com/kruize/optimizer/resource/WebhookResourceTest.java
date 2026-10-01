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
package com.kruize.optimizer.resource;

import com.kruize.optimizer.client.KruizeClient;
import com.kruize.optimizer.model.WebhookPayload;
import com.kruize.optimizer.model.kruize.BulkConfig;
import com.kruize.optimizer.service.BulkSchedulerService;
import com.kruize.optimizer.service.KruizeStateService;
import com.kruize.optimizer.utils.OptimizerConstants.MessageConstants;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Integration tests for WebhookResource
 */
@QuarkusTest
class WebhookResourceTest {

    @InjectMock
    BulkSchedulerService bulkSchedulerService;
    
    @InjectMock
    KruizeStateService kruizeStateService;
    
    @InjectMock
    @RestClient
    KruizeClient kruizeClient;

    @BeforeEach
    void setUp() {
        Mockito.reset(bulkSchedulerService, kruizeStateService, kruizeClient);
        
        // Mock the initialization to prevent startup from connecting to real Kruize
        doNothing().when(bulkSchedulerService).initialize();
        doNothing().when(kruizeStateService).refreshStateAndInstallProfiles();
    }

    private WebhookPayload createWebhookPayload(String jobId, String status, int total, int processed, int existing) {
        WebhookPayload payload = new WebhookPayload();
        WebhookPayload.Summary summary = new WebhookPayload.Summary();
        summary.setJobId(jobId);
        summary.setStatus(status);
        summary.setTotalExperiments(total);
        summary.setProcessedExperiments(processed);
        summary.setExistingExperiments(existing);
        payload.setSummary(summary);
        return payload;
    }

    /**
     * Test successful webhook reception with single payload
     *
     * Test Description: Verifies that the webhook endpoint successfully receives and processes
     * a single webhook payload from Kruize bulk API with completed status.
     *
     * Test Payload:
     * - jobId: "job-123"
     * - status: "COMPLETED"
     * - totalExperiments: 10
     * - processedExperiments: 8
     * - existingExperiments: 2
     *
     * Expected Output:
     * - HTTP Status: 200 OK
     * - BulkSchedulerService.handleWebhook() called once
     */
    @Test
    void testReceiveWebhook_Success() {
        // Arrange
        WebhookPayload payload = createWebhookPayload("job-123", "COMPLETED", 10, 8, 2);
        List<WebhookPayload> payloads = Collections.singletonList(payload);
        doNothing().when(bulkSchedulerService).handleWebhook(any());
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(payloads)
            .when()
            .post("/webhook")
            .then()
            .statusCode(200);
        
        verify(bulkSchedulerService, times(1)).handleWebhook(any());
    }

    /**
     * Test webhook reception with multiple payloads
     *
     * Test Description: Verifies that the webhook endpoint can handle multiple webhook
     * payloads in a single request.
     *
     * Test Payloads:
     * - Payload 1: jobId="job-123", status="COMPLETED", total=10, processed=8, existing=2
     * - Payload 2: jobId="job-124", status="COMPLETED", total=5, processed=5, existing=0
     *
     * Expected Output:
     * - HTTP Status: 200 OK
     * - BulkSchedulerService.handleWebhook() called once with both payloads
     */
    @Test
    void testReceiveWebhook_MultiplePayloads() {
        // Arrange
        WebhookPayload payload1 = createWebhookPayload("job-123", "COMPLETED", 10, 8, 2);
        WebhookPayload payload2 = createWebhookPayload("job-124", "COMPLETED", 5, 5, 0);
        List<WebhookPayload> payloads = Arrays.asList(payload1, payload2);
        doNothing().when(bulkSchedulerService).handleWebhook(any());
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(payloads)
            .when()
            .post("/webhook")
            .then()
            .statusCode(200);
        
        verify(bulkSchedulerService, times(1)).handleWebhook(any());
    }

    /**
     * Test webhook endpoint with invalid JSON
     *
     * Test Description: Verifies that the webhook endpoint rejects malformed JSON
     * and does not process the request.
     *
     * Expected Output:
     * - HTTP Status: 400 Bad Request
     * - BulkSchedulerService.handleWebhook() never called
     */
    @Test
    void testReceiveWebhook_InvalidJson() {
        // Test malformed JSON
        given()
            .contentType(ContentType.JSON)
            .body("{invalid json}")
            .when()
            .post("/webhook")
            .then()
            .statusCode(400);
        
        verify(bulkSchedulerService, never()).handleWebhook(any());
    }

    /**
     * Test webhook endpoint with null payload
     *
     * Test Description: Verifies that the webhook endpoint rejects null payload
     * and returns appropriate error.
     *
     * Expected Output:
     * - HTTP Status: 400 Bad Request
     * - BulkSchedulerService.handleWebhook() never called
     */
    @Test
    void testReceiveWebhook_NullPayload() {
        // Test with null payload
        given()
            .contentType(ContentType.JSON)
            .body("null")
            .when()
            .post("/webhook")
            .then()
            .statusCode(400);
        
        verify(bulkSchedulerService, never()).handleWebhook(any());
    }

    /**
     * Test webhook endpoint with empty payload list
     *
     * Test Description: Verifies that the webhook endpoint rejects empty payload list
     * as it requires at least one payload.
     *
     * Expected Output:
     * - HTTP Status: 400 Bad Request
     * - BulkSchedulerService.handleWebhook() never called
     */
    @Test
    void testReceiveWebhook_EmptyPayloadList() {
        // Test with empty list
        given()
            .contentType(ContentType.JSON)
            .body("[]")
            .when()
            .post("/webhook")
            .then()
            .statusCode(400);
        
        verify(bulkSchedulerService, never()).handleWebhook(any());
    }

    /**
     * Test webhook endpoint with null jobId
     *
     * Test Description: Verifies that the webhook endpoint validates required fields
     * and rejects payloads with null jobId.
     *
     * Expected Output:
     * - HTTP Status: 400 Bad Request
     * - BulkSchedulerService.handleWebhook() never called
     */
    @Test
    void testReceiveWebhook_NullJobId() {
        // Test with null jobId in summary
        String invalidPayload = "[{\"summary\": {\"jobId\": null, \"status\": \"COMPLETED\", \"totalExperiments\": 10, \"processedExperiments\": 8, \"existingExperiments\": 2}}]";
        
        given()
            .contentType(ContentType.JSON)
            .body(invalidPayload)
            .when()
            .post("/webhook")
            .then()
            .statusCode(400);
        
        verify(bulkSchedulerService, never()).handleWebhook(any());
    }

    /**
     * Test webhook endpoint with missing summary field
     *
     * Test Description: Verifies that the webhook endpoint validates payload structure
     * and rejects payloads missing the required summary field.
     *
     * Expected Output:
     * - HTTP Status: 400 Bad Request
     * - BulkSchedulerService.handleWebhook() never called
     */
    @Test
    void testReceiveWebhook_MissingSummary() {
        // Test with missing summary field
        String invalidPayload = "[{}]";
        
        given()
            .contentType(ContentType.JSON)
            .body(invalidPayload)
            .when()
            .post("/webhook")
            .then()
            .statusCode(400);
        
        verify(bulkSchedulerService, never()).handleWebhook(any());
    }

    /**
     * Test webhook reception with failed job status
     *
     * Test Description: Verifies that the webhook endpoint accepts and processes
     * webhook payloads with FAILED status (not just COMPLETED).
     *
     * Test Payload:
     * - jobId: "job-123"
     * - status: "FAILED"
     * - totalExperiments: 0
     * - processedExperiments: 0
     * - existingExperiments: 0
     *
     * Expected Output:
     * - HTTP Status: 200 OK
     * - BulkSchedulerService.handleWebhook() called once
     */
    @Test
    void testReceiveWebhook_FailedStatus() {
        // Arrange
        WebhookPayload payload = createWebhookPayload("job-123", "FAILED", 0, 0, 0);
        List<WebhookPayload> payloads = Collections.singletonList(payload);
        doNothing().when(bulkSchedulerService).handleWebhook(any());
        
        // Act & Assert
        given()
            .contentType(ContentType.JSON)
            .body(payloads)
            .when()
            .post("/webhook")
            .then()
            .statusCode(200);
        
        verify(bulkSchedulerService, times(1)).handleWebhook(any());
    }

    /**
     * Test successful config-update webhook
     *
     * Test Description: Verifies that POST /webhook/config-update accepts a bulk config
     * with a name and delegates it to the scheduler.
     *
     * Test Payload:
     * - config_name: "bulk-default"
     *
     * Expected Output:
     * - HTTP Status: 200 OK
     * - BulkSchedulerService.handleConfigUpdate() called once with that config name
     */
    @Test
    void testReceiveConfigUpdate_Success() {
        BulkConfig config = new BulkConfig();
        config.setConfigName("bulk-default");
        doNothing().when(bulkSchedulerService).handleConfigUpdate(any());

        given()
            .contentType(ContentType.JSON)
            .body(config)
            .when()
            .post("/webhook/config-update")
            .then()
            .statusCode(200);

        verify(bulkSchedulerService, times(1)).handleConfigUpdate(argThat(
                updated -> updated != null && "bulk-default".equals(updated.getConfigName())));
    }

    /**
     * Test config-update webhook with a null body
     *
     * Test Description: Verifies that a null config is rejected before the scheduler is called.
     *
     * Expected Output:
     * - HTTP Status: 400 Bad Request
     * - Body: config name is required
     * - BulkSchedulerService.handleConfigUpdate() never called
     */
    @Test
    void testReceiveConfigUpdate_NullConfig() {
        given()
            .contentType(ContentType.JSON)
            .body("null")
            .when()
            .post("/webhook/config-update")
            .then()
            .statusCode(400)
            .body(equalTo(MessageConstants.VALIDATION_ERROR_CONFIG_NAME_REQUIRED));

        verify(bulkSchedulerService, never()).handleConfigUpdate(any());
    }

    /**
     * Test config-update webhook with a missing config name
     *
     * Test Description: Verifies that a config object without config_name is rejected.
     *
     * Expected Output:
     * - HTTP Status: 400 Bad Request
     * - BulkSchedulerService.handleConfigUpdate() never called
     */
    @Test
    void testReceiveConfigUpdate_MissingConfigName() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
            .when()
            .post("/webhook/config-update")
            .then()
            .statusCode(400)
            .body(equalTo(MessageConstants.VALIDATION_ERROR_CONFIG_NAME_REQUIRED));

        verify(bulkSchedulerService, never()).handleConfigUpdate(any());
    }

    /**
     * Test config-update webhook with a blank config name
     *
     * Test Description: Verifies that a whitespace-only config_name is rejected.
     *
     * Expected Output:
     * - HTTP Status: 400 Bad Request
     * - BulkSchedulerService.handleConfigUpdate() never called
     */
    @Test
    void testReceiveConfigUpdate_BlankConfigName() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"config_name\": \"   \"}")
            .when()
            .post("/webhook/config-update")
            .then()
            .statusCode(400)
            .body(equalTo(MessageConstants.VALIDATION_ERROR_CONFIG_NAME_REQUIRED));

        verify(bulkSchedulerService, never()).handleConfigUpdate(any());
    }

    /**
     * Test config-update webhook when the scheduler fails
     *
     * Test Description: Verifies that an exception from handleConfigUpdate is returned as
     * an HTTP 500 and includes the failure message.
     *
     * Expected Output:
     * - HTTP Status: 500 Internal Server Error
     * - Body contains the scheduler exception message
     */
    @Test
    void testReceiveConfigUpdate_ProcessingError() {
        BulkConfig config = new BulkConfig();
        config.setConfigName("bulk-default");
        doThrow(new RuntimeException("timer failed")).when(bulkSchedulerService).handleConfigUpdate(any());

        given()
            .contentType(ContentType.JSON)
            .body(config)
            .when()
            .post("/webhook/config-update")
            .then()
            .statusCode(500)
            .body(equalTo(String.format(MessageConstants.ERROR_PROCESSING_CONFIG_UPDATE_WEBHOOK_WITH_MESSAGE, "timer failed")));

        verify(bulkSchedulerService, times(1)).handleConfigUpdate(any());
    }
}

