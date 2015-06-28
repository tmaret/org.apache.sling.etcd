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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.discovery.etcd.gzip.GzipRequestInterceptor;
import org.apache.sling.discovery.etcd.gzip.GzipResponseInterceptor;
import org.apache.sling.etcd.client.EtcdClient;
import org.apache.sling.etcd.client.LeaderStatsResponse;
import org.apache.sling.etcd.client.MemberStatsResponse;
import org.apache.sling.etcd.client.MembersResponse;
import org.apache.sling.etcd.client.VersionResponse;
import org.apache.sling.etcd.client.impl.LeaderStatsResponseImpl;
import org.apache.sling.etcd.client.impl.MemberStatsResponseImpl;
import org.apache.sling.etcd.client.impl.MembersResponseImpl;
import org.apache.sling.etcd.client.impl.VersionResponseImpl;
import org.apache.sling.etcd.common.ErrorCodes;
import org.apache.sling.etcd.client.EtcdNode;
import org.apache.sling.etcd.client.KeyResponse;
import org.apache.sling.etcd.client.impl.EtcdClientImpl;
import org.apache.sling.etcd.client.impl.KeyResponseImpl;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Test;

public class EtcdServiceTest {

    private Server server;

    private EtcdClient etcdClient;

    private CloseableHttpClient httpClient;

    private PoolingHttpClientConnectionManager connectionManager;

    @After
    public void tearDown() throws Exception {
        IOUtils.closeQuietly(httpClient);
        IOUtils.closeQuietly(connectionManager);
        if(server != null) {
            server.stop();
        }
    }

    @Test
    public void testAnnounceLocalInstance() throws Exception {
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
                if (! "POST".equals(req.getMethod())) {
                    throw new IllegalArgumentException("create key requires POST");
                }
                String ttl = req.getParameter("ttl");
                if (ttl == null) {
                    throw new IllegalArgumentException("ttl not specified");
                }
                String value = req.getParameter("value");
                if (value == null) {
                    throw new IllegalArgumentException("value not specified");
                }
                res.setStatus(201);
                res.getWriter().write(IOUtils.toString(
                        getClass().getResourceAsStream(
                                "/announce-local-instance.json")));
            }
        };
        AnnounceData annData = new AnnounceData("sling-id", "server-info", "default-cluster", 1926);
        server = startServer(servlet, "/v2/keys/discovery/announces");
        EtcdService etcdService = buildEtcdService(serverPort(server));
        EtcdNode annNode = etcdService.createAnnounce(annData.toString(), 10);
        Assert.assertNotNull(annNode);
        Assert.assertEquals(annNode.key(), "/discovery/announces/244");
    }

    @Test(expected = IOException.class)
    public void testAnnounceLocalInstanceIOError() throws Exception {
        EtcdService etcdService = new EtcdService(new IoExceptionEtcdClient(), "/discovery");
        try {
            etcdService.createAnnounce("/discovery/announces/244", 10);
        } catch (IOException e) {
            Assert.assertEquals(1, etcdService.getErrStats().consecutiveIoError());
            throw e;
        }
    }

    @Test(expected = EtcdServiceException.class)
    public void testAnnounceLocalInstanceEtcdError() throws Exception {
        EtcdService etcdService = new EtcdService(new IoExceptionEtcdClient(ErrorCodes.RAFT_INTERNAL_ERROR), "/discovery");
        try {
            etcdService.createAnnounce("/discovery/announces/244", 10);
        } catch (EtcdServiceException e) {
            Assert.assertEquals(1, etcdService.getErrStats().consecutiveEtcdError(ErrorCodes.RAFT_INTERNAL_ERROR, Integer.MAX_VALUE));
            throw e;
        }
    }

    @Test
    public void testSendLocalProperties() throws Exception {
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
                if (! "PUT".equals(req.getMethod())) {
                    throw new IllegalArgumentException("send props requires PUT");
                }
                String value = req.getParameter("value");
                if (value == null) {
                    throw new IllegalArgumentException("value not specified");
                }
                res.setStatus(201);
                res.getWriter().write(IOUtils.toString(
                        getClass().getResourceAsStream(
                                "/send-properties.json")));
            }
        };
        server = startServer(servlet, "/v2/keys/discovery/properties/sling-id");
        EtcdService etcdService = buildEtcdService(serverPort(server));
        long lastModified = etcdService.sendInstanceProperties(Collections.singletonMap("n1", "v1"), "sling-id");
        Assert.assertEquals(253L, lastModified);
    }

    @Test(expected = IOException.class)
    public void testSendLocalPropertiesIOError() throws Exception {
        EtcdService etcdService = new EtcdService(new IoExceptionEtcdClient(), "/discovery");
        try {
            etcdService.sendInstanceProperties(Collections.<String, String>emptyMap(), "sling-id");
        } catch (IOException e) {
            Assert.assertEquals(1, etcdService.getErrStats().consecutiveIoError());
            throw e;
        }
    }

    @Test(expected = EtcdServiceException.class)
    public void testSendLocalPropertiesEtcdError() throws Exception {
        EtcdService etcdService = new EtcdService(new IoExceptionEtcdClient(ErrorCodes.RAFT_INTERNAL_ERROR), "/discovery");
        try {
            etcdService.sendInstanceProperties(Collections.<String, String>emptyMap(), "sling-id");
        } catch (EtcdServiceException e) {
            Assert.assertEquals(1, etcdService.getErrStats().consecutiveEtcdError(ErrorCodes.RAFT_INTERNAL_ERROR, Integer.MAX_VALUE));
            throw e;
        }
    }

    @Test
    public void testGetAnnounces() throws Exception {
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
                if (! "GET".equals(req.getMethod())) {
                    throw new IllegalArgumentException("get announces requires GET");
                }
                String recursive = req.getParameter("recursive");
                if (recursive == null || "false".equals(recursive)) {
                    throw new IllegalArgumentException("recursive must be true");
                }
                String sorted = req.getParameter("sorted");
                if (sorted == null || "false".equals(sorted)) {
                    throw new IllegalArgumentException("sorted must be true");
                }
                res.setStatus(200);
                res.getWriter().write(IOUtils.toString(
                        getClass().getResourceAsStream(
                                "/get-announces.json")));
            }
        };
        server = startServer(servlet, "/v2/keys/discovery/announces");
        EtcdService etcdService = buildEtcdService(serverPort(server));
        List<EtcdNode> announces = etcdService.getAnnounces();
        Assert.assertNotNull(announces);
        Assert.assertEquals(2, announces.size());
    }

    @Test(expected = IOException.class)
    public void testGetAnnouncesIOError() throws Exception {
        EtcdService etcdService = new EtcdService(new IoExceptionEtcdClient(), "/discovery");
        try {
            etcdService.getAnnounces();
        } catch (IOException e) {
            Assert.assertEquals(1, etcdService.getErrStats().consecutiveIoError());
            throw e;
        }
    }

    @Test(expected = EtcdServiceException.class)
    public void testGetAnnouncesEtcdError() throws Exception {
        EtcdService etcdService = new EtcdService(new IoExceptionEtcdClient(ErrorCodes.RAFT_INTERNAL_ERROR), "/discovery");
        try {
            etcdService.getAnnounces();
        } catch (EtcdServiceException e) {
            Assert.assertEquals(1, etcdService.getErrStats().consecutiveEtcdError(ErrorCodes.RAFT_INTERNAL_ERROR, Integer.MAX_VALUE));
            throw e;
        }
    }

    @Test
    public void testGetAnnouncesKeyNotFound() throws Exception {
        EtcdService etcdService = new EtcdService(new IoExceptionEtcdClient(ErrorCodes.KEY_NOT_FOUND), "/discovery");
        List<EtcdNode> announces = etcdService.getAnnounces();
        Assert.assertEquals(0, announces.size());
    }

    @Test
    public void testGetProperties() throws Exception {
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
                if (! "GET".equals(req.getMethod())) {
                    throw new IllegalArgumentException("get properties requires GET");
                }
                String recursive = req.getParameter("recursive");
                if (recursive == null || "false".equals(recursive)) {
                    throw new IllegalArgumentException("recursive must be true");
                }
                res.setStatus(200);
                res.getWriter().write(IOUtils.toString(
                        getClass().getResourceAsStream(
                                "/get-properties.json")));
            }
        };
        server = startServer(servlet, "/v2/keys/discovery/properties/sling-id");
        EtcdService etcdService = buildEtcdService(serverPort(server));
        Map<String, String> properties = etcdService.getProperties("sling-id");
        Assert.assertNotNull(properties);
        Assert.assertEquals(1, properties.size());
    }

    @Test(expected = IOException.class)
    public void testGetPropertiesIOError() throws Exception {
        EtcdService etcdService = new EtcdService(new IoExceptionEtcdClient(), "/discovery");
        try {
            etcdService.getProperties("sling-id");
        } catch (IOException e) {
            Assert.assertEquals(1, etcdService.getErrStats().consecutiveIoError());
            throw e;
        }
    }

    @Test(expected = EtcdServiceException.class)
    public void testGetPropertiesEtcdError() throws Exception {
        EtcdService etcdService = new EtcdService(new IoExceptionEtcdClient(ErrorCodes.RAFT_INTERNAL_ERROR), "/discovery");
        try {
            etcdService.getProperties("sling-id");
        } catch (EtcdServiceException e) {
            Assert.assertEquals(1, etcdService.getErrStats().consecutiveEtcdError(ErrorCodes.RAFT_INTERNAL_ERROR, Integer.MAX_VALUE));
            throw e;
        }
    }

    @Test
    public void testGetPropertiesKeyNotFound() throws Exception {
        EtcdService etcdService = new EtcdService(new IoExceptionEtcdClient(ErrorCodes.KEY_NOT_FOUND), "/discovery");
        Map<String, String> properties = etcdService.getProperties("sling-id");
        Assert.assertNotNull(properties);
        Assert.assertEquals(0, properties.size());
    }

    @Test
    public void testGetAllInstancesProperties() throws Exception {
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
                if (! "GET".equals(req.getMethod())) {
                    throw new IllegalArgumentException("get all properties requires GET");
                }
                String recursive = req.getParameter("recursive");
                if (recursive == null || "false".equals(recursive)) {
                    throw new IllegalArgumentException("recursive must be true");
                }
                res.setStatus(200);
                res.getWriter().write(IOUtils.toString(
                        getClass().getResourceAsStream(
                                "/get-all-properties.json")));
            }
        };
        server = startServer(servlet, "/v2/keys/discovery/properties");
        EtcdService etcdService = buildEtcdService(serverPort(server));
        Map<String, Map<String, String>> properties = etcdService.getInstancesProperties();
        Assert.assertNotNull(properties);
        Assert.assertEquals(2, properties.size());
        Map<String, String> props = properties.get("sling-id");
        Assert.assertNotNull(props);
        Assert.assertEquals(1, props.size());
    }

    @Test(expected = IOException.class)
    public void testGetAllInstancesPropertiesIOError() throws Exception {
        EtcdService etcdService = new EtcdService(new IoExceptionEtcdClient(), "/discovery");
        try {
            etcdService.getInstancesProperties();
        } catch (IOException e) {
            Assert.assertEquals(1, etcdService.getErrStats().consecutiveIoError());
            throw e;
        }
    }

    @Test(expected = EtcdServiceException.class)
    public void testGetAllInstancesPropertiesEtcdError() throws Exception {
        EtcdService etcdService = new EtcdService(new IoExceptionEtcdClient(ErrorCodes.RAFT_INTERNAL_ERROR), "/discovery");
        try {
            etcdService.getInstancesProperties();
        } catch (EtcdServiceException e) {
            Assert.assertEquals(1, etcdService.getErrStats().consecutiveEtcdError(ErrorCodes.RAFT_INTERNAL_ERROR, Integer.MAX_VALUE));
            throw e;
        }
    }

    @Test
    public void testRefreshAnnounce() throws Exception {
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
                if (! "PUT".equals(req.getMethod())) {
                    throw new IllegalArgumentException("refresh announce requires PUT");
                }
                String recursive = req.getParameter("prevExist");
                if (recursive == null || "false".equals(recursive)) {
                    throw new IllegalArgumentException("prevExist must be true");
                }
                String value = req.getParameter("value");
                if (value == null) {
                    throw new IllegalArgumentException("value not specified");
                }
                res.setStatus(200);
                res.getWriter().write(IOUtils.toString(
                        getClass().getResourceAsStream(
                                "/refresh-existing-announce.json")));
            }
        };
        server = startServer(servlet, "/v2/keys/discovery/announces/265");
        EtcdService etcdService = buildEtcdService(serverPort(server));
        AnnounceData annData = new AnnounceData("sling-id-2", "server-info-2", "default-cluster", 1928);
        etcdService.refreshAnnounce("/discovery/announces/265", annData.toString(), 20000);
    }

    @Test(expected = IOException.class)
    public void testRefreshAnnounceIOError() throws Exception {
        EtcdService etcdService = new EtcdService(new IoExceptionEtcdClient(), "/discovery");
        try {
            AnnounceData annData = new AnnounceData("sling-id-2", "server-info-2", "default-cluster", 1928);
            etcdService.refreshAnnounce("/discovery/announces/265", annData.toString(), 20000);
        } catch (IOException e) {
            Assert.assertEquals(1, etcdService.getErrStats().consecutiveIoError());
            throw e;
        }
    }

    @Test(expected = EtcdServiceException.class)
    public void testRefreshAnnounceEtcdError() throws Exception {
        EtcdService etcdService = new EtcdService(new IoExceptionEtcdClient(ErrorCodes.RAFT_INTERNAL_ERROR), "/discovery");
        try {
            AnnounceData annData = new AnnounceData("sling-id-2", "server-info-2", "default-cluster", 1928);
            etcdService.refreshAnnounce("/discovery/announces/265", annData.toString(), 20000);
        } catch (EtcdServiceException e) {
            Assert.assertEquals(1, etcdService.getErrStats().consecutiveEtcdError(ErrorCodes.RAFT_INTERNAL_ERROR, Integer.MAX_VALUE));
            throw e;
        }
    }

    @Test
    public void testGetGzipped() throws Exception {
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
                String acceptEncoding = req.getHeader("Accept-Encoding");
                if (acceptEncoding != null && acceptEncoding.equalsIgnoreCase("gzip")) {
                    res.setHeader("Content-Encoding", "gzip");
                    res.setStatus(200);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    GZIPOutputStream gzip = new GZIPOutputStream(out);
                    String data = IOUtils.toString(
                            getClass().getResourceAsStream(
                                    "/get-properties.json"), "UTF-8");
                    gzip.write(data.getBytes("UTF-8"));
                    gzip.close();
                    res.getOutputStream().write(out.toByteArray());
                } else {
                    throw new IllegalArgumentException("accept-encoding not found or not gzip");
                }
            }
        };
        server = startServer(servlet, "/v2/keys/discovery/properties/sling-id");
        EtcdService etcdService = buildEtcdService(serverPort(server));
        Map<String, String> properties = etcdService.getProperties("sling-id");
        Assert.assertNotNull(properties);
        Assert.assertEquals(1, properties.size());
    }

    private class IoExceptionEtcdClient implements EtcdClient {

        final boolean throwIoException;

        final int errorCode;

        public IoExceptionEtcdClient() {
            throwIoException = true;
            errorCode = -1;
        }

        public IoExceptionEtcdClient(int errorCode) {
            throwIoException = false;
            this.errorCode = errorCode;
        }

        @Nonnull
        public KeyResponse getKey(@Nonnull String s, @Nonnull Map<String, String> map)
                throws IOException {
            return throwOrReturnError();
        }

        @Nonnull
        public KeyResponse putKey(@Nonnull String s, String s1, @Nonnull Map<String, String> map)
                throws IOException {
            return throwOrReturnError();
        }

        @Nonnull
        public KeyResponse putKey(@Nonnull String s, @Nonnull InputStream inputStream, @Nonnull Map<String, String> map)
                throws IOException {
            return throwOrReturnError();
        }

        @Nonnull
        public KeyResponse postKey(@Nonnull String s, String s1, @Nonnull Map<String, String> map)
                throws IOException {
            return throwOrReturnError();
        }

        @Nonnull
        public KeyResponse postKey(@Nonnull String s, @Nonnull InputStream inputStream, @Nonnull Map<String, String> map)
                throws IOException {
            return throwOrReturnError();
        }

        @Nonnull
        public KeyResponse deleteKey(@Nonnull String s, @Nonnull Map<String, String> map)
                throws IOException {
            return throwOrReturnError();
        }

        @Nonnull
        public MembersResponse getMembers() throws IOException {
            if (throwIoException) {
                throw new IOException();
            }
            JSONObject members = new JSONObject();
            try {
                members.putOpt("members", new JSONArray());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return new MembersResponseImpl(200, "OK", Collections.<String, List<String>>emptyMap(), members);
        }

        @Nonnull
        public LeaderStatsResponse getLeaderStats(@Nonnull URI leaderPeerEndpoint)
                throws IOException {
            if (throwIoException) {
                throw new IOException();
            }
            JSONObject stats = new JSONObject();
            try {
                stats.putOpt("leader", "some-leader-id");
                stats.putOpt("followers", new JSONObject());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return new LeaderStatsResponseImpl(200, "OK", Collections.<String, List<String>>emptyMap(), stats);
        }

        @Nonnull
        public MemberStatsResponse getMemberStats(@Nonnull URI peerEndpoint) throws IOException {
            if (throwIoException) {
                throw new IOException();
            }
            JSONObject stats = new JSONObject();
            try {
                stats.putOpt("name", "c3");
                stats.putOpt("id", "324473db0474a678");
                stats.putOpt("state", "StateLeader");
                stats.putOpt("startTime", "2015-05-09T15:50:26.274028984+02:00");
                stats.putOpt("startTime", "2015-05-09T15:50:26.274028984+02:00");
                JSONObject leaderInfo = new JSONObject();
                leaderInfo.putOpt("leader", "324473db0474a678");
                leaderInfo.putOpt("uptime", "8h45m20.069720963s");
                leaderInfo.putOpt("uptime", "8h45m20.069720963s");
                leaderInfo.putOpt("startTime", "2015-05-09T15:50:33.77877435+02:00");
                stats.putOpt("leaderInfo", leaderInfo);
                stats.putOpt("recvAppendRequestCnt", 0L);
                stats.putOpt("sendAppendRequestCnt", 245368L);
                stats.putOpt("sendPkgRate", 9.302272541567971);
                stats.putOpt("sendBandwidthRate", 763.6235529373149);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return new MemberStatsResponseImpl(200, "OK", Collections.<String, List<String>>emptyMap(), stats);
        }

        @Nonnull
        public VersionResponse getVersion(@Nonnull URI peerEndpoint) throws IOException {
            if (throwIoException) {
                throw new IOException();
            }
            return new VersionResponseImpl(200, "OK", Collections.<String, List<String>>emptyMap(), "etcd 2.0.8");
        }

        private KeyResponse throwOrReturnError() throws IOException {
            if (throwIoException) {
                throw new IOException();
            }
            JSONObject errorData = new JSONObject();
            try {
                errorData.putOpt("errorCode", errorCode);
                errorData.putOpt("message", "error");
                errorData.putOpt("cause", "/test/is/the/cause");
                errorData.putOpt("index", 12);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return new KeyResponseImpl(400, "error", Collections.<String, List<String>>emptyMap(), errorData);
        }

    }

    private EtcdService buildEtcdService(int port) throws Exception {
        connectionManager = new PoolingHttpClientConnectionManager();
        final RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(10000)
                .setConnectTimeout(10000)
                .setRedirectsEnabled(true)
                .setStaleConnectionCheckEnabled(true)
                .build();
        httpClient = HttpClients
                .custom()
                .addInterceptorFirst(new GzipRequestInterceptor())
                .addInterceptorFirst(new GzipResponseInterceptor())
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
        etcdClient = new EtcdClientImpl(httpClient, new URI("http://localhost:" + port));
        return new EtcdService(etcdClient, "/discovery");
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