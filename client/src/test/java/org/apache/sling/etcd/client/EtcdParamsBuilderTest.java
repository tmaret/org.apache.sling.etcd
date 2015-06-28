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
package org.apache.sling.etcd.client;

import java.util.Map;

import junit.framework.Assert;
import org.junit.Test;

public class EtcdParamsBuilderTest {

    @Test
    public void testSettingAllParameters() throws Exception {
        Map<String, String> params = new EtcdParamsBuilder()
                .dir(true)
                .sorted(true)
                .prevExist(true)
                .prevIndex(10)
                .prevValue("some value")
                .quorum(true)
                .recursive(true)
                .ttl(20)
                .wait(true)
                .waitIndex(30)
                .build();
        Assert.assertEquals("true", params.get("dir"));
        Assert.assertEquals("true", params.get("sorted"));
        Assert.assertEquals("true", params.get("prevExist"));
        Assert.assertEquals("10", params.get("prevIndex"));
        Assert.assertEquals("some value", params.get("prevValue"));
        Assert.assertEquals("true", params.get("quorum"));
        Assert.assertEquals("true", params.get("recursive"));
        Assert.assertEquals("20", params.get("ttl"));
        Assert.assertEquals("true", params.get("wait"));
        Assert.assertEquals("30", params.get("waitIndex"));
    }
}