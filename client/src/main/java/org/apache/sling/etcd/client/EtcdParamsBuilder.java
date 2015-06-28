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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This builder allows to construct a map of the required parameters when
 * accessing the etcd service through the key API.
 */
public final class EtcdParamsBuilder {

    private final Map<String, String> params = new HashMap<String, String>();

    /**
     * Set the expiration time for a key.
     *
     * @param ttl the time to live in seconds.
     * @return {@code this}
     */
    @Nonnull
    public EtcdParamsBuilder ttl(long ttl) {
        params.put("ttl", String.valueOf(ttl));
        return this;
    }

    /**
     * Directory flag.
     *
     * @param dir {@code true} to flag a directory ; {@code false} otherwise.
     * @return {@code this}
     */
    @Nonnull
    public EtcdParamsBuilder dir(boolean dir) {
        params.put("dir", String.valueOf(dir));
        return this;
    }

    /**
     * Key already exist condition.
     *
     * @param prevExist {@code true} to require existence of previous key ; {@code false} otherwise.
     * @return {@code this}
     */
    @Nonnull
    public EtcdParamsBuilder prevExist(boolean prevExist) {
        params.put("prevExist", String.valueOf(prevExist));
        return this;
    }

    /**
     * Previous value matches condition.
     *
     * @param prevValue the value of the previous value.
     * @return {@code this}
     */
    @Nonnull
    public EtcdParamsBuilder prevValue(@Nonnull String prevValue) {
        nonNull(prevValue, "prevValue");
        params.put("prevValue", prevValue);
        return this;
    }

    /**
     * Previous index matches condition.
     *
     * @param prevIndex the value of the previous index.
     * @return {@code this}
     */
    @Nonnull
    public EtcdParamsBuilder prevIndex(long prevIndex) {
        params.put("prevIndex", String.valueOf(prevIndex));
        return this;
    }

    /**
     * Indicates whether the action must be applied recursively on the keys or not.
     *
     * @param recursive {@code true} fo a recursive action ; {@code false} otherwise.
     * @return {@code this}
     */
    @Nonnull
    public EtcdParamsBuilder recursive(boolean recursive) {
        params.put("recursive", String.valueOf(recursive));
        return this;
    }

    /**
     * Wait for change flag.
     *
     * @param wait {@code true} for waiting for a change ; {@code false} otherwise.
     * @return {@code this}
     */
    @Nonnull
    public EtcdParamsBuilder wait(boolean wait) {
        params.put("wait", String.valueOf(wait));
        return this;
    }

    /**
     * Index from which to wait for changes.
     *
     * @param waitIndex the index value.
     * @return {@code this}
     */
    @Nonnull
    public EtcdParamsBuilder waitIndex(long waitIndex) {
        params.put("waitIndex", String.valueOf(waitIndex));
        return this;
    }

    /**
     * Requires to fully linearized read operations.
     * This can be applied to GET only.
     *
     * @param quorum {@code true} if full linearized reads are required ; {@code false} otherwise.
     * @return {@code this}
     */
    @Nonnull
    public EtcdParamsBuilder quorum(boolean quorum) {
        params.put("quorum", String.valueOf(quorum));
        return this;
    }

    /**
     * Requires to return sorted list when reading in ordered keys.
     *
     * @param sorted {@code true} if sorted keys are required ; {@code false} otherwise.
     * @return {@code this}
     */
    @Nonnull
    public EtcdParamsBuilder sorted(boolean sorted) {
        params.put("sorted", String.valueOf(sorted));
        return this;
    }

    /**
     * Build the required parameters.
     *
     * @return a new map containing the required parameters.
     */
    @Nonnull
    public Map<String, String> build() {
        return Collections.unmodifiableMap(params);
    }

    @Nonnull
    private <T> T nonNull(@Nullable T o, String fieldName) {
        if (o != null) {
            return o;
        }
        throw new NullPointerException(String.format("%s must not be null", fieldName));
    }

}
