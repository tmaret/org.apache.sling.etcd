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

import java.io.IOException;
import java.net.URI;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.etcd.client.EtcdClient;
import org.apache.sling.etcd.client.LeaderStatsResponse;
import org.apache.sling.etcd.client.Member;
import org.apache.sling.etcd.client.MemberStatsResponse;
import org.apache.sling.etcd.client.MembersResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EtcdStats {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdStats.class);

    private static final long STATS_CACHING_DELAY = 30 * 1000; // 30 seconds

    private final EtcdClient etcdClient;

    private volatile long nextFetchTime;

    private volatile Stats stats;

    public EtcdStats(@Nonnull EtcdClient etcdClient) {
        this.etcdClient = etcdClient;
    }

    @Nullable
    public Stats getStats() {
        if (System.currentTimeMillis() > nextFetchTime) {
            fetchStats();
            nextFetchTime = System.currentTimeMillis() + STATS_CACHING_DELAY;
        }
        return stats;
    }

    //

    private void fetchStats() {
        Map<String, MemberStatsResponse> membersStats = new HashMap<String, MemberStatsResponse>();
        try {
            LeaderStatsResponse leaderStatsResponse = null;
            MembersResponse membersResponse = etcdClient.getMembers();
            for (Member member : membersResponse.members()) {
                String peerId = member.id();
                List<URI> clientUrls = member.clientUrls();
                if (clientUrls.size() > 0) {
                    URI clientUrl = clientUrls.get(0);

                    try {
                        MemberStatsResponse memberStats = etcdClient.getMemberStats(clientUrl);
                        if (memberStats.leaderId().equals(peerId)) {
                            leaderStatsResponse = etcdClient.getLeaderStats(clientUrl);
                        }
                        membersStats.put(peerId, memberStats);
                    } catch (IOException e) {
                        LOG.debug("I/O error while fetching the etcd member stats: {}", e.getMessage());
                    }
                } else {
                    LOG.debug("No peerUrl defined for member with id: {}", peerId);
                }
            }
            stats = new Stats(Calendar.getInstance(), leaderStatsResponse, membersResponse, membersStats);
        } catch (IOException e) {
            LOG.info("I/O error while fetching the etcd stats: {}", e.getMessage());
        }
    }

    public class Stats {

        private final Calendar fetchTime;

        private final LeaderStatsResponse leaderStats;

        private final MembersResponse members;

        private final Map<String, MemberStatsResponse> membersStats;

        public Stats(@Nonnull Calendar fetchTime, @Nullable LeaderStatsResponse leaderStats,
                     @Nonnull MembersResponse members, @Nonnull Map<String, MemberStatsResponse> membersStats) {
            this.fetchTime = fetchTime;
            this.leaderStats = leaderStats;
            this.members = members;
            this.membersStats = membersStats;
        }

        @Nonnull
        public Calendar fetchTime() {
            return fetchTime;
        }

        @Nullable
        public LeaderStatsResponse getLeaderStats() {
            return leaderStats;
        }

        @Nonnull
        public MembersResponse getMembers() {
            return members;
        }

        @Nonnull
        public Map<String, MemberStatsResponse> getMembersStats() {
            return membersStats;
        }
    }
}
