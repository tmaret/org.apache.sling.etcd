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

import junit.framework.Assert;
import org.junit.Test;

public class EtcdParamsTest {

    @Test
    public void testBuilder() throws Exception {
        EtcdParamsBuilder b1 = EtcdParams.builder();
        Assert.assertNotNull(b1);
        EtcdParamsBuilder b2 = EtcdParams.builder();
        Assert.assertNotSame(b1, b2);
    }

    @Test
    public void testNoParams() throws Exception {
        Assert.assertNotNull(EtcdParams.noParams());
        Assert.assertEquals(0, EtcdParams.noParams().size());
    }
}