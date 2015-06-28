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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

/**
 * The {@code AnnounceData} wraps an instance announce in json format.
 */
public class AnnounceData {

    protected static final String PROPERTIES_MODIFIED_INDEX = "propertiesModifiedIndex";

    protected static final String SERVER_INFO = "serverInfo";

    protected static final String SLING_ID = "slingId";

    protected static final String CLUSTER_ID = "clusterId";

    /**
     * holds the announce properties in json format
     */
    private final JSONObject data;

    /**
     * @param slingId the Sling identifier of the instance associated to the announce
     * @param serverInfo the server info of the instance associated to the announce
     * @param clusterId the cluster identifier which the instance associated to the announce belongs to
     * @param modifiedIndex the properties last modified index of the instance associated to the announce
     */
    public AnnounceData(@Nonnull String slingId, @Nonnull String serverInfo, @Nonnull String clusterId, long modifiedIndex) {
        data = new JSONObject();
        try {
            data.put(SLING_ID, slingId);
            data.put(SERVER_INFO, serverInfo);
            data.put(PROPERTIES_MODIFIED_INDEX, modifiedIndex);
            data.put(CLUSTER_ID, clusterId);
        } catch (JSONException e) {
            throw new EtcdDiscoveryRuntimeException("Failed to build announce message", e);
        }
    }

    /**
     * @param json the announce in json format
     */
    public AnnounceData(@Nonnull String json) {
        data = parse(json);
    }

    /**
     * @return the last modified index for the instance properties
     */
    public long propertiesModifiedIndex() {
        return data.optLong(PROPERTIES_MODIFIED_INDEX, -1);
    }

    /**
     * @return the instance Sling identifier contained in the announce
     */
    @Nonnull
    public String slingId() {
        return data.optString(SLING_ID, "");
    }

    /**
     * @return the instance server info contained in the announce
     */
    @Nonnull
    public String serverInfo() {
        return data.optString(SERVER_INFO, "");
    }

    /**
     * @return the cluster identifier the instance belongs to.
     */
    @Nonnull
    public String clusterId() {
        return data.optString(CLUSTER_ID, "");
    }

    /**
     * @return the json representation of the announce
     */
    @Nonnull
    public JSONObject json() {
        return data;
    }

    /**
     * @return the json string representation of the announce
     */
    @Nonnull
    public String toString() {
        return data.toString();
    }

    @Nullable
    private JSONObject parse(@Nonnull String json) {
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            String msg = String.format("Failed to parse announcement from json: %s", json);
            throw new EtcdDiscoveryRuntimeException(msg);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnnounceData that = (AnnounceData) o;
        if (propertiesModifiedIndex() != that.propertiesModifiedIndex()) return false;
        if (!serverInfo().equals(that.serverInfo())) return false;
        if (!slingId().equals(that.slingId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = slingId().hashCode();
        result = 31 * result + serverInfo().hashCode();
        result = 31 * result + (int) (propertiesModifiedIndex() ^ (propertiesModifiedIndex() >>> 32));
        return result;
    }
}
