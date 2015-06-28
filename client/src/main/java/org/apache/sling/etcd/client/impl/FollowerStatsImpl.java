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
package org.apache.sling.etcd.client.impl;

import javax.annotation.Nonnull;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.etcd.client.EtcdException;
import org.apache.sling.etcd.client.FollowerStats;

public class FollowerStatsImpl implements FollowerStats {

    private static final String MISSING_PROPERTY = "Missing or invalid property '%s'";

    private final String id;

    private final JSONObject data;

    public FollowerStatsImpl(@Nonnull String id, @Nonnull JSONObject data) {
        Check.nonNull(id, "id");
        this.id = id;
        this.data = data;
    }

    @Nonnull
    public String id() {
        return id;
    }

    public long failCount() {
        try {
            return data.getJSONObject("counts").optLong("fail", -1);
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "counts"));
        }
    }

    public long successCount() {
        try {
            return data.getJSONObject("counts").optLong("success", -1);
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "counts"));
        }
    }

    public double latencyAverage() {
        try {
            return data.getJSONObject("latency").optDouble("average", Double.NaN);
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "latency"));
        }
    }

    public double latencyCurrent() {
        try {
            return data.getJSONObject("latency").optDouble("current", Double.NaN);
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "latency"));
        }
    }

    public double latencyMax() {
        try {
            return data.getJSONObject("latency").optDouble("maximum", Double.NaN);
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "latency"));
        }
    }

    public double latencyMin() {
        try {
            return data.getJSONObject("latency").optDouble("minimum", Double.NaN);
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "latency"));
        }
    }

    public double latencyStdDev() {
        try {
            return data.getJSONObject("latency").optDouble("standardDeviation", Double.NaN);
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "latency"));
        }
    }

}
