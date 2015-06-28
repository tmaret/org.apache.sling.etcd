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
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

/**
 * The {@code AnnouncesMap} aggregates the announces for the local and remote instances.
 */
public class AnnouncesMap {

    private Announce local;

    private Announces remote;

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * @param local the initial local instance announce
     */
    public AnnouncesMap(@Nonnull Announce local) {
        remote = new Announces();
        this.local = local;
    }

    /**
     * Get the list of announces for local and remote instances.
     *
     * @return the announces
     */
    @Nonnull
    public Announces getAll() {
        List<Announce> all = new ArrayList<Announce>();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            all.add(local);
            all.addAll(remote.getAnnounces());
        } finally {
            lock.unlock();
        }
        return new Announces(all);
    }

    /**
     * Get the announce for local instance
     *
     * @return the announce
     */
    @Nonnull
    public Announce getLocal() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return local;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Set the local announce
     *
     * @param announce the local announce
     */
    public void setLocal(@Nonnull Announce announce) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            local = announce;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the announces for remote instances
     *
     * @return the announces
     */
    @Nonnull
    public Announces getRemote () {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return remote;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Set the remote announces
     *
     * @param announces the remote announces
     */
    public void setRemote(@Nonnull Announces announces) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            remote = announces;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return "AnnouncesMap{" +
                "local=" + local +
                ", remote=" + remote +
                '}';
    }
}
