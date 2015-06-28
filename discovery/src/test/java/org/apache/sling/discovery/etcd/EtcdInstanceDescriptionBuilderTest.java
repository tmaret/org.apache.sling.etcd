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

import java.util.Collections;
import java.util.List;

import junit.framework.Assert;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.junit.Test;

public class EtcdInstanceDescriptionBuilderTest {

    @Test
    public void testBuild() throws Exception {
        EtcdInstanceDescriptionBuilder builder = new EtcdInstanceDescriptionBuilder("sling-id-1", Collections.<String, String>emptyMap(), true, true);
        ClusterView cv = new ClusterView() {
            public String getId() {
                return null;
            }

            public List<InstanceDescription> getInstances() {
                return null;
            }

            public InstanceDescription getLeader() {
                return null;
            }
        };
        InstanceDescription id = builder.build(cv);
        Assert.assertNotNull(id);
        Assert.assertTrue(id.isLocal());
        Assert.assertTrue(id.isLeader());
        Assert.assertEquals("sling-id-1", id.getSlingId());
        Assert.assertEquals(cv, id.getClusterView());
    }


}