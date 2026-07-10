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
package com.kruize.optimizer.client;

import com.kruize.optimizer.model.api.DatasourceListResponse;
import com.kruize.optimizer.model.kruize.KruizeProfile;
import com.kruize.optimizer.utils.OptimizerConstants;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

/**
 * REST client interface for Kruize API
 */
@RegisterRestClient(configKey = "kruize-api")
public interface KruizeClient {

    @GET
    @Path(OptimizerConstants.KruizeClientConstants.LIST_DATASOURCE_ENDPOINT)
    @Produces(MediaType.APPLICATION_JSON)
    DatasourceListResponse getDatasources();

    @GET
    @Path(OptimizerConstants.KruizeClientConstants.LIST_METADATA_PROFILE_ENDPOINT)
    @Produces(MediaType.APPLICATION_JSON)
    List<KruizeProfile> getMetadataProfiles(
            @QueryParam(OptimizerConstants.KruizeClientConstants.VERBOSE) boolean verbose);

    @GET
    @Path(OptimizerConstants.KruizeClientConstants.LIST_METRIC_PROFILE_ENDPOINT)
    @Produces(MediaType.APPLICATION_JSON)
    List<KruizeProfile> getMetricProfiles(
            @QueryParam(OptimizerConstants.KruizeClientConstants.VERBOSE) boolean verbose);

    @GET
    @Path(OptimizerConstants.KruizeClientConstants.LIST_LAYERS_ENDPOINT)
    @Produces(MediaType.APPLICATION_JSON)
    List<KruizeProfile> getLayers();

    @POST
    @Path(OptimizerConstants.KruizeClientConstants.CREATE_METADATA_PROFILE_ENDPOINT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String createMetadataProfile(Object profileDefinition);

    @POST
    @Path(OptimizerConstants.KruizeClientConstants.CREATE_METRIC_PROFILE_ENDPOINT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String createMetricProfile(Object profileDefinition);

    @POST
    @Path(OptimizerConstants.KruizeClientConstants.CREATE_LAYERS_ENDPOINT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String createLayer(Object layerDefinition);

    @POST
    @Path(OptimizerConstants.KruizeClientConstants.BULK_ENDPOINT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String bulkCreateExperiments(Object bulkRequest);

    @GET
    @Path(OptimizerConstants.KruizeClientConstants.BULK_ENDPOINT)
    @Produces(MediaType.APPLICATION_JSON)
    String getBulkJobStatus(@QueryParam(OptimizerConstants.KruizeClientConstants.JOB_ID) String jobId);

    @GET
    @Path(OptimizerConstants.KruizeClientConstants.BULK_CONFIGS_ENDPOINT)
    @Produces(MediaType.APPLICATION_JSON)
    String getBulkConfigs();

    @GET
    @Path(OptimizerConstants.KruizeClientConstants.BULK_CONFIGS_ENDPOINT)
    @Produces(MediaType.APPLICATION_JSON)
    String getBulkConfig(@QueryParam(OptimizerConstants.KruizeClientConstants.CONFIG_NAME) String configName);

    @POST
    @Path(OptimizerConstants.KruizeClientConstants.BULK_CONFIGS_ENDPOINT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    void createBulkConfig(Object configDefinition);
}
