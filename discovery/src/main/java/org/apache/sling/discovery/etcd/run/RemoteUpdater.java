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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.sling.discovery.etcd.Announce;
import org.apache.sling.discovery.etcd.Announces;
import org.apache.sling.discovery.etcd.AnnouncesMap;
import org.apache.sling.discovery.etcd.EtcdService;
import org.apache.sling.discovery.etcd.EtcdServiceException;
import org.apache.sling.discovery.etcd.PropertiesMap;
import org.apache.sling.discovery.etcd.backoff.BackOff;
import org.apache.sling.discovery.etcd.fsm.BaseRunner;
import org.apache.sling.discovery.etcd.fsm.Context;
import org.apache.sling.discovery.etcd.fsm.Event;
import org.apache.sling.discovery.etcd.fsm.States;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The RemoteUpdater fetches the announces for all remote instances from etcd.
 * It computes the announce changes and load changed properties from etcd as required.
 * The {@code RemoteUpdater} runs periodically (defined by topology update period) unless stopped.
 */
public class RemoteUpdater extends BaseRunner {

    /**
     * Default logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RemoteUpdater.class);

    /**
     * The discovery protocol context
     */
    private final Context context;

    /**
     * The local instance Sling identifier.
     */
    private final String slingId;

    /**
     * Service to communicate with the etcd machines.
     */
    private final EtcdService etcdService;

    /**
     * The local and remote announces
     */
    private final AnnouncesMap announcesMap;

    /**
     * The local and remote properties
     */
    private final PropertiesMap propertiesMap;

    /**
     * The topology update period.
     */
    private final BackOff topologyUpdatePeriod;


    public RemoteUpdater(@Nonnull Context context, @Nonnull EtcdService etcdService,
                         @Nonnull BackOff topologyUpdatePeriod, @Nonnull AnnouncesMap announcesMap,
                         @Nonnull PropertiesMap propertiesMap, @Nonnull String slingId) {
        super();
        this.slingId = slingId;
        this.context = context;
        this.etcdService = etcdService;
        this.announcesMap = announcesMap;
        this.topologyUpdatePeriod = topologyUpdatePeriod;
        this.propertiesMap = propertiesMap;
    }

    public void run() {
        for ( ; running ; ) {
            long waitTime = topologyUpdatePeriod.value();
            try {
                internalRun();
            } catch (Exception e) {
                LOG.error("Error while fetching state for remote instances", e);
            } finally {
                sleep(waitTime);
            }
        }
        LOG.debug("Stopped remote updater thread.");
    }

    private void internalRun() {
        if (context.is(States.RUNNING)) {

            try {

                // fetch all announces from etcd, only keep the announces for
                // the remote instances (filter out local instance announce)
                // We remove announces with (ttl <= 0). Those ttl may be emit by etcd
                // in case the cluster quorum is lost (majority of the etcd peers are down).
                Announces newAnns = new Announces(etcdService.getAnnounces(), true).filterBySlingId(slingId);

                // check for change in the remote announces
                Announces oldAnns = announcesMap.getRemote();
                if (! newAnns.equals(oldAnns)) {

                    // compute which remote instance properties must be loaded

                    Set<String> loadIds = new HashSet<String>();
                    Set<String> addedIds = new HashSet<String>(newAnns.getSlingIds());
                    addedIds.removeAll(oldAnns.getSlingIds());
                    loadIds.addAll(addedIds);
                    LOG.debug("Instances with ids: {} have been added", addedIds);

                    Set<String> retainedIds = new HashSet<String>(newAnns.getSlingIds());
                    retainedIds.retainAll(oldAnns.getSlingIds());
                    LOG.debug("Instances with ids: {} have been retained", retainedIds);
                    for (String retainedId : retainedIds) {
                        // check if properties have changed
                        Announce newAnn = newAnns.getBySlingId(retainedId);
                        Announce oldAnn = oldAnns.getBySlingId(retainedId);
                        if (newAnn == null || oldAnn == null) {
                            LOG.warn("Could not find announce for slingId: {} new announce: {} old announce: {}", new Object[]{retainedId, oldAnn, newAnn});
                            loadIds.add(retainedId);
                        } else if (newAnn.getData().propertiesModifiedIndex() != oldAnn.getData().propertiesModifiedIndex()) {
                            LOG.debug("Detected property change for instance with slingId: {}", retainedId);
                            loadIds.add(retainedId);
                        } else {
                            LOG.debug("No change detected for instance with slingId: {}", retainedId);
                        }
                    }
                    // Load the required properties, minimizing the number of requests
                    // If the properties for only one instance are required, we issue an
                    // instance specific request. If the properties for more than one
                    // instance are required, we fetch the properties for all instances in one request
                    final Map<String, Map<String, String>> remoteProps;
                    if (loadIds.size() == 0) {
                        LOG.debug("No remote instance requires property loading");
                        remoteProps = Collections.emptyMap();
                    } else if (loadIds.size() == 1) {
                        LOG.debug("One remote instance requires property loading: {}", loadIds);
                        String slingId = loadIds.iterator().next();
                        Map<String, String> props = etcdService.getProperties(slingId);
                        remoteProps = Collections.singletonMap(slingId, props);
                    } else {
                        LOG.debug("More than one remote instance requires loading its properties: {}", loadIds);
                        remoteProps = etcdService.getInstancesProperties();
                    }

                    // Build the new properties with the remote changes
                    Map<String, Map<String, String>> newProps =
                            new HashMap<String, Map<String, String>>(propertiesMap.getRemote());
                    newProps.putAll(remoteProps);

                    propertiesMap.setRemote(newProps);
                    announcesMap.setRemote(newAnns);
                    long oldPeriod = topologyUpdatePeriod.increment();
                    LOG.debug("Successfully fetched state for remote instances (topologyUpdatePeriod: {} to {})", new Object[]{oldPeriod, topologyUpdatePeriod.value()});
                } else {
                    long oldPeriod = topologyUpdatePeriod.increment();
                    LOG.debug("Remote announces have not changed (topologyUpdatePeriod: {} to {})", new Object[]{oldPeriod, topologyUpdatePeriod.value()});
                }

            } catch (EtcdServiceException e) {
                long oldPeriod = topologyUpdatePeriod.reset();
                LOG.info("Failed to fetch remote announces {}", e.getError());
                LOG.debug("topologyUpdatePeriod: {} to {}", new Object[]{oldPeriod, topologyUpdatePeriod});
                context.next(Event.RESET);
            } catch (IOException e) {
                long oldPeriod = topologyUpdatePeriod.reset();
                LOG.info("Fetching remote announces failed with I/O error: {}", e.getMessage());
                LOG.debug("topologyUpdatePeriod: {} to {}", new Object[]{oldPeriod, topologyUpdatePeriod});
                context.next(Event.RESET);
            }
        } else {
            long oldPeriod = topologyUpdatePeriod.reset();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Nothing to do in state {} (topologyUpdatePeriod: {} to {})", new Object[]{context.getState(), oldPeriod, topologyUpdatePeriod});
            }
        }
    }

    protected void sleep(long ms) {
        LOG.trace("wait {} ms before proceeding.", ms);
        super.sleep(ms);
    }
}
