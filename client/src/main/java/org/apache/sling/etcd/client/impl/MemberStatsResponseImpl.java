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

import java.text.ParseException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.etcd.client.EtcdException;
import org.apache.sling.etcd.client.MemberStatsResponse;

public class MemberStatsResponseImpl extends BaseResponse implements MemberStatsResponse {

    private static final String MISSING_PROPERTY = "Missing or invalid property '%s'";

    private final JSONObject data;

    public MemberStatsResponseImpl(int status, @Nonnull String reasonPhrase, @Nonnull Map<String, List<String>> headers, @Nonnull JSONObject data) {
        super(status, reasonPhrase, headers);
        Check.nonNull(data, "data");
        this.data = data;
    }

    @Nonnull
    public String id() {
        try {
            return data.getString("id");
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "id"));
        }
    }

    @Nonnull
    public String leaderId() {
        try {
            return data.getJSONObject("leaderInfo").getString("leader");
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "leaderInfo.leader"));
        }
    }

    @Nonnull
    public Calendar leaderStartTime() {
        String startTime = null;
        try {
            startTime = data.getJSONObject("leaderInfo").getString("startTime");
            return DateUtil.parseDate(startTime);
        } catch (ParseException e) {
            throw new EtcdException(String.format("Failed to parse startTime: %s", startTime));
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "leaderInfo.startTime"));
        }
    }

    @Nonnull
    public String leaderUptime() {
        try {
            return data.getJSONObject("leaderInfo").getString("uptime");
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "leaderInfo.uptime"));
        }
    }

    @Nonnull
    public String name() {
        try {
            return data.getString("name");
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "name"));
        }
    }

    public long recvAppendRequestCnt() {
        return data.optLong("recvAppendRequestCnt", -1);
    }

    public double recvBandwidthRate() {
        return data.optDouble("recvBandwidthRate", Double.NaN);
    }

    public double recvPkgRate() {
        return data.optDouble("recvPkgRate", Double.NaN);
    }

    public long sendAppendRequestCnt() {
        return data.optLong("sendAppendRequestCnt", -1);
    }

    public double sendBandwidthRate() {
        return data.optDouble("sendBandwidthRate", Double.NaN);
    }

    public double sendPkgRate() {
        return data.optDouble("sendPkgRate", Double.NaN);
    }

    @Nonnull
    public String state() {
        try {
            return data.getString("state");
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "state"));
        }
    }

    @Nonnull
    public Calendar startTime() {
        String startTime = null;
        try {
            startTime = data.getString("startTime");
            return DateUtil.parseDate(startTime);
        } catch (ParseException e) {
            throw new EtcdException(String.format("Failed to parse startTime: %s", startTime));
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "startTime"));
        }
    }

}
