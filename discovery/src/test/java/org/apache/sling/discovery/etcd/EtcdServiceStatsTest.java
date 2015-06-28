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
import org.junit.Test;

public class EtcdServiceStatsTest {

    @Test
    public void testConsecutiveEtcdError() throws Exception {
        EtcdServiceStats stats = new EtcdServiceStats();
        Assert.assertEquals(0, stats.consecutiveEtcdError(0, Integer.MAX_VALUE));
        stats.increaseEtcdError(10);
        Assert.assertEquals(1, stats.consecutiveEtcdError(0, Integer.MAX_VALUE));
    }

    @Test
    public void testConsecutiveEtcdErrorOutOfRange() throws Exception {
        EtcdServiceStats stats = new EtcdServiceStats();
        stats.increaseEtcdError(10);
        Assert.assertEquals(1, stats.consecutiveEtcdError(10, Integer.MAX_VALUE));
        Assert.assertEquals(0, stats.consecutiveEtcdError(11, Integer.MAX_VALUE));
        Assert.assertEquals(0, stats.consecutiveEtcdError(0, 10));
    }

    @Test
    public void testConsecutiveIoError() throws Exception {
        EtcdServiceStats stats = new EtcdServiceStats();
        Assert.assertEquals(0, stats.consecutiveIoError());
        stats.increaseIoError();
        Assert.assertEquals(1, stats.consecutiveIoError());
    }

    @Test
    public void testResetIoError() throws Exception {
        EtcdServiceStats stats = new EtcdServiceStats();
        stats.increaseIoError();
        stats.increaseIoError();
        Assert.assertEquals(2, stats.consecutiveIoError());
        stats.resetIoError();
        Assert.assertEquals(0, stats.consecutiveIoError());
    }

    @Test
    public void testResetEtcdError() throws Exception {
        EtcdServiceStats stats = new EtcdServiceStats();
        stats.increaseEtcdError(10);
        stats.increaseEtcdError(11);
        Assert.assertEquals(2, stats.consecutiveEtcdError(0, Integer.MAX_VALUE));
        stats.resetEtcdError();
        Assert.assertEquals(0, stats.consecutiveEtcdError(0, Integer.MAX_VALUE));
    }
}