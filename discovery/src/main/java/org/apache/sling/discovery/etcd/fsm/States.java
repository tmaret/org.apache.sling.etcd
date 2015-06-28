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
 * Internal states transitions mapping.
 */
public enum States implements State {

    GET_CLUSTER {

        public State next(Event event) {
            switch(event) {
                case CLUSTER_DEFINED:
                    return ANNOUNCE;
                case CLUSTER_UNDEFINED:
                    return CREATE_CLUSTER;
                case STOPPED:
                    return STOP;
                case RESET:
                    return GET_CLUSTER;
                default:
                    return this;
            }
        }
    },
    CREATE_CLUSTER {

        public State next(Event event) {
            switch(event) {
                case CLUSTER_CREATED:
                    return ANNOUNCE;
                case STOPPED:
                    return STOP;
                case RESET:
                    return GET_CLUSTER;
                default:
                    return this;
            }
        }
    },
    ANNOUNCE {

        public State next(Event event) {
            switch (event) {
                case ANNOUNCED:
                    return RUNNING;
                case ANNOUNCE_DUPLICATED:
                    return CLEAR_ANNOUNCE;
                case STOPPED:
                    return STOP;
                case RESET:
                    return GET_CLUSTER;
                case ANNOUNCE_LOCAL_INSTANCE_NOT_FOUND:
                    return ANNOUNCE;
                default:
                    return this;
            }
        }
    },
    CLEAR_ANNOUNCE {

        public State next(Event event) {
            switch (event) {
                case ANNOUNCE_CLEARED:
                    return ANNOUNCE;
                case STOPPED:
                    return STOP;
                case ANNOUNCE_LOCAL_INSTANCE_NOT_FOUND:
                    return ANNOUNCE;
                case RESET:
                    return GET_CLUSTER;
                default:
                    return this;
            }
        }
    },
    RUNNING {

        public State next(Event event) {
            switch (event) {
                case ANNOUNCE_REFRESH_FAILED_WITH_KEY:
                    return CLEAR_ANNOUNCE;
                case ANNOUNCE_REFRESH_FAILED_WITH_NO_KEY:
                    return ANNOUNCE;
                case STOPPED:
                    return STOP;
                case ANNOUNCE_LOCAL_INSTANCE_NOT_FOUND:
                    return ANNOUNCE;
                case RESET:
                    return GET_CLUSTER;
                default:
                    return this;
            }
        }
    },

    STOP {

        public State next(Event event) {
            switch (event) {
                default:
                    return this;
            }
        }
    };

}
