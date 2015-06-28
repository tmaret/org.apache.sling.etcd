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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import aQute.bnd.annotation.ProviderType;

/**
 * Defines a client for accessing the CoreOS etcd service through the REST API version 2.0.
 *
 * @see <a href="https://coreos.com/docs/distributed-configuration/etcd-api">etcd-api</a>
 */
@ProviderType
public interface EtcdClient {

    /**
     * Execute a HTTP GET request against the etcd key API.
     * GET operations allow to get the value of a key, list keys in a hierarchy or wait for changes of a key.
     *
     * @param key the key to be fetched.
     * @param parameters the parameters to be added to the request uri.
     * @return a {@link KeyResponse} etcd response object.
     * @throws IOException if an IO exception occurred.
     */
    @Nonnull
    KeyResponse getKey(@Nonnull String key,
                        @Nonnull Map<String, String> parameters)
            throws IOException;

    /**
     * Execute a HTTP PUT request against the etcd key API.
     * PUT operations allow to set the value and update the ttl of a key,
     * perform atomic compare-and-swap operations.
     *
     * @param key the key to be set.
     * @param value the value to be stored at the given key.
     * @param parameters the parameters to be added to the request uri.
     * @return a {@link KeyResponse} etcd response object.
     * @throws IOException if an IO exception occurred.
     */
    @Nonnull
    KeyResponse putKey(@Nonnull String key,
                        @Nullable String value,
                        @Nonnull Map<String, String> parameters)
            throws IOException;

    /**
     * Execute a HTTP PUT request against the etcd key API.
     * PUT operations allow to set the value and update the ttl of a key,
     * perform atomic compare-and-swap operations.
     *
     * @param key the key to be set.
     * @param value a stream containing the value to be stored at the given key.
     * @param parameters the parameters to be added to the request uri.
     * @return a {@link KeyResponse} etcd response object.
     * @throws IOException if an IO exception occurred.
     */
    @Nonnull
    KeyResponse putKey(@Nonnull String key,
                        @Nonnull InputStream value,
                        @Nonnull Map<String, String> parameters)
            throws IOException;

    /**
     * Execute a HTTP POST request against the etcd key API.
     * POST operations allow to create in-order keys.
     *
     * @param key the parent key of the in-order keys to be created.
     * @param value the value to be stored at the created key.
     * @param parameters the parameters to be added to the request uri.
     * @return a {@link KeyResponse} etcd response object.
     * @throws IOException if an IO exception occurred.
     */
    @Nonnull
    KeyResponse postKey(@Nonnull String key,
                         @Nullable String value,
                         @Nonnull Map<String, String> parameters)
            throws IOException;

    /**
     * Execute a HTTP POST request against the etcd key API.
     *
     * @param key the parent key of the in-order keys to be created.
     * @param value a stream containing the value to be stored at the created key.
     * @param parameters the parameters to be added to the request uri.
     * @return a {@link KeyResponse} etcd response object.
     * @throws IOException if an IO exception occurred.
     */
    @Nonnull
    KeyResponse postKey(@Nonnull String key,
                         @Nonnull InputStream value,
                         @Nonnull Map<String, String> parameters)
            throws IOException;

    /**
     * Execute a HTTP DELETE request against the etcd key API.
     * DELETE operation allow to delete a key and perform compare-and-delete operations.
     *
     * @param key the key to be deleted.
     * @param parameters the parameters to be added to the request uri.
     * @return a {@link KeyResponse} etcd response object.
     * @throws IOException if an IO exception occurred.
     */
    @Nonnull
    KeyResponse deleteKey(@Nonnull String key,
                           @Nonnull Map<String, String> parameters)
            throws IOException;

    /**
     * Execute a GET request against the etcd members API.
     * GET operation return the coordinates for each peers in the etcd cluster.
     *
     * @return a {@link MembersResponse} response object.
     * @throws IOException if an IO exception occurred.
     */
    @Nonnull
    MembersResponse getMembers() throws IOException;

    /**
     * Fetch the statistics (follower latency, follower request error rate) from the etcd leader peer.
     *
     * @param leaderPeerEndpoint the leader peer endpoint.
     * @return the {@link LeaderStatsResponse} response object.
     * @throws IOException if an IO exception occurred.
     */
    @Nonnull
    LeaderStatsResponse getLeaderStats(@Nonnull URI leaderPeerEndpoint) throws IOException;

    /**
     * Fetch the statistics from an etcd peer.
     *
     * @param peerEndpoint the peer endpoint to fetch the statistics.
     * @return the {@link MemberStatsResponse} response object.
     * @throws IOException if an IO Exception occurred.
     */
    @Nonnull
    MemberStatsResponse getMemberStats(@Nonnull URI peerEndpoint) throws IOException;

    /**
     * Fetch the version string of the etcd process running on the instance handling the request.
     *
     * @param peerEndpoint the peer endpoint to fetch the version information.
     * @return a {@link VersionResponse} response object.
     * @throws IOException if an IO Exception occurred.
     */
    @Nonnull
    VersionResponse getVersion(@Nonnull URI peerEndpoint) throws IOException;

}
