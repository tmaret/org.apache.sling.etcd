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
package org.apache.sling.discovery.etcd.fsm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;

import junit.framework.Assert;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolConfig;
import org.junit.Test;

public class ContextTest {

    @Test
    public void testIs() throws Exception {
        Context context = new Context(States.GET_CLUSTER, new TestRunnerFactory(), buildThreadPool());
        Assert.assertTrue(context.is(States.GET_CLUSTER));
        Assert.assertFalse(context.is(States.ANNOUNCE));
    }

    @Test
    public void testIsAny() throws Exception {
        Context context = new Context(States.GET_CLUSTER, new TestRunnerFactory(), buildThreadPool());
        Assert.assertTrue(context.isAny(States.ANNOUNCE, States.GET_CLUSTER, States.RUNNING));
        Assert.assertFalse(context.isAny(States.ANNOUNCE, States.RUNNING));
        Assert.assertFalse(context.isAny());

    }

    @Test
    public void testGetState() throws Exception {
        Context context = new Context(States.ANNOUNCE, new TestRunnerFactory(), buildThreadPool());
        Assert.assertEquals(States.ANNOUNCE, context.getState());
    }

    @Test
    public void testNext() throws Exception {
        Context context = new Context(States.GET_CLUSTER, new TestRunnerFactory(), buildThreadPool());
        context.next(Event.CLUSTER_DEFINED);
        Assert.assertEquals(States.ANNOUNCE, context.getState());
    }

    @Test
    public void testInit() throws Exception {
        Context context = new Context(States.GET_CLUSTER, new TestRunnerFactory(), buildThreadPool());
        context.init(States.RUNNING);
        Assert.assertEquals(States.RUNNING, context.getState());
    }

    @Test
    public void testStartThreadWithInit() throws Exception {
        TestRunnerFactory factory = new TestRunnerFactory();
        Context context = new Context(States.GET_CLUSTER, factory, buildThreadPool());
        Assert.assertEquals(0, factory.runners.size());
        context.init(States.GET_CLUSTER);
        Assert.assertEquals(1, factory.runners.size());
    }

    @Test
    public void testStartOrReuseThreadWithNext() throws Exception {
        TestRunnerFactory factory = new TestRunnerFactory();
        Context context = new Context(States.GET_CLUSTER, factory, buildThreadPool());
        Assert.assertEquals(0, factory.runners.size());
        context.next(Event.CLUSTER_CREATED);
        Assert.assertEquals(1, factory.runners.size());
        context.next(Event.ANNOUNCE_CLEARED);
        Assert.assertEquals(1, factory.runners.size());
    }

    @Test
    public void testStopWhenRunning() throws Exception {
        TestRunnerFactory factory = new TestRunnerFactory();
        Context context = new Context(States.GET_CLUSTER, factory, buildThreadPool());
        context.next(Event.CLUSTER_DEFINED);
        Assert.assertEquals(1, factory.runners.size());
        context.next(Event.ANNOUNCED);
        Assert.assertEquals(1, factory.runners.size());
        context.next(Event.ANNOUNCE_REFRESH_FAILED_WITH_NO_KEY);
        Assert.assertEquals(2, factory.runners.size());
    }

    @Test
    public void testStartThread() throws Exception {
        TestRunnerFactory factory = new TestRunnerFactory();
        Context context = new Context(States.GET_CLUSTER, factory, buildThreadPool());
        context.next(Event.CLUSTER_DEFINED);
        Assert.assertEquals(1, factory.runners.size());
        TestRunner runner = factory.runners.get(0);
        Assert.assertNotNull(runner);
        Thread.sleep(100); // for the thread to start
        Assert.assertTrue(runner.hasInvokedStart());
    }

    @Test
    public void testStopThread() throws Exception {
        TestRunnerFactory factory = new TestRunnerFactory();
        Context context = new Context(States.GET_CLUSTER, factory, buildThreadPool());
        context.next(Event.CLUSTER_DEFINED);
        Assert.assertEquals(1, factory.runners.size());
        context.next(Event.STOPPED);
        Assert.assertTrue(factory.runners.get(0).hasInvokedStop());
    }

    private class TestRunnerFactory implements RunnerFactory {

        volatile List<TestRunner> runners = new ArrayList<TestRunner>();

        @Nonnull
        public Runner build(@Nonnull Context context) {
            TestRunner runner = new TestRunner();
            runners.add(runner);
            return runner;
        }
    }

    private class TestRunner implements Runner {

        volatile boolean stoppedInvoked = false;

        volatile boolean startedInvoked = false;

        public void stop() {
            stoppedInvoked = true;
        }

        public void run() {
            startedInvoked = true;
        }

        public boolean hasInvokedStart() {
            return startedInvoked;
        }

        public boolean hasInvokedStop() {
            return stoppedInvoked;
        }
    }

    private ThreadPool buildThreadPool() {
        final ExecutorService executor = Executors.newCachedThreadPool();
        return new ThreadPool(){
            @Override
            public void execute(Runnable runnable) {
                executor.execute(runnable);
            }

            @Override
            public String getName() {
                return "Test Thread Pool";
            }

            @Override
            public ThreadPoolConfig getConfiguration() {
                return null;
            }
        };
    }

}