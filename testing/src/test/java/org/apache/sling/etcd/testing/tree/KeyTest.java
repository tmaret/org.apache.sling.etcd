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
import org.apache.sling.commons.json.JSONObject;
import org.junit.Test;

public class KeyTest {

    @Test(expected = IllegalArgumentException.class)
    public void testWrongName() throws Exception {
        new Key("some/wrong/name", "value", null, 100);
    }

    @Test
    public void testValue() throws Exception {
        Key key = new Key("name", "value", null, 100);
        Assert.assertEquals("value", key.value());
        key.value("value2", 101);
        Assert.assertEquals("value2", key.value());
        key.value(null, 102);
        Assert.assertNull(key.value());
    }

    @Test
    public void testIsFolder() throws Exception {
        Key key = new Key("name", "value", null, 100);
        Assert.assertFalse(key.isFolder());
    }

    @Test
    public void testTtl() throws Exception {
        Key key = new Key("name", "value", null, 100);
        Assert.assertEquals(null, key.ttl());
        key.ttl(1, 101);
        Integer ttl = key.ttl();
        Assert.assertNotNull(ttl);
        Assert.assertTrue(ttl > 0);
        Thread.sleep(1000);
        Assert.assertEquals((Integer) 0, key.ttl());
        key.ttl(null, 102);
        Assert.assertNull(key.ttl());
    }

    @Test
    public void testParent() throws Exception {
        Key key = new Key("name", "value", null, 100);
        Assert.assertNull(key.parent());
        key.parent(key);
        Assert.assertNotNull(key.parent());
    }

    @Test
    public void testToJson1() throws Exception {
        Folder folder = new Folder("root", null, 10);
        Key key = new Key("a", "a1", null, 10);
        folder.putChild(key, 11);
        String json = key.toJson(false, true).toString();
        Assert.assertEquals("{\"key\":\"/a\",\"createdIndex\":10,\"modifiedIndex\":10,\"value\":\"a1\"}", json);
    }

    @Test
    public void testToJson2() throws Exception {
        Folder folder = new Folder("root", null, 10);
        Key key = new Key("a", "a1", 100, 10);
        folder.putChild(key, 11);
        JSONObject json = key.toJson(false, true);
        Assert.assertTrue(json.has("expiration"));
        Assert.assertTrue(json.has("ttl"));
    }
}