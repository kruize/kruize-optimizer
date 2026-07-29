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
package com.kruize.optimizer.service;

import com.kruize.optimizer.client.KruizeClient;
import com.kruize.optimizer.exception.KruizeServiceException;
import com.kruize.optimizer.model.api.DatasourceListResponse;
import com.kruize.optimizer.model.kruize.Datasource;
import com.kruize.optimizer.utils.OptimizerConstants.MessageConstants;
import jakarta.ws.rs.core.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing datasources
 */
@ApplicationScoped
public class DatasourceService {

    private static final Logger LOG = Logger.getLogger(DatasourceService.class);

    @Inject
    @RestClient
    KruizeClient kruizeClient;

    /**
     * Fetch all datasources from Kruize
     *
     * @return List of datasources
     */
    public List<Datasource> getDatasources() {
        try {
            LOG.info("Fetching datasources from Kruize");
            DatasourceListResponse response = kruizeClient.getDatasources();
            
            return Optional.ofNullable(response)
                    .map(DatasourceListResponse::getDatasources)
                    .orElse(Collections.emptyList());
                    
        } catch (Exception e) {
            LOG.error(MessageConstants.KRUIZE_SERVICE_UNAVAILABLE, e);
            throw new KruizeServiceException(
                MessageConstants.KRUIZE_SERVICE_UNAVAILABLE,
                e,
                Response.Status.SERVICE_UNAVAILABLE.getStatusCode()
            );
        }
    }

    /**
     * Check if Kruize service is available
     *
     * @return true if available, false otherwise
     */
    public boolean isKruizeAvailable() {
        try {
            kruizeClient.getDatasources();
            return true;
        } catch (Exception e) {
            LOG.warn("Kruize service is not available: " + e.getMessage());
            return false;
        }
    }
}
