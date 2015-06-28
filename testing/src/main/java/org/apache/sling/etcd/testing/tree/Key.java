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
package org.apache.sling.etcd.testing.tree;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

/**
 * Represent an etcd key.
 */
public class Key extends Node {

    private String value;

    public Key(@Nonnull String name, @Nullable String value, @Nullable Integer ttl, long modifiedIndex) {
        super(name, ttl, modifiedIndex);
        this.value = value;
    }

    @Nullable
    public String value() {
        return value;
    }

    public void value(@Nullable String value, long modifiedIndex) {
        this.modifiedIndex = modifiedIndex;
        this.value = value;
    }

    public boolean isFolder() {
        return false;
    }

    @Nonnull
    public JSONObject toJson(boolean recursive, boolean sorted)
            throws JSONException {
        JSONObject data = super.toJson(recursive, sorted);
        data.put("value", (value == null) ? "" : value);
        return data;
    }
}