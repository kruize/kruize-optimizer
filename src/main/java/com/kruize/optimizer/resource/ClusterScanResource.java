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

import com.kruize.optimizer.exceptions.clusterScanExceptions.ClusterScanException;
import com.kruize.optimizer.exceptions.clusterScanExceptions.InvalidParameterException;
import com.kruize.optimizer.exceptions.clusterScanExceptions.ResourceNotFoundException;
import com.kruize.optimizer.exceptions.targetLabelsProcessing.InvalidTargetLabelFormatException;
import com.kruize.optimizer.model.ApiResponse;
import com.kruize.optimizer.model.ClusterScanResult;
import com.kruize.optimizer.model.EnableOptimizationRequest;
import com.kruize.optimizer.service.WorkloadLabelService;
import com.kruize.optimizer.service.WorkloadScanService;
import com.kruize.optimizer.utils.TargetLabelUtils;
import com.kruize.optimizer.utils.constants.MessageConstants;
import com.kruize.optimizer.utils.constants.OptimizerConstants;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * REST resource for scanning Kubernetes clusters and managing workload optimization labels.
 * <p>
 * This resource provides endpoints to:
 * <ul>
 *   <li>Scan clusters for namespaces and workloads (Deployments, StatefulSets, ReplicaSets)</li>
 *   <li>Enable autotune optimization by labeling resources</li>
 * </ul>
 * </p>
 * <p>
 * This is a lightweight REST controller that delegates business logic to dedicated services,
 * following production-grade separation of concerns principles.
 * </p>
 *
 * @see ClusterScanResult
 * @see WorkloadScanService
 * @see WorkloadLabelService
 */
@Path(OptimizerConstants.ApiEndpoints.SCAN_PATH)
public class ClusterScanResource {

    private static final Logger LOG = Logger.getLogger(ClusterScanResource.class);

    @Inject
    WorkloadScanService scanService;

    @Inject
    WorkloadLabelService labelService;

    @Inject
    TargetLabelUtils targetLabelUtils;

    /**
     * Scans the Kubernetes cluster for namespaces and workloads.
     * <p>
     * This endpoint scans the cluster and returns information about namespaces and workloads
     * (Deployments, StatefulSets, ReplicaSets). By default, it only returns resources that
     * match the configured target labels. Set {@code scanAllWorkloads} to true to scan all
     * resources regardless of labels.
     * </p>
     *
     * @param scanAllWorkloads if true, scans all workloads; if false, only scans workloads
     *                         matching target labels (default: false)
     * @return Response containing ApiResponse with ClusterScanResult on success or error details on failure
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response scan(@QueryParam(OptimizerConstants.ApiEndpoints.SCAN_ALL_WORKLOADS_PARAM) boolean scanAllWorkloads) {
        try {
            ClusterScanResult result = scanService.scanCluster(scanAllWorkloads);
            return Response.ok(ApiResponse.success(result)).build();
        } catch (ClusterScanException e) {
            LOG.error("Cluster scan failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Cluster scan failed: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Enables autotune optimization by adding labels to Kubernetes resources.
     * 
     * This endpoint accepts a JSON payload to specify which resources to label and with which labels.
     * If labels are not provided in the request, the default label (kruize/autotune=enabled) is used.
     * All provided labels must be present in the configured target labels.
     * 
     * 
     * Example payload to label a namespace:
     * {
     *   "namespace": "my-namespace",
     *   "labels": {
     *     "kruize/autotune": "enabled",
     *   }
     * }
     * 
     * Example payload to label a workload:
     * {
     *   "namespace": "my-namespace",
     *   "workloadName": "my-deployment",
     *   "workloadType": "Deployment",
     *   "labels": {
     *     "kruize/autotune": "enabled"
     *   }
     * }
     *
     * @param request the enable autotune request containing namespace, workload details, and labels
     * @return Response indicating success or failure of the labeling operation
     */
    @POST
    @Path(OptimizerConstants.ApiEndpoints.ENABLE_OPTIMIZATION_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response enableAutotune(EnableOptimizationRequest request) {

        try {
            // Validate namespace parameter
            if (request.getNamespace() == null || request.getNamespace().trim().isEmpty()) {
                throw new InvalidParameterException(MessageConstants.ErrorMessage.NAMESPACE_REQUIRED_ERROR, OptimizerConstants.NAMESPACE);
            }

            // Determine labels to apply: use provided labels or default
            Map<String, String> labelsToApply;

            if (request.getLabels() == null || request.getLabels().isEmpty()) {
                // Use default label if none provided
                labelsToApply = targetLabelUtils.getDefaultLabel();
                LOG.info(MessageConstants.WarningMessage.NO_TARGET_LABELS_WARNING);
            } else {
                // Validate that provided labels are in target labels
                targetLabelUtils.validateLabels(request.getLabels());
                labelsToApply = request.getLabels();
            }

            String namespace = request.getNamespace().trim();

            // Check if labeling a workload or namespace
            if (request.getWorkloadName() != null && !request.getWorkloadName().trim().isEmpty()) {
                // Label workload
                String workloadName = request.getWorkloadName().trim();
                String workloadType = (request.getWorkloadType() == null || request.getWorkloadType().trim().isEmpty())
                        ? OptimizerConstants.WorkloadType.DEPLOYMENT.getValue()
                        : request.getWorkloadType().trim();

                labelService.labelWorkload(namespace, workloadName, workloadType, labelsToApply);
                
                String successMessage = String.format(MessageConstants.SuccessMessage.WORKLOAD_LABELED_SUCCESS, workloadType, workloadName, namespace, labelsToApply);
                LOG.info(successMessage);
                return Response.ok(ApiResponse.successMessage(successMessage)).build();
            } else {
                // Label namespace
                labelService.labelNamespace(namespace, labelsToApply);
                
                String successMessage = String.format(MessageConstants.SuccessMessage.NAMESPACE_LABELED_SUCCESS, namespace);
                LOG.info(successMessage);
                return Response.ok(ApiResponse.successMessage(successMessage)).build();
            }
        } catch (InvalidParameterException | InvalidTargetLabelFormatException e) {
            LOG.error(e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        } catch (ResourceNotFoundException e) {
            LOG.error(e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error(e.getMessage()))
                    .build();
        } catch (Exception e) {
            String errorMessage = String.format(
                    MessageConstants.ErrorMessage.RESOURCE_LABELING_ERROR,
                    e.getMessage());
            LOG.error(errorMessage, e);
            return Response.serverError()
                    .entity(ApiResponse.error(errorMessage))
                    .build();
        }
    }
}

