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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.Charsets;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.sling.discovery.etcd.backoff.BackOff;
import org.apache.sling.discovery.etcd.backoff.ConstantBackOff;
import org.apache.sling.discovery.etcd.backoff.SquareBackOff;
import org.apache.sling.discovery.etcd.cluster.AutomaticClustering;
import org.apache.sling.discovery.etcd.cluster.Clustering;
import org.apache.sling.discovery.etcd.cluster.ConfigClustering;
import org.apache.sling.discovery.etcd.fsm.Context;
import org.apache.sling.discovery.etcd.fsm.Event;
import org.apache.sling.discovery.etcd.fsm.RunnerFactory;
import org.apache.sling.discovery.etcd.fsm.RunnerFactoryImpl;
import org.apache.sling.discovery.etcd.gzip.GzipRequestInterceptor;
import org.apache.sling.discovery.etcd.gzip.GzipResponseInterceptor;
import org.apache.sling.discovery.etcd.run.Announcer;
import org.apache.sling.discovery.etcd.run.LocalUpdater;
import org.apache.sling.discovery.etcd.fsm.States;
import org.apache.sling.discovery.etcd.run.RemoteUpdater;
import org.apache.sling.etcd.client.EtcdClient;
import org.apache.sling.etcd.client.EtcdClientFactory;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.discovery.PropertyProvider;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
@Component(immediate = true, metatype = true, label = "CoreOS etcd discovery",
        description = "Note regarding wait function parameters: The mathematical function can either be I. a constant or II. a square function. " +
                "The function is determined by the parameters provided. In order to use a constant function, provides a constant in " +
                "millisecond (e.g. '20000' for a constant interval of 20 seconds). In order to use a power function, provides the minimum " +
                "and maximum interval in milliseconds as well as the number of consecutive steps to go from the minimum to the maximum " +
                "interval (e.g. '10000:60000:3' for an interval of minimum 10 seconds, maximum 60 seconds and a progression in 3 steps).")
@References({
        @Reference(name = "topologyEventListener", referenceInterface = TopologyEventListener.class,
                bind = "bindTopologyEventListener", unbind = "unbindTopologyEventListener",
                cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "propertyProvider", referenceInterface = PropertyProvider.class,
                bind = "bindPropertyProvider", unbind = "unbindPropertyProvider",
                cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)})
public class EtcdDiscoveryService implements DiscoveryService {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdDiscoveryService.class);

    /**
     * The UTF8 charset.
     */
    private static final String UTF8 = "UTF-8";

    /**
     * Leeway factor to be applied to the announce TTL
     */
    public static final double ANNOUNCE_TTL_LEEWAY = 1.25D;

    /**
     * Define the path of the node holding the clusterId for automatic clustering mode.
     * The path must point to a resource that is shared among all instances.
     */
    private static final String DISCOVERY_PATH = "/etc/discovery/etcd";

    private static final String DEFAULT_ROOT_KEY = "/discovery";

    /**
     * The root key for implementing the discovery protocol.
     */
    @Property(label = "Root key", description = "The etcd root key used for implementing the discovery protocol, e.g. '/discovery'",
            value = DEFAULT_ROOT_KEY)
    protected static final String ROOT_KEY = "rootKey";

    private static final String DEFAULT_ENDPOINT = "http://127.0.0.1:4001";

    /**
     * The etcd endpoint to access the etcd peers.
     */
    @Property(label = "etcd endpoint", description = "The endpoint uri to access the etcd peers (e.g. 'http://127.0.0.1:4001'). " +
            "If the client is communicating with an etcd cluster, then the endpoint should reference a readwrite etcd proxy node. " +
            "If the client is communicating with a single etcd node, the endpoint could reference it directly.",
            value = DEFAULT_ENDPOINT)
    protected static final String ENDPOINT = "endpoint";

    private static final int DEFAULT_SOCKET_TIMEOUT = 5000;

    /**
     * The connection socket timeout.
     * @link org.apache.http.client.configRequestConfig#getSocketTimeout()
     */
    @Property(label = "Socket Timeout", description = "Defines the socket timeout (SO_TIMEOUT) in milliseconds, which " +
            "is the timeout for waiting for data. A timeout value of zero is interpreted as an infinite timeout. " +
            "A negative value is interpreted as undefined (system default).",
            intValue = DEFAULT_SOCKET_TIMEOUT)
    protected static final String SOCKET_TIMEOUT = "socketTimeout";

    private static final int DEFAULT_CONNECTION_TIMEOUT = 2500;

    /**
     * The connection socket connection timeout.
     * @link org.apache.http.client.configRequestConfig#getConnectTimeout()
     */
    @Property(label = "Connection Timeout", description = "Determines the timeout in milliseconds until a connection is " +
            "established. A timeout value of zero is interpreted as an infinite timeout. A timeout value of zero is " +
            "interpreted as an infinite timeout. A negative value is interpreted as undefined (system default).",
            intValue = DEFAULT_CONNECTION_TIMEOUT)
    protected static final String CONNECTION_TIMEOUT = "connectionTimeout";

    private static final String DEFAULT_ANNOUNCE_RENEWAL_PERIOD = "20000";

    @Property(label = "Announce Renewal Period", description = "The parameters of the mathematical function used to compute the time " +
            "interval between two announce renewals. An announce renewal consists of extending the ttl of the local instance announce key on etcd." +
            "See 'wait function parameters' above for details regarding the possible functions." +
            "The steps are incremented whenever the operation is successful and reset to 0 in case of failure.",
            value = DEFAULT_ANNOUNCE_RENEWAL_PERIOD)
    protected static final String ANNOUNCE_RENEWAL_PERIOD = "announceRenewalPeriod";

    private static final String DEFAULT_TOPOLOGY_UPDATE_PERIOD = "10000";

    @Property(label = "Remote Topology Update Period", description = "The parameters of the mathematical function used to compute the time " +
            "interval between two remote topology updates. A remote topology update consists of fetching the announces and properties (if needed) from etcd for the remote instances." +
            "See 'wait function parameters' above for details regarding the possible functions." +
            "The steps are incremented whenever the operation is successful and reset to 0 in case of failure.",
            value = DEFAULT_TOPOLOGY_UPDATE_PERIOD)
    protected static final String TOPOLOGY_UPDATE_PERIOD = "topologyUpdatePeriod";

    private static final String DEFAULT_VIEW_UPDATE_PERIOD = "2000";

    @Property(label = "Topology View Update Period", description = "The period in seconds between two updates of the" +
            "topology view", value = DEFAULT_VIEW_UPDATE_PERIOD, propertyPrivate = true)
    protected static final String VIEW_UPDATE_PERIOD = "viewUpdatePeriod";

    /**
     * 1 second to 20 minutes in 50 steps.
     */
    private static final String DEFAULT_ETCD_BACK_OFF = "1000:1200000:50";

    @Property(label = "etcd error Back-off period", description = "The parameters of the mathematical function used to compute the back-off " +
            "interval to wait upon etcd internal errors." +
            "See 'wait function parameters' above for details regarding the possible functions." +
            "The steps are incremented whenever the etcd produces an internal error and reset to 0 in case of non erroneous behavior.",
            value = DEFAULT_ETCD_BACK_OFF)
    protected static final String ETCD_BACK_OFF = "etcdBackOff";

    /**
     * 1 seconds to 10 minutes in 50 steps.
     */
    private static final String DEFAULT_IO_ERROR_BACK_OFF = "1000:600000:50";

    @Property(label = "etcd I/O error back-off period", description = "The parameters of the mathematical function used to compute the " +
            "back-off interval upon I/O errors." +
            "See 'wait function parameters' above for details regarding the possible functions." +
            "The steps are incremented whenever the communication with etcd produces an I/O error and reset to 0 in case of  successful communication.",
            value = DEFAULT_IO_ERROR_BACK_OFF)
    protected static final String IO_ERROR_BACK_OFF = "ioErrorBackOff";

    /**
     * Automatic clustering leverages the repository for defining the
     * cluster id in which the local instance belongs.
     */
    protected static final String AUTOMATIC_CLUSTERING = "autoClustering";

    /**
     * Config clustering uses a configuration to define the cluster id in
     * which the local instance belongs.
     */
    protected static final String CONFIG_CLUSTERING = "configClustering";

    @Property(label = "Clustering mode", description = "With 'Automatic' mode (default) the implementation computes " +
            "the cluster identifier of the local instance automatically, by using the repository. " +
            "The 'Automatic' mode create clusters of instances that share the same repository. " +
            "The 'Automatic' mode is only supported by Oak based repositories. " +
            "If the 'Automatic' mode is selected but the repository is not supported, then the implementation " +
            "falls back to the 'Configuration' mode." +
            "The 'Configuration' mode allows to specify the cluster identifier the local instance belongs to, " +
            "via the 'clusterId' property.",
            value = AUTOMATIC_CLUSTERING, options = {
            @PropertyOption(name = AUTOMATIC_CLUSTERING, value = "Automatic"),
            @PropertyOption(name = CONFIG_CLUSTERING, value = "Configuration") })
    protected static final String CLUSTERING_MODE = "clusteringMode";

    /**
     * Default cluster identifier.
     */
    private static final String DEFAULT_CLUSTER_ID = "default";

    @Property(label = "Cluster identifier", description = "The cluster identifier in which the instance belongs to. " +
            "This identifier is used with 'Configuration' clustering mode and when the 'Automatic' clustering mode " +
            "is not supported by the repository. The cluster identifier is case sensitive and must contain only " +
            "alphanumeric characters as well as the characters '-' and '_'. If the configured cluster identifier does " +
            "not comply, the default cluster id '" + DEFAULT_CLUSTER_ID + "' will be used.",
            value = DEFAULT_CLUSTER_ID)
    protected static final String CLUSTER_ID = "clusterId";

    @Property(label = "Keystore file path", description = "The path to the keystore containing key material " +
            "(private key, certificate) for the local instance and/or trust material (certificates) used by the " +
            "etcd client. The configuration is optional. If the path is blank, the Keystore provided by the standard " +
            "JSSE mechanism is used. This configuration is aiming at deployments which use certificates signed by " +
            "a non standard CA root and which want to use the Keystore for etcd support only. The keystore default " +
            "type is JKS but can be overriden with the 'keystore.type' system property.", value = "")
    protected static final String KEYSTORE_FILE_PATH = "keystoreFilePath";

    @Property(label = "Keystore password file path", description = "The path to the UTF-8 encoded file that contains " +
            "the KeyStore password in clear text. The password is required only when the 'keystoreFilePath' " +
            "is defined.", value = "")
    protected static final String KEYSTORE_PWD_FILE_PATH = "keystorePwdFilePath";

    @Reference
    private EtcdClientFactory etcdClientFactory;

    @Reference
    private SlingSettingsService slingSettingsService;

    @Reference
    private ThreadPoolManager threadPoolManager;

    @Reference
    private SlingRepository slingRepository;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private ThreadPool etcdThreadPool;

    private Announcer announcer;

    private RemoteUpdater remoteUpdater;

    private LocalUpdater localUpdater;

    private CloseableHttpClient httpClient;

    private EtcdService etcdService;

    private int socketTimeout;

    private int connectionTimeout;

    private Context context;

    private PoolingHttpClientConnectionManager connectionManager;

    private final ViewManager viewManager = new ViewManager();

    private final PropertiesService propertiesService = new PropertiesService();

    private volatile EtcdStats etcdStats;

    @Activate
    protected void activate(ComponentContext cc) {

        final ThreadPoolManager threadPoolManager = this.threadPoolManager;
        if (threadPoolManager == null) {
            throw new IllegalStateException("ThreadPoolManager service not found");
        }

        final SlingSettingsService slingSettingsService = this.slingSettingsService;
        if (slingSettingsService == null) {
            throw new IllegalStateException("SlingSettingsService service not found");
        }

        final EtcdClientFactory etcdClientFactory = this.etcdClientFactory;
        if (etcdClientFactory == null) {
            throw new IllegalStateException("EtcdClientFactory service not found");
        }

        Dictionary props = cc.getProperties();
        String rootKey = PropertiesUtil.toString(
                props.get(ROOT_KEY),
                DEFAULT_ROOT_KEY);
        if (rootKey.endsWith("/") && rootKey.length() > 1) {
            rootKey = rootKey.substring(0, rootKey.length() - 1);
            LOG.debug("Chopped last slash from rootKey: {}", rootKey);
        }
        socketTimeout = PropertiesUtil.toInteger(
                props.get(SOCKET_TIMEOUT),
                DEFAULT_SOCKET_TIMEOUT);
        connectionTimeout = PropertiesUtil.toInteger(
                props.get(CONNECTION_TIMEOUT),
                DEFAULT_SOCKET_TIMEOUT);
        String slingId = slingSettingsService.getSlingId();
        String serverInfo = getServerInfo(cc);
        LOG.debug("slingId: {} serverInfo: {}", new String[]{slingId, serverInfo});
        BackOff announceRenewalPeriod = build(PropertiesUtil.toString(props.get(ANNOUNCE_RENEWAL_PERIOD),
                DEFAULT_ANNOUNCE_RENEWAL_PERIOD), DEFAULT_ANNOUNCE_RENEWAL_PERIOD);
        LOG.debug("announce renewal period: {}", announceRenewalPeriod);
        BackOff topologyUpdatePeriod = build(PropertiesUtil.toString(props.get(TOPOLOGY_UPDATE_PERIOD),
                DEFAULT_TOPOLOGY_UPDATE_PERIOD), DEFAULT_TOPOLOGY_UPDATE_PERIOD);
        LOG.debug("topology update period: {}", topologyUpdatePeriod);
        BackOff viewUpdatePeriod = build(PropertiesUtil.toString(props.get(VIEW_UPDATE_PERIOD),
                DEFAULT_VIEW_UPDATE_PERIOD), DEFAULT_VIEW_UPDATE_PERIOD);
        LOG.debug("view update period: {}", viewUpdatePeriod);
        BackOff etcdBackOff = build(PropertiesUtil.toString(props.get(ETCD_BACK_OFF),
                DEFAULT_ETCD_BACK_OFF), DEFAULT_ETCD_BACK_OFF);
        LOG.debug("etcd back-off: {}", etcdBackOff);
        BackOff ioErrorBackOff = build(PropertiesUtil.toString(props.get(IO_ERROR_BACK_OFF),
                DEFAULT_IO_ERROR_BACK_OFF), DEFAULT_IO_ERROR_BACK_OFF);
        LOG.debug("I/O error back-off: {}", ioErrorBackOff);
        int maxAnnounceTtl = new BigDecimal(announceRenewalPeriod.max() * EtcdDiscoveryService.ANNOUNCE_TTL_LEEWAY / 1000.0D)
                .setScale(0, BigDecimal.ROUND_UP).intValue();
        LOG.debug("max announce ttl in second: {}", maxAnnounceTtl);
        String clusterId = PropertiesUtil.toString(
                props.get(CLUSTER_ID),
                DEFAULT_CLUSTER_ID);
        String clusteringMode = PropertiesUtil.toString(
                props.get(CLUSTERING_MODE),
                AUTOMATIC_CLUSTERING);
        final Clustering clustering;
        if (AUTOMATIC_CLUSTERING.equals(clusteringMode)) {

            final SlingRepository slingRepository = this.slingRepository;
            if (slingRepository == null) {
                throw new IllegalStateException("SlingRepository service not found");
            }

            final ResourceResolverFactory resourceResolverFactory = this.resourceResolverFactory;
            if (resourceResolverFactory == null) {
                throw new IllegalStateException("ResourceResolverFactory service not found");
            }

            Clustering automatic = new AutomaticClustering(slingRepository, resourceResolverFactory, DISCOVERY_PATH);
            if (automatic.isSupported()) {
                // repository mode supported.
                // we don't get the cluster id here,
                // as the method call may take time.
                clustering = automatic;
                LOG.info("Clustering mode 'Automatic' enabled.");
            } else {
                LOG.warn("The repository does not support 'Automatic' clustering mode. Falling back to 'Configuration' clustering mode.");
                clustering = buildConfigClustering(clusterId);
            }
        } else {
            clustering = buildConfigClustering(clusterId);
        }

        buildHttpClient(PropertiesUtil.toString(props.get(KEYSTORE_FILE_PATH), "").trim(),
                PropertiesUtil.toString(props.get(KEYSTORE_PWD_FILE_PATH), "").trim());
        URI endpoint = parseEndpoint(PropertiesUtil.toString(
                props.get(ENDPOINT),
                DEFAULT_ENDPOINT));
        EtcdClient etcdClient = etcdClientFactory.create(httpClient, endpoint);
        etcdStats = new EtcdStats(etcdClient);
        PropertiesMap propertiesMap = new PropertiesMap(slingId);
        Announce initAnnounce = buildInitAnnounce(slingId, serverInfo);
        AnnouncesMap announcesMap = new AnnouncesMap(initAnnounce);
        viewManager.updateView(buildInitView(initAnnounce, slingId)); // must happen before starting the LocalUpdater thread.
        etcdService = new EtcdService(etcdClient, rootKey);
        RunnerFactory factory = new RunnerFactoryImpl(etcdService, announcesMap, clustering, etcdBackOff, ioErrorBackOff, slingId, serverInfo, maxAnnounceTtl);
        etcdThreadPool = threadPoolManager.get("CoreOS etcd client threads");
        context = new Context(States.GET_CLUSTER, factory, etcdThreadPool);
        context.init(States.GET_CLUSTER);
        announcer = new Announcer(context,
                etcdService,
                announcesMap,
                propertiesMap,
                slingId,
                serverInfo,
                announceRenewalPeriod);
        etcdThreadPool.execute(announcer);
        remoteUpdater = new RemoteUpdater(context,
                etcdService,
                topologyUpdatePeriod,
                announcesMap,
                propertiesMap,
                slingId);
        etcdThreadPool.execute(remoteUpdater);
        localUpdater = new LocalUpdater(context,
                propertiesService,
                viewManager,
                viewUpdatePeriod,
                announcesMap,
                propertiesMap,
                slingId,
                serverInfo);
        etcdThreadPool.execute(localUpdater);
        LOG.info("Activated etcd discovery service for slingId: {}, serverInfo: {}, rootKey: {}", new Object[]{slingId, serverInfo, rootKey});
    }

    @Deactivate
    protected void deactivate() {
        if (context != null) {
            context.next(Event.STOPPED);
        }
        IOUtils.closeQuietly(httpClient);
        if (connectionManager != null) {
            IOUtils.closeQuietly(connectionManager);
        }
        if (announcer != null) {
            announcer.stop();
        }
        if (remoteUpdater != null) {
            remoteUpdater.stop();
        }
        if (localUpdater != null) {
            localUpdater.stop();
        }
        if (etcdThreadPool != null) {
            threadPoolManager.release(etcdThreadPool);
            etcdThreadPool = null;
        }
        httpClient = null;
        connectionManager = null;
        etcdService = null;
        LOG.info("Deactivated etcd discovery service");
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public TopologyView getTopology() {
        return viewManager.getView();
    }

    @Nonnull
    protected Context getContext() {
        return context;
    }

    @Nullable
    protected EtcdStats getEtcdStats() {
        return etcdStats;
    }

    private void buildHttpClient(@Nonnull String keystoreFilePath, @Nonnull String keystorePwdFilePath) {

        boolean hasKeyStore = ! isEmpty(keystoreFilePath);

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(socketTimeout)
                .setConnectTimeout(connectionTimeout)
                .setRedirectsEnabled(true)
                .setStaleConnectionCheckEnabled(true)
                .build();

        HttpClientBuilder builder = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .addInterceptorFirst(new GzipRequestInterceptor())
                .addInterceptorFirst(new GzipResponseInterceptor());

        if (hasKeyStore) {

            final SSLContextBuilder sslContextBuilder = SSLContexts.custom();

            LOG.info("Loading keystore from file: {}", keystoreFilePath);
            char[] pwd = readPwd(keystorePwdFilePath);
            try {
                KeyStore keystore = loadKeyStore(keystoreFilePath, pwd);
                sslContextBuilder.loadTrustMaterial(keystore);
                sslContextBuilder.loadKeyMaterial(keystore, pwd);
                LOG.info("Setup custom SSL context");
                SSLConnectionSocketFactory sslConnectionSocketFactory =
                        new SSLConnectionSocketFactory(sslContextBuilder.build());
                Registry<ConnectionSocketFactory> connectionSocketFactory =
                        RegistryBuilder.<ConnectionSocketFactory> create()
                                .register("http", PlainConnectionSocketFactory.INSTANCE)
                                .register("https", sslConnectionSocketFactory).build();

                builder.setSSLSocketFactory(sslConnectionSocketFactory);

                connectionManager = new PoolingHttpClientConnectionManager(connectionSocketFactory);

            } catch (UnrecoverableKeyException e) {
                throw wrap(e);
            } catch (NoSuchAlgorithmException e) {
                throw wrap(e);
            } catch (KeyStoreException e) {
                throw wrap(e);
            } catch (KeyManagementException e) {
                throw wrap(e);
            } finally {
                reset(pwd);
            }

        } else {
            connectionManager = new PoolingHttpClientConnectionManager();
        }

        builder.setConnectionManager(connectionManager);
        httpClient = builder.build();
    }

    @Nonnull
    private URI parseEndpoint(@Nonnull String ep) {
        try {
            return new URI(ep);
        } catch (URISyntaxException e) {
            LOG.info("Failed to parse endpoint: {}, using default endpoint: {}", new Object[]{ep, DEFAULT_ENDPOINT});
            try {
                return new URI(DEFAULT_ENDPOINT);
            } catch (URISyntaxException shouldNeverHappen) {
                throw new IllegalStateException(String.format("Failed to create uri from the default endpoint: %s", e.getMessage()));
            }
        }
    }

    @Nonnull
    private String getServerInfo(ComponentContext componentContext) {
        String servicePort = componentContext.getBundleContext()
                .getProperty("org.osgi.service.http.port");
        String port = servicePort != null ? servicePort : "";
        try {
            return InetAddress.getLocalHost().getCanonicalHostName()
                    + ":" + port;
        } catch (Exception e) {
            LOG.warn("Failed to filterBySlingId the instance canonical hostname", e);
            return "localhost:" + port;
        }
    }

    @Nonnull
    private Announce buildInitAnnounce(@Nonnull String slingId, @Nonnull String serverInfo) {
        AnnounceData data = new AnnounceData(slingId, serverInfo, "isolated", 0);
        return new Announce(data, "/isolated/0");
    }

    @Nonnull
    private EtcdTopologyView buildInitView(@Nonnull Announce local, @Nonnull String slingId) {
        return new EtcdTopologyView(new Announces(local),
                Collections.<String, Map<String, String>>emptyMap(), slingId, false);
    }

    @Nonnull
    private BackOff build(@Nonnull String config, @Nonnull String defaultConfig) {
        try {
            return build(config);
        } catch (Exception e) {
            LOG.info("Failed to parse back-off: {}, using default: {}", new String[]{config, defaultConfig});
            return build(defaultConfig);
        }
    }

    @Nonnull
    private BackOff build(@Nonnull String config) {
        String[] chunks = config.trim().split(":");
        if (chunks.length == 3) {
            // power back-off
            return new SquareBackOff(Long.valueOf(chunks[0]), Long.valueOf(chunks[1]), Integer.valueOf(chunks[2]));
        } else {
            return new ConstantBackOff(Long.parseLong(config));
        }
    }

    private Clustering buildConfigClustering(String clusterId) {
        Clustering clustering = new ConfigClustering(clusterId, DEFAULT_CLUSTER_ID);
        LOG.info(String.format("Clustering mode 'Configuration' enabled with cluster id: %s", clustering.getClusterId()));
        return clustering;
    }

    private boolean isEmpty(@Nullable String value) {
        return (value == null) || ("".equals(value));
    }

    @Nullable
    private char[] readPwd(@Nonnull String filePath) {
        if (! isEmpty(filePath)) {
            InputStream fis = null;
            try {
                fis = new FileInputStream(checkFile(new File(filePath)));
                return IOUtils.toString(fis, Charsets.toCharset(UTF8)).trim().toCharArray();
            } catch (FileNotFoundException e) {
                throw wrap(e);
            } catch (IOException e) {
                throw wrap(e);
            } finally {
                IOUtils.closeQuietly(fis);
            }
        }
        return null;

    }

    private void reset(@Nullable char[] array) {
        if (array != null) {
            Arrays.fill(array, ' ');
        }
    }

    @Nonnull
    private KeyStore loadKeyStore(@Nonnull String filePath, @Nullable char[] pwd) {
        InputStream is = null;
        try {
            is = new FileInputStream(checkFile(new File(filePath)));
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(is, pwd);
            return keyStore;
        } catch (CertificateException e) {
            throw wrap(e);
        } catch (NoSuchAlgorithmException e) {
            throw wrap(e);
        } catch (KeyStoreException e) {
            throw wrap(e);
        } catch (IOException e) {
            throw wrap(e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private File checkFile(@Nonnull File file) {
        if (! file.exists()) {
            throw new EtcdDiscoveryRuntimeException(String.format("The file: %s does not exist", file));
        }

        if (file.isDirectory()) {
            throw new EtcdDiscoveryRuntimeException(String.format("The file: %s is a directory", file));
        }

        if (! file.canRead()) {
            throw new EtcdDiscoveryRuntimeException(String.format("The file: %s can't be read", file));
        }
        return file;
    }

    private EtcdDiscoveryRuntimeException wrap(Exception e) {
        return new EtcdDiscoveryRuntimeException(e.getMessage(), e);
    }

    protected void bindTopologyEventListener(TopologyEventListener listener) {
        viewManager.bind(listener);
    }

    protected void unbindTopologyEventListener(TopologyEventListener listener) {
        viewManager.unbind(listener);
    }

    protected void bindPropertyProvider(PropertyProvider propertyProvider, Map<String, Object> properties) {
        propertiesService.bind(propertyProvider, properties);
    }

    protected void unbindPropertyProvider(PropertyProvider propertyProvider, Map<String, Object> properties) {
        propertiesService.unbind(propertyProvider, properties);
    }

}
