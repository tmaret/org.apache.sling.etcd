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

import org.apache.sling.etcd.client.EtcdClient;
import org.apache.sling.etcd.client.EtcdClientFactory;
import junit.framework.Assert;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.mockito.Mockito;

public class EtcdClientFactoryImplTest {

    @Test
    public void testCreate() throws Exception {
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        EtcdClientFactory factory = new EtcdClientFactoryImpl();
        EtcdClient client = factory.create(httpClient, new URI("localhost:4001"));
        Assert.assertNotNull(client);
    }
}