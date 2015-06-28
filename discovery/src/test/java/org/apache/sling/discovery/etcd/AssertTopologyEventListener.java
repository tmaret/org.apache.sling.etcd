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

import java.util.LinkedList;
import java.util.List;

import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.TopologyView;

public class AssertTopologyEventListener implements TopologyEventListener {

    private final LinkedList<TopologyEvent> events = new LinkedList<TopologyEvent>();

    @Override
    public void handleTopologyEvent(TopologyEvent event) {

        if (event == null) {
            throw new Error("Received null event");
        }

        // assert new view defined except for changing events
        if (event.getType() == TopologyEvent.Type.TOPOLOGY_CHANGING) {
            if (event.getNewView() != null) {
                throw new Error("newView must not be defined for topology changing events");
            }
        } else {
            if (event.getNewView() == null) {
                throw new Error("newView must be defined for thise event: " + event);
            }
        }

        // assert old view defined except for topology init
        if (event.getType() == TopologyEvent.Type.TOPOLOGY_INIT) {
            if (event.getOldView() != null) {
                throw new Error("oldView must not be defined for topology init event: " + event);
            }
        } else {
            if (event.getOldView() == null) {
                throw new Error("oldView must be defined for this event type: " + event);
            }
        }

        // assert old view is not current
        TopologyView oldView = event.getOldView();
        if (oldView != null && oldView.isCurrent()) {
            throw new Error("received event with current old view: " + event);
        }

        // Assert first and only first item is a TOPOLOGY_INIT
        if (TopologyEvent.Type.TOPOLOGY_INIT == event.getType() && events.size() != 0) {
            throw new Error("received more than one topology init: " + event);
        }
        if (events.size() == 0 && TopologyEvent.Type.TOPOLOGY_INIT != event.getType()) {
            throw new Error("received event: " + event + "before TOPOLOGY_INIT event");
        }

        // Assert the TOPOLOGY_INIT event contains a current view
        if (TopologyEvent.Type.TOPOLOGY_INIT == event.getType() && ! event.getNewView().isCurrent()) {
            throw new Error("TOPOLOGY_INIT must contain a current view");
        }

        // Assert isolate mode yields non current views
        if (event.getNewView() != null) {
            for (ClusterView newClusterView : event.getNewView().getClusterViews()) {
                if ("isolated".equals(newClusterView.getId())) {
                    if (event.getNewView().isCurrent()) {
                        throw new Error("isolated view must not be current: " + event);
                    }
                }
            }
        }

        // Assert the CHANGING event is sent before a CHANGED event
        if(event.getType() == TopologyEvent.Type.TOPOLOGY_CHANGED) {
            if (events.size() == 0 || events.getLast().getType() != TopologyEvent.Type.TOPOLOGY_CHANGING) {
                throw new Error("TOPOLOGY_CHANGED event sent without prior TOPOLOGY_CHANGING event (ten last events received): "
                        + events.subList(Math.max(0, events.size() - 10), events.size()).toString());
            }
        }

        events.add(event);
    }

    public List<TopologyEvent> getHistory() {
        return events;
    }
}