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
import org.apache.sling.etcd.client.KeyError;
import org.apache.sling.etcd.client.EtcdException;
import org.apache.sling.commons.json.JSONObject;

public class KeyErrorImpl implements KeyError {

    private static final String MISSING_PROPERTY = "Missing or invalid property '%s'";

    private final JSONObject data;

    /**
     * @param data the error json response
     */
    public KeyErrorImpl(@Nonnull JSONObject data) {
        this.data = data;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public String cause() {
        try {
            return data.getString("cause");
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "cause"));
        }
    }

    /**
     * {@inheritDoc}
     */
    public int errorCode() {
        try {
            return data.getInt("errorCode");
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "errorCode"));
        }
    }

    /**
     * {@inheritDoc}
     */
    public long index() {
        try {
            return data.getLong("index");
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "index"));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public String message() {
        try {
            return data.getString("message");
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "message"));
        }
    }

    @Override
    public String toString() {
        return "EtcdErrorImpl{" +
                "data=" + data +
                '}';
    }
}
