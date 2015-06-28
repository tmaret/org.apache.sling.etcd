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
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import junit.framework.Assert;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.TopologyView;
import org.junit.Test;

public class ViewManagerTest {

    @Test
    public void testGetView() throws Exception {
        EtcdTopologyView view1 = buildView("cluster-1", true);
        ViewManager manager = new ViewManager();
        manager.updateView(view1);
        EtcdTopologyView view2 = manager.getView();
        // until equals/hashcode is implemented
        Assert.assertEquals("cluster-1", view2.getClusterViews().iterator().next().getId());
    }

    @Test
    public void testSetView() throws Exception {
        EtcdTopologyView view1 = buildView("cluster-1", true);
        ViewManager manager = new ViewManager();
        manager.updateView(view1);
        EtcdTopologyView view2 = buildView("cluster-2", true);
        manager.updateView(view2);
        Assert.assertEquals("cluster-2", manager.getView().getClusterViews().iterator().next().getId());
    }

    @Test
    public void testSetViewDisableOldView() throws Exception {
        EtcdTopologyView view1 = buildView("cluster-1", true);
        ViewManager manager = new ViewManager();
        manager.updateView(view1);
        EtcdTopologyView view2 = manager.getView();
        Assert.assertTrue(view2.isCurrent());
        EtcdTopologyView view3 = buildView("cluster-3", true);
        manager.updateView(view3);
        EtcdTopologyView view4 = manager.getView();
        Assert.assertFalse(view2.isCurrent());
        Assert.assertTrue(view4.isCurrent());
        Assert.assertEquals("cluster-3", manager.getView().getClusterViews().iterator().next().getId());
    }

    @Test
    public void testSendNoListener() {
        ViewManager viewManager = new ViewManager();
        EtcdTopologyView initView = buildView("cluster-id", true);
        viewManager.updateView(initView);
        TopologyView view = buildView("another-cluster-id", true);
        viewManager.send(new TopologyEvent(TopologyEvent.Type.PROPERTIES_CHANGED, view, view));
    }

    @Test
    public void testFailingListener() throws Exception {
        ViewManager viewManager = new ViewManager();
        TeL tel1 = new TeL();
        viewManager.bind(tel1);
        viewManager.bind(new FailingTeL());
        EtcdTopologyView initView = buildView("cluster-id", true);
        viewManager.updateView(initView);
        TopologyView view = buildView("another-cluster-id", true);
        viewManager.send(new TopologyEvent(TopologyEvent.Type.PROPERTIES_CHANGED, view, view));
        Assert.assertEquals(2, tel1.events.size());
    }



    @Test
    public void testSendListeners() throws Exception {
        ViewManager viewManager = new ViewManager();
        // listener one is bind before the event service is initialized
        TeL tel1 = new TeL();
        viewManager.bind(tel1);
        EtcdTopologyView initView = buildView("cluster-id", true);
        viewManager.updateView(initView);
        // listener two is bind after the event service is initialized
        TeL tel2 = new TeL();
        viewManager.bind(tel2);
        EtcdTopologyView view = buildView("another-cluster-id", true);
        viewManager.updateView(view);
        Assert.assertEquals(3, tel1.events.size());
        Assert.assertEquals(1, tel2.events.size());

        // Both listeners must have received the INIT event first, then the PROP_CHANGE event.
        Assert.assertEquals(TopologyEvent.Type.TOPOLOGY_INIT, tel1.events.get(0).getType());
        Assert.assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGING, tel1.events.get(1).getType());
        Assert.assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGED, tel1.events.get(2).getType());
        Assert.assertEquals(TopologyEvent.Type.TOPOLOGY_INIT, tel2.events.get(0).getType());
    }

    @Test
    public void testNonInitNonCurrentView() throws Exception {
        ViewManager viewManager = new ViewManager();
        TeL tel1 = new TeL();
        viewManager.bind(tel1);
        EtcdTopologyView initView = buildView("cluster-id", false);
        viewManager.updateView(initView);
        Assert.assertEquals(0, tel1.events.size());
        EtcdTopologyView view = buildView("cluster-id-changed", false);
        viewManager.updateView(view);
        Assert.assertEquals(0, tel1.events.size());
    }

    @Test
    public void testNonInitCurrentView() throws Exception {
        ViewManager viewManager = new ViewManager();
        TeL tel1 = new TeL();
        viewManager.bind(tel1);
        EtcdTopologyView initView = buildView("cluster-id", true);
        viewManager.updateView(initView);
        Assert.assertEquals(1, tel1.events.size());
        Assert.assertEquals(TopologyEvent.Type.TOPOLOGY_INIT, tel1.events.get(0).getType());
    }

    @Test
    public void testNonInitCurrentViewWithoutChange() throws Exception {
        ViewManager viewManager = new ViewManager();
        EtcdTopologyView initView = buildView("cluster-id", true);
        viewManager.updateView(initView);
        TeL tel1 = new TeL();
        viewManager.bind(tel1);
        viewManager.updateView(initView);
        Assert.assertEquals(1, tel1.events.size());
        Assert.assertEquals(TopologyEvent.Type.TOPOLOGY_INIT, tel1.events.get(0).getType());
    }

    @Test
    public void testPropertyChange() throws Exception {
        ViewManager viewManager = new ViewManager();
        TeL tel1 = new TeL();
        viewManager.bind(tel1);
        viewManager.updateView(buildView("cluster-id", true));
        viewManager.updateView(buildView("cluster-id", Collections.singletonMap("sling-id", Collections.singletonMap("p1", "v1")), true));
        Assert.assertEquals(3, tel1.events.size());
        Assert.assertEquals(TopologyEvent.Type.TOPOLOGY_INIT, tel1.events.get(0).getType());
        Assert.assertEquals(TopologyEvent.Type.TOPOLOGY_CHANGING, tel1.events.get(1).getType());
        Assert.assertEquals(TopologyEvent.Type.PROPERTIES_CHANGED, tel1.events.get(2).getType());
    }

    @Test
    public void testUnbind() throws Exception {
        ViewManager viewManager = new ViewManager();
        // listener one is bind before the event service is initialized
        TeL tel1 = new TeL();
        viewManager.bind(tel1);
        viewManager.unbind(tel1);
        viewManager.updateView(buildView("cluster-id", true));
        Assert.assertEquals(0, tel1.events.size());
    }

    public EtcdTopologyView buildView(@Nonnull String clusterId, @Nonnull Map<String, Map<String, String>> properties, boolean current) {
        AnnounceData data = new AnnounceData("sling-id", "server-info", clusterId, 21);
        Announce announce = new Announce(data, "/announces/1");
        return new EtcdTopologyView(new Announces(announce), properties, "sling-id", current);
    }

    public EtcdTopologyView buildView(@Nonnull String clusterId, boolean current) {
        return buildView(clusterId, Collections.<String, Map<String, String>>emptyMap(), current);
    }

    private class TeL implements TopologyEventListener {
        final List<TopologyEvent> events = new ArrayList<TopologyEvent>();
        public void handleTopologyEvent(TopologyEvent event) {
            events.add(event);
        }
    }

    private class FailingTeL implements TopologyEventListener {

        @Override
        public void handleTopologyEvent(TopologyEvent topologyEvent) {
            throw new RuntimeException();
        }
    }

}