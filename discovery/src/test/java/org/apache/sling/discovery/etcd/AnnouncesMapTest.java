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

import junit.framework.Assert;
import org.junit.Test;

public class AnnouncesMapTest {

    @Test
    public void testGetAll() throws Exception {
        AnnounceData annData = new AnnounceData("sling-id", "server-info", "default-cluster", 10);
        Announce local = new Announce(annData, "/announces/1");
        AnnouncesMap map = new AnnouncesMap(local);
        Assert.assertEquals(1, map.getAll().size());
        map.setRemote(new Announces(new Announce(new AnnounceData("sling-id2", "server-info2", "default-cluster", 12), "/announces/2")));
        Assert.assertEquals(2, map.getAll().size());
    }

    @Test
    public void testGetAllWithDuplicates() throws Exception {
        AnnounceData annData = new AnnounceData("sling-id", "server-info", "default-cluster", 10);
        Announce local = new Announce(annData, "/announces/1");
        AnnouncesMap map = new AnnouncesMap(local);
        Assert.assertEquals(1, map.getAll().size());
        map.setRemote(new Announces(new Announce(new AnnounceData("sling-id", "server-info", "default-cluster", 10), "/announces/1")));
        Assert.assertEquals(1, map.getAll().size());
    }

    @Test
    public void testGetLocal() throws Exception {
        AnnounceData annData = new AnnounceData("sling-id", "server-info", "default-cluster", 10);
        Announce local = new Announce(annData, "/announces/1");
        AnnouncesMap map = new AnnouncesMap(local);
        Assert.assertEquals("/announces/1", map.getLocal().getAnnounceKey());
    }

    @Test
    public void testSetLocal() throws Exception {
        AnnounceData annData = new AnnounceData("sling-id", "server-info", "default-cluster", 10);
        Announce local = new Announce(annData, "/announces/1");
        AnnouncesMap map = new AnnouncesMap(local);
        Announce local2 = new Announce(annData, "/announces/2");
        map.setLocal(local2);
        Assert.assertEquals("/announces/2", map.getLocal().getAnnounceKey());
    }

    @Test
    public void testGetRemote() throws Exception {
        AnnounceData annData = new AnnounceData("sling-id", "server-info", "default-cluster", 10);
        Announce local = new Announce(annData, "/announces/1");
        AnnouncesMap map = new AnnouncesMap(local);
        map.setRemote(new Announces(new Announce(new AnnounceData("sling-id2", "server-info2", "default-cluster", 12), "/announces/2")));
        Assert.assertEquals(1, map.getRemote().size());
    }

}