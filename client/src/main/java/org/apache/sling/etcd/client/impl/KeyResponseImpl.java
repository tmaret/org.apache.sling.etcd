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

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.etcd.client.KeyAction;
import org.apache.sling.etcd.client.KeyError;
import org.apache.sling.etcd.client.KeyResponse;
import org.apache.sling.commons.json.JSONObject;

public class KeyResponseImpl extends BaseResponse implements KeyResponse {

    private final JSONObject data;

    private final boolean isAction;

    public KeyResponseImpl(int status, @Nonnull String reasonPhrase, @Nonnull Map<String, List<String>> headers, @Nonnull JSONObject data) {
        super(status, reasonPhrase, headers);
        Check.nonNull(data, "data");
        this.data = data;
        isAction = ! data.has("errorCode");
    }

    public boolean isAction() {
        return isAction;
    }

    @Nullable
    public KeyError error() {
        return isAction ? null : new KeyErrorImpl(data);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    public KeyAction action() {
        return isAction ? new KeyActionImpl(data) : null;
    }

    @Override
    public String toString() {
        return "KeyResponseImpl{" +
                "data=" + data +
                ", isAction=" + isAction +
                '}';
    }
}