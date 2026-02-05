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
package com.kruize.optimizer.testutils;

import com.kruize.optimizer.model.ClusterScanResult;
import com.kruize.optimizer.model.EnableOptimizationRequest;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.*;

import java.util.*;

/**
 * Factory class for creating test data objects used across unit tests.
 * Provides consistent test data for Kubernetes resources, scan results, and requests.
 */
public class TestDataFactory {

    // Common test constants
    public static final String TEST_NAMESPACE = "test-namespace";
    public static final String TEST_DEPLOYMENT_NAME = "test-deployment";
    public static final String TEST_STATEFULSET_NAME = "test-statefulset";
    public static final String TEST_REPLICASET_NAME = "test-replicaset";
    public static final String TEST_CONTAINER_NAME = "test-container";
    public static final String TEST_CONTAINER_IMAGE = "nginx:latest";
    public static final String DEFAULT_LABEL_KEY = "kruize/autotune";
    public static final String DEFAULT_LABEL_VALUE = "enabled";
    public static final String CUSTOM_LABEL_KEY = "custom/label";
    public static final String CUSTOM_LABEL_VALUE = "custom-value";

    /**
     * Creates a test Namespace with optional labels.
     */
    public static Namespace createNamespace(String name, Map<String, String> labels) {
        Namespace namespace = new Namespace();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(name);
        metadata.setLabels(labels != null ? new HashMap<>(labels) : new HashMap<>());
        namespace.setMetadata(metadata);
        return namespace;
    }

    /**
     * Creates a test Deployment with optional labels.
     */
    public static Deployment createDeployment(String name, String namespace, Map<String, String> labels) {
        Deployment deployment = new Deployment();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(name);
        metadata.setNamespace(namespace);
        metadata.setLabels(labels != null ? new HashMap<>(labels) : new HashMap<>());
        deployment.setMetadata(metadata);

        DeploymentSpec spec = new DeploymentSpec();
        PodTemplateSpec template = createPodTemplateSpec();
        spec.setTemplate(template);
        deployment.setSpec(spec);

        return deployment;
    }

    /**
     * Creates a test StatefulSet with optional labels.
     */
    public static StatefulSet createStatefulSet(String name, String namespace, Map<String, String> labels) {
        StatefulSet statefulSet = new StatefulSet();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(name);
        metadata.setNamespace(namespace);
        metadata.setLabels(labels != null ? new HashMap<>(labels) : new HashMap<>());
        statefulSet.setMetadata(metadata);

        StatefulSetSpec spec = new StatefulSetSpec();
        PodTemplateSpec template = createPodTemplateSpec();
        spec.setTemplate(template);
        statefulSet.setSpec(spec);

        return statefulSet;
    }

    /**
     * Creates a test ReplicaSet with optional labels.
     */
    public static ReplicaSet createReplicaSet(String name, String namespace, Map<String, String> labels) {
        ReplicaSet replicaSet = new ReplicaSet();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(name);
        metadata.setNamespace(namespace);
        metadata.setLabels(labels != null ? new HashMap<>(labels) : new HashMap<>());
        replicaSet.setMetadata(metadata);

        ReplicaSetSpec spec = new ReplicaSetSpec();
        PodTemplateSpec template = createPodTemplateSpec();
        spec.setTemplate(template);
        replicaSet.setSpec(spec);

        return replicaSet;
    }

    /**
     * Creates a PodTemplateSpec with a test container.
     */
    private static PodTemplateSpec createPodTemplateSpec() {
        PodTemplateSpec template = new PodTemplateSpec();
        PodSpec podSpec = new PodSpec();
        
        Container container = new Container();
        container.setName(TEST_CONTAINER_NAME);
        container.setImage(TEST_CONTAINER_IMAGE);
        
        podSpec.setContainers(Collections.singletonList(container));
        template.setSpec(podSpec);
        
        return template;
    }

    /**
     * Creates a ClusterScanResult with test data.
     */
    public static ClusterScanResult createClusterScanResult(int namespaceCount, int workloadCount) {
        ClusterScanResult result = new ClusterScanResult();
        
        List<ClusterScanResult.NamespaceInfo> namespaces = new ArrayList<>();
        for (int i = 0; i < namespaceCount; i++) {
            namespaces.add(new ClusterScanResult.NamespaceInfo(
                "namespace-" + i,
                i % 2 == 0 // Alternate between optimized and not optimized
            ));
        }
        result.setNamespaces(namespaces);

        List<ClusterScanResult.WorkloadInfo> workloads = new ArrayList<>();
        for (int i = 0; i < workloadCount; i++) {
            List<ClusterScanResult.ContainerInfo> containers = Collections.singletonList(
                new ClusterScanResult.ContainerInfo(TEST_CONTAINER_NAME, TEST_CONTAINER_IMAGE)
            );
            
            workloads.add(new ClusterScanResult.WorkloadInfo(
                "workload-" + i,
                "namespace-" + (i % namespaceCount),
                "Deployment",
                i % 2 == 0,
                containers,
                createDefaultLabels()
            ));
        }
        result.setWorkloads(workloads);

        return result;
    }

    /**
     * Creates an EnableOptimizationRequest for namespace labeling.
     */
    public static EnableOptimizationRequest createNamespaceLabelRequest(String namespace, Map<String, String> labels) {
        EnableOptimizationRequest request = new EnableOptimizationRequest();
        request.setNamespace(namespace);
        request.setLabels(labels);
        return request;
    }

    /**
     * Creates an EnableOptimizationRequest for workload labeling.
     */
    public static EnableOptimizationRequest createWorkloadLabelRequest(
            String namespace, String workloadName, String workloadType, Map<String, String> labels) {
        EnableOptimizationRequest request = new EnableOptimizationRequest();
        request.setNamespace(namespace);
        request.setWorkloadName(workloadName);
        request.setWorkloadType(workloadType);
        request.setLabels(labels);
        return request;
    }

    /**
     * Creates default labels map.
     */
    public static Map<String, String> createDefaultLabels() {
        Map<String, String> labels = new HashMap<>();
        labels.put(DEFAULT_LABEL_KEY, DEFAULT_LABEL_VALUE);
        return labels;
    }

    /**
     * Creates custom labels map.
     */
    public static Map<String, String> createCustomLabels() {
        Map<String, String> labels = new HashMap<>();
        labels.put(CUSTOM_LABEL_KEY, CUSTOM_LABEL_VALUE);
        return labels;
    }

    /**
     * Creates a NamespaceList with test namespaces.
     */
    public static NamespaceList createNamespaceList(int count, boolean withLabels) {
        NamespaceList list = new NamespaceList();
        List<Namespace> items = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Map<String, String> labels = withLabels && i % 2 == 0 ? createDefaultLabels() : new HashMap<>();
            items.add(createNamespace("namespace-" + i, labels));
        }
        
        list.setItems(items);
        return list;
    }

    /**
     * Creates a DeploymentList with test deployments.
     */
    public static DeploymentList createDeploymentList(int count, String namespace, boolean withLabels) {
        DeploymentList list = new DeploymentList();
        List<Deployment> items = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Map<String, String> labels = withLabels && i % 2 == 0 ? createDefaultLabels() : new HashMap<>();
            items.add(createDeployment("deployment-" + i, namespace, labels));
        }
        
        list.setItems(items);
        return list;
    }

    /**
     * Creates a StatefulSetList with test stateful sets.
     */
    public static StatefulSetList createStatefulSetList(int count, String namespace, boolean withLabels) {
        StatefulSetList list = new StatefulSetList();
        List<StatefulSet> items = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Map<String, String> labels = withLabels && i % 2 == 0 ? createDefaultLabels() : new HashMap<>();
            items.add(createStatefulSet("statefulset-" + i, namespace, labels));
        }
        
        list.setItems(items);
        return list;
    }

    /**
     * Creates a ReplicaSetList with test replica sets.
     */
    public static ReplicaSetList createReplicaSetList(int count, String namespace, boolean withLabels) {
        ReplicaSetList list = new ReplicaSetList();
        List<ReplicaSet> items = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Map<String, String> labels = withLabels && i % 2 == 0 ? createDefaultLabels() : new HashMap<>();
            items.add(createReplicaSet("replicaset-" + i, namespace, labels));
        }
        
        list.setItems(items);
        return list;
    }
}

