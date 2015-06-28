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
package org.apache.sling.etcd.common;

/**
 * CoreOS etcd headers.
 */
public final class EtcdHeaders {

    /**
     * The current etcd index
     */
    public static final String ETCD_INDEX = "X-Etcd-Index";

    /**
     * The current raft index
     */
    public static final String RAFT_INDEX = "X-Raft-Index";

    /**
     * An integer increased whenever an etcd master election happens in the cluster
     */
    public static final String RAFT_TERM = "X-Raft-Term";

    /**
     * etcd cluster identifier
     */
    public static final String ETCD_CLUSTER_ID = "X-Etcd-Cluster-Id";

}
