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
package org.apache.sling.discovery.etcd.run;


import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.sling.discovery.etcd.Announce;
import org.apache.sling.discovery.etcd.AnnounceData;
import org.apache.sling.discovery.etcd.Announces;
import org.apache.sling.discovery.etcd.AnnouncesMap;
import org.apache.sling.discovery.etcd.EtcdTopologyView;
import org.apache.sling.discovery.etcd.PropertiesMap;
import org.apache.sling.discovery.etcd.PropertiesService;
import org.apache.sling.discovery.etcd.ViewManager;
import org.apache.sling.discovery.etcd.backoff.BackOff;
import org.apache.sling.discovery.etcd.fsm.BaseRunner;
import org.apache.sling.discovery.etcd.fsm.Context;
import org.apache.sling.discovery.etcd.fsm.States;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code LocalUpdater} loads local properties and update the topology view if it changed.
 * The {@code LocalUpdater} runs periodically (defined by view update period) unless stopped.
 */
public class LocalUpdater extends BaseRunner {
    /**
     * Default logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(LocalUpdater.class);

    /**
     * The discovery protocol context
     */
    private final Context context;

    /**
     * The local instance Sling identifier.
     */
    private final String slingId;

    /**
     * Manage and provides access to the current view.
     */
    private final ViewManager viewManager;

    /**
     * The service allowing to load the properties for the local instance.
     */
    private final PropertiesService propertiesService;

    /**
     * The local and remote announces
     */
    private final AnnouncesMap announcesMap;

    /**
     * The local and remote properties
     */
    private final PropertiesMap propertiesMap;

    /**
     * The local instance hostname.
     */
    private final String serverInfo;

    /**
     * The view update period.
     */
    private final BackOff viewUpdatePeriod;


    public LocalUpdater(@Nonnull Context context, @Nonnull PropertiesService propertiesService, @Nonnull ViewManager viewManager,
                        @Nonnull BackOff viewUpdatePeriod, @Nonnull AnnouncesMap announcesMap, @Nonnull PropertiesMap propertiesMap,
                        @Nonnull String slingId, @Nonnull String serverInfo) {
        super();
        this.slingId = slingId;
        this.context = context;
        this.viewManager = viewManager;
        this.announcesMap = announcesMap;
        this.viewUpdatePeriod = viewUpdatePeriod;
        this.propertiesMap = propertiesMap;
        this.propertiesService = propertiesService;
        this.serverInfo = serverInfo;
    }

    public void run() {
        for ( ; running ; ) {
            long waitTime = viewUpdatePeriod.value();
            try {
                internalRun();
            } catch (Exception e) {
                LOG.error("Error while updating view", e);
            } finally {
                sleep(waitTime);
            }
        }
        LOG.debug("Stopped local updater thread.");
    }

    private void internalRun() {

        // update local properties

        Map<String, String> newLocal = propertiesService.load();
        propertiesMap.setLocal(newLocal);

        // build new view

        final Announces anns;
        final Map<String, Map<String, String>> props;
        final EtcdTopologyView newView;
        if (context.is(States.RUNNING)) {
            anns = announcesMap.getAll();
            props = propertiesMap.getAll();
            newView = new EtcdTopologyView(anns, props, slingId, true);
        } else {
            AnnounceData data = new AnnounceData(slingId, serverInfo, "isolated", 0);
            anns = new Announces(new Announce(data, "/isolated/0"));
            props = Collections.singletonMap(slingId, newLocal);
            newView = new EtcdTopologyView(anns, props, slingId, false);
        }

        viewManager.updateView(newView);
        long oldPeriod = viewUpdatePeriod.increment();
        LOG.debug("Updated view (viewUpdatePeriod: {} to {})", new Object[]{oldPeriod, viewUpdatePeriod.value()});
    }

    protected void sleep(long ms) {
        LOG.trace("wait {} ms before proceeding.", ms);
        super.sleep(ms);
    }
}
