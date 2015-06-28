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

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;

/**
 * The {@code EtcdInstanceDescription} is the etcd {@code InstanceDescription} implementation.
 * Instances of this class are immutable.
 */
public class EtcdInstanceDescription implements InstanceDescription {

    private final ClusterView view;

    private final Map<String, String> properties;

    private final String slingId;

    private final boolean local;

    private final boolean leader;

    /**
     * @param view the cluster view associated to this instance
     * @param properties the properties associated to this instance
     * @param slingId the Sling identifier associated to this instance
     * @param local {@code true} if the instance is the local instance, {@code false} otherwise
     * @param leader {@code true} if the instance is the leader, {@code false} otherwise
     */
    public EtcdInstanceDescription(@Nonnull ClusterView view, @Nonnull Map<String, String> properties, @Nonnull String slingId, boolean local, boolean leader) {
        this.properties = Collections.unmodifiableMap(properties);
        this.view = view;
        this.slingId = slingId;
        this.local = local;
        this.leader = leader;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public ClusterView getClusterView() {
        return view;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLeader() {
        return leader;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLocal() {
        return local;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public String getSlingId() {
        return slingId;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    public String getProperty(String name) {
        return properties.get(name);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "EtcdInstanceDescription{" +
                "properties=" + properties +
                ", slingId='" + slingId + '\'' +
                ", local=" + local +
                ", leader=" + leader +
                '}';
    }
}
