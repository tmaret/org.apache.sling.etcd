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

import junit.framework.Assert;
import org.apache.sling.etcd.client.VersionResponse;
import org.junit.Test;

public class VersionResponseImplTest {

    @Test
    public void testVersion() throws Exception {
        VersionResponse response = new VersionResponseImpl(200, "OK", Collections.singletonMap("header", Collections.singletonList("value")), "etcd 2.0.0");
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.status());
        Assert.assertEquals("OK", response.reasonPhrase());
        Assert.assertEquals("value", response.headerFirst("header"));
        Assert.assertEquals("etcd 2.0.0", response.version());
    }
}