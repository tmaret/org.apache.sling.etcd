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
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;

/**
 * The etcd {@code ClusterView} implementation.
 * Instances of this class are immutable.
 */
public class EtcdClusterView implements ClusterView {

    private final String viewId;

    private final List<InstanceDescription> instances;

    private final InstanceDescription leader;

    /**
     * @param viewId the view identifier
     * @param builders the list of instance description builders sorted to maintain the required 'stable ordering'
     */
    public EtcdClusterView(@Nonnull String viewId, @Nonnull List<EtcdInstanceDescriptionBuilder> builders) {
        this.viewId = viewId;
        List<InstanceDescription> instances = new ArrayList<InstanceDescription>(builders.size());
        InstanceDescription leader = null;
        for (EtcdInstanceDescriptionBuilder builder : builders) {
            InstanceDescription id = builder.build(this);
            instances.add(id);
            if (id.isLeader()) {
                if (leader == null) {
                    leader = id;
                } else {
                    String msg = "Duplicated leader instances found: " + leader + ", " + id;
                    throw new EtcdDiscoveryRuntimeException(msg);
                }
            }
        }
        if (leader == null) {
            throw new EtcdDiscoveryRuntimeException("No leader instance found in the view");
        }
        this.instances = Collections.unmodifiableList(instances);
        this.leader = leader;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public String getId() {
        return viewId;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public List<InstanceDescription> getInstances() {
        return instances;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public InstanceDescription getLeader() {
        return leader;
    }

    @Override
    public String toString() {
        return "EtcdClusterView{" +
                "viewId='" + viewId + '\'' +
                ", instances=" + instances +
                ", leader=" + leader +
                '}';
    }

}
