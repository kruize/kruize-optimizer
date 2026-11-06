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
import com.kruize.optimizer.model.kruize.Datasource;
import com.kruize.optimizer.service.DatasourceService;
import com.kruize.optimizer.utils.OptimizerConstants.MessageConstants;
import com.kruize.optimizer.utils.OptimizerConstants.OptimizerApiConstants;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * REST resource for datasource operations
 */
@Path(OptimizerApiConstants.KRUIZE_BASE_PATH + OptimizerApiConstants.DATASOURCES_PATH)
public class DatasourceResource {

    private static final Logger LOG = Logger.getLogger(DatasourceResource.class);

    @Inject
    DatasourceService datasourceService;

    /**
     * List all datasources
     * GET /kruize/datasources/list
     *
     * @return Response with list of datasources
     */
    @GET
    @Path(OptimizerApiConstants.LIST_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listDatasources() {
        try {
            LOG.info("Fetching datasources list");
            List<Datasource> datasources = datasourceService.getDatasources();
            
            if (datasources.isEmpty()) {
                return Response.ok(ApiResponse.success(
                        MessageConstants.NO_DATASOURCES_FOUND,
                        datasources
                )).build();
            }
            
            return Response.ok(ApiResponse.success(
                    MessageConstants.DATASOURCES_FETCHED_SUCCESS,
                    datasources
            )).build();
            
        } catch (Exception e) {
            LOG.errorf("Error fetching datasources: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error(MessageConstants.ERROR_FETCHING_DATASOURCES))
                    .build();
        }
    }
}
