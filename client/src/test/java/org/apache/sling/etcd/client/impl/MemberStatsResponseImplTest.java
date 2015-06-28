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
package org.apache.sling.etcd.client.impl;

import java.util.Collections;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.etcd.client.MemberStatsResponse;
import org.junit.Test;

public class MemberStatsResponseImplTest {

    @Test
    public void testLeaderMemberStats() throws Exception {
        JSONObject data = new JSONObject(IOUtils.toString(
                getClass().getResourceAsStream(
                        "/peer-leader-stats.json")));
        MemberStatsResponse response = new MemberStatsResponseImpl(200, "OK", Collections.singletonMap("header", Collections.singletonList("value")),data);
        Assert.assertNotNull(response);
        Assert.assertEquals("324473db0474a678", response.id());
        Assert.assertEquals("c3", response.name());
        Assert.assertEquals("StateLeader", response.state());
        Assert.assertEquals(1431453454984L, response.startTime().getTimeInMillis());
        Assert.assertEquals("324473db0474a678", response.leaderId());
        Assert.assertEquals("8h6m43.99465648s", response.leaderUptime());
        Assert.assertEquals(1431257310435L, response.leaderStartTime().getTimeInMillis());
        Assert.assertEquals(0, response.recvAppendRequestCnt());
        Assert.assertEquals(224133, response.sendAppendRequestCnt());
        Assert.assertEquals(9.090537264382222, response.sendPkgRate());
        Assert.assertEquals(752.0601478823412, response.sendBandwidthRate());
        // undefined for leader
        Assert.assertEquals(Double.NaN, response.recvBandwidthRate());
        Assert.assertEquals(Double.NaN, response.recvPkgRate());
    }

    @Test
    public void testFollowerMemberStats() throws Exception {
        JSONObject data = new JSONObject(IOUtils.toString(
                getClass().getResourceAsStream(
                        "/peer-follower-stats.json")));
        MemberStatsResponse response = new MemberStatsResponseImpl(200, "OK", Collections.singletonMap("header", Collections.singletonList("value")),data);
        Assert.assertEquals("7e3bd17c66e004e8", response.id());
        Assert.assertEquals("c2", response.name());
        Assert.assertEquals("StateFollower", response.state());
        Assert.assertEquals(1431202055463L, response.startTime().getTimeInMillis());
        Assert.assertEquals("324473db0474a678", response.leaderId());
        Assert.assertEquals("8h7m13.504300576s", response.leaderUptime());
        Assert.assertEquals(1431959231127L, response.leaderStartTime().getTimeInMillis());
        Assert.assertEquals(59475, response.recvAppendRequestCnt());
        Assert.assertEquals(0, response.sendAppendRequestCnt());
        Assert.assertEquals(245.0030142367229, response.recvBandwidthRate());
        Assert.assertEquals(2.0618810371279017, response.recvPkgRate());
        // undefined for follower
        Assert.assertEquals(Double.NaN, response.sendBandwidthRate());
        Assert.assertEquals(Double.NaN, response.sendPkgRate());
    }
}