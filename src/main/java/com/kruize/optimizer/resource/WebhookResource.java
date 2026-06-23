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
import com.kruize.optimizer.model.kruize.BulkProfile;
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
 * REST resource for handling webhook callbacks from Kruize bulk API
 */
@Path(OptimizerApiConstants.WEBHOOK_PATH)
public class WebhookResource {

    private static final Logger LOG = Logger.getLogger(WebhookResource.class);

    @Inject
    BulkSchedulerService bulkSchedulerService;

    /**
     * Receive webhook callback from Kruize bulk API
     *
     * @param payload List of webhook payloads
     * @return HTTP response
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
     * Receive profile update webhook from Kruize
     *
     * @param profile Updated bulk profile
     * @return HTTP response
     */
    @POST
    @Path("/profile-update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response receiveProfileUpdate(BulkProfile profile) {
        LOG.infof("Received profile update webhook for: %s",
            profile != null ? profile.getProfileName() : "null");
        
        // Validate profile
        if (profile == null || profile.getProfileName() == null || profile.getProfileName().trim().isEmpty()) {
            LOG.error("Invalid profile update: profile or profile name is null/empty");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid profile update: profile name is required")
                    .build();
        }
        
        try {
            bulkSchedulerService.handleProfileUpdate(profile);
            return Response.ok().build();
        } catch (Exception e) {
            LOG.error("Error processing profile update webhook", e);
            return Response.serverError()
                    .entity("Error processing profile update: " + e.getMessage())
                    .build();
        }
    }
}

