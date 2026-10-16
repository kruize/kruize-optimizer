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
 * REST resource for layer operations
 */
@Path(OptimizerApiConstants.KRUIZE_BASE_PATH + OptimizerApiConstants.LAYERS_PATH)
public class LayerResource {

    private static final Logger LOG = Logger.getLogger(LayerResource.class);

    @Inject
    ProfileService profileService;

    /**
     * List all layers
     * GET /kruize/layers/list
     *
     * @return Response with list of layers
     */
    @GET
    @Path(OptimizerApiConstants.LIST_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listLayers() {
        try {
            LOG.info("Fetching layers list");
            List<KruizeProfile> layers = profileService.getLayers();
            
            if (layers.isEmpty()) {
                return Response.ok(ApiResponse.success(
                        MessageConstants.NO_PROFILES_FOUND,
                        layers
                )).build();
            }
            
            return Response.ok(ApiResponse.success(
                    MessageConstants.PROFILES_FETCHED_SUCCESS,
                    layers
            )).build();
            
        } catch (Exception e) {
            LOG.errorf("Error fetching layers: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error(MessageConstants.ERROR_FETCHING_PROFILES))
                    .build();
        }
    }

    /**
     * Install missing layers
     * POST /kruize/layers/install
     *
     * @return Response with installation results
     */
    @POST
    @Path(OptimizerApiConstants.INSTALL_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    public Response installLayers() {
        try {
            LOG.info("Installing missing layers");
            List<String> results = profileService.installMissingProfiles(ProfileType.LAYER);
            
            return Response.ok(ApiResponse.success(
                    MessageConstants.PROFILES_INSTALLED_SUCCESS,
                    results
            )).build();
            
        } catch (Exception e) {
            LOG.errorf("Error installing layers: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error(MessageConstants.ERROR_INSTALLING_PROFILES))
                    .build();
        }
    }
}
