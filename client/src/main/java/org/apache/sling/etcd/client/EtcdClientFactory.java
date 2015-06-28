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

import java.net.URI;

import javax.annotation.Nonnull;

import aQute.bnd.annotation.ProviderType;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * Factory that allows to create {@link EtcdClient} instances.
 */
@ProviderType
public interface EtcdClientFactory {

    /**
     * <p>Creates a new instance of {@link EtcdClient}. The client require a single endpoint.</p>
     *
     * <p>If the client is communicating with an etcd cluster, then the endpoint should reference
     * a readwrite etcd proxy node.</p>
     *
     * <p>If the client is communicating with a single etcd node, the endpoint could reference it directly.</p>
     *
     * @param httpClient the HTTP client used for communicating with the etcd peers.
     * @param endpoint the etcd endpoint to communicate with the etcd peers.
     * @return the new {@link EtcdClient} instance.
     */
    @Nonnull
    EtcdClient create(@Nonnull CloseableHttpClient httpClient, @Nonnull URI endpoint);

}
