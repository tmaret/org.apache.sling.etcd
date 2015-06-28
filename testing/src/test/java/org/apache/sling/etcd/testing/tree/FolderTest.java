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

public class FolderTest {

    @Test(expected = IllegalArgumentException.class)
    public void testWrongName() throws Exception {
        new Folder("some/wrong/name", null, 100);
    }

    @Test
    public void testIsFolder() throws Exception {
        Folder folder = new Folder("name", null, 100);
        Assert.assertTrue(folder.isFolder());
    }

    @Test
    public void testEmptyChildren() throws Exception {
        Folder root = new Folder("root", null, 100);
        Assert.assertEquals(0, root.children(false).size());
    }

    @Test
    public void testChildren() throws Exception {
        Folder root = new Folder("root", null, 100);
        Folder f1 = new Folder("f1", null, 101);
        root.putChild(f1, 102);
        Folder f2 = new Folder("f2", null, 103);
        root.putChild(f2, 103);
        Assert.assertEquals(2, root.children(false).size());
    }

    @Test
    public void testNonExistingChild() throws Exception {
        Folder root = new Folder("root", null, 100);
        Assert.assertNull(root.child("nonExisting"));
    }

    @Test
    public void testChild() throws Exception {
        Folder root = new Folder("root", null, 100);
        Folder f1 = new Folder("f1", null, 101);
        root.putChild(f1, 102);
        Node child = root.child("f1");
        Assert.assertNotNull(child);
        Assert.assertEquals("f1", child.name());
    }

    @Test
    public void testTtl() throws Exception {
        Folder folder = new Folder("name", null, 100);
        Assert.assertEquals(null, folder.ttl());
        folder.ttl(10, 101);
        Integer ttl = folder.ttl();
        Assert.assertNotNull(ttl);
        Assert.assertTrue(ttl > 0);
        folder.ttl(null, 102);
        Assert.assertNull(folder.ttl());
    }

    @Test
    public void testToJson1() throws Exception {
        Folder root = new Folder("root", null, 10);
        Folder folder1 = new Folder("a", null, 11);
        root.putChild(folder1, 11);
        Folder folder2 = new Folder("b", null, 12);
        folder1.putChild(folder2, 13);
        String json1 = root.toJson(false, true).toString();
        Assert.assertEquals("{\"key\":\"/\",\"createdIndex\":11,\"modifiedIndex\":11,\"dir\":true,\"nodes\":[{\"key\":\"/a\",\"createdIndex\":13,\"modifiedIndex\":13,\"dir\":true}]}", json1);
        String json2 = root.toJson(true, true).toString();
        Assert.assertEquals("{\"key\":\"/\",\"createdIndex\":11,\"modifiedIndex\":11,\"dir\":true,\"nodes\":[{\"key\":\"/a\",\"createdIndex\":13,\"modifiedIndex\":13,\"dir\":true,\"nodes\":[{\"key\":\"/a/b\",\"createdIndex\":12,\"modifiedIndex\":12,\"dir\":true,\"nodes\":[]}]}]}", json2);
    }
}