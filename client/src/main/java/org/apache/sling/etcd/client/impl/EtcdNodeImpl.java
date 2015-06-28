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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.etcd.client.EtcdException;
import org.apache.sling.etcd.client.EtcdNode;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EtcdNodeImpl implements EtcdNode {

    private static final String MISSING_PROPERTY = "Missing or invalid property '%s'";

    private static final Logger LOG = LoggerFactory.getLogger(EtcdNodeImpl.class);

    private final JSONObject data;

    /**
     * @param data the node json
     */
    public EtcdNodeImpl(@Nonnull JSONObject data) {
        this.data = data;
    }

    /**
     * {@inheritDoc}
     */
    public long createdIndex() {
        return data.optLong("createdIndex", -1);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public String key() {
        try {
            return data.getString("key");
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "key"));
        }
    }

    /**
     * {@inheritDoc}
     */
    public long modifiedIndex() {
        return data.optLong("modifiedIndex", -1);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    public String value() {
        return data.optString("value", null);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    public Long ttl() {
        return data.has("ttl") ? data.optLong("ttl") : null;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    public Calendar expiration() {
        String expiration = data.optString("expiration", null);
        if (expiration != null) {
            try {
                return DateUtil.parseDate(expiration);
            } catch (ParseException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean dir() {
        return data.optBoolean("dir", false);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public List<EtcdNode> nodes() {
        JSONArray nodes = data.optJSONArray("nodes");
        if (nodes != null) {
            List<EtcdNode> children = new ArrayList<EtcdNode>(nodes.length());
            for (int i = 0 ; i < nodes.length() ; i++) {
                EtcdNode child = new EtcdNodeImpl(nodes.optJSONObject(i));
                children.add(child);
            }
            return children;
        }
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public String toJson() {
        return data.toString();
    }

    @Override
    public String toString() {
        return "EtcdNodeImpl{" +
                "data=" + data +
                '}';
    }
}
