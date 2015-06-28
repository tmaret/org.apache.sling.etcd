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

import junit.framework.Assert;
import org.junit.Test;

public class PropertiesMapTest {

    @Test
    public void testGet() throws Exception {
        PropertiesMap map = new PropertiesMap("sling-id");
        Assert.assertEquals(0, map.getLocal().size());
        map.setLocal(Collections.singletonMap("k1", "v1"));
        map.setRemote(Collections.singletonMap("remote-1", Collections.singletonMap("k2", "v2")));
        Assert.assertEquals(1, map.getLocal().size());
        Assert.assertEquals(1, map.getRemote().size());
        Assert.assertEquals(2, map.getAll().size());
    }
}