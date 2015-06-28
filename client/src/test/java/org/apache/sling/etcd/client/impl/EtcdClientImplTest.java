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
package org.apache.sling.etcd.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.sling.etcd.client.KeyAction;
import org.apache.sling.etcd.client.EtcdClient;
import org.apache.sling.etcd.client.KeyError;
import org.apache.sling.etcd.client.EtcdParams;
import org.apache.sling.etcd.client.KeyResponse;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.sling.etcd.client.LeaderStatsResponse;
import org.apache.sling.etcd.client.MembersResponse;
import org.apache.sling.etcd.client.Member;
import org.apache.sling.etcd.client.MemberStatsResponse;
import org.apache.sling.etcd.client.VersionResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.After;
import org.junit.Test;

public class EtcdClientImplTest {

    private Server server1;

    private Server server2;

    private EtcdClient etcdClient;

    private CloseableHttpClient httpClient;

    private PoolingHttpClientConnectionManager connectionManager;

    @After
    public void tearDown() throws Exception {
        IOUtils.closeQuietly(httpClient);
        IOUtils.closeQuietly(connectionManager);
        if(server1 != null) {
            server1.stop();
        }
        if(server2 != null) {
            server2.stop();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testNoEndPoint() throws Exception {
        new EtcdClientImpl(null, null);
    }

    @Test
    public void testGetVersion() throws Exception {
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
                res.setStatus(200);
                res.getWriter().write("etcd 2.0.6");
            }
        };
        server1 = startServer(servlet, "/version");
        int port = serverPort(server1);
        buildEtcdClient(port);
        VersionResponse version = etcdClient.getVersion(new URI("http://localhost:" + port));
        Assert.assertEquals("etcd 2.0.6", version.version());
    }

    @Test(expected = IOException.class)
    public void testGetVersionThroughProxyAndNoPeerAvailable() throws Exception {
        server1 = startServer(new ProxyNoServiceAvailable(), "/version");
        int port = serverPort(server1);
        buildEtcdClient(port);
        etcdClient.getVersion(new URI("http://localhost:" + port));
    }

    @Test
    public void testGetLeaderStatistics() throws Exception {
        server1 = startServer(new StaticHandler(200, "/leader-stats.json"), "/v2/stats/leader");
        int port = serverPort(server1);
        buildEtcdClient(port);
        LeaderStatsResponse leaderStats = etcdClient.getLeaderStats(new URI("http://localhost:" + port));
        Assert.assertNotNull(leaderStats);
        Assert.assertEquals("tmaret-osx", leaderStats.leaderId());
    }

    @Test(expected = IOException.class)
    public void testGetLeaderStatisticsThroughProxyAndNoPeerAvailable() throws Exception {
        server1 = startServer(new ProxyNoServiceAvailable(), "/v2/stats/leader");
        int port = serverPort(server1);
        buildEtcdClient(port);
        etcdClient.getLeaderStats(new URI("http://localhost:" + port));
    }


    @Test
    public void testGetExistingKey() throws Exception {
        server1 = startServer(new StaticHandler(200, "/action-2.json"), "/v2/keys/test");
        buildEtcdClient(serverPort(server1));
        KeyResponse response = etcdClient.getKey("/test", EtcdParams.noParams());
        Assert.assertTrue(response.isAction());
        KeyAction action = response.action();
        Assert.assertEquals("get", action.action());
        Assert.assertEquals("/test", action.node().key());
    }

    @Test(expected = IOException.class)
    public void testGetExistingKeyThroughProxyAndNoPeerAvailable() throws Exception {
        server1 = startServer(new ProxyNoServiceAvailable(), "/v2/keys/test");
        buildEtcdClient(serverPort(server1));
        etcdClient.getKey("/test", EtcdParams.noParams());
    }

    @Test
    public void testGetRedirect() throws Exception {
        server2 = startServer(new StaticHandler(200, "/action-2.json"), "/v2/keys/redi");
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
                res.setHeader("Location", "http://localhost:" + (serverPort(server2)) + "/v2/keys/redi");
                res.setStatus(307);
            }
        };
        server1 = startServer(servlet, "/v2/keys/test");
        buildEtcdClient(serverPort(server1));
        KeyResponse response = etcdClient.getKey("/test", EtcdParams.noParams());
        Assert.assertTrue(response.isAction());
        KeyAction action = response.action();
        Assert.assertEquals("get", action.action());
        Assert.assertEquals("/test", action.node().key());
    }

    @Test
    public void testGetMissingKey() throws Exception {
        server1 = startServer(new StaticHandler(404, "/error-4.json"), "/v2/keys/test");
        buildEtcdClient(serverPort(server1));
        KeyResponse response = etcdClient.getKey("/test", EtcdParams.noParams());
        Assert.assertNotNull(response);
        Assert.assertFalse(response.isAction());
        KeyError error = response.error();
        Assert.assertNotNull(error);
        Assert.assertEquals(100, error.errorCode());
    }

    @Test
    public void testPutKey() throws Exception {
        server1 = startServer(new StaticHandler(201, "/action-1.json"), "/v2/keys/test");
        buildEtcdClient(serverPort(server1));
        KeyResponse response = etcdClient.putKey("/test", "test-data", EtcdParams.noParams());
        Assert.assertTrue(response.isAction());
        KeyAction action = response.action();
        Assert.assertNotNull(action);
        Assert.assertEquals("/test", action.node().key());
    }

    @Test
    public void testPutKey1() throws Exception {
        server1 = startServer(new StaticHandler(201, "/action-1.json"), "/v2/keys/test");
        buildEtcdClient(serverPort(server1));
        KeyResponse response = etcdClient.putKey("/test", IOUtils.toInputStream("test-data"), EtcdParams.noParams());
        Assert.assertTrue(response.isAction());
        KeyAction action = response.action();
        Assert.assertNotNull(action);
        Assert.assertEquals("/test", action.node().key());
    }

    @Test
    public void testPutKeyWithParameter() throws Exception {
        HttpServlet servlet = new HttpServlet(){
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
                String ttl = req.getParameter("ttl");
                if ("10".equals(ttl)) {
                    res.getWriter().write(IOUtils.toString(
                            getClass().getResourceAsStream(
                                    "/action-1.json")));
                }
            }
        };
        server1 = startServer(servlet, "/v2/keys/test");
        buildEtcdClient(serverPort(server1));
        KeyResponse response = etcdClient.putKey("/test", "test-data", EtcdParams.builder().ttl(10).build());
        Assert.assertTrue(response.isAction());
        KeyAction action = response.action();
        Assert.assertNotNull(action);
    }

    @Test
    public void testMultiValueHeader() throws Exception {
        HttpServlet servlet = new HttpServlet(){
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
                res.addHeader("headerName", "value1");
                res.addHeader("headerName", "value2");
                res.getWriter().write(IOUtils.toString(
                        getClass().getResourceAsStream(
                                "/action-1.json")));
            }
        };
        server1 = startServer(servlet, "/v2/keys/test");
        buildEtcdClient(serverPort(server1));
        KeyResponse response = etcdClient.putKey("/test", "test-data", EtcdParams.builder().ttl(10).build());
        Assert.assertNotNull(response.headers());
        Assert.assertNotNull(response.header("headerName"));
        List<String> headers = response.header("headerName");
        Assert.assertEquals(2, headers.size());
        Assert.assertEquals("value1", headers.get(0));
        Assert.assertEquals("value2", headers.get(1));
        Assert.assertEquals("value1", response.headerFirst("headerName"));
    }

    @Test
    public void testPutRequestFormat() throws Exception {
        HttpServlet servlet = new HttpServlet(){
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
                ContentType contentType = ContentType.parse(req.getContentType());
                if (! contentType.getMimeType().equals(EtcdClientImpl.FORM_URLENCODED.getMimeType())) {
                    throw new IllegalArgumentException("wrong mime type");
                }
                if (! contentType.getCharset().equals(EtcdClientImpl.FORM_URLENCODED.getCharset())) {
                    throw new IllegalArgumentException("wrong content charset");
                }
                String value = req.getParameter("value");
                if (value == null) {
                    throw new IllegalArgumentException("missing value parameter");
                }
                String ttl = req.getParameter("ttl");
                if (! "10".equals(ttl)) {
                    throw new IllegalArgumentException("missing ttl parameter");
                }
                res.setStatus(201);
                res.getWriter().write(IOUtils.toString(
                        getClass().getResourceAsStream(
                                "/action-3.json")));
            }
        };
        server1 = startServer(servlet, "/v2/keys/post/test");
        buildEtcdClient(serverPort(server1));
        KeyResponse response = etcdClient.putKey("/post/test", "test-data", Collections.singletonMap("ttl", "10"));
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isAction());
    }

    @Test
    public void testPostKey() throws Exception {
        server1 = startServer(new StaticHandler(201, "/action-3.json"), "/v2/keys/post/test");
        buildEtcdClient(serverPort(server1));
        KeyResponse response = etcdClient.postKey("/post/test", "test-data", EtcdParams.noParams());
        Assert.assertTrue(response.isAction());
        KeyAction action = response.action();
        Assert.assertNotNull(action);
        Assert.assertEquals("/post/test/221", action.node().key());
    }

    @Test
    public void testPostKey1() throws Exception {
        server1 = startServer(new StaticHandler(201, "/action-3.json"), "/v2/keys/post/test");
        buildEtcdClient(serverPort(server1));
        KeyResponse response = etcdClient.postKey("/post/test", IOUtils.toInputStream("test-data"), EtcdParams.noParams());
        Assert.assertTrue(response.isAction());
        KeyAction action = response.action();
        Assert.assertNotNull(action);
        Assert.assertEquals("/post/test/221", action.node().key());
    }

    @Test
    public void testPostRequestFormat() throws Exception {
        HttpServlet servlet = new HttpServlet(){
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
                ContentType contentType = ContentType.parse(req.getContentType());
                if (! contentType.getMimeType().equals(EtcdClientImpl.FORM_URLENCODED.getMimeType())) {
                    throw new IllegalArgumentException("wrong mime type");
                }
                if (! contentType.getCharset().equals(EtcdClientImpl.FORM_URLENCODED.getCharset())) {
                    throw new IllegalArgumentException("wrong content charset");
                }
                String value = req.getParameter("value");
                if (value == null) {
                    throw new IllegalArgumentException("missing value parameter");
                }
                String ttl = req.getParameter("ttl");
                if (! "10".equals(ttl)) {
                    throw new IllegalArgumentException("missing ttl parameter");
                }
                res.setStatus(201);
                res.getWriter().write(IOUtils.toString(
                        getClass().getResourceAsStream(
                                "/action-3.json")));
            }
        };
        server1 = startServer(servlet, "/v2/keys/post/test");
        buildEtcdClient(serverPort(server1));
        KeyResponse response = etcdClient.postKey("/post/test", "test-data", Collections.singletonMap("ttl", "10"));
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isAction());
    }


    @Test
    public void testDeleteKey() throws Exception {
        server1 = startServer(new StaticHandler(200, "/action-4.json"), "/v2/keys/post/test");
        buildEtcdClient(serverPort(server1));
        KeyResponse response = etcdClient.deleteKey("/post/test", EtcdParams.noParams());
        Assert.assertTrue(response.isAction());
        KeyAction action = response.action();
        Assert.assertNotNull(action);
        Assert.assertEquals("/post/test/221", action.node().key());
    }

    @Test
    public void testGetMembers() throws Exception {
        server1 = startServer(new StaticHandler(200, "/members.json"), "/v2/members");
        buildEtcdClient(serverPort(server1));
        MembersResponse response = etcdClient.getMembers();
        Assert.assertNotNull(response);
        List<Member> members = response.members();
        Assert.assertEquals(3, members.size());
    }

    @Test
    public void testGetPeerStatsForLeader() throws Exception {
        server1 = startServer(new StaticHandler(200, "/peer-leader-stats.json"), "/v2/stats/self");
        int port = serverPort(server1);
        buildEtcdClient(port);
        MemberStatsResponse response = etcdClient.getMemberStats(new URI("http://localhost:" + port));
        Assert.assertNotNull(response);
        Assert.assertEquals("324473db0474a678", response.id());
    }

    @Test
    public void testGetPeerStatsForFollower() throws Exception {
        server1 = startServer(new StaticHandler(200, "/peer-follower-stats.json"), "/v2/stats/self");
        int port = serverPort(server1);
        buildEtcdClient(port);
        MemberStatsResponse response = etcdClient.getMemberStats(new URI("http://localhost:" + port));
        Assert.assertEquals("7e3bd17c66e004e8", response.id());
    }

    @Test
    public void testSslServerAndClientAuthenticationWithCustomCA() throws Exception {

        /*
         * The client JKS keystore containing the client private key and certificate signed by the custom CA.
         */
        String clientKeyStorePath = "/client-keystore.jks";

        /*
         * The client JKS trust store containing the custom CA root certificate.
         */
        String clientTrustStorePath = "/client-truststore.jks";

        /**
         * The server key store containing the CA root certificate as well as the server private key and certificate.
         */
        String serverKeyStorePath = "/server-keystore.jks";

        String pwd = "testit";

        // start the server requiring client certificate

        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
                res.setStatus(200);
                res.getWriter().write("etcd 2.0.6");
            }
        };

        server1 = startSecureServer(servlet, "/version", serverKeyStorePath, pwd);
        int port = serverPort(server1);

        buildSecureEtcdClient(port, clientKeyStorePath, pwd, clientTrustStorePath, pwd);

        VersionResponse version = etcdClient.getVersion(new URI("https://127.0.0.1:" + port));
        Assert.assertEquals("etcd 2.0.6", version.version());
    }


    private void buildSecureEtcdClient(int port, @Nonnull String keyStorePath, @Nullable String keyStorePwd,
                                       @Nonnull String trustStorePath, @Nullable String trustStorePwd)
            throws Exception {

        char[] ksp = (keyStorePwd != null) ? keyStorePwd.toCharArray() : null;
        KeyStore keyStore = loadKeyStore(keyStorePath, ksp);

        char[] tsp = (trustStorePwd != null) ? trustStorePwd.toCharArray() : null;
        KeyStore trustStore = loadKeyStore(trustStorePath, tsp);

        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(trustStore)
                .loadKeyMaterial(keyStore, tsp)
                .build();

        SSLConnectionSocketFactory sslConnectionSocketFactory =
                new SSLConnectionSocketFactory(sslContext);

        Registry<ConnectionSocketFactory> connectionSocketFactory =
                RegistryBuilder.<ConnectionSocketFactory> create()
                        .register("https", sslConnectionSocketFactory).build();

        connectionManager = new PoolingHttpClientConnectionManager(connectionSocketFactory);

        final RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(10000)
                .setConnectTimeout(10000)
                .setRedirectsEnabled(true)
                .setStaleConnectionCheckEnabled(true)
                .build();
        httpClient = HttpClients
                .custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setSSLSocketFactory(sslConnectionSocketFactory)
                .build();

        etcdClient = new EtcdClientImpl(httpClient, new URI("https://127.0.0.1:" + port));

    }

    private void buildEtcdClient(int port) throws Exception {
        connectionManager = new PoolingHttpClientConnectionManager();
        final RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(10000)
                .setConnectTimeout(10000)
                .setRedirectsEnabled(true)
                .setStaleConnectionCheckEnabled(true)
                .build();
        httpClient = HttpClients
                .custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
        etcdClient = new EtcdClientImpl(httpClient, new URI("http://localhost:" + port));
    }

    private Server startSecureServer(HttpServlet servlet, String pathSpec,
                                     @Nonnull String keyStorePath, @Nullable String keyStorePwd)
            throws Exception {

        char[] ksp = (keyStorePwd != null) ? keyStorePwd.toCharArray() : null;
        KeyStore keyStore = loadKeyStore(keyStorePath, ksp);

        Server server = new Server();

        SslContextFactory sslContextFactory =new SslContextFactory();
        sslContextFactory.setKeyStore(keyStore);
        sslContextFactory.setKeyStorePassword(keyStorePwd);
        sslContextFactory.setTrustStore(keyStore);
        sslContextFactory.setTrustStorePassword(keyStorePwd);

        SslConnector connector = new SslSelectChannelConnector(sslContextFactory);
        connector.setNeedClientAuth(true);
        server.setConnectors(new Connector[]{connector});
        ServletContextHandler sch = new ServletContextHandler(null, "/", false, false);

        sch.addServlet(new ServletHolder(servlet), pathSpec);
        server.setHandler(sch);
        server.start();
        return server;

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

    private class StaticHandler extends HttpServlet {

        int status;

        final String resourcePath;

        StaticHandler(int status, String resourcePath) {
            this.resourcePath = resourcePath;
            this.status = status;
        }

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse res)
                throws ServletException, IOException {
            res.setStatus(status);
            res.getWriter().write(IOUtils.toString(
                    getClass().getResourceAsStream(
                            resourcePath)));
        }
    }

    private class ProxyNoServiceAvailable extends HttpServlet {
        @Override
        protected void service(HttpServletRequest req, HttpServletResponse res)
                throws ServletException, IOException {
            res.setStatus(503);
            res.getWriter().write("{\"message\":\"proxy: zero endpoints currently available\"}");
        }
    }

    private KeyStore loadKeyStore(String path, char[] pwd)
            throws Exception {
        InputStream keyStoreInputStream = null;
        try {
            keyStoreInputStream = getClass().getResourceAsStream(path);
            KeyStore keyStore = KeyStore.getInstance("jks");
            keyStore.load(keyStoreInputStream, pwd);
            return keyStore;
        } finally {
            IOUtils.closeQuietly(keyStoreInputStream);
        }
    }

}