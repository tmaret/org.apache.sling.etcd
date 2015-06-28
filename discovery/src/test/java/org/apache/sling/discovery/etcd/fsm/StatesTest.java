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

import junit.framework.Assert;
import org.junit.Test;

public class StatesTest {

    @Test
    public void testTransition() throws Exception {
        Assert.assertEquals(States.ANNOUNCE, States.GET_CLUSTER.next(Event.CLUSTER_DEFINED));
        Assert.assertEquals(States.CREATE_CLUSTER, States.GET_CLUSTER.next(Event.CLUSTER_UNDEFINED));
        Assert.assertEquals(States.RUNNING, States.ANNOUNCE.next(Event.ANNOUNCED));
        Assert.assertEquals(States.STOP, States.ANNOUNCE.next(Event.STOPPED));
        Assert.assertEquals(States.STOP, States.STOP.next(Event.RESET));
    }

    @Test
    public void testDefaultTransition() throws Exception {
        Assert.assertEquals(States.GET_CLUSTER, States.GET_CLUSTER.next(Event.ANNOUNCED));
        Assert.assertEquals(States.ANNOUNCE, States.ANNOUNCE.next(Event.CLUSTER_CREATED));
    }

}