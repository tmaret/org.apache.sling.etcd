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
package org.apache.sling.discovery.etcd.cluster;

import junit.framework.Assert;
import org.junit.Test;

public class ConfigClusteringTest {

    @Test
    public void testGetClusterId() throws Exception {
        String DEFAULT = "default";
        Assert.assertEquals(DEFAULT, new ConfigClustering("", DEFAULT).getClusterId());
        Assert.assertEquals(DEFAULT, new ConfigClustering("*as", DEFAULT).getClusterId());
        Assert.assertEquals(DEFAULT, new ConfigClustering("does not comply", DEFAULT).getClusterId());
        Assert.assertEquals("some", new ConfigClustering("some", DEFAULT).getClusterId());
        Assert.assertEquals("some", new ConfigClustering(" some  ", DEFAULT).getClusterId());
        Assert.assertEquals("valid-id", new ConfigClustering("valid-id", DEFAULT).getClusterId());
        Assert.assertEquals("valid-id_Containing-Uppercase", new ConfigClustering("valid-id_Containing-Uppercase", DEFAULT).getClusterId());
    }

    @Test
    public void testIsSupported() throws Exception {
        Assert.assertTrue(new ConfigClustering("clusterId", "defaultClusterId").isSupported());
    }
}