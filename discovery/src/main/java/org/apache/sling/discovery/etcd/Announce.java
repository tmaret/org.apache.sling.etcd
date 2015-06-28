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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The {@code Announce} aggregates an instance announce key, announce data and ttl.
 * The {@code Announce} is comparable following the etcd created key ordering.
 * The key are compared on their name using Long natural order.
 * As an example, the announceKey: <b>/announces/207</b> has the name: <b>207</b> which
 * is compared numerically.
 */
public class Announce implements Comparable<Announce> {

    /**
     * The announce key
     */
    private final String announceKey;

    /**
     * The etcd created key name
     */
    private final long keyName;

    /**
     * The announce data
     */
    private final AnnounceData data;

    /**
     * The announce ttl (optional)
     */
    private final Long ttl;

    /**
     * Create a new {@code Announce} announce, setting the ttl to {@code null}.
     *
     * @param data the announce data
     * @param announceKey the announce key
     */
    public Announce(@Nonnull AnnounceData data, @Nonnull String announceKey) {
        keyName = keyName(announceKey);
        this.announceKey = announceKey;
        this.data = data;
        this.ttl = null;
    }

    /**
     * Create a new {@code Announce} announce.
     *
     * @param data the announce data
     * @param announceKey the announce key
     * @param ttl the announce ttl (possibly {@code null})
     */
    public Announce(@Nonnull AnnounceData data, @Nonnull String announceKey, @Nullable Long ttl) {
        keyName = keyName(announceKey);
        this.announceKey = announceKey;
        this.data = data;
        this.ttl = ttl;
    }

    /**
     * @return the announce key
     */
    @Nonnull
    public String getAnnounceKey() {
        return announceKey;
    }

    /**
     * @return the announce data
     */
    @Nonnull
    public AnnounceData getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Announce{" +
                "announceKey='" + announceKey + '\'' +
                ", data=" + data +
                '}';
    }

    /**
     * @return the announce ttl or {@code null} if not defined
     */
    @Nullable
    public Long getTtl() {
        return ttl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Announce announce = (Announce) o;
        if (!announceKey.equals(announce.announceKey)) return false;
        if (!data.equals(announce.data)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = announceKey.hashCode();
        result = 31 * result + data.hashCode();
        return result;
    }

    @Override
    public int compareTo(@Nonnull Announce that) {
        if (this.keyName < that.keyName) {
            return -1;
        } else if (this.keyName > that.keyName) {
            return 1;
        }
        return 0;
    }

    //

    private long keyName(@Nonnull String announceKey) {
        String keyName = announceKey.substring(announceKey.lastIndexOf('/') + 1);
        return Long.valueOf(keyName);
    }

}
