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
package org.apache.sling.etcd.testing.tree;

import junit.framework.Assert;
import org.junit.Test;

public class NodeTest {

    @Test
    public void testFilterByTtl() throws Exception {
        Node n = new Key("a", "value", null, 10);
        Assert.assertNotNull(Node.filterByTtl(n));
        Node n1 = new Key("a", "value", 10, 10);
        Assert.assertNotNull(Node.filterByTtl(n1));
        Node n2 = new Key("a", "value", 0, 10);
        Assert.assertNull(Node.filterByTtl(n2));
    }

    @Test
    public void testNames() throws Exception {
        Assert.assertEquals(3, Node.names("/a/b/c").size());
        Assert.assertEquals(1, Node.names("/a").size());
        Assert.assertEquals(0, Node.names("/").size());
    }


    @Test
    public void testParent() throws Exception {
        Assert.assertEquals("/a/b", Node.parent("/a/b/c"));
        Assert.assertEquals("/a", Node.parent("/a/b"));
        Assert.assertEquals("/", Node.parent("/a"));
    }

    @Test
    public void testName() throws Exception {
        Assert.assertEquals("a", Node.name("/a/b/a"));
        Assert.assertEquals("c", Node.name("/a/b/c"));
        Assert.assertEquals("c", Node.name("/c"));
    }
}