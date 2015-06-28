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
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServlet;

import org.apache.sling.etcd.testing.Etcd;
import org.apache.sling.etcd.testing.EtcdHandler;
import org.apache.sling.etcd.common.ErrorCodes;
import junit.framework.Assert;
import org.apache.sling.discovery.TopologyView;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClusterTest {

    private Server server;

    private volatile ExecutorService executor;

    private EtcdHandler handler;

    private List<Instance> instances;

    @Before
    public void setUp() throws Exception {
        executor = Executors.newCachedThreadPool();
        handler = new EtcdHandler(new Etcd());
        server = startServer(handler, "/v2/keys/*");
        instances = new ArrayList<Instance>();
    }

    @After
    public void tearDown() throws Exception {
        if(executor != null) {
            executor.shutdownNow();
        }
        if (instances != null) {
            stopInstances(instances);
        }
        if (server != null) {
            server.stop();
        }
    }

    @Test(timeout = 5000L)
    public void testStartOneInstance() throws Exception {
        // start instance
        String slingId = UUID.randomUUID().toString();
        Instance instance = startInstance(slingId, "default-cluster", 9000);
        // wait for the view to be established
        waitForEstablishedView(instances);
        // assert
        Assert.assertEquals(slingId, instance.topology().getLocalInstance().getSlingId());
    }

    @Test(timeout = 10000L)
    public void testStartTwentyInstancesInParallel() throws Exception {
        // start instances
        for (int i = 0 ; i < 20 ; i++) {
            startInstance(UUID.randomUUID().toString(), "default-cluster", 9000 + i);
        }
        // on all instances, wait for the view
        waitForEstablishedView(instances);
        // assert all the instances are in the same cluster
        String clusterId = null;
        for (Instance instance : instances) {
            if(clusterId == null) {
                clusterId = instance.clusterViewId();
            } else {
                Assert.assertEquals(clusterId, instance.clusterViewId());
            }
        }
    }

    @Test(timeout = 10000L)
    public void testStartTenInstancesWithSameSlingIdConcurrently() throws Exception {
        String slingId = UUID.randomUUID().toString();
        // start instances
        for (int i = 0 ; i < 10 ; i++) {
            startInstance(slingId, "default-cluster", 9000 + i);
        }
        waitForOneOrMoreEstablished(instances);
        // assert only one is established
        int current = 0;
        for (Instance instance : instances) {
            if (instance.current()) {
                current++;
            }
        }
        Assert.assertEquals(1, current);
    }

    @Test(timeout = 10000L)
    public void testStartTwoInstancesWithSameSlingIdSequentially() throws Exception {
        String slingId = UUID.randomUUID().toString();
        // start instance 1
        Instance i1 = startInstance(slingId, "default-cluster", 9000);
        // wait for the instance 1 to be established
        waitForEstablishedView(Collections.singletonList(i1));
        int nbEvents = i1.getListeners().get(0).getHistory().size();
        // start instance 2
        Instance i2 = startInstance(slingId, "default-cluster", 9001);
        // assert that the instance 1 view is still established and instance 2 is not
        Thread.sleep(2000L);
        Assert.assertTrue(i1.current());
        Assert.assertTrue(i1.instancesInView(1));
        Assert.assertFalse(i2.current());
        Assert.assertEquals(nbEvents, i1.getListeners().get(0).getHistory().size());
    }

    @Test(timeout = 10000L)
    public void testRemovingOneInstance() throws Exception {
        // start instances
        for (int i = 0 ; i < 50 ; i++) {
            startInstance(UUID.randomUUID().toString(), "default-cluster", 9000 + i);
        }
        waitForEstablishedView(instances); // on all instances
        // remove first instance
        Instance first = instances.remove(0);
        first.getEtcdDiscoveryService().deactivate();
        waitForEstablishedView(instances); // on all instances except the one we just removed

    }

    @Test(timeout = 10000L)
    public void testRemovingLeaderInstance() throws Exception {
        // start instances
        for (int i = 0 ; i < 20 ; i++) {
            startInstance(UUID.randomUUID().toString(), "default-cluster", 9000 + i);
        }
        waitForEstablishedView(instances); // on all instances
        // remove leader instance
        Instance leader = null;
        for (Instance instance : instances) {
            if (instance.localInstance().isLeader()) {
                leader = instance;
            }
        }
        if (leader == null) {
            throw new Exception("Failed to find the leader instance");
        }
        instances.remove(leader);
        leader.getEtcdDiscoveryService().deactivate();
        waitForEstablishedView(instances); // on all instances except the one we just removed
    }

    @Test(timeout = 10000L)
    public void testMultiClusterViews() throws Exception {
        // start instances
        List<Instance> clusterOne = new ArrayList<Instance>();
        for (int i = 0 ; i < 5 ; i++) {
            clusterOne.add(startInstance(UUID.randomUUID().toString(), "cluster-1", 9000 + i));
        }
        List<Instance> clusterTwo = new ArrayList<Instance>();
        for (int i = 10 ; i < 15 ; i++) {
            clusterTwo.add(startInstance(UUID.randomUUID().toString(), "cluster-2", 9000 + i));
        }
        waitForEstablishedView(clusterOne);
        waitForEstablishedView(clusterTwo);
        TopologyView topologyView = clusterOne.get(0).getEtcdDiscoveryService().getTopology();
        Assert.assertNotNull(topologyView);
        Assert.assertEquals(2, topologyView.getClusterViews().size());
    }

    @Test(timeout = 10000L)
    public void testRemoveInstancesInBatch() throws Exception {
        // start instances
        for (int i = 0 ; i < 20 ; i++) {
            startInstance(UUID.randomUUID().toString(), "default-cluster", 9000 + i);
        }
        waitForEstablishedView(instances); // on all instances
        // remove half of the instances
        for (int i = 0 ; i < instances.size() ; i++) {
            Instance remove = instances.remove(i);
            remove.getEtcdDiscoveryService().deactivate();
        }
        waitForEstablishedView(instances); // on remaining instances
    }

    @Test(timeout = 10000L)
    public void testPropagationOfAddedProperties() throws Exception {
        // start instances
        String slingIdOne = UUID.randomUUID().toString();
        Instance i1 = startInstance(slingIdOne, "default-cluster", 9000);
        i1.bindPropertyProvider(Collections.<String, Object>singletonMap("prop", "v1"), 10, 100);

        String slingIdTwo = UUID.randomUUID().toString();
        Instance i2 = startInstance(slingIdTwo, "default-cluster", 9001);
        i2.bindPropertyProvider(Collections.<String, Object>singletonMap("prop", "v2"), 10, 100);

        // wait for the view to establish
        for ( ; ! i1.current() || ! i1.instancesInView(2) || ! i1.propertyValue(slingIdOne, "prop", "v1") ||
                ! i2.current() || ! i2.instancesInView(2) || ! i2.propertyValue(slingIdTwo, "prop", "v2") ; ) {
            Thread.sleep(250);
        }
        // change properties on instance 1
        i1.bindPropertyProvider(Collections.<String, Object>singletonMap("new-prop", "value"), 11, 101);
        // wait for the new property to propagate
        for ( ; ! i1.propertyValue(slingIdOne, "new-prop", "value") ||
                ! i2.propertyValue(slingIdOne, "new-prop", "value") ; ) {
            Thread.sleep(250);
        }
    }

    @Test(timeout = 10000L)
    public void testPropagationOfModifiedProperties() throws Exception {
        // start instances
        String slingIdOne = UUID.randomUUID().toString();
        Instance i1 = startInstance(slingIdOne, "default-cluster", 9000);
        i1.bindPropertyProvider(Collections.<String, Object>singletonMap("prop", "v1"), 10, 100);

        String slingIdTwo = UUID.randomUUID().toString();
        Instance i2 = startInstance(slingIdTwo, "default-cluster", 9001);
        i2.bindPropertyProvider(Collections.<String, Object>singletonMap("prop", "v2"), 10, 100);

        // wait for the view to establish
        for ( ; ! i1.current() || ! i1.instancesInView(2) || ! i1.propertyValue(slingIdOne, "prop", "v1") ||
                ! i2.current() || ! i2.instancesInView(2) || ! i2.propertyValue(slingIdTwo, "prop", "v2") ; ) {
            Thread.sleep(250);
        }
        // change properties on instance 1
        i1.bindPropertyProvider(Collections.<String, Object>singletonMap("prop", "v1-modified"), 11, 101);
        // wait for the new property to propagate
        for ( ; ! i1.propertyValue(slingIdOne, "prop", "v1-modified") ||
                ! i2.propertyValue(slingIdOne, "prop", "v1-modified") ; ) {
            Thread.sleep(250);
        }
    }

    @Test(timeout = 10000L)
    public void testStartTenInstancesWithIoTimeout() throws Exception {
        handler.setProcessingDelay(300); // greater than the connection timeout
        // start instances
        for (int i = 0 ; i < 10 ; i++) {
            startInstance(UUID.randomUUID().toString(), "default-cluster", 9000 + i);
        }
        Thread.sleep(2000);
        handler.setProcessingDelay(0); // disable processing delay
        waitForEstablishedView(instances); // on all instances
    }

    @Test(timeout = 10000L)
    public void testStartTenInstancesWithErrors() throws Exception {
        handler.setErrors(100, ErrorCodes.RAFT_INTERNAL_ERROR, ErrorCodes.LEADER_ELECTION_ERROR);
        // start instances
        for (int i = 0 ; i < 10 ; i++) {
            startInstance(UUID.randomUUID().toString(), "default-cluster", 9000 + i);
        }
        Thread.sleep(1500);
        handler.setErrors(0);
        waitForEstablishedView(instances); // on all instances
    }

    @Test(timeout = 10000L)
    public void testRunningTenInstancesWithIoTimeout() throws Exception {
        // start instances
        for (int i = 0 ; i < 10 ; i++) {
            startInstance(UUID.randomUUID().toString(), "default-cluster", 9000 + i);
        }
        waitForEstablishedView(instances);
        handler.setProcessingDelay(300); // greater than the connection timeout
        Thread.sleep(1500);
        handler.setProcessingDelay(0); // disable processing delay
        // wait for the view to establish
        waitForEstablishedView(instances);
    }

    @Test(timeout = 10000L)
    public void testRunningTenInstancesWithEtcdError() throws Exception {
        // start instances
        for (int i = 0 ; i < 10 ; i++) {
            startInstance(UUID.randomUUID().toString(), "default-cluster", 9000 + i);
        }
        waitForEstablishedView(instances);
        handler.setErrors(100, ErrorCodes.RAFT_INTERNAL_ERROR, ErrorCodes.LEADER_ELECTION_ERROR);
        Thread.sleep(1500);
        handler.setErrors(0);
        waitForEstablishedView(instances);
    }

    @Test(timeout = 10000L)
    public void testStartingTenInstancesWithRandomErrors_50_150() throws Exception {
        testStartingInstanceWithRandomErrors(50, 150);
    }

    @Test(timeout = 10000L)
    public void testStartingTenInstancesWithRandomErrors_90_150() throws Exception {
        testStartingInstanceWithRandomErrors(90, 150);
    }

    @Test(timeout = 10000L)
    public void testStartingTenInstancesWithRandomErrors_50_290() throws Exception {
        testStartingInstanceWithRandomErrors(50, 290);
    }

    @Test(timeout = 10000L)
    public void testStartingTenInstancesWithRandomErrors_90_290() throws Exception {
        testStartingInstanceWithRandomErrors(90, 290);
    }

    @Test(timeout = 10000L)
    public void testRunningTenInstancesWithRandomErrors_50_150() throws Exception {
        testRunningInstanceWithRandomErrors(50, 150);
    }

    @Test(timeout = 10000L)
    public void testRunningTenInstancesWithRandomErrors_90_150() throws Exception {
        testRunningInstanceWithRandomErrors(90, 150);
    }

    @Test(timeout = 10000L)
    public void testRunningTenInstancesWithRandomErrors_50_290() throws Exception {
        testRunningInstanceWithRandomErrors(50, 290);
    }

    @Test(timeout = 10000L)
    public void testRunningTenInstancesWithRandomErrors_90_290() throws Exception {
        testRunningInstanceWithRandomErrors(90, 290);
    }

    private void testStartingInstanceWithRandomErrors(int frequency, int minDelay) throws Exception {

        handler.setErrors(frequency, ErrorCodes.RAFT_INTERNAL_ERROR, ErrorCodes.LEADER_ELECTION_ERROR,
                ErrorCodes.KEY_NOT_FOUND, ErrorCodes.TEST_FAILED);
        handler.setProcessingDelay(minDelay, 300);

        for (int i = 0 ; i < 10 ; i++) {
            startInstance(UUID.randomUUID().toString(), "default-cluster", 9000 + i);
        }

        Thread.sleep(3000);

        handler.setErrors(0);
        handler.setProcessingDelay(0);

        waitForEstablishedView(instances);
    }

    private void testRunningInstanceWithRandomErrors(int frequency, int minDelay) throws Exception {

        for (int i = 0 ; i < 10 ; i++) {
            startInstance(UUID.randomUUID().toString(), "default-cluster", 9000 + i);
        }

        waitForEstablishedView(instances);

        handler.setErrors(frequency, ErrorCodes.RAFT_INTERNAL_ERROR, ErrorCodes.LEADER_ELECTION_ERROR,
                ErrorCodes.KEY_NOT_FOUND, ErrorCodes.TEST_FAILED);
        handler.setProcessingDelay(minDelay, 300);

        Thread.sleep(3000);

        handler.setErrors(0);
        handler.setProcessingDelay(0);

        waitForEstablishedView(instances);
    }

    private Instance startInstance(String slingId, String clusterId, int instancePort) throws Exception {
        Instance instance = new Instance(
                instancePort,
                null,
                null,
                clusterId,
                "/discovery",
                "250",           /* announce renewal period */
                "250",           /* topology update period  */
                "200",           /* view update period      */
                250,             /* connection timeout      */
                250,             /* socket timeout          */
                "250:500:2",     /* etcd back-off           */
                "250:500:2",     /* io error back-off       */
                slingId,
                "http://localhost:" + serverPort(server));
        instances.add(instance);
        executor.submit(instance);
        return instance;
    }

    private void waitForOneOrMoreEstablished(List<Instance> instances) throws InterruptedException  {
        for (;;) {
            for (Instance instance : instances) {
                if (instance.current()) {
                    return;
                }
            }
            Thread.sleep(250);
        }
    }

    private void waitForEstablishedView(List<Instance> instances) throws InterruptedException {
        int size = instances.size();
        boolean established = false;
        for ( ; ! established ; ) {
            boolean ie = true;
            for (Instance instance : instances) {
                ie &= instance.current() && instance.instancesInView(size);
            }
            established = ie;
            if (! established) {
                Thread.sleep(250);
            }
        }
    }

    private void stopInstances(List<Instance> instances) {
        for (Instance instance : instances) {
            instance.stop();
        }
    }

    private static Server startServer(HttpServlet servlet, String pathSpec)
            throws Exception {
        Server server = new Server();
        server.setConnectors(new Connector[]{new SelectChannelConnector()});
        ServletContextHandler sch = new ServletContextHandler(null, "/", false, false);
        sch.addServlet(new ServletHolder(servlet), pathSpec);
        server.setHandler(sch);
        server.start();
        return server;
    }

    private static int serverPort(Server server) {
        return server.getConnectors()[0].getLocalPort();
    }

}