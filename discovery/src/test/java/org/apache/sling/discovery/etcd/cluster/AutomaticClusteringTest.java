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
package org.apache.sling.discovery.etcd.cluster;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junit.framework.Assert;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

public class AutomaticClusteringTest {

    @Test
    public void notSupportedRepository() throws Exception {
        SlingRepository nonOak = Mockito.mock(SlingRepository.class);
        Clustering clustering = new AutomaticClustering(nonOak, buildResolverFactory(buildSession()), "/etc/discovery/etcd");
        Assert.assertFalse(clustering.isSupported());
    }

    @Test
    public void supportedRepository() throws Exception {
        Clustering clustering = buildAutoClustering(buildResolverFactory(buildSession()));
        Assert.assertTrue(clustering.isSupported());
    }

    @Test
    public void testGetClusterId() throws Exception {
        Clustering clustering = buildAutoClustering(buildResolverFactory(buildSession()));
        Assert.assertNull(clustering.getClusterId());
    }

    @Test
    public void testSetClusterId() throws Exception {
        Clustering clustering = buildAutoClustering(buildResolverFactory(buildSession()));
        String clusterId = clustering.setClusterId();
        Assert.assertNotNull(clusterId);
        Assert.assertEquals(clusterId, clustering.getClusterId());
    }

    @Test
    public void testSetClusterIdMigrationFromSlingImpl() throws Exception {
        String id = "917956eb-6a71-4106-9b62-47229057679e";
        Clustering clustering = buildAutoClustering(buildResolverFactory(buildSession("/var/discovery/impl/establishedView/" + id)));
        String clusterId = clustering.setClusterId();
        Assert.assertEquals(id, clusterId);
    }

    private AutomaticClustering buildAutoClustering(ResourceResolverFactory factory) throws LoginException, RepositoryException {
        return new AutomaticClustering(buildOakMock(), factory, "/etc/discovery/etcd");
    }

    private Session buildSession(String... paths) throws RepositoryException {
        Session session = Mockito.spy(MockJcr.newSession("admin", null));
        for (String path : paths) {
            JcrUtils.getOrCreateByPath(path, "nt:unstructured", session);
        }
        return session;
    }

    private ResourceResolverFactory buildResolverFactory(Session session)
            throws LoginException, RepositoryException {
        doReturn(session).when(session).impersonate(any(Credentials.class));
        doReturn(false).when(session).isLive();
        ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
        Mockito.when(resolver.adaptTo(Session.class))
                .thenReturn(session);
        ResourceResolverFactory factory = Mockito.mock(ResourceResolverFactory.class);
        Mockito.when(factory.getAdministrativeResourceResolver(Mockito.anyMapOf(String.class, Object.class)))
                .thenReturn(resolver);
        return factory;
    }

    private SlingRepository buildOakMock() {
        SlingRepository mock = Mockito.mock(SlingRepository.class);
        Mockito.when(mock.getDescriptor(Repository.REP_NAME_DESC))
                .thenReturn("Apache Jackrabbit Oak");
        return mock;
    }

}