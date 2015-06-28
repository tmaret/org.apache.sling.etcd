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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

/**
 * Represent an etcd folder.
 */
public class Folder extends Node {

    private final SortedMap<String, Node> children = new TreeMap<String, Node>(new IntegerStringComparator());

    public Folder(@Nonnull String name, @Nullable Integer ttl, long modifiedIndex) {
        super(name, ttl, modifiedIndex);
    }

    @Nonnull
    public List<Node> children(boolean sorted) {
        List<Node> nodes = new ArrayList<Node>(children.values());
        if (! sorted) {
            Collections.shuffle(nodes);
        }
        return Collections.unmodifiableList(filterByTtl(nodes));
    }

    @Nullable
    public Node child(@Nonnull String name) {
        return filterByTtl(children.get(name));
    }

    @Nullable
    public Node putChild(@Nonnull Node child, long modifiedIndex) {
        this.modifiedIndex = modifiedIndex;
        child.parent(this);
        return filterByTtl(children.put(child.name(), child));
    }

    @Nullable
    public Node removeChild(@Nonnull String name, long modifiedIndex) {
        this.modifiedIndex = modifiedIndex;
        Node removed = children.remove(name);
        return filterByTtl(removed);
    }

    @Nonnull
    public JSONObject toJson(boolean recursive, boolean sorted)
            throws JSONException {

        JSONObject data = toJson();
        JSONArray nodes = new JSONArray();

        for (Node child : children(sorted)) {
            if (! recursive && child.isFolder()) {
                JSONObject childData = ((Folder)child).toJson();
                childData.putOpt("dir", true);
                nodes.put(childData);
            } else {
                nodes.put(child.toJson(recursive, sorted));
            }
        }

        data.put("nodes", nodes);
        return data;
    }

    public boolean isFolder() {
        return true;
    }

    private JSONObject toJson()
            throws JSONException {
        JSONObject data = super.toJson(false, false);
        return data.putOpt("dir", true);
    }

    private class IntegerStringComparator implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            Integer i1 = integer(o1);
            Integer i2 = integer(o2);
            if (i1 != null && i2 != null) {
                return i1.compareTo(i2);
            } else {
                return o1.compareTo(o2);
            }
        }

        private Integer integer(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

}