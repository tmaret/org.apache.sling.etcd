/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.discovery.etcd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.InstanceFilter;
import org.apache.sling.discovery.TopologyView;

/**
 * The etcd {@code TopologyView} implementation.
 * Instances of this class are not immutable (due to "current" state) but thread safe.
 */
public class EtcdTopologyView implements TopologyView {

    private final InstanceDescription local;

    private final Set<InstanceDescription> instances;

    private final Set<ClusterView> views;

    private final Set<String> clusterIds;

    private final Announces announces;

    private final String slingId;

    private volatile boolean current;

    /**
     * Holds the properties for all instances.
     */
    private final Map<String, Map<String, String>> properties;

    /**
     * @param announces The list of announces
     * @param properties The properties for all instances (including local instance). key is the instance slingId,
     *                   value is the map of name,value properties.
     * @param slingId The local instance Sling identifier
     * @param current {@code true} if the view is current ; {@code false} otherwise.
     */
    public EtcdTopologyView(@Nonnull Announces announces, @Nonnull Map<String, Map<String, String>> properties, @Nonnull String slingId, boolean current) {
        this.current = current;
        this.properties = Collections.unmodifiableMap(properties);
        this.announces = announces;
        this.slingId = slingId;
        int size = announces.size();
        if (size > 0) {
            // build cluster views and instances
            Set<EtcdClusterView> clusterViews = new HashSet<EtcdClusterView>();
            List<InstanceDescription> instances = new ArrayList<InstanceDescription>();
            Set<String> clusterIds = new HashSet<String>();
            Map<String, List<EtcdInstanceDescriptionBuilder>> clusters = getClusteredAnnounces(announces.getAnnounces());
            for (Map.Entry<String, List<EtcdInstanceDescriptionBuilder>> entry : clusters.entrySet()) {
                EtcdClusterView clusterView = new EtcdClusterView(entry.getKey(), entry.getValue());
                clusterViews.add(clusterView);
                instances.addAll(clusterView.getInstances());
                clusterIds.add(clusterView.getId());
            }
            // find the local instance and assert
            // that only one instance is local
            InstanceDescription local = null;
            for (InstanceDescription instance : instances) {
                if (instance.isLocal()) {
                    if (local == null) {
                        local = instance;
                    } else {
                        String msg = String.format("Duplicated local instances found: %s, %s", local, instance);
                        throw new EtcdDiscoveryRuntimeException(msg);
                    }
                }
            }
            if (local == null) {
                throw new EtcdDiscoveryRuntimeException("No local instance found in the view");
            }
            this.local = local;
            this.views = Collections.unmodifiableSet(Collections.<ClusterView>unmodifiableSet(clusterViews));
            this.instances = Collections.unmodifiableSet(new HashSet<InstanceDescription>(instances));
            this.clusterIds = Collections.unmodifiableSet(clusterIds);
        } else {
            throw new EtcdDiscoveryRuntimeException("Can't compute the topology with empty announces");
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCurrent() {
        return current;
    }

    /**
     * {@inheritDoc}
     */
    public InstanceDescription getLocalInstance() {
        return local;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public Set<InstanceDescription> getInstances() {
        return instances;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public Set<InstanceDescription> findInstances(InstanceFilter filter) {
        Set<InstanceDescription> match = new HashSet<InstanceDescription>();
        for (InstanceDescription id : instances) {
            if (filter.accept(id)) {
                match.add(id);
            }
        }
        return match;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public Set<ClusterView> getClusterViews() {
        return views;
    }

    @Override
    public String toString() {
        return "EtcdTopologyView{" +
                "current=" + current +
                ", local=" + local +
                ", instances=" + instances +
                ", views=" + views +
                '}';
    }

    @Nonnull
    public Set<String> getClusterIds() {
        return clusterIds;
    }

    @Nonnull
    public Announces getAnnounces() {
        return announces;
    }

    @Nonnull
    public Map<String, Map<String, String>> getProperties() {
        return properties;
    }

    /**
     * @return {@code true} if the view was current before setting it to non current ; {@code false} otherwise.
     */
    public boolean setNotCurrent() {
        boolean old = current;
        current = false;
        return old;
    }

    private EtcdInstanceDescriptionBuilder builder(@Nonnull Announce announce, boolean leader) {
        String id = announce.getData().slingId();
        boolean local = id.equals(slingId);
        Map<String, String> props = properties.get(id);
        if (props == null) {
            props = Collections.emptyMap();
        }
        return new EtcdInstanceDescriptionBuilder(id, props, local, leader);
    }

    /**
     * Group the announces per cluster identifier.
     * The first announce per cluster is the cluster leader.
     *
     * @return the clustered map of announces
     */
    private Map<String, List<EtcdInstanceDescriptionBuilder>> getClusteredAnnounces(List<Announce> announces) {
        Map<String, List<EtcdInstanceDescriptionBuilder>> clusters = new HashMap<String, List<EtcdInstanceDescriptionBuilder>>();
        for (Announce announce : announces) {
            String clusterId = announce.getData().clusterId();
            List<EtcdInstanceDescriptionBuilder> cluster = clusters.get(clusterId);
            if (cluster == null) {
                cluster = new ArrayList<EtcdInstanceDescriptionBuilder>();
                cluster.add(builder(announce, true));
                clusters.put(clusterId, cluster);
            } else {
                cluster.add(builder(announce, false));
            }
        }
        return clusters;
    }
}
