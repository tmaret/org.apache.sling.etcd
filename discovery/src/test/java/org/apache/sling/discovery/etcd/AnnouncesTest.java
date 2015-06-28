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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.etcd.client.EtcdNode;
import junit.framework.Assert;
import org.junit.Test;

public class AnnouncesTest {

    @Test
    public void testGetEmptyAnnouncements() throws Exception {
        List<EtcdNode> nodes = new ArrayList<EtcdNode>();
        Announces announces = new Announces(nodes, false);
        Assert.assertNotNull(announces.getAnnounces());
    }

    @Test
    public void testGet() throws Exception {
        List<EtcdNode> nodes = buildAnnouncementNodes();
        Announces announces = new Announces(nodes, false);
        Assert.assertEquals(3, announces.size());
        Announce n1 = announces.getAnnounces().iterator().next();
        Assert.assertEquals("/announces/1", n1.getAnnounceKey());
        Assert.assertEquals("sling-id-2", n1.getData().slingId());
        Assert.assertEquals(1, n1.getData().propertiesModifiedIndex());
    }

    @Test
    public void testFilterBySlingId() throws Exception {
        List<EtcdNode> nodes = buildAnnouncementNodes();
        Announces announces = new Announces(nodes, false);
        Announces filtered = announces.filterBySlingId("sling-id-2");
        Assert.assertEquals(2, filtered.size());
        Assert.assertFalse(filtered.containsBySlingId("sling-id-2"));
    }

    @Test
    public void testEquals() throws Exception {
        Announces announces1 = new Announces(buildAnnouncementNodes(), false);
        Announces announces2 = new Announces(buildAnnouncementNodes(), false);
        Assert.assertTrue(announces1.equals(announces2) & announces2.equals(announces1));
        Assert.assertTrue(announces1.hashCode() == announces2.hashCode());
        LinkedList<EtcdNode> nodes3 = buildAnnouncementNodes();
        nodes3.addFirst(buildAnnouncementNode("/announces/0", "sling-id-1", 0));
        Announces announces3 = new Announces(nodes3, false);
        Assert.assertFalse(announces1.equals(announces3) | announces3.equals(announces1));
        List<EtcdNode> nodes4 = buildAnnouncementNodes();
        nodes4.add(buildAnnouncementNode("/announces/10", "sling-id-4", 0));
        Announces announces4 = new Announces(nodes4, false);
        Assert.assertFalse(announces1.equals(announces4) | announces4.equals(announces1));
    }

    @Test
    public void testAnnouncesOrder() throws Exception {
        int nbAnnounces = 1000;
        List<Announce> announces = new ArrayList<Announce>();
        for (int i = 0 ; i < nbAnnounces ; i++) {
            announces.add(new Announce(new AnnounceData("id-" + i, "localhost:80", "cluster-id", 0), "/announces/" + i));
        }
        List<Announce> shuffle = new ArrayList<Announce>(announces);
        Collections.shuffle(shuffle);
        Announces anns = new Announces(shuffle);
        List<Announce> back = anns.getAnnounces();
        for (int i = 0 ; i < nbAnnounces ; i++) {
            Assert.assertEquals(announces.get(i).getAnnounceKey(), back.get(i).getAnnounceKey());
        }
    }

    /**
     * @return a list of announcements nodes containing
     *         duplicates (same slingId) and unordered nodes (key)
     */
    private LinkedList<EtcdNode> buildAnnouncementNodes() {
        LinkedList<EtcdNode> ann = new LinkedList<EtcdNode>();
        ann.add(buildAnnouncementNode("/announces/1", "sling-id-2", 1));
        ann.add(buildAnnouncementNode("/announces/3", "sling-id-1", 3));
        ann.add(buildAnnouncementNode("/announces/2", "sling-id-3", 2));
        ann.add(buildAnnouncementNode("/announces/4", "sling-id-2", 4));
        return ann;
    }

    EtcdNode buildAnnouncementNode(String key, String slingId, int index) {
        Calendar c = Calendar.getInstance();
        String value = new AnnounceData(slingId, "localhost:4502", "default-cluster", index).json().toString();
        return new EtcdNodeBuilder()
                .key(key)
                .value(value)
                .createdIndex(index)
                .modifiedIndex(index)
                .expiration(c)
                .ttl(10)
                .build();
    }
}