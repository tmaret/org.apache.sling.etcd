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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.etcd.client.EtcdException;
import org.apache.sling.etcd.client.Member;

public class MemberImpl implements Member {

    private static final String MISSING_PROPERTY = "Missing or invalid property '%s'";

    private final JSONObject data;

    public MemberImpl(@Nonnull JSONObject data) {
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
    public String name() {
        try {
            return data.getString("name");
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "name"));
        }
    }

    @Nonnull
    public List<URI> peerUrls() {
        try {
            return parseUris(data.getJSONArray("peerURLs"));
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "peerURLs"));
        }
    }

    @Nonnull
    public List<URI> clientUrls() {
        try {
            return parseUris(data.getJSONArray("clientURLs"));
        } catch (JSONException e) {
            throw new EtcdException(String.format(MISSING_PROPERTY, "clientURLs"));
        }
    }

    @Nonnull
    private List<URI> parseUris(JSONArray peers) {
        List<URI> uris = new ArrayList<URI>();
        for (int i = 0 ; i < peers.length() ; i++) {
            uris.add(parseUri(peers.optString(i)));
        }
        return Collections.unmodifiableList(uris);
    }

    @Nonnull
    private URI parseUri(@Nonnull String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new EtcdException(e.getMessage(), e);
        }
    }
}
