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
import java.util.List;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.etcd.client.Member;
import org.junit.Test;

public class MemberImplTest {

    @Test
    public void testMember() throws Exception {
        JSONObject data = new JSONObject(IOUtils.toString(
                getClass().getResourceAsStream(
                        "/member.json")));
        Member m1 = new MemberImpl(data);
        Assert.assertNotNull(m1);
        Assert.assertEquals("324473db0474a678", m1.id());
        Assert.assertEquals("c3", m1.name());
        List<URI> clientUrls = m1.clientUrls();
        Assert.assertNotNull(clientUrls);
        Assert.assertEquals(1, clientUrls.size());
        Assert.assertEquals("http://localhost:4003", clientUrls.get(0).toString());
        List<URI> peerUrls = m1.peerUrls();
        Assert.assertNotNull(peerUrls);
        Assert.assertEquals(1, peerUrls.size());
        Assert.assertEquals("http://localhost:2383", peerUrls.get(0).toString());
    }
}