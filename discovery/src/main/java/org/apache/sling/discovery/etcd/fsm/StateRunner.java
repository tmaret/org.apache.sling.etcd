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
package org.apache.sling.discovery.etcd.fsm;

import java.io.IOException;
import java.math.BigDecimal;

import javax.annotation.Nonnull;

import org.apache.sling.discovery.etcd.AnnouncesMap;
import org.apache.sling.discovery.etcd.backoff.BackOff;
import org.apache.sling.discovery.etcd.EtcdServiceException;
import org.apache.sling.discovery.etcd.EtcdServiceStats;
import org.apache.sling.discovery.etcd.Announce;
import org.apache.sling.discovery.etcd.AnnounceData;
import org.apache.sling.discovery.etcd.Announces;
import org.apache.sling.discovery.etcd.EtcdService;
import org.apache.sling.discovery.etcd.cluster.Clustering;
import org.apache.sling.etcd.client.KeyError;
import org.apache.sling.etcd.client.EtcdNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code StateRunner} process the internal state of the discovery protocol.
 */
public class StateRunner extends BaseRunner {

    /**
     * Default logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(StateRunner.class);

    /**
     * Leeway factor to be applied to the max ttl
     */
    private static final double CLEAR_ANNOUNCE_LEEWAY = 1.15D;

    /**
     * The discovery protocol context
     */
    private final Context context;

    /**
     * Service to communicate with the etcd machines.
     */
    private final EtcdService etcdService;

    /**
     * The local and remote announces
     */
    private final AnnouncesMap announcesMap;

    /**
     * The local instance Sling identifier.
     */
    private final String slingId;

    /**
     * The max announce ttl (including leeway) in seconds.
     */
    private int maxAnnounceTtl;

    /**
     * The clustering object allowing to define the cluster id the local instance belongs to.
     */
    private final Clustering clustering;

    /**
     * The remaining ttl in seconds for the announce duplicating the local instance Sling identifier.
     */
    private long remainingAnnounceTtl;

    /**
     * The local instance hostname.
     */
    private final String serverInfo;

    /**
     * The object to compute back-offs when I/O exceptions occur.
     */
    private final BackOff ioExceptionBackOff;

    /**
     * The object to compute back-offs when etcd errors occur.
     */
    private final BackOff etcdBackOff;

    private static final int ONE_SECOND = 1000;

    public StateRunner(@Nonnull Context context, @Nonnull EtcdService etcdService, @Nonnull AnnouncesMap announcesMap,
                       @Nonnull Clustering clustering, @Nonnull BackOff etcdBackOff,
                       @Nonnull BackOff ioExceptionBackOff, @Nonnull String slingId,
                       @Nonnull String serverInfo, int maxAnnounceTtl) {
        super();
        this.context = context;
        this.etcdService = etcdService;
        this.announcesMap = announcesMap;
        this.clustering = clustering;
        this.slingId = slingId;
        this.serverInfo = serverInfo;
        this.etcdBackOff = etcdBackOff;
        this.ioExceptionBackOff = ioExceptionBackOff;
        this.maxAnnounceTtl = maxAnnounceTtl;
    }

    public void run() {
        for ( ; running ; ) {
            State state = null;
            try {
                state = context.getState();
                processState(state);
            } catch (EtcdServiceException e) {
                KeyError error = e.getError();
                LOG.info("Etcd error received in state: {} {}", new Object[]{state, e.getMessage()});
                LOG.debug(e.getMessage(), e);
                int code = error.errorCode();
                if ((code >= 300) && (code < 400)) {
                    EtcdServiceStats errorStats = etcdService.getErrStats();
                    int index = errorStats.consecutiveEtcdError(300, 400);
                    long waitTime = etcdBackOff.value(index);
                    LOG.info("Etcd Raft related error, wait {} ms (consecutiveEtcdError index: {}) in order to allow etcd to recover.", new Object[]{waitTime, index});
                    context.next(Event.RESET);
                    sleep(waitTime);
                } else if ((code >= 200) && (code < 300)) {
                    // Post Form Related Error
                    // error caused by a bug in discovery code
                    // which we can't recover from unless
                    // fix the issue.
                    LOG.info("Discovery protocol issue, stop.");
                    context.next(Event.STOPPED);
                } else {
                    // Command Related Error or Etcd Related Error
                    LOG.info("Command related error or etcd related error, reset.");
                    context.next(Event.RESET);
                    sleep(ONE_SECOND);
                }
            } catch (IOException e) {
                LOG.debug(e.getMessage(), e);
                int index = etcdService.getErrStats().consecutiveIoError();
                long waitTime = ioExceptionBackOff.value(index);
                LOG.info("I/O error in state: {} {}, wait {} ms (consecutiveIoError index: {}) before proceeding.", new Object[]{state, e.getMessage(), waitTime, index});
                context.next(Event.RESET);
                sleep(waitTime);
            } catch (Exception e) {
                LOG.error("Exception in state: {}", state, e);
                context.next(Event.RESET);
                sleep(60 * ONE_SECOND); // one minute
            }
        }
        LOG.debug("Stopped state runner thread.");
    }

    private void processState(State state) throws IOException, EtcdServiceException {

        LOG.debug("Processing state: {}", state);

        if (States.GET_CLUSTER.equals(state)) {

            String clusterId = clustering.getClusterId();
            if (clusterId != null) {
                LOG.info("Found cluster id: {}", clusterId);
                context.setClusterId(clusterId);
                context.next(Event.CLUSTER_DEFINED);
            } else {
                LOG.info("No cluster found");
                context.next(Event.CLUSTER_UNDEFINED);
            }

        } else if (States.CREATE_CLUSTER.equals(state)) {

            String clusterId = clustering.setClusterId();
            if (clusterId != null) {
                LOG.info("Created cluster with cluster id: {}", clusterId);
                context.setClusterId(clusterId);
                context.next(Event.CLUSTER_CREATED);
            } else {
                LOG.info("Failed to create cluster");
                sleep(ONE_SECOND);
            }
        } else if (States.ANNOUNCE.equals(state)) {

            String clusterId = context.getClusterId();
            if (clusterId != null) {

                // check if there exists an announce for
                // the same Sling id as the local instance.
                // For detecting duplicates, we consider all returned announces from etcd,
                // including those returning a negative ttl.
                Announces existingAnnounces = new Announces(etcdService.getAnnounces(), false);
                Announce annWithLocalSlingId = existingAnnounces.getBySlingId(slingId);
                if (annWithLocalSlingId != null) {
                    LOG.info("Found an existing announce for the same slingId as the local instance: {} with ttl: {}.", new Object[]{annWithLocalSlingId, annWithLocalSlingId.getTtl()});
                    // determine the time to wait as the remaining ttl
                    // set in the existing announce
                    Long ttl = annWithLocalSlingId.getTtl();
                    remainingAnnounceTtl = (ttl != null && ttl > 0) ? ttl : maxAnnounceTtl;
                    context.next(Event.ANNOUNCE_DUPLICATED);
                } else {
                    LOG.debug("No announce found for the local slingId: {}", slingId);
                    // create an announce for the local instance
                    AnnounceData annData = new AnnounceData(slingId, serverInfo, clusterId, 0);
                    EtcdNode annNode = etcdService.createAnnounce(annData.toString(), maxAnnounceTtl);
                    String annKey = annNode.key();
                    // check if the announce created is the first for the Sling identifier
                    Announces announces = new Announces(etcdService.getAnnounces(), false);
                    if (announces.size() > 0) {
                        Announce local = announces.getBySlingId(slingId);
                        if (local != null) {
                            // check for duplicate.
                            if (local.getAnnounceKey().equals(annKey)) {
                                // The local instance is the first to announce with
                                // the local Sling identifier.
                                LOG.info("Successfully announced the local instance: {} with key: {}", new Object[]{slingId, annKey});
                                announcesMap.setLocal(local);
                                context.next(Event.ANNOUNCED);
                            } else {
                                LOG.info("Found an earlier announce: {} with ttl: {} and containing the same slingId as the local instance. The announce created by the local instance with key: {} is considered invalid.", new Object[]{local, local.getTtl(), annKey});
                                // the time to wait correspond to the max announce ttl
                                // as the announce just created also need to elapse
                                // before starting again.
                                remainingAnnounceTtl = maxAnnounceTtl;
                                context.next(Event.ANNOUNCE_DUPLICATED);
                            }
                        } else {
                            LOG.warn("No announce found for the local instance: {}", slingId);
                            context.next(Event.ANNOUNCE_LOCAL_INSTANCE_NOT_FOUND);
                        }
                    } else {
                        LOG.warn("No announce found");
                        context.next(Event.ANNOUNCE_LOCAL_INSTANCE_NOT_FOUND);
                    }
                }
            } else {
                LOG.error("Undefined cluster identifier");
                context.next(Event.RESET);
            }

        } else if (States.RUNNING.equals(state)) {

            sleep(ONE_SECOND);

        } else if (States.CLEAR_ANNOUNCE.equals(state)) {
            long waitTime = (long) new BigDecimal(remainingAnnounceTtl * CLEAR_ANNOUNCE_LEEWAY * 1000.0D)
                    .setScale(0, BigDecimal.ROUND_UP).intValue();
            LOG.info("Wait: {} ms until the existing announce with slingId: {} elapses",
                    new Object[]{waitTime, slingId} );
            sleep(waitTime);
            context.next(Event.ANNOUNCE_CLEARED);

        } else if (States.STOP.equals(state)) {

            sleep(ONE_SECOND);

        } else {
            LOG.error("Unknown state found: {}", state);
            context.next(Event.STOPPED);
        }
    }

    protected void sleep(long ms) {
        LOG.trace("wait {} ms before proceeding.", ms);
        super.sleep(ms);
    }

}
