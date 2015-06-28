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
import javax.annotation.Nullable;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.etcd.client.KeyAction;
import org.apache.sling.etcd.client.EtcdNode;
import org.apache.sling.etcd.client.EtcdException;
import org.apache.sling.commons.json.JSONObject;

public class KeyActionImpl implements KeyAction {

    private static final String MISSING_PROPERTY = "Missing or invalid property '%s'";

    private final JSONObject data;

    /**
     * @param data the action json response
     */
    public KeyActionImpl(@Nonnull JSONObject data) {
        this.data = data;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public String action() {
        try {
            return data.getString("action");
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "action"));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public EtcdNode node() {
        try {
            return new EtcdNodeImpl(data.getJSONObject("node"));
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "node"));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    public EtcdNode prevNode() {
        JSONObject prevNode = data.optJSONObject("prevNode");
        return prevNode != null ? new EtcdNodeImpl(prevNode) : null;
    }

    @Override
    public String toString() {
        return "EtcdActionImpl{" +
                "data=" + data +
                '}';
    }
}
