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

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServlet;

import org.apache.sling.etcd.common.ErrorCodes;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EtcdHandlerTest {

    private Server server;

    private CloseableHttpClient httpClient;

    private PoolingHttpClientConnectionManager connectionManager;

    @Before
    public void setUp() {
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
    }

    @After
    public void tearDown() throws Exception {
        IOUtils.closeQuietly(httpClient);
        IOUtils.closeQuietly(connectionManager);
        if(server != null) {
            server.stop();
        }
    }

    @Test
    public void testDoGetNonExistingResource() throws Exception {
        server = startServer(new EtcdHandler(new Etcd()), "/v2/keys/*");
        HttpGet get = new HttpGet("http://localhost:" + serverPort(server) + "/v2/keys/some/non/existing/resource");
        CloseableHttpResponse response = httpClient.execute(get);
        Assert.assertEquals(404, response.getStatusLine().getStatusCode());
        JSONObject body = body(response);
        assertError(body, 100);
    }

    @Test
    public void testDoGetExistingKey() throws Exception {
        server = startServer(new EtcdHandler(new Etcd(TestContent.build())), "/v2/keys/*");
        HttpGet get = new HttpGet("http://localhost:" + serverPort(server) + "/v2/keys/a/k1");
        CloseableHttpResponse response = httpClient.execute(get);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        JSONObject body = body(response);
        assertAction(body, "get");
        JSONObject node = body.optJSONObject("node");
        Assert.assertEquals("/a/k1", node.optString("key"));
    }

    @Test
    public void testDoGetExistingFolderNonRecursiveSorted() throws Exception {
        server = startServer(new EtcdHandler(new Etcd(TestContent.build())), "/v2/keys/*");
        HttpGet get = new HttpGet("http://localhost:" + serverPort(server) + "/v2/keys/b?sorted=true");
        CloseableHttpResponse response = httpClient.execute(get);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        JSONObject body = body(response);
        assertAction(body, "get");
        JSONObject b = body.optJSONObject("node");
        Assert.assertEquals("/b", b.get("key"));
        JSONArray bChildren = b.getJSONArray("nodes");
        Assert.assertEquals(1, bChildren.length());
        JSONObject b1 = bChildren.getJSONObject(0);
        Assert.assertEquals("/b/b1", b1.getString("key"));
        Assert.assertFalse(b1.has("nodes"));
    }

    @Test
    public void testDoGetExistingFolderRecursiveSorted() throws Exception {
        server = startServer(new EtcdHandler(new Etcd(TestContent.build())), "/v2/keys/*");
        HttpGet get = new HttpGet("http://localhost:" + serverPort(server) + "/v2/keys/b?recursive=true&sorted=true");
        CloseableHttpResponse response = httpClient.execute(get);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        JSONObject body = body(response);
        assertAction(body, "get");
        JSONObject b = body.optJSONObject("node");
        Assert.assertEquals("/b", b.get("key"));
        JSONArray bChildren = b.getJSONArray("nodes");
        Assert.assertEquals(1, bChildren.length());
        JSONObject b1 = bChildren.getJSONObject(0);
        Assert.assertEquals("/b/b1", b1.getString("key"));
        JSONArray b1Children = b1.getJSONArray("nodes");
        Assert.assertEquals(3, b1Children.length());
        Assert.assertEquals("/b/b1/b2", b1Children.getJSONObject(0).getString("key"));
        Assert.assertEquals("/b/b1/k2", b1Children.getJSONObject(1).getString("key"));
        Assert.assertEquals("/b/b1/k3", b1Children.getJSONObject(2).getString("key"));
    }

    @Test
    public void testDoPutNewKey() throws Exception {
        server = startServer(new EtcdHandler(new Etcd()), "/v2/keys/*");
        HttpPut put = new HttpPut("http://localhost:" + serverPort(server) + "/v2/keys/some/new/keys");
        CloseableHttpResponse response = httpClient.execute(put);
        Assert.assertEquals(201, response.getStatusLine().getStatusCode());
        JSONObject body = body(response);
        assertAction(body, "set");
        Assert.assertEquals("/some/new/keys", body.getJSONObject("node").getString("key"));
    }

    @Test
    public void testDoPutNewFolder() throws Exception {
        server = startServer(new EtcdHandler(new Etcd()), "/v2/keys/*");
        HttpPut put = new HttpPut("http://localhost:" + serverPort(server) + "/v2/keys/some/new/folder?dir=true");
        CloseableHttpResponse response = httpClient.execute(put);
        Assert.assertEquals(201, response.getStatusLine().getStatusCode());
        JSONObject body = body(response);
        assertAction(body, "set");
        Assert.assertEquals("/some/new/folder", body.getJSONObject("node").getString("key"));
    }

    @Test
    public void testDoPutExistingKey() throws Exception {
        server = startServer(new EtcdHandler(new Etcd(TestContent.build())), "/v2/keys/*");
        HttpPut put = new HttpPut("http://localhost:" + serverPort(server) + "/v2/keys/a/k1");
        CloseableHttpResponse response = httpClient.execute(put);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        JSONObject body = body(response);
        assertAction(body, "set");
        Assert.assertEquals("/a/k1", body.getJSONObject("node").getString("key"));
        Assert.assertEquals("", body.getJSONObject("node").getString("value"));
        Assert.assertEquals("/a/k1", body.getJSONObject("prevNode").getString("key"));
        Assert.assertEquals("value-k1", body.getJSONObject("prevNode").getString("value"));
    }

    @Test
    public void testDoPostCreateNewKey() throws Exception {
        server = startServer(new EtcdHandler(new Etcd(TestContent.build())), "/v2/keys/*");
        HttpPost post = new HttpPost("http://localhost:" + serverPort(server) + "/v2/keys/created/keys");
        CloseableHttpResponse response = httpClient.execute(post);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        JSONObject body = body(response);
        assertAction(body, "create");
        Assert.assertEquals("/created/keys/1", body.getJSONObject("node").getString("key"));
    }

    @Test
    public void testDoPostFailToCreateKeyUnderAnotherKey() throws Exception {
        server = startServer(new EtcdHandler(new Etcd(TestContent.build())), "/v2/keys/*");
        HttpPost post = new HttpPost("http://localhost:" + serverPort(server) + "/v2/keys/a/k1");
        CloseableHttpResponse response = httpClient.execute(post);
        Assert.assertEquals(403, response.getStatusLine().getStatusCode());
        JSONObject body = body(response);
        assertError(body, ErrorCodes.NOT_DIR);
    }

    @Test
    public void testDoDeleteExistingKey() throws Exception {
        server = startServer(new EtcdHandler(new Etcd(TestContent.build())), "/v2/keys/*");
        HttpDelete delete = new HttpDelete("http://localhost:" + serverPort(server) + "/v2/keys/a/k1");
        CloseableHttpResponse response = httpClient.execute(delete);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        JSONObject body = body(response);
        assertAction(body, "delete");
        Assert.assertEquals("/a/k1", body.getJSONObject("node").getString("key"));
    }

    @Test
    public void testCreatingKeyWithTtl() throws Exception {
        server = startServer(new EtcdHandler(new Etcd()), "/v2/keys/*");
        HttpPost post = new HttpPost("http://localhost:" + serverPort(server) + "/v2/keys/some/key?ttl=10");
        CloseableHttpResponse response1 = httpClient.execute(post);
        JSONObject body1 = body(response1);
        assertAction(body1, "create");
        Integer ttl = body1.getJSONObject("node").optInt("ttl");
        Assert.assertNotNull(ttl);
        Assert.assertTrue(ttl > 0);
    }

    @Test
    public void testElapsedKey() throws Exception {
        server = startServer(new EtcdHandler(new Etcd()), "/v2/keys/*");

        HttpPost post = new HttpPost("http://localhost:" + serverPort(server) + "/v2/keys/some/key?ttl=1");
        CloseableHttpResponse response1 = httpClient.execute(post);
        JSONObject body1 = body(response1);
        assertAction(body1, "create");
        String createdKey = body1.getJSONObject("node").getString("key");
        Thread.sleep(1000); // wait for the key to elapse

        HttpGet get = new HttpGet("http://localhost:" + serverPort(server) + "/v2/keys" + createdKey);
        CloseableHttpResponse response2 = httpClient.execute(get);
        JSONObject body2 = body(response2);
        assertError(body2, ErrorCodes.KEY_NOT_FOUND);
    }

    @Test
    public void testRefreshKeyWithTtl() throws Exception {
        Etcd etcd = new Etcd();
        server = startServer(new EtcdHandler(etcd), "/v2/keys/*");

        // create key with ttl = 2
        HttpPost post = new HttpPost("http://localhost:" + serverPort(server) + "/v2/keys/some/key?ttl=1");
        CloseableHttpResponse response1 = httpClient.execute(post);
        JSONObject body1 = body(response1);
        assertAction(body1, "create");
        String createdKey = body1.getJSONObject("node").getString("key");

        // update ttl of the created key with ttl = 1000

        HttpPut put = new HttpPut("http://localhost:" + serverPort(server) + "/v2/keys" + createdKey + "?ttl=1000&prevExist=true");
        CloseableHttpResponse response2 = httpClient.execute(put);
        JSONObject body2 = body(response2);
        assertAction(body2, "update");

        Thread.sleep(1000); // wait for the key to elapse (using first ttl set)

        // check the key is still there and ttl is bigger than 1 (the initial ttl)

        HttpGet get = new HttpGet("http://localhost:" + serverPort(server) + "/v2/keys" + createdKey);
        CloseableHttpResponse response3 = httpClient.execute(get);
        JSONObject body3 = body(response3);
        assertAction(body3, "get");
        Integer ttl = body3.getJSONObject("node").optInt("ttl");
        Assert.assertNotNull(ttl);
        Assert.assertTrue(ttl > 1);
    }

    @Test
    public void testFailToSetTtlWithConditions() throws Exception {
        Etcd etcd = new Etcd();
        server = startServer(new EtcdHandler(etcd), "/v2/keys/*");

        // create key with ttl = 1
        HttpPost post = new HttpPost("http://localhost:" + serverPort(server) + "/v2/keys/some/key?ttl=1");
        CloseableHttpResponse response1 = httpClient.execute(post);
        JSONObject body1 = body(response1);
        assertAction(body1, "create");
        String createdKey = body1.getJSONObject("node").getString("key");

        // wait for the key to elapse
        Thread.sleep(1000);

        // try updating the ttl on the key checking if the key still exists

        HttpPut put = new HttpPut("http://localhost:" + serverPort(server) + "/v2/keys" + createdKey + "?ttl=1000&prevExist=true");
        CloseableHttpResponse response2 = httpClient.execute(put);
        JSONObject body2 = body(response2);
        assertError(body2, ErrorCodes.KEY_NOT_FOUND);
    }

    @Nonnull
    private JSONObject body(CloseableHttpResponse response)
            throws IOException, JSONException {
        return (response.getEntity() != null)
                ? new JSONObject(IOUtils.toString(response.getEntity().getContent()))
                : new JSONObject();
    }

    private void assertAction(@Nonnull JSONObject body, @Nonnull String type) {
        String action = body.optString("action");
        Assert.assertNotNull(action);
        Assert.assertEquals(type, action);
    }

    private void assertError(@Nonnull JSONObject error, int errorCode) {
        Assert.assertTrue(error.has("errorCode"));
        int code = error.optInt("errorCode");
        Assert.assertEquals(errorCode, code);
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