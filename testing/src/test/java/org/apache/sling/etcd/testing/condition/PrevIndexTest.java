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
package org.apache.sling.etcd.testing.condition;

import org.apache.sling.etcd.testing.EtcdException;
import org.apache.sling.etcd.testing.tree.Key;
import org.junit.Test;

public class PrevIndexTest {

    @Test(expected = EtcdException.class)
    public void testNodeIsNull() throws Exception {
        PrevIndex c = new PrevIndex(10L);
        c.check("/some/key", null, 10);
    }

    @Test
    public void testRequiredIndexIsZero() throws Exception {
        PrevIndex c = new PrevIndex(0L);
        c.check("/some/key", new Key("key", "value", null, 4), 10);
    }

    @Test
    public void testRequiredIndexMatches() throws Exception {
        PrevIndex c = new PrevIndex(10L);
        c.check("/some/key", new Key("key", "value", null, 10), 35);
    }

    @Test(expected = EtcdException.class)
    public void testRequiredIndexDoesNotMatch() throws Exception {
        PrevIndex c = new PrevIndex(10L);
        c.check("/some/key", new Key("key", "value", null, 5), 35);
    }
}