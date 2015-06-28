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
package org.apache.sling.etcd.testing.tree;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

/**
 * Represent an etcd node (key or folder).
 */
public abstract class Node {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX";

    private final String name;

    private Long timeout;

    protected long modifiedIndex;

    private Node parent;

    public Node(@Nonnull String name, @Nullable Integer ttl, long modifiedIndex) {
        this.name = checkName(name);
        this.modifiedIndex = modifiedIndex;
        ttl(ttl, modifiedIndex);
    }

    /**
     * @return {@code true} if the node is a folder, {@code false} otherwise.
     */
    public abstract boolean isFolder();

    public long modifiedIndex() {
        return modifiedIndex;
    }

    /**
     * @return the etcd node name
     */
    @Nonnull
    public String name() {
        return name;
    }

    /**
     * @return the etcd parent node
     */
    @Nullable
    public Node parent() {
        return parent;
    }

    /**
     * @return the etcd key
     */
    @Nonnull
    public String path() {
        return path(path(this, new ArrayDeque<String>()));
    }

    /**
     * Set ttl for the node
     *
     * @param ttl the ttl to be set in seconds
     * @param modifiedIndex the etcd index for the modification
     */
    public void ttl(@Nullable Integer ttl, long modifiedIndex) {
        if (ttl != null && ttl < 0) {
            throw new IllegalArgumentException("ttl must be greater or equal to 0");
        }
        this.modifiedIndex = modifiedIndex;
        this.timeout = (ttl != null) ? System.currentTimeMillis() + ttl * 1000 : null ;
    }

    /**
     * @return the timeout in second or {@code null} if no timeout is set. A timeout of {@code 0} is elapsed.
     */
    @Nullable
    public Integer ttl() {
        if (timeout != null) {
            long now = System.currentTimeMillis();
            BigDecimal delta = new BigDecimal((timeout - now) / 1000.0D);
            // we round up the ttl to 1 if the value in ms is > 1
            return Math.max(0, delta.setScale(0, BigDecimal.ROUND_UP).intValue());
        }
        return null;
    }

    /**
     * @return {@code true} if the ttl is set and has elapsed, {@code false} otherwise.
     */
    public boolean ttlElapsed() {
        Integer ttl = ttl();
        return ttl != null && ttl == 0;
    }

    @Nonnull
    public JSONObject toJson(boolean recursive, boolean sorted)
            throws JSONException {
        JSONObject data = new JSONObject();
        data.put("key", path());
        data.put("createdIndex", modifiedIndex());
        data.put("modifiedIndex", modifiedIndex());
        Integer ttl = ttl();
        if (ttl != null) {
            data.put("ttl", ttl);
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            Date expiration = new Date(timeout);
            String formatted = sdf.format(expiration.getTime());
            data.put("expiration", formatted);
        }
        return data;
    }

    // static utils

    @Nullable
    public static Node filterByTtl(@Nullable Node node) {
        return node != null && ! node.ttlElapsed() ? node : null;
    }

    @Nonnull
    public static List<Node> filterByTtl(@Nonnull List<Node> nodes) {
        List<Node> filtered = new ArrayList<Node>();
        for (Node node : nodes) {
            if (! node.ttlElapsed()) {
                filtered.add(node);
            }
        }
        return filtered;
    }

    @Nonnull
    public static String parent(@Nonnull String path) {
        Deque<String> names = names(path);
        if (! names.isEmpty()) {
            names.removeLast();
        }
        return path(names);
    }

    @Nonnull
    public static String name(@Nonnull String path) {
        Deque<String> names = names(path);
        return names.isEmpty() ? "/" : names.getLast();
    }

    @Nonnull
    public static Deque<String> names(@Nonnull String key) {
        Deque<String> names = new ArrayDeque<String>(Arrays.asList(key.split("/")));
        if (! names.isEmpty()) {
            names.removeFirst();
        }
        return names;
    }

    @Nonnull
    public static String path(@Nonnull Deque<String> names) {
        return "/" + StringUtils.join(names, "/");
    }


    //

    protected void parent(@Nullable Node parent) {
        this.parent = parent;
    }

    @Nonnull
    private Deque<String> path(@Nonnull Node node, @Nonnull Deque<String> acc) {
        Node parent = node.parent();
        if (parent != null) {
            acc.addFirst(node.name());
            return path(parent, acc);
        } else {
            return acc;
        }
    }

    private String checkName(@Nonnull String name) {
        if (name.contains("/")) {
            throw new IllegalArgumentException("name must not contain the char '/'");
        }
        return name;
    }

}