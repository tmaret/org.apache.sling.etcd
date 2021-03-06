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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.etcd.client.EtcdResponse;

public class BaseResponse implements EtcdResponse {

    private final Map<String, List<String>> headers;

    private final String reasonPhrase;

    private int status;

    public BaseResponse(int status, @Nonnull String reasonPhrase, @Nonnull Map<String, List<String>> headers) {
        Check.nonNull(headers, "headers");
        Check.nonNull(reasonPhrase, "reasonPhrase");
        this.headers = Collections.unmodifiableMap(headers);
        this.reasonPhrase = reasonPhrase;
        this.status = status;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public List<String> header(@Nonnull String headerName) {
        Check.nonNull(headerName, "headerName");
        List<String> values = headers.get(headerName);
        return (values != null) ? Collections.unmodifiableList(values) : Collections.<String>emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    public String headerFirst(@Nonnull String headerName) {
        Check.nonNull(headerName, "headerName");
        List<String> values = headers.get(headerName);
        return (values != null && values.size() > 0) ? values.get(0) : null;
    }

    /**
     * {@inheritDoc}
     */
    public int status() {
        return status;
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public String reasonPhrase() {
        return reasonPhrase;
    }
}