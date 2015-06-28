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
package org.apache.sling.discovery.etcd;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.sling.etcd.client.EtcdNode;
import junit.framework.Assert;
import org.junit.Test;

public class NodeBuilderTest {

    @Test
    public void testBuildingNode() throws Exception {
        Calendar expiration = Calendar.getInstance();
        EtcdNode node = new EtcdNodeBuilder()
                .expiration(expiration)
                .createdIndex(3)
                .key("/k1")
                .modifiedIndex(5)
                .ttl(7)
                .value("v1")
                .build();
        Assert.assertEquals(expiration, node.expiration());
        Assert.assertEquals(3, node.createdIndex());
        Assert.assertEquals("/k1", node.key());
        Assert.assertEquals(5, node.modifiedIndex());
        Long ttl = node.ttl();
        Assert.assertNotNull(ttl);
        Assert.assertEquals(7L, ttl.longValue());
        Assert.assertEquals("v1", node.value());
    }

    @Test
    public void testBuildingNodeWithSubNodes() throws Exception {
        List<EtcdNode> children = new ArrayList<EtcdNode>();
        EtcdNode child1 = new EtcdNodeBuilder()
                .key("/root/child1")
                .build();
        children.add(child1);
        EtcdNode child2 = new EtcdNodeBuilder()
                .key("/root/child2")
                .build();
        children.add(child2);
        EtcdNode root = new EtcdNodeBuilder()
                .key("/root")
                .nodes(children)
                .build();
        Assert.assertEquals(2, root.nodes().size());
        EtcdNode rn1 = root.nodes().get(0);
        Assert.assertEquals("/root/child1", rn1.key());
        Assert.assertEquals("{\"nodes\":[{\"key\":\"/root/child1\"},{\"key\":\"/root/child2\"}],\"key\":\"/root\"}", root.toJson());
    }

}