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

import java.util.UUID;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeType;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code AutomaticClustering} clustering does use the repository in order to
 * determine the cluster identifier the local instance belongs to.<p>
 *
 * The cluster identifier is stored in the repository at a location shared by all
 * instances connected to the same repository.<p>
 *
 * If the cluster id is not yet present in the repository,
 * a random UUID will be generated and set to the shared location atomically.<p>
 *
 * The {@code AutomaticClustering} clustering supports Oak repositories only.
 */
/*
 * Note: The implementation uses an admin session where a service user may fit better.
 * This is motivated by the fact Sling does not allow installing a service user
 * via its content loader: https://cwiki.apache.org/confluence/display/SLING/Content+Loading
 *
 * Furthermore, the Oak API to create service users is not exposed which makes it
 * impossible to create service users programmatically.
 * The implementation could be improved to use service users once support is available.
 */
public class AutomaticClustering implements Clustering {

    private static final Logger LOG = LoggerFactory.getLogger(AutomaticClustering.class);

    /**
     * Pattern to detect oak backend from the repository name.
     */
    private static final Pattern OAK = Pattern.compile(Pattern.quote("oak"), Pattern.CASE_INSENSITIVE);

    /**
     * Sling repository based discovery impl view path.
     */
    private static final String SLING_DISCOVERY_VIEW = "/var/discovery/impl/establishedView";

    /**
     * Path to the discovery node path. The path must be shared among instances.
     */
    private final String discoveryPath;

    /**
     * Property to store the cluster identifier on the discovery node.
     */
    private static final String PN_CLUSTER_ID = "clusterId";

    /**
     * Name of the Oak session attribute to set the session refresh strategy.
     */
    private static final String REFRESH_INTERVAL = "oak.refresh-interval";

    private final ResourceResolverFactory resolverFactory;

    private final boolean supported;

    private final String repositoryName;

    public AutomaticClustering(@Nonnull SlingRepository repository, @Nonnull ResourceResolverFactory resolverFactory, @Nonnull String discoveryPath) {
        this.discoveryPath = discoveryPath;
        this.resolverFactory = resolverFactory;
        repositoryName = getRepositoryName(repository);
        // Apache Jackrabbit Oak
        // use run modes instead ?
        supported = OAK.matcher(repositoryName).find();
    }

    @Nullable
    public String getClusterId() {

        if(! supported) {
            throwUnsupported();
        }

        ResourceResolver adminResolver = null;
        try {
            adminResolver = resolverFactory.getAdministrativeResourceResolver(null);
            return getClusterId(adminResolver);
        } catch (LoginException e) {
            LOG.warn("Failed to login to the repository: {}", e.getMessage());
            return null;
        } finally {
            if (adminResolver != null) {
                adminResolver.close();
            }
        }
    }

    /**
     * Attempt to set a random cluster identifier in the repository
     * using an atomic "set if not present" operation (leveraging Oak sequential consistency model).
     *
     * @return the cluster identifier from the repository or {@code null} if the operation failed.
     */
    @Nullable
    public String setClusterId() {

        if(! supported) {
            throwUnsupported();
        }

        Session autoRefreshAdmin = null;
        Session noRefreshAdmin = null;

        try {
            // get an auto refresh admin session
            autoRefreshAdmin = resolverFactory.getAdministrativeResourceResolver(null).adaptTo(Session.class);

            // setup the discovery structure if needed
            try {
                JcrResourceUtil.createPath(discoveryPath, NodeType.NT_UNSTRUCTURED, NodeType.NT_UNSTRUCTURED, autoRefreshAdmin, true);
            } catch (Exception e) {
                LOG.error("Failed to setup automatic clustering at path: {}", discoveryPath, e);
                return null;
            }

            String generatedClusterId = generateClusterId(autoRefreshAdmin);

            // attempt to set the cluster id property in the repository
            // this is done using a session not leveraging the auto-refresh policy.
            // by default, admin session is auto save where others are not.
            // the implementation uses a hack to impersonate the admin session to
            // another admin session with auto-save mode disabled.
            SimpleCredentials credentials = new SimpleCredentials(autoRefreshAdmin.getUserID(), new char[0]);
            credentials.setAttribute(REFRESH_INTERVAL, null);
            noRefreshAdmin = autoRefreshAdmin.impersonate(credentials);

            try {
                Node discoveryNode = noRefreshAdmin.getNode(discoveryPath);
                String clusterId = getClusterId(discoveryNode);
                if(clusterId != null) {
                    LOG.debug("Found cluster id: {} at path: {}", new Object[]{discoveryPath, generatedClusterId});
                    return clusterId;
                } else {
                    // attempt to store the random
                    // cluster id in the repository
                    discoveryNode.setProperty(PN_CLUSTER_ID, generatedClusterId);
                    noRefreshAdmin.save();
                    LOG.debug("Successfully stored cluster id: {} under path: {}", new Object[]{discoveryPath, generatedClusterId});
                    return generatedClusterId;
                }
            } catch (RepositoryException e) {
                LOG.error("Failed to get the discovery node at path: {}", discoveryPath, e);
                return null;
            }
        } catch (LoginException e) {
            LOG.warn("Failed to log to the repository: {}", e.getMessage());
            return null;
        } catch (RepositoryException e) {
            LOG.warn(e.getMessage(), e);
            return null;
        } finally {
            close(noRefreshAdmin);
            close(autoRefreshAdmin);
        }
    }

    /**
     * @return {@code true} if the repository is Oak based ; {@code false} otherwise.
     */
    @Override
    public boolean isSupported() {
        return supported;
    }

    //

    @Nonnull
    private String generateClusterId(@Nonnull Session session) {
        // Attempt to reuse the cluster identifier set by the Sling discovery.impl
        // in order to allow migrating from Sling discovery.impl to etcd.discovery.
        try {
            if (session.itemExists(SLING_DISCOVERY_VIEW)) {
                Node parent =  session.getNode(SLING_DISCOVERY_VIEW);
                if (parent.hasNodes()) {
                    return parent.getNodes().nextNode().getName();
                }
            }
        } catch (RepositoryException e) {
            LOG.debug(e.getMessage(), e);
        }
        // If not defined, then a random uuid is generated.
        return UUID.randomUUID().toString();
    }


    @Nullable
    private String getClusterId(@Nonnull ResourceResolver resolver) {
        String tenantIdPath = String.format("%s/%s", discoveryPath, PN_CLUSTER_ID);
        try {
            return resolver.adaptTo(Session.class).getProperty(tenantIdPath).getString();
        } catch (RepositoryException e) {
            LOG.debug(e.getMessage(), e);
            return null;
        }
    }

    @Nullable
    private String getClusterId(@Nonnull Node clusterNode) {
        try {
            return clusterNode.getProperty(PN_CLUSTER_ID).getString();
        } catch (RepositoryException e) {
            return null;
        }
    }

    @Nonnull
    private String getRepositoryName(@Nonnull SlingRepository repository) {
        String name = repository.getDescriptor(Repository.REP_NAME_DESC);
        return (name != null) ? name : "";
    }

    private void throwUnsupported() {
        throw new IllegalArgumentException(String.format("The automatic clustering is not supported for the repository: %s", repositoryName));
    }

    private void close(Session session) {
        if (session != null && session.isLive()) {
            session.logout();
        }
    }
}
