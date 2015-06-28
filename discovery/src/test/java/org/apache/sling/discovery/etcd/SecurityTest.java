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

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServlet;

import org.apache.commons.io.IOUtils;
import org.apache.sling.etcd.testing.Etcd;
import org.apache.sling.etcd.testing.EtcdHandler;
import junit.framework.Assert;
import org.eclipse.jetty.http.ssl.SslContextFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ssl.SslConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SecurityTest {

    private Server server;

    private volatile ExecutorService executor;

    private EtcdHandler handler;

    private List<Instance> instances;

    @Before
    public void setUp() throws Exception {
        executor = Executors.newCachedThreadPool();
        handler = new EtcdHandler(new Etcd());
        instances = new ArrayList<Instance>();
    }

    @After
    public void tearDown() throws Exception {
        if(executor != null) {
            executor.shutdownNow();
        }
        if (instances != null) {
            stopInstances(instances);
        }
        if (server != null) {
            server.stop();
        }
    }

    @Test(timeout = 5000L)
    public void testStartOneInstanceClientAuthRequired() throws Exception {

        server = startSecureServer(handler, "/v2/keys/*", true);

        // start instance
        String slingId = UUID.randomUUID().toString();

        String keystorePath = getAbsolutePath("/client-keystore.jks");
        String keystorePwdPath = getAbsolutePath("/client-pwd");

        Instance instance = startInstance(slingId, "default-cluster", 9000, keystorePath, keystorePwdPath);
        // wait for the view to be established
        waitForEstablishedView(instances);
        // assert
        Assert.assertEquals(slingId, instance.topology().getLocalInstance().getSlingId());
    }

    private Instance startInstance(String slingId, String clusterId, int instancePort, String keystorePath, String keystorePwdPath) throws Exception {
        Instance instance = new Instance(
                instancePort,
                keystorePath,
                keystorePwdPath,
                clusterId,
                "/discovery",
                "500",           /* announce renewal period */
                "500",           /* topology update period  */
                "250",           /* view update period      */
                1000,            /* connection timeout      */
                1000,            /* socket timeout          */
                "250:500:2",     /* etcd back-off           */
                "250:500:2",     /* io error back-off       */
                slingId,
                "https://127.0.0.1:" + serverPort(server));
        instances.add(instance);
        executor.submit(instance);
        return instance;
    }

    private void waitForEstablishedView(List<Instance> instances) throws InterruptedException {
        int size = instances.size();
        boolean established = false;
        for ( ; ! established ; ) {
            boolean ie = true;
            for (Instance instance : instances) {
                ie &= instance.current() && instance.instancesInView(size);
            }
            established = ie;
            if (! established) {
                Thread.sleep(250);
            }
        }
    }

    private void stopInstances(List<Instance> instances) {
        for (Instance instance : instances) {
            instance.stop();
        }
    }

    private Server startSecureServer(HttpServlet servlet, String pathSpec, boolean requireClientAuth)
            throws Exception {

        String keyStorePwd = "testit";
        String keyStorePath = "/server-keystore.jks";

        char[] ksp = keyStorePwd.toCharArray();
        KeyStore keyStore = loadKeyStore(keyStorePath, ksp);

        Server server = new Server();

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStore(keyStore);
        sslContextFactory.setKeyStorePassword(keyStorePwd);
        sslContextFactory.setTrustStore(keyStore);
        sslContextFactory.setTrustStorePassword(keyStorePwd);

        SslConnector connector = new SslSelectChannelConnector(sslContextFactory);

        connector.setNeedClientAuth(requireClientAuth);
        server.setConnectors(new Connector[]{connector});
        ServletContextHandler sch = new ServletContextHandler(null, "/", false, false);

        sch.addServlet(new ServletHolder(servlet), pathSpec);
        server.setHandler(sch);
        server.start();
        return server;
    }

    private static int serverPort(Server server) {
        return server.getConnectors()[0].getLocalPort();
    }

    private String getAbsolutePath(String resourcePath)
            throws Exception {
        URL resource = getClass().getResource(resourcePath);
        return Paths.get(resource.toURI()).toFile().getAbsolutePath();
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