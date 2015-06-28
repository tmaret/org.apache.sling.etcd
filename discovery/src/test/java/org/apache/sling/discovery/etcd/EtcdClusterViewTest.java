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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

public class EtcdClusterViewTest {

    @Test
    public void testBuild() throws Exception {
        List<EtcdInstanceDescriptionBuilder> builders = new ArrayList<EtcdInstanceDescriptionBuilder>();
        builders.add(new EtcdInstanceDescriptionBuilder("sling-id-1", Collections.<String, String>emptyMap(), true, false));
        builders.add(new EtcdInstanceDescriptionBuilder("sling-id-2", Collections.<String, String>emptyMap(), false, true));
        EtcdClusterView cv = new EtcdClusterView("view-id", builders);
        Assert.assertEquals("view-id", cv.getId());
        Assert.assertEquals(2, cv.getInstances().size());
        Assert.assertFalse(cv.getLeader().isLocal());
        Assert.assertTrue(cv.getLeader().isLeader());
    }

    @Test(expected = EtcdDiscoveryRuntimeException.class)
    public void testBuildNoInstance() throws Exception {
        List<EtcdInstanceDescriptionBuilder> builders = new ArrayList<EtcdInstanceDescriptionBuilder>();
        new EtcdClusterView("view-id", builders);
    }

    @Test(expected = EtcdDiscoveryRuntimeException.class)
    public void testBuildMissingLeader() throws Exception {
        List<EtcdInstanceDescriptionBuilder> builders = new ArrayList<EtcdInstanceDescriptionBuilder>();
        builders.add(new EtcdInstanceDescriptionBuilder("sling-id-1", Collections.<String, String>emptyMap(), true, false));
        new EtcdClusterView("view-id", builders);
    }

    @Test(expected = EtcdDiscoveryRuntimeException.class)
    public void testBuildDuplicatedLeader() throws Exception {
        List<EtcdInstanceDescriptionBuilder> builders = new ArrayList<EtcdInstanceDescriptionBuilder>();
        builders.add(new EtcdInstanceDescriptionBuilder("sling-id-1", Collections.<String, String>emptyMap(), true, true));
        builders.add(new EtcdInstanceDescriptionBuilder("sling-id-1", Collections.<String, String>emptyMap(), false, true));
        new EtcdClusterView("view-id", builders);
    }

    @Test
    public void testInstanceOrder() throws Exception {
        List<EtcdInstanceDescriptionBuilder> builders = new ArrayList<EtcdInstanceDescriptionBuilder>();
        builders.add(new EtcdInstanceDescriptionBuilder("sling-id-2", Collections.<String, String>emptyMap(), false, true));
        builders.add(new EtcdInstanceDescriptionBuilder("sling-id-1", Collections.<String, String>emptyMap(), true, false));
        EtcdClusterView cv = new EtcdClusterView("view-id", builders);
        Assert.assertEquals("sling-id-2", cv.getInstances().get(0).getSlingId());
        Assert.assertEquals("sling-id-1", cv.getInstances().get(1).getSlingId());
    }

}