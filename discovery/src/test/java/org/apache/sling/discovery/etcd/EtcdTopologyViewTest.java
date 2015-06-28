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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.etcd.client.EtcdNode;
import junit.framework.Assert;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.InstanceFilter;
import org.apache.sling.discovery.TopologyView;
import org.junit.Test;

public class EtcdTopologyViewTest {

    @Test
    public void testIsCurrent() throws Exception {
        EtcdNode an1 = new EtcdNodeBuilder()
                .value(new AnnounceData("sling-id-1", "localhost:4502", "default-cluster", 1000L).json().toString())
                .modifiedIndex(10)
                .createdIndex(10)
                .ttl(10)
                .key("/announces/1")
                .build();
        TopologyView view = new EtcdTopologyView(new Announces(Collections.singletonList(an1), false),
                Collections.<String, Map<String, String>>emptyMap(),
                "sling-id-1", true);
        Assert.assertTrue(view.isCurrent());
    }

    @Test
    public void testIsNotCurrent() throws Exception {
        EtcdNode an1 = new EtcdNodeBuilder()
                .value(new AnnounceData("sling-id-1", "localhost:4502", "default-cluster", 1000L).json().toString())
                .modifiedIndex(10)
                .createdIndex(10)
                .ttl(10)
                .key("/announces/1")
                .build();
        EtcdTopologyView view = new EtcdTopologyView(new Announces(Collections.singletonList(an1), false),
                Collections.<String, Map<String, String>>emptyMap(),
                "sling-id-1", true);
        view.setNotCurrent();
        Assert.assertFalse(view.isCurrent());
    }

    @Test
    public void testConstructor() {
        Announce announce = new Announce(new AnnounceData("sling-id", "default-cluster", "localhost:4502", 32L), "/announces/1");
        Announces announces = new Announces(announce);
        Assert.assertEquals(1, announces.size());
        Assert.assertTrue(announces.containsBySlingId("sling-id"));
        Assert.assertEquals(1, announces.getSlingIds().size());
        Announce an1 = announces.getBySlingId("sling-id");
        Assert.assertNotNull(an1);
        Assert.assertEquals(an1, announce);
    }

    @Test(expected = EtcdDiscoveryRuntimeException.class)
    public void testBuildMissingLocal() throws Exception {
        String sid1 = "sling-id-1";
        EtcdNode an1 = new EtcdNodeBuilder()
                .value(new AnnounceData(sid1, "localhost:4502", "default-cluster", 1001L).json().toString())
                .modifiedIndex(10)
                .createdIndex(10)
                .ttl(10)
                .key("/announces/1")
                .build();
        Map<String, String> p1 = Collections.singletonMap("p1", "v1");

        List<EtcdNode> announcements = new ArrayList<EtcdNode>();
        announcements.add(an1);


        Map<String, Map<String, String>> props = new HashMap<String, Map<String, String>>();
        props.put(sid1, p1);

        new EtcdTopologyView(new Announces(announcements, false), props, "id-not-announced", true);
    }

    @Test
    public void testGetLocalInstance() throws Exception {
        String sid1 = "sling-id-1";
        EtcdNode an1 = new EtcdNodeBuilder()
                .value(new AnnounceData(sid1, "localhost:4502", "default-cluster", 1001L).json().toString())
                .modifiedIndex(10)
                .createdIndex(10)
                .ttl(10)
                .key("/announces/1")
                .build();
        Map<String, String> p1 = Collections.singletonMap("p1", "v1");

        String sid2 = "sling-id-2";
        EtcdNode an2 = new EtcdNodeBuilder()
                .value(new AnnounceData(sid2, "localhost:4503", "default-cluster", 1002L).json().toString())
                .modifiedIndex(11)
                .createdIndex(11)
                .ttl(10)
                .key("/announces/2")
                .build();
        Map<String, String> p2 = Collections.singletonMap("p2", "v2");

        Map<String, Map<String, String>> props = new HashMap<String, Map<String, String>>();
        props.put(sid1, p1);
        props.put(sid2, p2);

        List<EtcdNode> announcements = new ArrayList<EtcdNode>();
        announcements.add(an1);
        announcements.add(an2);

        TopologyView view = new EtcdTopologyView(new Announces(announcements, false),
                props,
                sid2,
                true);

        //

        InstanceDescription local = view.getLocalInstance();
        Assert.assertTrue(local.isLocal());
        Assert.assertFalse(local.isLeader());
        ClusterView cvLocal = local.getClusterView();
        Map<String, String> localPros = local.getProperties();
        Assert.assertEquals(1, localPros.size());
        Assert.assertEquals(sid2, local.getSlingId());

        //

        Set<InstanceDescription> instances = view.getInstances();
        Assert.assertEquals(2, instances.size());

        //

        Set<ClusterView> clusterViews = view.getClusterViews();
        Assert.assertEquals(1, clusterViews.size());
        ClusterView clusterView = clusterViews.iterator().next();
        Assert.assertEquals(clusterView, cvLocal);
        Assert.assertEquals("default-cluster", clusterView.getId());
        InstanceDescription leader = clusterView.getLeader();
        Assert.assertEquals(sid1, leader.getSlingId());
        Assert.assertEquals("v1", leader.getProperty("p1"));
        Assert.assertTrue(leader.isLeader());
        Assert.assertEquals(leader.getClusterView(), clusterView);
    }

    @Test
    public void testFindInstances() throws Exception {
        String sid1 = "sling-id-1";
        EtcdNode an1 = new EtcdNodeBuilder()
                .value(new AnnounceData(sid1, "localhost:4502", "default-cluster", 1001L).json().toString())
                .modifiedIndex(10)
                .createdIndex(10)
                .ttl(10)
                .key("/announces/1")
                .build();
        Map<String, String> p1 = Collections.singletonMap("p1", "v1");

        String sid2 = "sling-id-2";
        EtcdNode an2 = new EtcdNodeBuilder()
                .value(new AnnounceData(sid2, "localhost:4503", "default-cluster", 1002L).json().toString())
                .modifiedIndex(11)
                .createdIndex(11)
                .ttl(10)
                .key("/announces/2")
                .build();
        Map<String, String> p2 = Collections.singletonMap("p2", "v2");

        Map<String, Map<String, String>> props = new HashMap<String, Map<String, String>>();
        props.put(sid1, p1);
        props.put(sid2, p2);

        List<EtcdNode> announcements = new ArrayList<EtcdNode>();
        announcements.add(an1);
        announcements.add(an2);

        TopologyView view = new EtcdTopologyView(new Announces(announcements, false),
                props,
                sid2,
                true);

        //

        Set<InstanceDescription> match = view.findInstances(new InstanceFilter() {
            public boolean accept(InstanceDescription instanceDescription) {
                return instanceDescription.isLeader();
            }
        });
        Assert.assertEquals(1, match.size());
        Assert.assertEquals(sid1, match.iterator().next().getSlingId());

    }

    @Test
    public void testMultiClusterViews() throws Exception {

        String clusterOne = "cluster-1";
        String clusterTwo = "cluster-2";

        String sid1 = "sling-id-1";
        EtcdNode an1 = buildAnnounceNode(sid1, clusterOne, "/announces/3");
        Map<String, String> p1 = Collections.singletonMap("p1", "v1");

        String sid2 = "sling-id-2";
        EtcdNode an2 = buildAnnounceNode(sid2, clusterTwo, "/announces/2");
        Map<String, String> p2 = Collections.singletonMap("p2", "v2");

        String sid3 = "sling-id-3";
        EtcdNode an3 = buildAnnounceNode(sid3, clusterTwo, "/announces/1");
        Map<String, String> p3 = Collections.singletonMap("p3", "v3");

        // props

        Map<String, Map<String, String>> props = new HashMap<String, Map<String, String>>();
        props.put(sid1, p1);
        props.put(sid2, p2);
        props.put(sid3, p3);

        // announces

        List<EtcdNode> announcements = new ArrayList<EtcdNode>();
        announcements.add(an1);
        announcements.add(an2);
        announcements.add(an3);

        Collections.shuffle(announcements);

        // view

        TopologyView view = new EtcdTopologyView(new Announces(announcements, false),
                props,
                sid2,
                true);

        Assert.assertNotNull(view.getClusterViews());
        Assert.assertEquals(2, view.getClusterViews().size());
        Assert.assertNotNull(view.getInstances());
        Assert.assertEquals(3, view.getInstances().size());
        InstanceDescription local = view.getLocalInstance();
        Assert.assertNotNull(local);
        Assert.assertEquals(sid2, local.getSlingId());

        InstanceDescription i1 = findInstance(view, sid1);
        Assert.assertNotNull(i1);
        Assert.assertTrue(i1.isLeader());
        Assert.assertFalse(i1.isLocal());
        Assert.assertEquals(p1, i1.getProperties());

        InstanceDescription i2 = findInstance(view, sid2);
        Assert.assertNotNull(i2);
        Assert.assertFalse(i2.isLeader());
        Assert.assertTrue(i2.isLocal());
        Assert.assertEquals(p2, i2.getProperties());

        InstanceDescription i3 = findInstance(view, sid3);
        Assert.assertNotNull(i3);
        Assert.assertTrue(i3.isLeader());
        Assert.assertFalse(i3.isLocal());
        Assert.assertEquals(p3, i3.getProperties());

        Assert.assertNotSame(i1.getClusterView(), i2.getClusterView());
        Assert.assertEquals(i2.getClusterView(), i3.getClusterView());

    }

    private EtcdNode buildAnnounceNode(String slingId, String clusterId, String announceKey) {
        return new EtcdNodeBuilder()
                .value(new AnnounceData(slingId, "localhost:4502", clusterId, 1001L).json().toString())
                .modifiedIndex(10)
                .createdIndex(10)
                .ttl(10)
                .key(announceKey)
                .build();
    }

    private InstanceDescription findInstance(TopologyView view, final String slingId) {
        Set<InstanceDescription> match = view.findInstances(new InstanceFilter() {
            public boolean accept(InstanceDescription instanceDescription) {
                return instanceDescription.getSlingId().equals(slingId);
            }
        });
        return match != null ? match.iterator().next() : null;
    }
}