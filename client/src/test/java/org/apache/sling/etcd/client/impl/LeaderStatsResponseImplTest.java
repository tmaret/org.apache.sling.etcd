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
import java.util.List;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.etcd.client.FollowerStats;
import org.apache.sling.etcd.client.LeaderStatsResponse;
import org.junit.Test;

public class LeaderStatsResponseImplTest {

    @Test
    public void testLeaderResponse() throws Exception {
        JSONObject data = new JSONObject(IOUtils.toString(
                getClass().getResourceAsStream(
                        "/leader-stats.json")));
        LeaderStatsResponse response = new LeaderStatsResponseImpl(200, "OK", Collections.<String, List<String>>emptyMap(), data);
        Assert.assertNotNull(response);
        Assert.assertEquals("tmaret-osx", response.leaderId());
        List<FollowerStats> followers = response.followers();
        Assert.assertNotNull(followers);
        Assert.assertEquals(1, followers.size());
        FollowerStats f1 = followers.get(0);
        Assert.assertNotNull(f1);
        Assert.assertEquals("machine2", f1.id());
        Assert.assertEquals(0.446614, f1.latencyCurrent());
        Assert.assertEquals(0.505968584949263, f1.latencyAverage());
        Assert.assertEquals(0.505968584949263, f1.latencyAverage());
        Assert.assertEquals(0.42786445556291247, f1.latencyStdDev());
        Assert.assertEquals(0.191323, f1.latencyMin());
        Assert.assertEquals(40.466276, f1.latencyMax());
        Assert.assertEquals(0, f1.failCount());
        Assert.assertEquals(34191, f1.successCount());

    }

}