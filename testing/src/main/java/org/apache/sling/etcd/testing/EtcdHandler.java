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
package org.apache.sling.etcd.testing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.etcd.testing.condition.Condition;
import org.apache.sling.etcd.testing.condition.PrevExists;
import org.apache.sling.etcd.testing.condition.PrevIndex;
import org.apache.sling.etcd.testing.condition.PrevValue;
import org.apache.sling.etcd.testing.tree.Key;
import org.apache.sling.etcd.testing.tree.Node;
import org.apache.sling.etcd.common.ErrorCodes;
import org.apache.sling.etcd.common.EtcdHeaders;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

/**
 * HTTP servlet that mocks the etcd REST service for
 * GET, POST, PUT and DELETE under the path: /v2/keys.
 */
public class EtcdHandler extends HttpServlet {

    private static final String CONTEXT = "/v2/keys";

    private final Etcd etcd;

    private final List<Integer> errors = new ArrayList<Integer>();

    private int frequency;

    private int minDelay;

    private int maxDelay;

    private Random random = new Random();

    public EtcdHandler(Etcd etcd) {
        this.etcd = etcd;
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String path = unmap(req.getRequestURI());
        if (path == null) {
            res.setStatus(404);
            return;
        }

        if (parseBoolean(req, "wait")) {
            res.setStatus(501);
            return;
        }

        boolean recursive = parseBoolean(req, "recursive");
        boolean sorted = parseBoolean(req, "sorted");

        setResponseHeaders(res);

        Node node = etcd.getNode(path);

        if (node == null) {
            res.setStatus(404);
            JSONObject error = error(ErrorCodes.KEY_NOT_FOUND, path, etcd.index());
            res.getWriter().write(error.toString());
        } else {
            try {
                JSONObject action = action("get", node, recursive, sorted);
                res.getWriter().write(action.toString());
            } catch (EtcdException e) {
                res.setStatus(403);
                res.getWriter().write(error(e).toString());
            }
        }
    }

    protected void doPut(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String path = unmap(req.getRequestURI());
        if (path == null) {
            res.setStatus(404);
            return;
        }

        boolean dir = parseBoolean(req, "dir");
        String value = req.getParameter("value");
        Integer ttl = parseTtl(req);

        PrevExists prevExists = parsePrevExist(req);
        PrevIndex prevIndex = parsePrevIndex(req);
        PrevValue prevValue = parsePrevValue(req);
        Condition condition = condition(prevExists, prevIndex, prevValue);

        setResponseHeaders(res);

        try {
            final JSONObject action;
            if (dir) {
                Change<Node> change = etcd.putFolder(path, ttl, condition);
                action = action("set", change, res);
            } else {
                Change<Key> change = etcd.putKey(path, value, ttl, condition);
                if (prevExists != null) {
                    boolean exists = prevExists.exists();
                    String type = exists ? "update" : "create";
                    action = action(type, change, res);
                } else if (prevIndex != null) {
                    long index = prevIndex.index();
                    String type = (index == 0) ? "set" : "compareAndSwap";
                    action = action(type, change, res);
                } else if (prevValue != null) {
                    action = action("compareAndSwap", change, res);
                } else {
                    action = action("set", change, res);
                }
            }
            res.getWriter().write(action.toString());
        } catch (EtcdException e) {
            res.setStatus(403);
            res.getWriter().write(error(e).toString());
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String path = unmap(req.getRequestURI());
        if (path == null) {
            res.setStatus(404);
            return;
        }

        Integer ttl = parseTtl(req);
        String value = req.getParameter("value");

        PrevExists prevExists = parsePrevExist(req);
        PrevIndex prevIndex = parsePrevIndex(req);
        PrevValue prevValue = parsePrevValue(req);
        Condition condition = condition(prevExists, prevIndex, prevValue);

        try {
            Key created = etcd.createKey(path, value, ttl, condition);
            setResponseHeaders(res);
            JSONObject action = action("create", created, false, false);
            res.getWriter().write(action.toString());
        } catch (EtcdException e) {
            res.setStatus(403);
            res.getWriter().write(error(e).toString());
        }
    }

    protected void doDelete(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String path = unmap(req.getRequestURI());
        if (path == null) {
            res.setStatus(404);
            return;
        }

        boolean dir = parseBoolean(req, "dir");
        boolean recursive = parseBoolean(req, "recursive");

        PrevIndex prevIndex = parsePrevIndex(req);
        PrevValue prevValue = parsePrevValue(req);
        Condition condition = condition(prevIndex, prevValue);

        try {
            final Node old ;
            if (dir) {
                old = etcd.deleteFolder(path, recursive, condition);
            } else {
                old = etcd.deleteKey(path, condition);
            }
            String type = (condition != null) ? "compareAndDelete" : "delete";
            setResponseHeaders(res);
            JSONObject action = action(type, old, false, false);
            res.getWriter().write(action.toString());
        } catch (EtcdException e) {
            res.setStatus(403);
            res.getWriter().write(error(e).toString());
        }
    }

    public void setProcessingDelay(int minDelay, int maxDelay) {
        this.maxDelay = maxDelay;
        this.minDelay = minDelay;
    }

    public void setProcessingDelay(int delay) {
        maxDelay = delay;
        minDelay = delay;
    }

    public void setErrors(int frequency, Integer... errorCodes) {
        if (frequency < 0 || frequency > 100) {
            throw new IllegalArgumentException("frequency must be greater or equal to 0 and smaller or equal to 100");
        }
        this.frequency = frequency;
        errors.clear();
        errors.addAll(Arrays.asList(errorCodes));
    }

    //

    private void setResponseHeaders(@Nonnull HttpServletResponse res) {
        setIfUndefined(res, EtcdHeaders.ETCD_INDEX, "35");
        setIfUndefined(res, EtcdHeaders.RAFT_INDEX, "5398");
        setIfUndefined(res, EtcdHeaders.RAFT_TERM, "0");
    }

    private void setIfUndefined(@Nonnull HttpServletResponse res, @Nonnull String name, @Nullable String value) {
        if (res.containsHeader(name)) {
            res.setHeader(name, value);
        }
    }

    @Nullable
    private String unmap(@Nonnull String path) {
        return (! path.startsWith(CONTEXT)) ? null : path.substring(CONTEXT.length());
    }

    @Nonnull
    private JSONObject error(@Nonnull EtcdException e)
            throws ServletException {
        return error(e.code(), e.cause(), e.index());
    }

    @Nonnull
    private JSONObject error(int code, @Nonnull String cause, long index)
            throws ServletException {
        JSONObject error = new JSONObject();
        try {
            error.putOpt("errorCode", code);
            error.putOpt("message", String.format("message for errorCode: %s", code));
            error.putOpt("cause", cause);
            error.putOpt("index", index);
            return error;
        } catch (JSONException e) {
            throw new ServletException(e);
        }
    }

    @Nonnull
    private JSONObject action(@Nonnull String type, @Nonnull Change<? extends Node> change, @Nonnull HttpServletResponse res)
            throws ServletException, EtcdException {
        simulateErrors();
        processingDelay();
        if (change.getPrevious() != null) {
            res.setStatus(200);
            return action(type, change.getCurrent(), change.getPrevious(), false, false);
        } else {
            res.setStatus(201);
            return action(type, change.getCurrent(), false, false);
        }
    }

    @Nonnull
    private JSONObject action(@Nonnull String type, @Nonnull Node node, boolean recursive, boolean sorted)
            throws ServletException, EtcdException {
        simulateErrors();
        processingDelay();
        JSONObject action = new JSONObject();
        try {
            action.putOpt("action", type);
            action.putOpt("node", node.toJson(recursive, sorted));
            return action;
        } catch (JSONException e) {
            throw new ServletException(e);
        }
    }

    @Nonnull
    private JSONObject action(@Nonnull String type, @Nonnull Node node, @Nonnull Node prevNode, boolean recursive, boolean sorted)
            throws ServletException, EtcdException {
        processingDelay();
        simulateErrors();
        JSONObject action = new JSONObject();
        try {
            action.putOpt("action", type);
            action.putOpt("node", node.toJson(recursive, sorted));
            action.putOpt("prevNode", prevNode.toJson(recursive, sorted));
            return action;
        } catch (JSONException e) {
            throw new ServletException(e);
        }
    }

    @Nullable
    private PrevExists parsePrevExist(@Nonnull HttpServletRequest req) {
        if (req.getParameter("prevExist") != null) {
            boolean prevExist = Boolean.parseBoolean(req.getParameter("prevExist"));
            return new PrevExists(prevExist);
        }
        return null;
    }

    @Nullable
    private PrevValue parsePrevValue(@Nonnull HttpServletRequest req) {
        if (req.getParameter("prevValue") != null) {
            String prevValue = req.getParameter("prevValue");
            return new PrevValue(prevValue);
        }
        return null;
    }

    @Nullable
    private PrevIndex parsePrevIndex(@Nonnull HttpServletRequest req) {
        if (req.getParameter("prevIndex") != null) {
            Long prevIndex = Long.parseLong(req.getParameter("prevIndex"));
            return new PrevIndex(prevIndex);
        }
        return null;
    }

    private Condition condition(Condition... conditions) {
        for (Condition condition : conditions) {
            if (condition != null) {
                return condition;
            }
        }
        return null;
    }

    private boolean parseBoolean(@Nonnull HttpServletRequest req, @Nonnull String name) {
        return req.getParameter(name) != null && Boolean.parseBoolean(req.getParameter(name));
    }

    private Integer parseTtl(@Nonnull HttpServletRequest req) {
        return req.getParameter("ttl") != null ? Integer.parseInt(req.getParameter("ttl")) : null;
    }

    private void processingDelay() {
        try {
            long delay = random.nextInt(maxDelay - minDelay + 1) + minDelay;
            Thread.sleep(delay);
        } catch (InterruptedException ignore) {
            // ignore
        }
    }

    private void simulateErrors() throws EtcdException {
        int size = errors.size();
        if (size > 0 && random.nextInt(100) < frequency) {
            Integer error = errors.get(random.nextInt(size));
            throw new EtcdException(error, "simulated error", etcd.index());
        }
    }

}
