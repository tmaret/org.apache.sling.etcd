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
package org.apache.sling.etcd.client;

import java.util.Calendar;

import javax.annotation.Nonnull;

import aQute.bnd.annotation.ProviderType;

/**
 * Represent the response returned by the etcd peer stats API (Self Statistics).
 */
@ProviderType
public interface MemberStatsResponse extends EtcdResponse {

    /**
     * @return the unique identifier for the peer.
     */
    @Nonnull
    String id();

    /**
     * @return the id of the current leader peer.
     */
    @Nonnull
    String leaderId();

    /**
     * @return a calendar containing the time when the leader has started.
     */
    @Nonnull
    Calendar leaderStartTime();

    /**
     * @return the amount of time the leader has been leader.
     */
    @Nonnull
    String leaderUptime();

    /**
     *
     * @return the peer name.
     */
    @Nonnull
    String name();

    /**
     * @return the number of append requests the peer has processed ; or {@code -1} if undefined.
     */
    long recvAppendRequestCnt();

    /**
     * @return the number of bytes per second this peer is receiving if the peer is a follower,
     *         {@code Double.NaN} if the peer is the leader.
     */
    double recvBandwidthRate();

    /**
     * @return the number of requests per second this peer is receiving if the peer is a follower,
     *         {@code Double.NaN} if the peer is the leader.
     */
    double recvPkgRate();

    /**
     * @return the number of requests that this peer has sent ; or {@code -1} if undefined.
     */
    long sendAppendRequestCnt();

    /**
     * @return the number of bytes per second the peer is sending if the peer is the leader,
     *         {@code Double.NaN} if the peer is a follower or if the cluster is composed of a single member.
     */
    double sendBandwidthRate();

    /**
     * @return the number of requests per second this peer is sending if the peer is the leader,
     *         {@code Double.NaN} if the peer is a follower or if the cluster is composed of a single member.
     */
    double sendPkgRate();

    /**
     * @return the string {@code StateFollower} if the peer is a follower, or
     *         {@code StateLeader} if the peer is the leader.
     */
    @Nonnull
    String state();

    /**
     *
     * @return the time when this peer was started.
     */
    @Nonnull
    Calendar startTime();

}
