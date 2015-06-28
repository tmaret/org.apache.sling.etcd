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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import junit.framework.Assert;
import org.junit.Test;

public class AnnounceTest {

    @Test
    public void createAnnouncementWithValues() throws Exception {
        Announce ann = new Announce(new AnnounceData("sling-id", "server-info", "default-cluster", 10), "/announces/123");
        Assert.assertEquals("/announces/123", ann.getAnnounceKey());
        Assert.assertEquals(10, ann.getData().propertiesModifiedIndex());
        Assert.assertEquals("server-info", ann.getData().serverInfo());
        Assert.assertEquals("sling-id", ann.getData().slingId());
    }

    @Test
    public void testEquals() throws Exception {
        AnnounceData data1 = new AnnounceData("sling-id", "server-id", "default-cluster", 10);
        Announce ann1 = new Announce(data1, "/key/1");
        AnnounceData data2 = new AnnounceData("sling-id", "server-id", "default-cluster", 10);
        Announce ann2 = new Announce(data2, "/key/1");
        Assert.assertTrue(ann1.equals(ann2) & ann2.equals(ann1));
        Assert.assertTrue((ann1.hashCode() == ann2.hashCode()));
        Announce ann3 = new Announce(data2, "/another/key/1");
        Assert.assertFalse(ann1.equals(ann3) | ann3.equals(ann1));
        AnnounceData data4 = new AnnounceData("other-sling-id", "server-id", "default-cluster", 10);
        Announce ann4 = new Announce(data4, "/key/1");
        Assert.assertFalse(ann1.equals(ann4) | ann4.equals(ann1));
    }

    @Test
    public void testSortAnnounces() throws Exception {
        List<Announce> announces = new ArrayList<Announce>(Arrays.asList(
                new Announce(new AnnounceData("sling-id-1", "localhost:80", "cluster-id", 10), "/announces/1"),
                new Announce(new AnnounceData("sling-id-3", "localhost:80", "cluster-id", 10), "/announces/3"),
                new Announce(new AnnounceData("sling-id-4", "localhost:80", "cluster-id", 10), "/announces/50"),
                new Announce(new AnnounceData("sling-id-2", "localhost:80", "cluster-id", 10), "/announces/100"),
                new Announce(new AnnounceData("sling-id-5", "localhost:80", "cluster-id", 10), "/announces/5000")));
        List<Announce> shuffled = new ArrayList<Announce>(announces);
        Collections.shuffle(shuffled);
        List<Announce> back = new ArrayList<Announce>(new TreeSet<Announce>(shuffled));
        for (int i = 0 ; i < announces.size() ; i++) {
            Assert.assertEquals(announces.get(i).getAnnounceKey(), back.get(i).getAnnounceKey());
        }
    }

    @Test
    public void testCompareTo() throws Exception {
        List<Announce> announces = new ArrayList<Announce>(Arrays.asList(
                new Announce(new AnnounceData("sling-id-0", "localhost:80", "cluster-id", 10), "/announces/0"),
                new Announce(new AnnounceData("sling-id-1", "localhost:80", "cluster-id", 10), "/announces/1"),
                new Announce(new AnnounceData("sling-id-3", "localhost:80", "cluster-id", 10), "/announces/3"),
                new Announce(new AnnounceData("sling-id-3", "localhost:80", "cluster-id", 10), "/announces/23"),
                new Announce(new AnnounceData("sling-id-3", "localhost:80", "cluster-id", 10), "/announces/31"),
                new Announce(new AnnounceData("sling-id-4", "localhost:80", "cluster-id", 10), "/announces/50"),
                new Announce(new AnnounceData("sling-id-2", "localhost:80", "cluster-id", 10), "/announces/100"),
                new Announce(new AnnounceData("sling-id-5", "localhost:80", "cluster-id", 10), "/announces/5000")));
        for (Announce x : announces) {
            for (Announce y : announces) {
                Assert.assertTrue(Math.signum(x.compareTo(y)) == -Math.signum(y.compareTo(x)));
                Assert.assertTrue((x.compareTo(y) == 0) == (x.equals(y)));
            }
        }
    }

}