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
package org.apache.sling.discovery.etcd.backoff;

import junit.framework.Assert;
import org.junit.Test;

public class ConstantBackOffTest {

    @Test
    public void testValue() throws Exception {
        BackOff backOff = new ConstantBackOff(0);
        Assert.assertEquals(0, backOff.value(0));
        Assert.assertEquals(0, backOff.value(10));
        Assert.assertEquals(0, backOff.value(20));
        Assert.assertEquals(0, backOff.value(30));
    }

    @Test
    public void testValue1() throws Exception {
        BackOff backOff = new ConstantBackOff(10);
        Assert.assertEquals(10, backOff.value(0));
        Assert.assertEquals(10, backOff.value(10));
        Assert.assertEquals(10, backOff.value(20));
        Assert.assertEquals(10, backOff.value(30));
    }

    @Test
    public void testMaxValue() throws Exception {
        BackOff backOff = new ConstantBackOff(10);
        Assert.assertEquals(10, backOff.max());
    }

    @Test
    public void testIncrementValue() throws Exception {
        BackOff backOff = new ConstantBackOff(10);
        Assert.assertEquals(10, backOff.value());
        backOff.increment();
        Assert.assertEquals(10, backOff.value());
    }

    @Test
    public void testResetValue() throws Exception {
        BackOff backOff = new ConstantBackOff(10);
        Assert.assertEquals(10, backOff.value());
        backOff.reset();
        Assert.assertEquals(10, backOff.value());
    }
}