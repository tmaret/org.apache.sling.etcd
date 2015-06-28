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

import javax.annotation.Nonnull;

import aQute.bnd.annotation.ProviderType;

/**
 * Represent the statistics of the latency and amount of error/success
 * Raft RPC request from the leader to a follower.
 */
@ProviderType
public interface FollowerStats {

    /**
     * @return the follower peer unique id.
     */
    @Nonnull
    String id();

    /**
     * @return the sum of failed Raft RPC requests ; or {@code -1} if undefined.
     */
    long failCount();

    /**
     * @return the sum of successful Raft RPC requests ; or {@code -1} if undefined.
     */
    long successCount();

    /**
     * @return the average latency in millisecond ; or {@code Double.NaN} if undefined.
     */
    double latencyAverage();

    /**
     * @return the current latency in millisecond ; or {@code Double.NaN} if undefined.
     */
    double latencyCurrent();

    /**
     * @return the max latency in millisecond ; or {@code Double.NaN} if undefined.
     */
    double latencyMax();

    /**
     * @return the min latency in millisecond ; or {@code Double.NaN} if undefined.
     */
    double latencyMin();

    /**
     * @return the latency standard deviation ; or {@code Double.NaN} if undefined.
     */
    double latencyStdDev();

}
