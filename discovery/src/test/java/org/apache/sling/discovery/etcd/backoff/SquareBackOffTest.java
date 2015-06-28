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

public class SquareBackOffTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeIndex() throws Exception {
        SquareBackOff pb = new SquareBackOff(10, 60, 5);
        pb.value(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaxSmallerThanMin() throws Exception {
        new SquareBackOff(10, 3, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMin() throws Exception {
        new SquareBackOff(-10, 60, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeSteps() throws Exception {
        new SquareBackOff(10, 60, -1);
    }

    @Test
    public void testValue() throws Exception {
        SquareBackOff pb = new SquareBackOff(10, 60, 5);
        Assert.assertEquals(10, pb.value(0));
        Assert.assertEquals(12, pb.value(1));
        Assert.assertEquals(18, pb.value(2));
        Assert.assertEquals(28, pb.value(3));
        Assert.assertEquals(42, pb.value(4));
        Assert.assertEquals(60, pb.value(5));
        Assert.assertEquals(60, pb.value(6));
    }

    @Test
    public void testValue2() throws Exception {
        SquareBackOff pb = new SquareBackOff(0, 100, 10);
        Assert.assertEquals(0, pb.value(0));
        Assert.assertEquals(1, pb.value(1));
        Assert.assertEquals(4, pb.value(2));
        Assert.assertEquals(9, pb.value(3));
        Assert.assertEquals(16, pb.value(4));
        Assert.assertEquals(25, pb.value(5));
        Assert.assertEquals(36, pb.value(6));
        Assert.assertEquals(49, pb.value(7));
        Assert.assertEquals(64, pb.value(8));
        Assert.assertEquals(81, pb.value(9));
        Assert.assertEquals(100, pb.value(10));
        Assert.assertEquals(100, pb.value(11));
    }

    @Test
    public void testValue3() throws Exception {
        SquareBackOff pb = new SquareBackOff(0, 100, 4);
        Assert.assertEquals(0, pb.value(0));
        Assert.assertEquals(6, pb.value(1));
        Assert.assertEquals(25, pb.value(2));
        Assert.assertEquals(56, pb.value(3));
        Assert.assertEquals(100, pb.value(4));
        Assert.assertEquals(100, pb.value(5));
    }

    @Test
    public void testValue4() throws Exception {
        SquareBackOff pb = new SquareBackOff(0, 16, 4);
        Assert.assertEquals(0, pb.value(0));
        Assert.assertEquals(1, pb.value(1));
        Assert.assertEquals(4, pb.value(2));
        Assert.assertEquals(9, pb.value(3));
        Assert.assertEquals(16, pb.value(4));
    }

    @Test
    public void testValue5() throws Exception {
        SquareBackOff pb = new SquareBackOff(60000, 3600000, 4);
        Assert.assertEquals(60000, pb.value(0));
        Assert.assertEquals(281250, pb.value(1));
        Assert.assertEquals(945000, pb.value(2));
        Assert.assertEquals(2051250, pb.value(3));
        Assert.assertEquals(3600000, pb.value(4));
    }

    @Test
    public void testMaxValue() throws Exception {
        SquareBackOff pb = new SquareBackOff(60000, 3600000, 4);
        Assert.assertEquals(3600000, pb.max());
    }

    @Test
    public void testValueSameMinMax() throws Exception {
        SquareBackOff pb = new SquareBackOff(30, 30, 3);
        Assert.assertEquals(30, pb.value(0));
        Assert.assertEquals(30, pb.value(1));
        Assert.assertEquals(30, pb.value(2));
        Assert.assertEquals(30, pb.value(3));
        Assert.assertEquals(30, pb.value(4));
    }

    @Test
    public void testMin() throws Exception {
        Assert.assertEquals(10, new SquareBackOff(10, 60, 5).value(0));
        Assert.assertEquals(0, new SquareBackOff(0, 60, 5).value(0));
        Assert.assertEquals(10, new SquareBackOff(10, 20, 20).value(0));
    }

    @Test
    public void testMax() throws Exception {
        int max = 60;
        int steps = 5;
        SquareBackOff pb = new SquareBackOff(10, max, steps);
        Assert.assertEquals(max, pb.value(steps));
        Assert.assertEquals(max, pb.value(steps + 1));
        Assert.assertEquals(max, pb.value(steps + 2));
    }

    @Test
    public void testIncrement() throws Exception {
        SquareBackOff pb = new SquareBackOff(0, 16, 4);
        Assert.assertEquals(0, pb.value());
        pb.increment();
        Assert.assertEquals(1, pb.value());
        pb.increment();
        Assert.assertEquals(4, pb.value());
        pb.increment();
        Assert.assertEquals(9, pb.value());
        pb.increment();
        Assert.assertEquals(16, pb.value());
        pb.increment();
        Assert.assertEquals(16, pb.value());
    }

    @Test
    public void testReset() throws Exception {
        SquareBackOff pb = new SquareBackOff(0, 16, 4);
        pb.reset();
        pb.increment();
        pb.increment();
        pb.reset();
        Assert.assertEquals(0, pb.value());
    }
}