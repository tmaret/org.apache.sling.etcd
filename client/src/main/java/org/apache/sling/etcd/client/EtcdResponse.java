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
package org.apache.sling.etcd.client;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import aQute.bnd.annotation.ProviderType;

/**
 * Represent a generic response returned by the etcd REST API.
 */
@ProviderType
public interface EtcdResponse {

    /**
     * @return the HTTP response headers. The map key holds the header field name and
     *         map value holds the list of header field value.
     */
    @Nonnull
    Map<String, List<String>> headers();

    /**
     * @param headerName the header field name to get the values for.
     * @return the list of values for the given header field name
     */
    @Nonnull
    List<String> header(@Nonnull String headerName);

    /**
     * @param headerName the header field name the get the values for.
     * @return the first value for the given header field name ; or
     *         {@code null} if no value could be found.
     */
    @Nullable
    String headerFirst(@Nonnull String headerName);

    /**
     * @return the HTTP response status.
     */
    int status();

    /**
     * @return the HTTP response reason phrase.
     */
    @Nonnull
    String reasonPhrase();

}
