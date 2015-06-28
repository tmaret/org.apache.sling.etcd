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

import java.util.Calendar;
import java.util.List;

import org.apache.sling.etcd.client.EtcdNode;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Test;

public class NodeImplTest {

    @Test
    public void testBasicNode() throws Exception {
        EtcdNode node = new EtcdNodeImpl(new JSONObject(IOUtils.toString(
                getClass().getResourceAsStream(
                        "/node-1.json"))));
        Assert.assertEquals(3, node.createdIndex());
        Assert.assertEquals("/message", node.key());
        Assert.assertEquals(3, node.modifiedIndex());
        Assert.assertEquals("Hello etcd", node.value());
        Assert.assertFalse(node.dir());
        Assert.assertNull(node.expiration());
        Assert.assertEquals(0, node.nodes().size());
        Assert.assertNull(node.ttl());
    }

    @Test
    public void testNodeWithExpiration() throws Exception {
        EtcdNode node = new EtcdNodeImpl(new JSONObject(IOUtils.toString(
                getClass().getResourceAsStream(
                        "/node-2.json"))));
        Assert.assertEquals(5, node.createdIndex());
        Calendar expiration = node.expiration();
        Assert.assertNotNull(expiration);
        Assert.assertEquals("/foo", node.key());
        Assert.assertEquals(5, node.modifiedIndex());
        Long ttl = node.ttl();
        Assert.assertNotNull(ttl);
        Assert.assertEquals(5L, ttl.longValue());
        Assert.assertEquals("bar", node.value());
        Assert.assertFalse(node.dir());
        Assert.assertEquals(0, node.nodes().size());
    }

    @Test
    public void testNodeWithSubNodes() throws Exception {
        EtcdNode node = new EtcdNodeImpl(new JSONObject(IOUtils.toString(
                getClass().getResourceAsStream(
                        "/node-3.json"))));
        Assert.assertEquals(2, node.createdIndex());
        Assert.assertTrue(node.dir());
        Assert.assertEquals("/queue", node.key());
        Assert.assertEquals(2, node.modifiedIndex());
        Assert.assertNull(node.value());
        Assert.assertNull(node.ttl());
        List<EtcdNode> nodes = node.nodes();
        Assert.assertNotNull(nodes);
        Assert.assertEquals(2, nodes.size());
        EtcdNode child1 = nodes.get(0);
        Assert.assertEquals(2, child1.createdIndex());
        Assert.assertEquals("/queue/2", child1.key());
        Assert.assertEquals(2, child1.modifiedIndex());
        Assert.assertEquals("Job1", child1.value());
        Assert.assertEquals(0, child1.nodes().size());
    }

}