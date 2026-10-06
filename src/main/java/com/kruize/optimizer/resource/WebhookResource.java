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

import com.kruize.optimizer.model.WebhookPayload;
import com.kruize.optimizer.model.kruize.BulkConfig;
import com.kruize.optimizer.service.BulkSchedulerService;
import com.kruize.optimizer.utils.OptimizerConstants.MessageConstants;
import com.kruize.optimizer.utils.OptimizerConstants.OptimizerApiConstants;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * REST resource for handling webhook notifications from Kruize.
 * <ul>
 *   <li>{@code POST /webhook} — receives bulk-job completion payloads from Kruize</li>
 *   <li>{@code POST /webhook/config/bulk} — receives bulk config update notifications from Kruize</li>
 * </ul>
 */
@Path(OptimizerApiConstants.WEBHOOK_PATH)
public class WebhookResource {

    private static final Logger LOG = Logger.getLogger(WebhookResource.class);

    @Inject
    BulkSchedulerService bulkSchedulerService;

    /**
     * Receives bulk-job completion callbacks from Kruize ({@code POST /webhook}).
     * Expects a non-empty list of {@link WebhookPayload} objects, each with a non-null
     * summary and a non-blank jobId.
     *
     * @param payload list of webhook payloads sent by Kruize upon job completion
     * @return 200 OK on success, 400 Bad Request for invalid input, 500 on processing error
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response receiveWebhook(List<WebhookPayload> payload) {
        LOG.debugf(MessageConstants.INFO_RECEIVED_WEBHOOK, payload != null ? payload.size() : 0);
        
        // Validate payload
        if (payload == null || payload.isEmpty()) {
            LOG.error(MessageConstants.ERROR_INVALID_WEBHOOK_PAYLOAD_NULL_OR_EMPTY);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(MessageConstants.VALIDATION_ERROR_PAYLOAD_NULL_OR_EMPTY)
                    .build();
        }
        
        // Validate each payload in the list
        for (WebhookPayload webhookPayload : payload) {
            if (webhookPayload == null || webhookPayload.getSummary() == null) {
                LOG.error(MessageConstants.ERROR_INVALID_WEBHOOK_PAYLOAD_MISSING_SUMMARY);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(MessageConstants.VALIDATION_ERROR_SUMMARY_REQUIRED)
                        .build();
            }
            
            WebhookPayload.Summary summary = webhookPayload.getSummary();
            if (summary.getJobId() == null || summary.getJobId().trim().isEmpty()) {
                LOG.error(MessageConstants.ERROR_INVALID_WEBHOOK_PAYLOAD_MISSING_JOB_ID);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(MessageConstants.VALIDATION_ERROR_JOB_ID_REQUIRED)
                        .build();
            }
        }
        
        try {
            bulkSchedulerService.handleWebhook(payload);
            return Response.ok().build();
        } catch (Exception e) {
            LOG.error(MessageConstants.ERROR_PROCESSING_WEBHOOK, e);
            return Response.serverError().entity(String.format(MessageConstants.ERROR_PROCESSING_WEBHOOK_WITH_MESSAGE, e.getMessage())).build();
        }
    }

    /**
     * Receives bulk config update notifications from Kruize ({@code POST /webhook/config/bulk}).
     * Kruize calls this endpoint when a bulk config changes, allowing the scheduler to
     * adjust its timers without requiring a full restart.
     *
     * @param config the updated {@link BulkConfig} sent by Kruize; must have a non-blank config_name
     * @return 200 OK on success, 400 Bad Request for invalid input, 500 on processing error
     */
    @POST
    @Path(OptimizerApiConstants.WEBHOOK_CONFIG_BULK_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response receiveConfigUpdate(BulkConfig config) {
        LOG.debugf(MessageConstants.INFO_RECEIVED_CONFIG_UPDATE_WEBHOOK,
                config != null ? config.getConfigName() : "null");

        // Validate config
        if (config == null || config.getConfigName() == null || config.getConfigName().trim().isEmpty()) {
            LOG.error(MessageConstants.ERROR_INVALID_CONFIG_UPDATE_NULL_OR_EMPTY);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(MessageConstants.VALIDATION_ERROR_CONFIG_NAME_REQUIRED)
                    .build();
        }

        try {
            bulkSchedulerService.handleConfigUpdate(config);
            return Response.ok().build();
        } catch (Exception e) {
            LOG.error(MessageConstants.ERROR_PROCESSING_CONFIG_UPDATE_WEBHOOK, e);
            return Response.serverError()
                    .entity(String.format(MessageConstants.ERROR_PROCESSING_CONFIG_UPDATE_WEBHOOK_WITH_MESSAGE, e.getMessage()))
                    .build();
        }
    }
}

