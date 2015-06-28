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

/**
 * Event enum for moving the etcd discovery internal state.
 */
public enum Event {

    /**
     * A cluster could be found.
     */
    CLUSTER_DEFINED,

    /**
     * No cluster could be found.
     */
    CLUSTER_UNDEFINED,

    /**
     * A cluster has been created.
     */
    CLUSTER_CREATED,

    /**
     * The local instance has successfully announced to the cluster.
     */
    ANNOUNCED,

    /**
     * The local instance could not announce successfully because it duplicates an already
     * announced sling identifier.
     */
    ANNOUNCE_DUPLICATED,

    /**
     * The local instance failed to refresh its announce on etcd and the announce key exists.
     */
    ANNOUNCE_REFRESH_FAILED_WITH_KEY,

    /**
     * The local instance failed to refresh its announce on etcd and the announce key did not exist.
     */
    ANNOUNCE_REFRESH_FAILED_WITH_NO_KEY,

    /**
     * Previous announces using the local Sling identifier and announce key are removed from etcd.
     */
    ANNOUNCE_CLEARED,

    /**
     * The announce for the local instance was not found.
     */
    ANNOUNCE_LOCAL_INSTANCE_NOT_FOUND,

    /**
     * The discovery protocol has been reset.
     */
    RESET,

    /**
     * The discovery protocol has been stopped.
     */
    STOPPED

}
