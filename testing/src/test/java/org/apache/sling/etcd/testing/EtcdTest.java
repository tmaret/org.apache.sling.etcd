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
package org.apache.sling.etcd.testing;

import java.util.List;

import org.apache.sling.etcd.testing.condition.PrevExists;
import org.apache.sling.etcd.testing.condition.PrevIndex;
import org.apache.sling.etcd.testing.condition.PrevValue;
import org.apache.sling.etcd.testing.tree.Folder;
import org.apache.sling.etcd.testing.tree.Key;
import org.apache.sling.etcd.testing.tree.Node;
import junit.framework.Assert;
import org.junit.Test;

public class EtcdTest {

    // GET

    @Test
    public void testGetNonExistingNode() throws Exception {
        Etcd etcd = new Etcd(TestContent.build());
        Assert.assertNull(etcd.getNode("/a/i/dont/exist"));
        Assert.assertNull(etcd.getNode("/i/dont/exist"));
        Assert.assertNull(etcd.getNode("/i"));
    }

    @Test
    public void testGetExistingNode() throws Exception {
        Etcd etcd = new Etcd(TestContent.build());
        Assert.assertNotNull(etcd.getNode("/"));
        Assert.assertNotNull(etcd.getNode("/a"));
        Assert.assertNotNull(etcd.getNode("/a/k1"));
        Assert.assertNotNull(etcd.getNode("/b"));
        Assert.assertNotNull(etcd.getNode("/b/b1"));
        Assert.assertNotNull(etcd.getNode("/b/b1/k2"));
        Assert.assertNotNull(etcd.getNode("/b/b1/k3"));
        Assert.assertNotNull(etcd.getNode("/b/b1/b2/k4"));
        Assert.assertNotNull(etcd.getNode("/c"));
    }

    @Test
    public void testGetValues() throws Exception {
        Etcd etcd = new Etcd(TestContent.build());
        Node node = etcd.getNode("/a/k1");
        Assert.assertNotNull(node);
        Assert.assertFalse(node.isFolder());
        Key key = (Key) node;
        Assert.assertEquals("value-k1", key.value());
    }

    @Test
    public void testGetInOrder() throws Exception {
        Etcd etcd = new Etcd(TestContent.build());
        Node node = etcd.getNode("/b/b1");
        Assert.assertNotNull(node);
        Assert.assertTrue(node.isFolder());
        Folder b1 = (Folder) node;
        List<Node> children = b1.children(true);
        Assert.assertEquals(3, children.size());
        Assert.assertEquals("b2", children.get(0).name());
        Assert.assertEquals("k2", children.get(1).name());
        Assert.assertEquals("k3", children.get(2).name());
    }

    // PUT

    @Test
    public void testPutKey() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putKey("/some/key", "value", null, null);
        Node node = etcd.getNode("/some/key");
        Assert.assertNotNull(node);
        Assert.assertFalse(node.isFolder());
        Key key = (Key) node;
        Assert.assertEquals("value", key.value());
    }

    @Test
     public void testPutKeyModifyIndex() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putKey("/some/key", "value", null, null);
        Node node = etcd.getNode("/some/key");
        etcd.putKey("/some/key", "value", null, null);
        Node node2 = etcd.getNode("/some/key");
        Assert.assertTrue(node2.modifiedIndex() > node.modifiedIndex());
    }

    @Test
    public void testPutKeyOverwrite() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putKey("/some/key", "value", null, null);
        etcd.putKey("/some/key", "value2", null, null);
        Node node = etcd.getNode("/some/key");
        Assert.assertEquals("value2", ((Key) node).value());
    }

    @Test
    public void testPutKeyTtl() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putKey("/some/key", "value", 10, null);
        Node node = etcd.getNode("/some/key");
        Integer ttl = node.ttl();
        Assert.assertNotNull(ttl);
        Assert.assertTrue(ttl > 0);
        etcd.putKey("/some/key", "value", null, null);
        Assert.assertNull(etcd.getNode("/some/key").ttl());
    }

    @Test(expected = EtcdException.class)
    public void testPutKeyPrevExistsFails() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putKey("/some/key", "value", 10, new PrevExists(true));
    }

    @Test
    public void testPutKeyPrevExistsSucceed() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putKey("/some/key", "value", null, null);
        etcd.putKey("/some/key", "value2", null, new PrevExists(true));
        Node node = etcd.getNode("/some/key");
        Assert.assertEquals("value2", ((Key) node).value());
    }

    @Test(expected = EtcdException.class)
    public void testPutKeyPrevIndexFails() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putKey("/some/key", "value", 10, new PrevIndex(1000));
    }

    @Test(expected = EtcdException.class)
    public void testPutKeyPrevIndexFails2() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putKey("/some/key", "value", null, null);
        Node node = etcd.getNode("/some/key");
        etcd.putKey("/some/key", "value", 10, new PrevIndex(node.modifiedIndex() + 1));
    }

    @Test
    public void testPutKeyPrevIndexSucceed() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putKey("/some/key", "value", null, null);
        Node node = etcd.getNode("/some/key");
        etcd.putKey("/some/key", "value", 10, new PrevIndex(node.modifiedIndex()));
        Node node2 = etcd.getNode("/some/key");
        Assert.assertTrue(node2.modifiedIndex() > node.modifiedIndex());
    }

    @Test(expected = EtcdException.class)
    public void testPutKeyPrevValueFails() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putKey("/some/key", "value", 10, new PrevValue("some"));
    }

    @Test(expected = EtcdException.class)
    public void testPutKeyPrevValueFails2() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putKey("/some/key", "value", 10, null);
        etcd.putKey("/some/key", "value", 10, new PrevValue("some"));
    }

    @Test
    public void testPutKeyPrevValueSucceed() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putKey("/some/key", "value", 10, null);
        etcd.putKey("/some/key", "value", 10, new PrevValue("value"));
    }

    @Test(expected = EtcdException.class)
    public void testPutKeyParentIsAKey() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putKey("/some/key", "value", null, null);
        etcd.putKey("/some/key/subKey", "value", null, null);
    }

    @Test
    public void testPutKeyParentIsAFolder() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putFolder("/some/key", null, null);
        etcd.putKey("/some/key/subKey", "value", null, null);
    }

    @Test
    public void testPutFolder() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putFolder("/some/key", null, null);
        Node node = etcd.getNode("/some/key");
        Assert.assertNotNull(node);
        Assert.assertTrue(node.isFolder());
    }

    @Test(expected = EtcdException.class)
    public void testPutFolderParentIsAKey() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putKey("/some/key", "value", null, null);
        etcd.putFolder("/some/key/subKey", null, null);
    }

    @Test
    public void testPutFolderParentIsAFolder() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putFolder("/some/key", null, null);
        etcd.putFolder("/some/key/subKey", null, null);
    }

    @Test(expected = EtcdException.class)
    public void testPutKeyWasAFolder() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putFolder("/some/key", null, null);
        etcd.putKey("/some/key", "value", null, null);
    }

    @Test
    public void testPutFolderWasAKey() throws Exception {
        Etcd etcd = new Etcd();
        etcd.putKey("/some/key", "value", null, null);
        etcd.putFolder("/some/key", null, null);
        Node node = etcd.getNode("/some/key");
        Assert.assertNotNull(node);
        Assert.assertTrue(node.isFolder());
    }

    // POST

    @Test
    public void testCreateKey() throws Exception {
        Etcd etcd = new Etcd();
        Key key = etcd.createKey("/some/parent", "value", null, null);
        Assert.assertNotNull(key);
        Node retrieved = etcd.getNode(key.path());
        Assert.assertNotNull(retrieved);
    }

    // DELETE

    @Test(expected = EtcdException.class)
    public void testDeleteKeyNotFound() throws Exception {
        Etcd etcd = new Etcd();
        etcd.deleteKey("/does/not/exist", null);
    }

    @Test
    public void testDeleteKey() throws Exception {
        Etcd etcd = new Etcd(TestContent.build());
        Assert.assertNotNull(etcd.getNode("/a/k1"));
        etcd.deleteKey("/a/k1", null);
        Assert.assertNull(etcd.getNode("/a/k1"));
    }

    @Test(expected = EtcdException.class)
    public void testDeleteFolderNotFound() throws Exception {
        Etcd etcd = new Etcd();
        etcd.deleteFolder("/does/not/exist", false, null);
    }

    @Test(expected = EtcdException.class)
    public void testDeleteNonEmptyFolder() throws Exception {
        Etcd etcd = new Etcd(TestContent.build());
        etcd.deleteFolder("/a", false, null);
    }

    @Test
    public void testDeleteNonEmptyFolderRecursively() throws Exception {
        Etcd etcd = new Etcd(TestContent.build());
        Assert.assertNotNull(etcd.getNode("/a"));
        etcd.deleteFolder("/a", true, null);
        Assert.assertNull(etcd.getNode("/a"));
    }

}