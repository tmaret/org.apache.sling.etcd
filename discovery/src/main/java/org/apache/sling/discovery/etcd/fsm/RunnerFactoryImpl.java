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


import javax.annotation.Nonnull;

import org.apache.sling.discovery.etcd.AnnouncesMap;
import org.apache.sling.discovery.etcd.backoff.BackOff;
import org.apache.sling.discovery.etcd.EtcdService;
import org.apache.sling.discovery.etcd.cluster.Clustering;

public class RunnerFactoryImpl implements RunnerFactory {

    /**
     * Service to communicate with the etcd machines.
     */
    private final EtcdService etcdService;

    /**
     * The local and remote announces
     */
    private final AnnouncesMap announcesMap;

    /**
     * The clustering object allowing to define the cluster id the local instance belongs to.
     */
    private final Clustering clustering;

    /**
     * The local instance Sling identifier.
     */
    private final String slingId;

    /**
     * The max announce ttl (including leeway) in seconds.
     */
    private int maxAnnounceTtl;

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

    public RunnerFactoryImpl(@Nonnull EtcdService etcdService, @Nonnull AnnouncesMap announcesMap,
                             @Nonnull Clustering clustering, @Nonnull BackOff etcdBackOff,
                             @Nonnull BackOff ioExceptionBackOff, @Nonnull String slingId,
                             @Nonnull String serverInfo, int maxAnnounceTtl) {
        this.etcdService = etcdService;
        this.announcesMap = announcesMap;
        this.clustering = clustering;
        this.slingId = slingId;
        this.ioExceptionBackOff = ioExceptionBackOff;
        this.etcdBackOff = etcdBackOff;
        this.serverInfo = serverInfo;
        this.maxAnnounceTtl = maxAnnounceTtl;
    }


    @Nonnull
    public Runner build(@Nonnull Context context) {
        return new StateRunner(context, etcdService, announcesMap, clustering, etcdBackOff,
                ioExceptionBackOff, slingId, serverInfo, maxAnnounceTtl);
    }
}
