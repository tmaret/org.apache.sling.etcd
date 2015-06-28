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

import java.util.regex.Pattern;

import javax.annotation.Nonnull;

/**
 * The {@code ConfigClustering} determines the cluster identifier according to the pattern
 * of allowed characters ([A-Za-z-_]).
 */
public class ConfigClustering implements Clustering {

    /**
     * The pattern matching valid cluster identifiers.
     */
    private static final Pattern CLUSTER_ID = Pattern.compile("^[a-zA-Z0-9-_]+$");

    /**
     * The valid cluster identifier.
     */
    private final String clusterId;

    /**
     * Build a {@code ConfigClustering} instance using a cluster id and a default cluster id.
     * If the cluster id does not match the allowed pattern or is empty "", then the default
     * cluster identifier is used.
     *
     * @param clusterId the proposed cluster identifier (trimmed).
     * @param defaultClusterId the cluster identifier to use as fallback if the proposed
     *                         identifier does not complies to set of allowed characters.
     */
    @Nonnull
    public ConfigClustering(@Nonnull String clusterId, @Nonnull String defaultClusterId) {
        String trimmedId = clusterId.trim();
        this.clusterId = (CLUSTER_ID.matcher(trimmedId).matches()) ? trimmedId : defaultClusterId;
    }

    @Nonnull
    public String getClusterId() {
        return clusterId;
    }

    @Nonnull
    public String setClusterId() {
        return clusterId;
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
