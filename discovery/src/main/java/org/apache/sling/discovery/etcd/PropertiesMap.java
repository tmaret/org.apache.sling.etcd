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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

/**
 * The {@code PropertiesMap} aggregates the properties for the local and remote instances.
 */
public class PropertiesMap {

    private Map<String, String> local;

    private Map<String, Map<String, String>> remote;

    private final String slingId;

    private final ReentrantLock lock = new ReentrantLock();

    public PropertiesMap (@Nonnull String slingId) {
        this.slingId = slingId;
        remote = Collections.emptyMap();
        local = Collections.emptyMap();
    }

    @Nonnull
    public Map<String, Map<String, String>> getAll() {
        Map<String, Map<String, String>> all = new HashMap<String, Map<String, String>>();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            all.putAll(remote);
            all.put(slingId, local);
        } finally {
            lock.unlock();
        }
        return all;
    }

    @Nonnull
    public Map<String, String> getLocal() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return local;
        } finally {
            lock.unlock();
        }
    }

    public void setLocal(@Nonnull Map<String, String> properties) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            local = properties;
        } finally {
            lock.unlock();
        }
    }

    @Nonnull
    public Map<String, Map<String, String>> getRemote() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return remote;
        } finally {
            lock.unlock();
        }
    }

    public void setRemote(@Nonnull Map<String, Map<String, String>> properties) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            remote = properties;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return "PropertiesMap{" +
                "slingId='" + slingId + '\'' +
                ", local=" + local +
                ", remote=" + remote +
                '}';
    }
}
