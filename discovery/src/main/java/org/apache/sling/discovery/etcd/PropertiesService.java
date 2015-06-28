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
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.annotation.Nonnull;

import org.apache.sling.commons.osgi.ServiceUtil;
import org.apache.sling.discovery.PropertyProvider;

/**
 * The {@code PropertiesService} manages the {@code PropertyProvider}s instances running on the local instance.
 */
public class PropertiesService {

    /**
     * Concurrent sorted map holding the {@link PropertyProvider} references.
     * The providers are stored in natural order according to service ranking (lowest service ranking first).
     */
    private final SortedMap<Comparable<Object>, Provider> propertyProviders =
            new ConcurrentSkipListMap<Comparable<Object>, Provider>();

    /**
     * Load the properties for the local instance from all {@link PropertyProvider} providers.
     * @return A new map instance containing the local properties.
     */
    @Nonnull
    public Map<String, String> load() {
        Map<String, String> next = new HashMap<String, String>();
        for (Provider provider : getProviders()) {
            next.putAll(provider.properties());
        }
        return next;
    }

    public void bind(PropertyProvider propertyProvider, Map<String, Object> properties) {
        Provider provider = new Provider(properties, propertyProvider);
        propertyProviders.put(ServiceUtil.getComparableForServiceRanking(properties), provider);
    }

    public void unbind(PropertyProvider propertyProvider, Map<String, Object> properties) {
        propertyProviders.remove(ServiceUtil.getComparableForServiceRanking(properties));
    }

    @Nonnull
    protected Collection<Provider> getProviders() {
        return propertyProviders.values();
    }

    /**
     * Holds the service properties and properties for a {@link PropertyProvider} instance.
     */
    protected static class Provider {

        private static final String[] EMPTY = new String[0];

        final Map<String, Object> serviceProperties;

        final PropertyProvider provider;

        public Provider(@Nonnull Map<String, Object> serviceProperties, @Nonnull PropertyProvider provider) {
            this.serviceProperties = serviceProperties;
            this.provider = provider;
        }

        @Nonnull
        public Map<String,String> properties() {
            Map<String, String> props = new HashMap<String, String>();
            for (String name : names()) {
                String value = provider.getProperty(name);
                if (value != null) {
                    props.put(name, value);
                }
            }
            return props;
        }

        @Nonnull
        private String[] names() {
            Object props = serviceProperties.get(PropertyProvider.PROPERTY_PROPERTIES);
            if (props instanceof String) {
                return new String[]{(String) props};
            } else if (props instanceof String[]) {
                return (String[])props;
            } else {
                return EMPTY;
            }
        }
    }
}
