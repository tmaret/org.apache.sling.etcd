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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.etcd.client.KeyResponse;
import org.apache.sling.etcd.common.EtcdHeaders;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Test;

public class KeyResponseImplTest {

    private static final Map<String, List<String>> HEADERS = new HashMap<String, List<String>>(){{
        put(EtcdHeaders.ETCD_INDEX, Collections.singletonList("35"));
        put("X-Raft-Index", Collections.singletonList("5398"));
        put("X-Raft-Term", Collections.singletonList("0"));
    }};

    @Test
    public void testErrorResponse() throws Exception {
        KeyResponse response = new KeyResponseImpl(400, "Bad Request", HEADERS, new JSONObject(IOUtils.toString(
                getClass().getResourceAsStream(
                        "/error-1.json"))));
        Assert.assertFalse(response.isAction());
        Assert.assertNull(response.action());
        Assert.assertNotNull(response.error());
        Assert.assertNotNull(response.headers());
        Assert.assertEquals(3, response.headers().size());
        Assert.assertEquals("5398", response.headerFirst("X-Raft-Index"));
        Assert.assertEquals(105, response.error().errorCode());
        Assert.assertEquals(400, response.status());
        Assert.assertEquals("Bad Request", response.reasonPhrase());
    }

    @Test
    public void testActionResponse() throws Exception {
        KeyResponse response = new KeyResponseImpl(200, "OK", HEADERS, new JSONObject(IOUtils.toString(
                getClass().getResourceAsStream(
                        "/node-3.json"))));
        Assert.assertTrue(response.isAction());
        Assert.assertNull(response.error());
        Assert.assertNotNull(response.action());
    }
}