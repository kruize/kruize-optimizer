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

import com.kruize.optimizer.model.api.ApiResponse;
import com.kruize.optimizer.model.kruize.KruizeProfile;
import com.kruize.optimizer.service.ProfileService;
import com.kruize.optimizer.utils.OptimizerConstants.MessageConstants;
import com.kruize.optimizer.utils.OptimizerConstants.OptimizerApiConstants;
import com.kruize.optimizer.utils.OptimizerConstants.ProfileType;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * REST resource for metric profile operations
 */
@Path(OptimizerApiConstants.KRUIZE_BASE_PATH + OptimizerApiConstants.METRIC_PROFILES_PATH)
public class MetricProfileResource {

    private static final Logger LOG = Logger.getLogger(MetricProfileResource.class);

    @Inject
    ProfileService profileService;

    /**
     * List all metric profiles
     * GET /kruize/metricProfiles/list
     *
     * @return Response with list of metric profiles
     */
    @GET
    @Path(OptimizerApiConstants.LIST_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listMetricProfiles() {
        try {
            LOG.info(MessageConstants.INFO_FETCHING_METRIC_PROFILES_LIST);
            List<KruizeProfile> profiles = profileService.getMetricProfiles();
            
            if (profiles.isEmpty()) {
                return Response.ok(ApiResponse.success(
                        MessageConstants.NO_PROFILES_FOUND,
                        profiles
                )).build();
            }
            
            return Response.ok(ApiResponse.success(
                    MessageConstants.PROFILES_FETCHED_SUCCESS,
                    profiles
            )).build();
            
        } catch (Exception e) {
            LOG.error(MessageConstants.ERROR_FETCHING_METRIC_PROFILES, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error(MessageConstants.ERROR_FETCHING_PROFILES))
                    .build();
        }
    }

    /**
     * Install missing metric profiles
     * POST /kruize/metricProfiles/install
     *
     * @return Response with installation results
     */
    @POST
    @Path(OptimizerApiConstants.INSTALL_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    public Response installMetricProfiles() {
        try {
            LOG.info(MessageConstants.INFO_INSTALLING_METRIC_PROFILES);
            List<String> results = profileService.installMissingProfiles(ProfileType.METRIC);
            
            return Response.ok(ApiResponse.success(
                    MessageConstants.PROFILES_INSTALLED_SUCCESS,
                    results
            )).build();
            
        } catch (Exception e) {
            LOG.error(MessageConstants.ERROR_INSTALLING_METRIC_PROFILES, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error(MessageConstants.ERROR_INSTALLING_PROFILES))
                    .build();
        }
    }
}
