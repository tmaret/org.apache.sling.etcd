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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;
import org.apache.sling.discovery.PropertyProvider;
import org.junit.Test;

public class PropertiesServiceTest {

    @Test
    public void testBindPropertyProvider() throws Exception {
        PropertiesService service = new PropertiesService();
        Assert.assertNotNull(service.getProviders());
        Assert.assertEquals(0, service.getProviders().size());
        // add new provider
        Map<String, String> props1 = new HashMap<String, String>();
        PropProv prov1 = new PropProv(props1);
        service.bind(prov1, buildServiceConfig(prov1.getPropertyNames(), 1, 10));
        Assert.assertEquals(1, service.getProviders().size());
        // add new provider
        Map<String, String> props2 = new HashMap<String, String>();
        props2.put("name21", "value21");
        PropProv prov2 = new PropProv(props2);
        service.bind(prov2, buildServiceConfig(prov2.getPropertyNames(), 3, 20));
        Assert.assertEquals(2, service.getProviders().size());
        // add new provider
        Map<String, String> props3 = new HashMap<String, String>();
        props3.put("name31", "value31");
        props3.put("name32", "value32");
        PropProv prov3 = new PropProv(props3);
        service.bind(prov3, buildServiceConfig(prov3.getPropertyNames(), 2, 30));
        Assert.assertEquals(3, service.getProviders().size());
    }

    @Test
    public void testUnbindPropertyProvider() throws Exception {
        PropertiesService service = new PropertiesService();
        service.unbind(null, Collections.<String, Object>emptyMap());
        Assert.assertNotNull(service.getProviders());
        Assert.assertEquals(0, service.getProviders().size());
        // add new provider
        Map<String, String> props1 = new HashMap<String, String>();
        PropProv prov1 = new PropProv(props1);
        service.bind(prov1, buildServiceConfig(prov1.getPropertyNames(), 1, 10));
        Assert.assertEquals(1, service.getProviders().size());
        // remove provider
        service.unbind(prov1, buildServiceConfig(prov1.getPropertyNames(), 1, 10));
        Assert.assertEquals(0, service.getProviders().size());
    }

    @Test
    public void testGetPropertyProvidersOrder() throws Exception {
        PropertiesService service = new PropertiesService();
        // add new provider
        Map<String, String> props1 = new HashMap<String, String>();
        PropProv prov1 = new PropProv(props1);
        service.bind(prov1, buildServiceConfig(prov1.getPropertyNames(), 1, 10));
        // add new provider
        Map<String, String> props2 = new HashMap<String, String>();
        props2.put("name21", "value21");
        PropProv prov2 = new PropProv(props2);
        service.bind(prov2, buildServiceConfig(prov2.getPropertyNames(), 3, 20));
        // add new provider
        Map<String, String> props3 = new HashMap<String, String>();
        props3.put("name31", "value31");
        props3.put("name32", "value32");
        PropProv prov3 = new PropProv(props3);
        service.bind(prov3, buildServiceConfig(prov3.getPropertyNames(), 2, 30));
        // test order
        Collection<PropertiesService.Provider> providers = service.getProviders();
        Assert.assertNotNull(providers);
        Assert.assertEquals(3, providers.size());
        PropertiesService.Provider[] arrayOfProviders = providers.toArray(new PropertiesService.Provider[providers.size()]);
        Assert.assertEquals(10L, arrayOfProviders[0].serviceProperties.get("service.id"));
        Assert.assertEquals(30L, arrayOfProviders[1].serviceProperties.get("service.id"));
        Assert.assertEquals(20L, arrayOfProviders[2].serviceProperties.get("service.id"));
    }



    @Test
    public void testGetProperties() throws Exception {
        PropertiesService service = new PropertiesService();
        // add new provider
        Map<String, String> props1 = new HashMap<String, String>();
        PropProv prov1 = new PropProv(props1);
        service.bind(prov1, buildServiceConfig(prov1.getPropertyNames(), 1, 10));
        // add new provider
        Map<String, String> props2 = new HashMap<String, String>();
        props2.put("name21", "value21");
        PropProv prov2 = new PropProv(props2);
        service.bind(prov2, buildServiceConfig(prov2.getPropertyNames(), 3, 20));
        // add new provider
        Map<String, String> props3 = new HashMap<String, String>();
        props3.put("name31", "value31");
        props3.put("name32", "value32");
        PropProv prov3 = new PropProv(props3);
        service.bind(prov3, buildServiceConfig(prov3.getPropertyNames(), 2, 30));
        // test resulting properties
        Map<String, String> properties = service.load();
        Assert.assertNotNull(properties);
        Assert.assertEquals(3, properties.size());
    }

    @Test
    public void testGetPropertiesOverriding() throws Exception {
        PropertiesService service = new PropertiesService();
        // add new provider
        Map<String, String> props1 = new HashMap<String, String>();
        PropProv prov1 = new PropProv(props1);
        service.bind(prov1, buildServiceConfig(prov1.getPropertyNames(), 1, 10));
        // add new provider
        Map<String, String> props2 = new HashMap<String, String>();
        props2.put("name", "value21");
        PropProv prov2 = new PropProv(props2);
        service.bind(prov2, buildServiceConfig(prov2.getPropertyNames(), 3, 20));
        // add new provider
        Map<String, String> props3 = new HashMap<String, String>();
        props3.put("name", "value31");
        PropProv prov3 = new PropProv(props3);
        service.bind(prov3, buildServiceConfig(prov3.getPropertyNames(), 2, 30));
        // test resulting properties values
        Map<String, String>  properties = service.load();
        Assert.assertNotNull(properties);
        Assert.assertEquals("value21", properties.get("name"));
    }

    @Test
    public void testChangeDetection() throws Exception {
        PropertiesService service = new PropertiesService();
        // add new provider
        Map<String, String> props2 = new HashMap<String, String>();
        props2.put("name", "value");
        PropProv prov2 = new PropProv(props2);
        service.bind(prov2, buildServiceConfig(prov2.getPropertyNames(), 3, 20));
        Map<String, String> p2 = service.load();
        // add new provider (new instance) with same properties
        Map<String, String> props3 = new HashMap<String, String>();
        props3.put("name", "value");
        PropProv prov3 = new PropProv(props3);
        service.bind(prov3, buildServiceConfig(prov3.getPropertyNames(), 2, 30));
        Map<String, String>  p3 = service.load();
        // check that the index has not changed
        Assert.assertEquals(p2, p3);
        // add new provider with different properties
        Map<String, String> props1 = new HashMap<String, String>();
        props1.put("name", "new value");
        PropProv prov1 = new PropProv(props1);
        service.bind(prov1, buildServiceConfig(prov1.getPropertyNames(), 5, 50));
        // check that the index has changed
        Map<String, String> p1 = service.load();
        boolean same = p2.equals(p1);
        Assert.assertFalse(same);
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

    private class PropProv implements PropertyProvider {

        final Map<String, String> properties;

        PropProv(Map<String, String> properties) {
            this.properties = properties;
        }

        public String getProperty(String propName) {
            return properties.get(propName);
        }

        public Set<String> getPropertyNames() {
            return properties.keySet();
        }
    }

}