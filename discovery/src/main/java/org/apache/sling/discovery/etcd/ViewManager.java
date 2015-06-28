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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.TopologyView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code ViewManager} manages the current view and sending of events upon view changes.
 */
public class ViewManager {

    private static final Logger LOG = LoggerFactory.getLogger(ViewManager.class);

    /**
     * The undefined view is used to comply with the {@code DiscoveryService#getTopology} API which requires never
     * returning a {@code null} view. The minimum defined view (isolated) requires the Sling identifier which is obtained
     * in the EtcdDiscoveryService#activate method only. The undefined view should never be returned as components
     * should only be able to access the EtcdDiscoveryService once the EtcdDiscoveryService#activate method returns.
     * The undefined view is not current.
     */
    private static final EtcdTopologyView UNDEFINED_VIEW = new EtcdTopologyView(
            new Announces(new Announce(new AnnounceData("undefined", "undefined", "undefined", -1), "/undefined/0")),
            Collections.<String, Map<String, String>>emptyMap(), "undefined", false);

    /**
     * Holds the initialized topology listeners.
     */
    private final Set<TopologyEventListener> listeners = new HashSet<TopologyEventListener>();

    /**
     * Holds the non initialized listeners.
     */
    private final Set<TopologyEventListener> nonInitializedListeners = new HashSet<TopologyEventListener>();

    /**
     * The lock to keep the two listeners sets in sync.
     */
    private final Object lock = new Object();

    /**
     * The current view.
     */
    private volatile EtcdTopologyView currentView = UNDEFINED_VIEW;

    @Nonnull
    public EtcdTopologyView getView() {
        return get();
    }

    /**
     * Update the view and send the required events for each listeners.
     * @param newView the new view to be set.
     */
    /*
     * This method is invoked by two threads, LocalUpdater and OSGI thread running the
     * EtcdDiscovery#activate method. Those two threads never invoke the #updateView method
     * concurrently. Indeed, the EtcdDiscovery#activate method method always invoke #updateView
     * before starting the LocalUpdater thread.
     */
    public void updateView(@Nonnull EtcdTopologyView newView) {

        EtcdTopologyView oldView = get();
        boolean announcesChanged = ! newView.getAnnounces()
                .equals(oldView.getAnnounces());
        boolean propertiesChanged = ! newView.getProperties()
                .equals(oldView.getProperties());
        boolean clusterChanged = ! newView.getClusterIds()
                .equals(oldView.getClusterIds());
        boolean currentChanged = oldView.isCurrent()
                != newView.isCurrent();

        synchronized (lock) {
            if (currentChanged || announcesChanged || propertiesChanged || clusterChanged) {
                set(newView);
                boolean oldCurrent = oldView.setNotCurrent();
                if (newView.isCurrent()) {
                    if (oldCurrent) {
                        send(changingEvent(oldView), listeners);
                    }
                    if (announcesChanged || clusterChanged) {
                        send(changedEvent(oldView, newView), listeners);
                    } else {
                        send(propertyChangedEvent(oldView, newView), listeners);
                    }
                } else if (oldCurrent) {
                    send(changingEvent(oldView), listeners);
                }
            }

            // initialize pending listeners if the view is current
            EtcdTopologyView currentView = get();
            if (currentView.isCurrent()) {
                synchronized (lock) {
                    if (!nonInitializedListeners.isEmpty()) {
                        send(initEvent(currentView), nonInitializedListeners);
                        listeners.addAll(nonInitializedListeners);
                        nonInitializedListeners.clear();
                    }
                }
            }
        }
    }

    public void bind(TopologyEventListener listener) {
        LOG.debug("bind TopologyListener: {}", listener);
        synchronized (lock) {
            nonInitializedListeners.add(listener);
        }
    }

    public void unbind(TopologyEventListener listener) {
        boolean removed;
        synchronized (lock) {
             removed = listeners.remove(listener) || nonInitializedListeners.remove(listener);
        }
        if (removed) {
            LOG.debug("unbind TopologyListener: {}", listener);
        } else {
            LOG.debug("No TopologyListener to unbind: {}", listener);
        }
    }

    //

    private void set(@Nonnull EtcdTopologyView view) {
        this.currentView = view;
    }

    @Nonnull
    private EtcdTopologyView get() {
        return this.currentView;
    }

    protected void send(@Nonnull TopologyEvent event) {
        send(event, listeners);
    }

    private void send(@Nonnull TopologyEvent event, @Nonnull Collection<TopologyEventListener> listeners) {
        if (! LOG.isTraceEnabled()) {
            LOG.debug("Send event of type: {}", event.getType());
        }
        LOG.trace("Send event: {}", event);
        for (TopologyEventListener listener : listeners) {
            send(event, listener);
        }
    }

    private void send(@Nonnull TopologyEvent event, @Nonnull TopologyEventListener listener) {
        try {
            listener.handleTopologyEvent(event);
        } catch (Exception e) {
            LOG.info("Exception when dispatching the topology event", e);
        }
    }

    @Nonnull
    private TopologyEvent initEvent(@Nonnull TopologyView newView) {
        return new TopologyEvent(TopologyEvent.Type.TOPOLOGY_INIT, null, newView);
    }

    @Nonnull
    private TopologyEvent changingEvent(@Nonnull TopologyView oldView) {
        return new TopologyEvent(TopologyEvent.Type.TOPOLOGY_CHANGING, oldView, null);
    }

    @Nonnull
    private TopologyEvent changedEvent(@Nonnull TopologyView oldView, @Nonnull TopologyView newView) {
        return new TopologyEvent(TopologyEvent.Type.TOPOLOGY_CHANGED, oldView, newView);
    }

    @Nonnull
    private TopologyEvent propertyChangedEvent(@Nonnull TopologyView oldView, @Nonnull TopologyView newView) {
        return new TopologyEvent(TopologyEvent.Type.PROPERTIES_CHANGED, oldView, newView);
    }

}
