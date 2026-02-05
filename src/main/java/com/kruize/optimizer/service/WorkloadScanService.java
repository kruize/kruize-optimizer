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
import com.kruize.optimizer.model.ClusterScanResult;
import com.kruize.optimizer.utils.TargetLabelUtils;
import com.kruize.optimizer.utils.constants.MessageConstants;
import com.kruize.optimizer.utils.constants.OptimizerConstants;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for scanning Kubernetes workloads and namespaces.
 */
@ApplicationScoped
public class WorkloadScanService {

    private static final Logger LOG = Logger.getLogger(WorkloadScanService.class);

    @Inject
    KubernetesClient client;

    @Inject
    TargetLabelUtils targetLabelUtils;

    /**
     * Scans the cluster for namespaces and workloads.
     *
     * @param scanAllWorkloads if true, includes all workloads; otherwise only labeled ones
     * @return ClusterScanResult containing namespaces and workloads
     * @throws ClusterScanException if scanning fails
     */
    public ClusterScanResult scanCluster(boolean scanAllWorkloads) {
        try {
            LOG.info(MessageConstants.InfoMessage.SCANNING_CLUSTER_INFO);

            // Scan namespaces
            Map<String, Boolean> namespaceMap = scanNamespaces(scanAllWorkloads);
            
            // Scan all workload types
            List<ClusterScanResult.WorkloadInfo> allWorkloads = new ArrayList<>();

            allWorkloads.addAll(scanWorkloads(
                client.apps().deployments().inAnyNamespace().list().getItems(),
                OptimizerConstants.WorkloadType.DEPLOYMENT,
                Deployment::getSpec,
                namespaceMap,
                scanAllWorkloads
            ));
            allWorkloads.addAll(scanWorkloads(
                client.apps().statefulSets().inAnyNamespace().list().getItems(),
                OptimizerConstants.WorkloadType.STATEFULSET,
                StatefulSet::getSpec,
                namespaceMap,
                scanAllWorkloads
            ));
            allWorkloads.addAll(scanWorkloads(
                client.apps().replicaSets().inAnyNamespace().list().getItems(),
                OptimizerConstants.WorkloadType.REPLICASET,
                ReplicaSet::getSpec,
                namespaceMap,
                scanAllWorkloads
            ));

            ClusterScanResult result = new ClusterScanResult();
            result.setNamespaces(buildNamespaceInfoList(namespaceMap, scanAllWorkloads));
            result.setWorkloads(allWorkloads);

            LOG.infof(MessageConstants.SuccessMessage.SCAN_COMPLETED_INFO,
                    result.getNamespaces().size(),
                    result.getWorkloads().size());

            return result;
        } catch (Exception e) {
            String errorMessage = String.format(
                    MessageConstants.ErrorMessage.CLUSTER_SCAN_ERROR,
                    e.getMessage());
            throw new ClusterScanException(errorMessage, e);
        }
    }

    /**
     * Scans namespaces and returns a map of namespace names to their optimization status.
     */
    private Map<String, Boolean> scanNamespaces(boolean scanAllWorkloads) {
        List<Namespace> namespaces = client.namespaces().list().getItems();
        return namespaces.stream()
                .collect(Collectors.toMap(
                        ns -> ns.getMetadata().getName(),
                        ns -> isResourceOptimized(ns.getMetadata().getLabels())
                ));
    }

    /**
     * Builds the namespace info list from the map.
     */
    private List<ClusterScanResult.NamespaceInfo> buildNamespaceInfoList(
            Map<String, Boolean> namespaceMap, boolean scanAllWorkloads) {
        return namespaceMap.entrySet().stream()
                .filter(entry -> scanAllWorkloads || entry.getValue())
                .map(entry -> new ClusterScanResult.NamespaceInfo(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Generic method to scan workloads of any type.
     * Eliminates code duplication across Deployment, StatefulSet, and ReplicaSet scanning.
     *
     * @param <T> the workload type (Deployment, StatefulSet, or ReplicaSet)
     * @param <S> the spec type containing the pod template
     */
    private <T extends HasMetadata, S> List<ClusterScanResult.WorkloadInfo> scanWorkloads(
            List<T> workloads,
            OptimizerConstants.WorkloadType workloadType,
            Function<T, S> specExtractor,
            Map<String, Boolean> namespaceMap,
            boolean scanAllWorkloads) {

        return workloads.stream()
                .map(workload -> {
                    try {
                        String namespace = workload.getMetadata().getNamespace();
                        boolean isNamespaceOptimized = namespaceMap.getOrDefault(namespace, false);
                        boolean isWorkloadOptimized = isResourceOptimized(workload.getMetadata().getLabels());
                        boolean isOptimized = isNamespaceOptimized || isWorkloadOptimized;

                        if (!scanAllWorkloads && !isOptimized) {
                            return null;
                        }

                        PodTemplateSpec template = extractPodTemplate(specExtractor.apply(workload));
                        
                        List<ClusterScanResult.ContainerInfo> containers = template.getSpec()
                                .getContainers()
                                .stream()
                                .map(c -> new ClusterScanResult.ContainerInfo(c.getName(), c.getImage()))
                                .collect(Collectors.toList());

                        return new ClusterScanResult.WorkloadInfo(
                                workload.getMetadata().getName(),
                                namespace,
                                workloadType.getValue(),
                                isOptimized,
                                containers,
                                workload.getMetadata().getLabels()
                        );
                    } catch (Exception e) {
                        LOG.errorf("Error processing %s %s: %s",
                                workloadType.getValue(),
                                workload.getMetadata().getName(),
                                e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Extracts PodTemplateSpec from different spec types.
     */
    private PodTemplateSpec extractPodTemplate(Object spec) {
        if (spec instanceof io.fabric8.kubernetes.api.model.apps.DeploymentSpec) {
            return ((io.fabric8.kubernetes.api.model.apps.DeploymentSpec) spec).getTemplate();
        } else if (spec instanceof io.fabric8.kubernetes.api.model.apps.StatefulSetSpec) {
            return ((io.fabric8.kubernetes.api.model.apps.StatefulSetSpec) spec).getTemplate();
        } else if (spec instanceof io.fabric8.kubernetes.api.model.apps.ReplicaSetSpec) {
            return ((io.fabric8.kubernetes.api.model.apps.ReplicaSetSpec) spec).getTemplate();
        }
        throw new IllegalArgumentException("Unsupported spec type: " + spec.getClass().getName());
    }

    /**
     * Checks if a resource's labels match any configured target labels.
     *
     * @param resourceLabels the labels from the Kubernetes resource
     * @return true if any target label matches, false otherwise
     */
    private boolean isResourceOptimized(Map<String, String> resourceLabels) {
        if (resourceLabels == null || resourceLabels.isEmpty()) {
            return false;
        }
        return targetLabelUtils.getTargetLabels().entrySet().stream()
                .anyMatch(target -> target.getValue().equals(resourceLabels.get(target.getKey())));
    }
}