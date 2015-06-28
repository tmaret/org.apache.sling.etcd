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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.etcd.client.EtcdNode;
import org.apache.sling.etcd.client.impl.EtcdNodeImpl;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

public class EtcdNodeBuilder {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX";

    final Map<String, Object> params = new HashMap<String, Object>();

    public EtcdNodeBuilder key(String key) {
        params.put("key", key);
        return this;
    }

    public EtcdNodeBuilder expiration(Calendar expiration) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        String formatted = sdf.format(expiration.getTime());
        params.put("expiration", formatted);
        return this;
    }

    public EtcdNodeBuilder ttl(long ttl) {
        params.put("ttl", ttl);
        return this;
    }

    public EtcdNodeBuilder modifiedIndex(long modifiedIndex) {
        params.put("modifiedIndex", modifiedIndex);
        return this;
    }

    public EtcdNodeBuilder createdIndex(long createdIndex) {
        params.put("createdIndex", createdIndex);
        return this;
    }

    public EtcdNodeBuilder value(String value) {
        params.put("value", value);
        return this;
    }

    public EtcdNodeBuilder nodes(List<EtcdNode> nodes) {
        JSONArray children = new JSONArray();
        for (EtcdNode node : nodes) {
            try {
                children.put(new JSONObject(node.toJson()));
            } catch (JSONException ignore) {
                // ignore
            }
        }
        params.put("nodes", children);
        return this;
    }

    public EtcdNode build() {
        JSONObject data = new JSONObject(params);
        return new EtcdNodeImpl(data);
    }


}
