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

import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;

/**
 * The {@code EtcdInstanceDescriptionBuilder} allows to build {@code InstanceDescription} instances.
 */
public class EtcdInstanceDescriptionBuilder {

    private final Map<String, String> properties;

    private final String slingId;

    private final boolean local;

    private final boolean leader;

    /**
     * Create a new instance builder, passing all instance properties but the cluster view.
     */
    public EtcdInstanceDescriptionBuilder(@Nonnull String slingId, @Nonnull Map<String, String> properties, boolean local, boolean leader) {
        this.properties = properties;
        this.slingId = slingId;
        this.leader = leader;
        this.local = local;

    }

    /**
     * Builds the instance description and associate the provided cluster view.
     *
     * @param clusterView the cluster view that contains this instance
     * @return the instance description
     */
    public InstanceDescription build(ClusterView clusterView) {
        return new EtcdInstanceDescription(clusterView, properties, slingId, local, leader);
    }
}
