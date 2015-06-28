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

import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.discovery.etcd.EtcdDiscoveryService;
import org.apache.sling.commons.threads.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Context {

    private static final Logger LOG = LoggerFactory.getLogger(Context.class);

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * The etcd discovery service internal state.
     */
    private volatile State state;

    /**
     * Factory that allows to build context runner runnable.
     */
    private final RunnerFactory factory;

    /**
     * Thread pool to run the state runner thread.
     */
    private final ThreadPool etcdThreadPool;

    /**
     * Runner that implements joining the etcd discovery cluster
     */
    private Runner stateRunner;

    /**
     * The cluster identifier once init.
     */
    private volatile String clusterId;

    /**
     * @param state the initial state.
     */
    public Context(@Nonnull State state, @Nonnull RunnerFactory factory, @Nonnull ThreadPool etcdThreadPool) {
        this.etcdThreadPool = etcdThreadPool;
        this.state = state;
        this.factory = factory;
    }

    /**
     * Initialize the state using the given value.
     *
     * @param state the state to initialize.
     */
    public void init(@Nonnull State state) {
        final ReentrantLock lock = this.lock;
        LOG.info("init state: {}", state);
        lock.lock();
        try {
            this.state = state;
            thread(state);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Moves the context to the next state according to the event provided.
     *
     * @param event the event for deciding on the next state.
     */
    public void next(@Nonnull Event event) {
        final ReentrantLock lock = this.lock;
        final State past;
        final State next;
        lock.lock();
        try {
            past = state;
            next = state.next(event);
            state = next;
            thread(next);
        } finally {
            lock.unlock();
        }
        LOG.info("in state: {} from state: {} (event: {})", new Object[]{this.state, past, event});
    }

    /**
     * @return the current context state.
     */
    @Nonnull
    public State getState() {
        return state;
    }

    /**
     * Check whether the context is in a given state.
     *
     * @param state the state to check
     * @return {@code true} if the context is in the state provided ; {@code false} otherwise.
     */
    public boolean is(@Nonnull State state) {
        return this.state == state;
    }

    /**
     * Check whether the context is in any of the given states.
     *
     * @param states the states to check
     * @return {@code true} if the context is in any of the states provided ; {@code false} otherwise.
     */
    public boolean isAny(@Nonnull State... states) {
        for (State state : states) {
            if (this.state == state) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set the cluster identifier.
     *
     * @param clusterId the cluster identifier.
     */
    public void setClusterId(@Nonnull String clusterId) {
        this.clusterId = clusterId;
    }

    /**
     * @return the cluster identifier ; or {@code null} if not defined.
     */
    @Nullable
    public String getClusterId() {
        return clusterId;
    }

    private void thread(@Nonnull State state) {
        if (state == States.STOP) {
            LOG.info("Stop the discovery processing. Re-activate the component '{}' in order to start it again.", EtcdDiscoveryService.class);
            stopRunner();
        } else if (state == States.RUNNING) {
            stopRunner();
        } else {
            startRunnerIfNeeded();
        }
    }

    private void startRunnerIfNeeded() {
        if (stateRunner == null) {
            LOG.debug("Start state runner thread");
            stateRunner = factory.build(this);
            etcdThreadPool.execute(stateRunner);
        }
    }

    private void stopRunner() {
        if (stateRunner != null) {
            LOG.debug("Stop state runner thread");
            stateRunner.stop();
            stateRunner = null;
        }
    }

}
