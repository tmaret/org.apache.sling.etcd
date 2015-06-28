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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.sling.etcd.client.impl.EtcdClientFactoryImpl;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolConfig;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.InstanceFilter;
import org.apache.sling.discovery.PropertyProvider;
import org.apache.sling.discovery.TopologyView;
import org.apache.sling.settings.SlingSettingsService;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Instance implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(Instance.class);

    private final EtcdDiscoveryService etcdDiscoveryService;

    private final List<AssertTopologyEventListener> listeners = new ArrayList<AssertTopologyEventListener>();

    private final ExecutorService exec;

    private final String slingId;

    public Instance(int instancePort,
             String keystorePath,
             String keystorePwdPath,
             String clusterId,
             String rootKey,
             String announceRenewalPeriod,
             String topologyUpdatePeriod,
             String viewUpdatePeriod,
             int connectionTimeout,
             int socketTimeout,
             String etcdBackOff,
             String ioErrorBackOff,
             String slingId,
             String... endPoint) throws Exception {

        etcdDiscoveryService = new EtcdDiscoveryService();

        this.slingId = slingId;

        SlingSettingsService slingSettingsMock = Mockito.mock(SlingSettingsService.class);
        Mockito.when(slingSettingsMock.getSlingId())
                .thenReturn(slingId);
        setField(etcdDiscoveryService, "slingSettingsService", slingSettingsMock);
        exec = Executors.newCachedThreadPool();
        ThreadPool tp = new ThreadPool() {
            @Override
            public void execute(Runnable runnable) {
                exec.execute(runnable);
            }

            @Override
            public String getName() {
                return "Mock etcd Sling threadPool";
            }

            @Override
            public ThreadPoolConfig getConfiguration() {
                return null;
            }
        };
        ThreadPoolManager tpm = Mockito.mock(ThreadPoolManager.class);
        Mockito.when(tpm.get(Mockito.anyString()))
                .thenReturn(tp);
        setField(etcdDiscoveryService, "threadPoolManager", tpm);

        setField(etcdDiscoveryService, "etcdClientFactory", new EtcdClientFactoryImpl());

        bindTopologyEventListener(new AssertTopologyEventListener(), 10000);

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(EtcdDiscoveryService.ANNOUNCE_RENEWAL_PERIOD, announceRenewalPeriod);
        properties.put(EtcdDiscoveryService.ROOT_KEY, rootKey);
        properties.put(EtcdDiscoveryService.TOPOLOGY_UPDATE_PERIOD, topologyUpdatePeriod);
        properties.put(EtcdDiscoveryService.VIEW_UPDATE_PERIOD, viewUpdatePeriod);
        properties.put(EtcdDiscoveryService.CONNECTION_TIMEOUT, connectionTimeout);
        properties.put(EtcdDiscoveryService.SOCKET_TIMEOUT, socketTimeout);
        properties.put(EtcdDiscoveryService.ETCD_BACK_OFF, etcdBackOff);
        properties.put(EtcdDiscoveryService.IO_ERROR_BACK_OFF, ioErrorBackOff);
        properties.put(EtcdDiscoveryService.ENDPOINT, endPoint);
        properties.put(EtcdDiscoveryService.CLUSTERING_MODE, EtcdDiscoveryService.CONFIG_CLUSTERING);
        properties.put(EtcdDiscoveryService.CLUSTER_ID, clusterId);
        if (keystorePath != null) {
            properties.put(EtcdDiscoveryService.KEYSTORE_FILE_PATH, keystorePath);
            properties.put(EtcdDiscoveryService.KEYSTORE_PWD_FILE_PATH, keystorePwdPath);
        }

        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.getProperty("org.osgi.service.http.port"))
                .thenReturn(String.valueOf(instancePort));

        ComponentContext cc = Mockito.mock(ComponentContext.class);
        Mockito.when(cc.getProperties())
                .thenReturn(properties);
        Mockito.when(cc.getBundleContext())
                .thenReturn(bc);

        etcdDiscoveryService.activate(cc);

        bindTopologyEventListener(new AssertTopologyEventListener(), 20000);

    }

    public void bindPropertyProvider(final Map<String, Object> properties, int serviceRanking, int serviceId) {
        Map<String, Object> serviceConfig = buildServiceConfig(properties.keySet(), serviceRanking, serviceId);
        PropertyProvider provider = new PropertyProvider() {
            @Override
            public String getProperty(String key) {
                return String.valueOf(properties.get(key));
            }
        };
        getEtcdDiscoveryService().bindPropertyProvider(provider, serviceConfig);
    }

    public void bindTopologyEventListener(AssertTopologyEventListener listener, int serviceId) {
        getListeners().add(listener);
        getEtcdDiscoveryService().bindTopologyEventListener(listener);
    }

    public EtcdDiscoveryService getEtcdDiscoveryService() {
        return etcdDiscoveryService;
    }

    public List<AssertTopologyEventListener> getListeners() {
        return listeners;
    }

    public TopologyView topology() {
        return etcdDiscoveryService.getTopology();
    }

    public String clusterViewId() {
        return localInstance().getClusterView().getId();
    }

    public boolean current() {
        boolean isCurrent = topology().isCurrent();
        LOG.info("instance: {} sees not current view", dumpState());
        return isCurrent;
    }

    public boolean instancesInView(int nb) {
        int actual = localInstance().getClusterView().getInstances().size();
        if (actual != nb) {
            LOG.info("instance: {} sees a view with size: {} where expected size is: {}", new Object[]{dumpState(), actual, nb});
        }
        return nb == actual;
    }

    public boolean propertyValue(final String slingId, String valueName, String expectedValue) {
        Set<InstanceDescription> match = topology().findInstances(new InstanceFilter() {
            @Override
            public boolean accept(InstanceDescription id) {
                return id.getSlingId().equals(slingId);
            }
        });
        if (match.size() > 0) {
            String p = match.iterator().next().getProperties().get(valueName);
            return p != null && p.equals(expectedValue);
        }
        return false;
    }

    public InstanceDescription localInstance() {
        return topology().getLocalInstance();
    }

    @Override
    public void run() {

    }

    public void stop() {
        exec.shutdownNow();
    }

    //

    private String dumpState() {
        return String.format("%s state: %s", slingId, this.getEtcdDiscoveryService().getContext().getState());
    }

    private Map<String, Object> buildServiceConfig(Set<String> propertyNames, int ranking, long serviceId) {
        Map<String, Object> configs = new HashMap<String, Object>();
        if (propertyNames.size() == 1) {
            configs.put(PropertyProvider.PROPERTY_PROPERTIES, propertyNames.iterator().next());
        } else if (propertyNames.size() > 1) {
            configs.put(PropertyProvider.PROPERTY_PROPERTIES, propertyNames.toArray(new String[propertyNames.size()]));
        }
        configs.put("service.ranking", ranking);
        configs.put("service.id", serviceId);
        return configs;
    }

    private static void setField(Object o, String fieldName, Object value)
            throws Exception {
        Field field = o.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(o, value);
    }
}
