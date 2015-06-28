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

import junit.framework.Assert;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Test;

public class AnnounceDataTest {

    @Test
    public void testGet() throws Exception {
        AnnounceData annData = new AnnounceData("sling-id", "server-info", "default-cluster", 10);
        Assert.assertEquals("sling-id", annData.slingId());
        Assert.assertEquals("server-info", annData.serverInfo());
        Assert.assertEquals(10, annData.propertiesModifiedIndex());
    }

    @Test
    public void testJson() throws Exception {
        AnnounceData annData = new AnnounceData("sling-id", "server-info", "default-cluster", 10);
        JSONObject json = annData.json();
        Assert.assertNotNull(json);
        AnnounceData annData2 = new AnnounceData(json.toString());
        Assert.assertEquals(annData.slingId(), annData2.slingId());
        Assert.assertEquals(annData.serverInfo(), annData2.serverInfo());
        Assert.assertEquals(annData.propertiesModifiedIndex(), annData2.propertiesModifiedIndex());
    }

    @Test(expected = EtcdDiscoveryRuntimeException.class)
    public void testWrongFormat() {
        new AnnounceData("miss-formatted");
    }

    @Test
    public void testEquals() throws Exception {
        AnnounceData data1 = new AnnounceData("sling-id", "server-info", "default-cluster", 10);
        AnnounceData data2 = new AnnounceData("sling-id", "server-info", "default-cluster", 10);
        Assert.assertTrue(data1.equals(data2) & data2.equals(data1));
        Assert.assertTrue(data1.hashCode() == data2.hashCode());
        AnnounceData data3 = new AnnounceData("other-sling-id", "server-info", "default-cluster", 10);
        Assert.assertFalse(data1.equals(data3) | data3.equals(data1));
        AnnounceData data4 = new AnnounceData("sling-id", "other-server-info", "default-cluster", 10);
        Assert.assertFalse(data1.equals(data4) | data4.equals(data1));
        AnnounceData data5 = new AnnounceData("sling-id", "server-info", "default-cluster", 11);
        Assert.assertFalse(data1.equals(data5) | data5.equals(data1));
    }
}