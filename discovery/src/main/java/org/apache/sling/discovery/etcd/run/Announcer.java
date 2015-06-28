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
import java.math.BigDecimal;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.sling.discovery.etcd.Announce;
import org.apache.sling.discovery.etcd.AnnouncesMap;
import org.apache.sling.discovery.etcd.EtcdDiscoveryService;
import org.apache.sling.discovery.etcd.EtcdServiceException;
import org.apache.sling.discovery.etcd.PropertiesMap;
import org.apache.sling.discovery.etcd.backoff.BackOff;
import org.apache.sling.discovery.etcd.fsm.BaseRunner;
import org.apache.sling.discovery.etcd.fsm.Context;
import org.apache.sling.discovery.etcd.fsm.Event;
import org.apache.sling.discovery.etcd.fsm.States;
import org.apache.sling.discovery.etcd.AnnounceData;
import org.apache.sling.discovery.etcd.EtcdService;
import org.apache.sling.etcd.client.KeyError;
import org.apache.sling.etcd.common.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code Announcer} refresh the local instance announce on the cluster by extending the ttl of the announce key.
 * The announce contain the latest modified date for the local instance properties.
 * Prior to sending the announce, it sends the local properties to etcd if they changed.
 * The {@code Announcer} runs periodically (defined by announce renewal period) unless stopped.
 */
public class Announcer extends BaseRunner {

    /**
     * Default logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Announcer.class);

    /**
     * The discovery protocol context
     */
    private final Context context;

    /**
     * The local instance Sling identifier.
     */
    private final String slingId;

    /**
     * The local instance hostname.
     */
    private final String serverInfo;

    /**
     * The announce renewal period.
     */
    private final BackOff renewalPeriod;

    /**
     * The local and remote announces
     */
    private final AnnouncesMap announcesMap;

    /**
     * The local and remote properties.
     */
    private final PropertiesMap propertiesMap;

    /**
     * Keeps track of the last local properties sent to etcd.
     */
    private Map<String, String> lastPropertiesSent;

    /**
     * The last modified index from etcd.
     */
    private long lastModifiedIndex;

    /**
     * Service to communicate with the etcd machines.
     */
    private final EtcdService etcdService;


    public Announcer(@Nonnull Context context, @Nonnull EtcdService etcdService,
                     @Nonnull AnnouncesMap announcesMap, @Nonnull PropertiesMap propertiesMap, @Nonnull String slingId,
                     @Nonnull String serverInfo, @Nonnull BackOff renewalPeriod) {
        super();
        this.slingId = slingId;
        this.context = context;
        this.etcdService = etcdService;
        this.propertiesMap = propertiesMap;
        this.announcesMap = announcesMap;
        this.renewalPeriod = renewalPeriod;
        this.serverInfo = serverInfo;
    }

    public void run() {
        for ( ; running ; ) {
            long waitTime = renewalPeriod.value();
            try {
                internalRun(waitTime);
            } catch (Exception e) {
                long oldPeriod = renewalPeriod.reset();
                LOG.error("Error while refreshing the announce", e);
                LOG.debug("renewalPeriod: {} to {}", new Object[]{oldPeriod, renewalPeriod.value()});
            } finally {
                sleep(waitTime);
            }
        }
        LOG.debug("Stopped announcer thread.");
    }

    private void internalRun(long waitTime) {
        if (context.is(States.RUNNING)) {

            Announce local = announcesMap.getLocal();
            String key = local.getAnnounceKey();
            try {

                // send local properties if changed

                final Map<String, String> localProps = propertiesMap.getLocal();
                if (! localProps.equals(lastPropertiesSent)) {
                    lastModifiedIndex = etcdService.sendInstanceProperties(propertiesMap.getLocal(), slingId);
                    lastPropertiesSent = localProps;
                }

                // Refresh the announce key for the local instance.
                String clusterId = context.getClusterId();
                if(clusterId != null) {
                    AnnounceData annData = new AnnounceData(slingId, serverInfo, clusterId, lastModifiedIndex);
                    int ttl = new BigDecimal((waitTime * EtcdDiscoveryService.ANNOUNCE_TTL_LEEWAY) / 1000.0D)
                            .setScale(0, BigDecimal.ROUND_UP).intValue();
                    etcdService.refreshAnnounce(key, annData.toString(), ttl);
                    long oldPeriod = renewalPeriod.increment();
                    LOG.debug("Successfully refreshed the announce with key: {} and ttl: {} (renewalPeriod: {} to {})", new Object[]{key, ttl, oldPeriod, renewalPeriod.value()});
                } else {
                    LOG.error("Undefined cluster identifier");
                    context.next(Event.RESET);
                }
            } catch (EtcdServiceException e) {
                long oldPeriod = renewalPeriod.reset();
                KeyError error = e.getError();
                int code = error.errorCode();
                LOG.info("Failed to refresh announce key: {}, {}", new Object[]{key, error});
                LOG.debug("renewalPeriod: {} to {}", new Object[]{oldPeriod, renewalPeriod.value()});
                if (code == ErrorCodes.KEY_NOT_FOUND) {
                    // key not found indicates an elapsed ttl.
                    context.next(Event.ANNOUNCE_REFRESH_FAILED_WITH_NO_KEY);
                } else {
                    LOG.info("Announce refresh failed with etcd error: {} {}", new Object[]{code, error.message()});
                    context.next(Event.ANNOUNCE_REFRESH_FAILED_WITH_KEY);
                }
            } catch (IOException e) {
                long oldPeriod = renewalPeriod.reset();
                LOG.info("Announce refresh failed with I/O error: {}", e.getMessage());
                LOG.debug("renewalPeriod: {} to {}", new Object[]{oldPeriod, renewalPeriod.value()});
                context.next(Event.RESET);
            }
        } else {
            long oldPeriod = renewalPeriod.reset();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Nothing to do in state {} (renewalPeriod: {} to {})", new Object[]{oldPeriod, renewalPeriod.value(), context.getState()});
            }
        }
    }

    protected void sleep(long ms) {
        LOG.trace("wait {} ms before proceeding.", ms);
        super.sleep(ms);
    }
}

