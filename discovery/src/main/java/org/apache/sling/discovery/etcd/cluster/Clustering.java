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
package org.apache.sling.discovery.etcd.cluster;

import javax.annotation.Nullable;

/**
 * The {@code Clustering} interface allows to return the cluster identifier
 * of the local instance.
 */
public interface Clustering {

    /**
     * Returns the cluster identifier for the local instance.
     *
     * @return the valid cluster identifier of the local instance or {@code null} if undefined.
     */
    @Nullable
    String getClusterId();


    /**
     * Sets the cluster identifier for the local instance.
     *
     * @return the cluster identifier that has been set or
     * {@code null} if no cluster could be set.
     */
    @Nullable
    String setClusterId();


    /**
     * Check if the clustering is supported.
     *
     * @return {@code true} if the clustering is supported ; {@code false} otherwise.
     */
    boolean isSupported();
}
