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

import com.kruize.optimizer.exceptions.clusterScanExceptions.ClusterScanException;
import com.kruize.optimizer.exceptions.clusterScanExceptions.InvalidParameterException;
import com.kruize.optimizer.exceptions.clusterScanExceptions.ResourceNotFoundException;
import com.kruize.optimizer.utils.constants.MessageConstants;
import com.kruize.optimizer.utils.constants.OptimizerConstants;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for managing autotune labels on Kubernetes resources.
 * Provides a unified, production-grade approach to labeling operations.
 */
@ApplicationScoped
public class WorkloadLabelService {

    private static final Logger LOG = Logger.getLogger(WorkloadLabelService.class);

    @Inject
    KubernetesClient client;

    /**
     * Labels a namespace with custom labels.
     *
     * @param namespace the namespace to label
     * @param labelsToApply the labels to apply to the namespace
     * @throws ResourceNotFoundException if the namespace is not found
     * @throws ClusterScanException if labeling fails
     */
    public void labelNamespace(String namespace, Map<String, String> labelsToApply) {
        try {
            Namespace ns = client.namespaces().withName(namespace).get();
            if (ns == null) {
                String errorMessage = String.format(MessageConstants.ErrorMessage.NAMESPACE_NOT_FOUND_ERROR, namespace);
                throw new ResourceNotFoundException(errorMessage, "Namespace", namespace);
            }

            client.namespaces().withName(namespace).edit(n -> {
                ensureLabelsExist(n);
                applyLabels(n.getMetadata().getLabels(), labelsToApply);
                return n;
            });

            LOG.infof(MessageConstants.SuccessMessage.NAMESPACE_LABELED_SUCCESS, namespace);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = String.format(
                    MessageConstants.ErrorMessage.RESOURCE_LABELING_ERROR,
                    e.getMessage());
            throw new ClusterScanException(errorMessage, e);
        }
    }

    /**
     * Labels a workload with custom labels.
     *
     * @param namespace the namespace containing the workload
     * @param workloadName the name of the workload
     * @param workloadType the type of workload
     * @param labelsToApply the labels to apply to the workload
     * @throws ResourceNotFoundException if the workload is not found
     * @throws InvalidParameterException if the workload type is invalid
     * @throws ClusterScanException if labeling fails
     */
    public void labelWorkload(String namespace, String workloadName, String workloadType, Map<String, String> labelsToApply) {
        try {
            OptimizerConstants.WorkloadType type = OptimizerConstants.WorkloadType.valueOf(workloadType.toUpperCase());

            switch (type) {
                case DEPLOYMENT:
                    labelResource(
                            client.apps().deployments().inNamespace(namespace).withName(workloadName),
                            workloadName,
                            type,
                            labelsToApply,
                            MessageConstants.SuccessMessage.DEPLOYMENT_LABELED_SUCCESS,
                            MessageConstants.ErrorMessage.DEPLOYMENT_NOT_FOUND_ERROR
                    );
                    break;
                case STATEFULSET:
                    labelResource(
                            client.apps().statefulSets().inNamespace(namespace).withName(workloadName),
                            workloadName,
                            type,
                            labelsToApply,
                            MessageConstants.SuccessMessage.STATEFULSET_LABELED_SUCCESS,
                            MessageConstants.ErrorMessage.STATEFULSET_NOT_FOUND_ERROR
                    );
                    break;
                case REPLICASET:
                    labelResource(
                            client.apps().replicaSets().inNamespace(namespace).withName(workloadName),
                            workloadName,
                            type,
                            labelsToApply,
                            MessageConstants.SuccessMessage.REPLICASET_LABELED_SUCCESS,
                            MessageConstants.ErrorMessage.REPLICASET_NOT_FOUND_ERROR
                    );
                    break;
                default:
                    String errorMessage = String.format(
                            MessageConstants.ErrorMessage.INVALID_WORKLOAD_TYPE_ERROR,
                            workloadType);
                    throw new InvalidParameterException(errorMessage, OptimizerConstants.WORKLOAD_TYPE);
            }
        } catch (ResourceNotFoundException | InvalidParameterException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            String errorMessage = String.format(
                    MessageConstants.ErrorMessage.INVALID_WORKLOAD_TYPE_ERROR,
                    workloadType);
            throw new InvalidParameterException(errorMessage, OptimizerConstants.WORKLOAD_TYPE);
        } catch (Exception e) {
            String errorMessage = String.format(
                    MessageConstants.ErrorMessage.RESOURCE_LABELING_ERROR,
                    e.getMessage());
            throw new ClusterScanException(errorMessage, e);
        }
    }

    /**
     * Generic method to label any Kubernetes resource with custom labels.
     * Eliminates code duplication across different workload types.
     *
     * @param <T> the resource type
     */
    private <T extends HasMetadata> void labelResource(
            Resource<T> resource,
            String resourceName,
            OptimizerConstants.WorkloadType workloadType,
            java.util.Map<String, String> labelsToApply,
            String successMessageTemplate,
            String notFoundMessageTemplate) {

        T item = resource.get();
        if (item == null) {
            String errorMessage = String.format(notFoundMessageTemplate, resourceName);
            throw new ResourceNotFoundException(errorMessage, workloadType.getValue(), resourceName);
        }

        resource.edit(r -> {
            ensureLabelsExist(r);
            applyLabels(r.getMetadata().getLabels(), labelsToApply);
            return r;
        });

        LOG.infof(successMessageTemplate, resourceName);
    }

    /**
     * Ensures that the metadata labels map exists.
     */
    private void ensureLabelsExist(HasMetadata resource) {
        if (resource.getMetadata().getLabels() == null) {
            resource.getMetadata().setLabels(new HashMap<>());
        }
    }

    /**
     * Applies the provided labels to the existing labels map.
     *
     * @param existingLabels the existing labels map on the resource
     * @param labelsToApply the labels to apply
     */
    private void applyLabels(Map<String, String> existingLabels, Map<String, String> labelsToApply) {
        existingLabels.putAll(labelsToApply);
    }
}

