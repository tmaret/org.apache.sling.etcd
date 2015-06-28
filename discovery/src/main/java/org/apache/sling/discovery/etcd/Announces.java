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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.etcd.client.EtcdNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code Announces} collection contains a list of {@code Announce} announce
 * sorted according to the {@code Announce} natural order @see #Announce:toString.<p>
 *
 * The collection does not contain more than one {@code Announce} announce with
 * a given Sling identifier.
 */
public class Announces {

    private static final Logger LOG = LoggerFactory.getLogger(Announces.class);

    /**
     * Hold the sorted list of announces.
     */
    private final List<Announce> announces;

    /**
     * Holds the Sling identifier to announce mapping.<p>
     *
     * The map keys are the instance Sling identifier and values are
     * the {@code Announce} announces.
     */
    private final Map<String, Announce> slingIdToAnnounce;

    /**
     * Create an {@code Announces} collection.
     * The announce ttl is not checked and may be null (for local instances).
     *
     * @param announces the list of announces without particular order.
     */
    public Announces(@Nonnull Collection<Announce> announces) {
        List<Announce> filtered = new ArrayList<Announce>(announces);
        Map<String, Announce> slingIdToAnnounce = new HashMap<String, Announce>();
        // Filter the announces for duplicates (same Sling id).
        // In case of duplicate, only the first announce is kept.
        // The list must be sorted according to the keys.
        for (Iterator<Announce> iterator = filtered.iterator() ; iterator.hasNext() ; ) {
            Announce announce = iterator.next();
            String slingId = announce.getData().slingId();
            if (slingIdToAnnounce.containsKey(slingId)) {
                iterator.remove();
            } else {
                slingIdToAnnounce.put(slingId, announce);
            }
        }
        Collections.sort(filtered);
        this.announces = Collections.unmodifiableList(filtered);
        this.slingIdToAnnounce = Collections.unmodifiableMap(slingIdToAnnounce);
    }

    /**
     * Create an {@code Announces} collection.
     *
     * @param nodes the list of etcd node representing announces without particular order.
     * @param filterOutElapsedTtl {@code true} in order to remove the nodes which ttl is elapsed (ttl <= 0) ;
     *                            {@code false} in order to keep all nodes.
     */
    public Announces(@Nonnull List<EtcdNode> nodes, boolean filterOutElapsedTtl) {
        this(buildAnnounces(nodes, filterOutElapsedTtl));
    }

    /**
     * Create an {@code Announces} collection.
     *
     * @param announces zero or more announces without particular order.
     */
    public Announces(@Nonnull Announce... announces) {
        this(Arrays.asList(announces));
    }

    /**
     * Get the announce corresponding to the given Sling identifier.
     *
     * @param slingId the instance Sling identifier to retrieve the announce.
     * @return the {@code Announce} corresponding to the provided #slingId or {@code null}.
     */
    @Nullable
    public Announce getBySlingId(@Nonnull String slingId) {
        return slingIdToAnnounce.get(slingId);
    }

    /**
     * Checks whether the announces collection contain an announce for the given Sling identifier.
     *
     * @param slingId the instance Sling identifier to check
     * @return {@code true} if the announces contain an entry for the provided #slingId ;
     *         {@code false} otherwise.
     */
    public boolean containsBySlingId(@Nonnull String slingId) {
        return slingIdToAnnounce.containsKey(slingId);
    }

    /**
     * Build a new {@code Announces} collection of announces which does not contain the given Sling identifier.
     *
     * @param slingId the Sling identifier of the announce to be filtered out.
     * @return the new {@code Announces} collection.
     */
    @Nonnull
    public Announces filterBySlingId(@Nonnull String slingId) {
        List<Announce> filtered = new ArrayList<Announce>(announces.size());
        for (Announce announce : announces) {
            if (! slingId.equals(announce.getData().slingId())) {
                filtered.add(announce);
            }
        }
        return new Announces(filtered);
    }

    /**
     * @return the set of Sling identifier for all the announces contained in the collection.
     */
    @Nonnull
    public Set<String> getSlingIds() {
        return slingIdToAnnounce.keySet();
    }

    /**
     * @return all the announces contained in the collection.
     */
    @Nonnull
    public List<Announce> getAnnounces() {
        return announces;
    }

    /**
     * @return the number of announces contained in the collection.
     */
    public int size() {
        return announces.size();
    }

    //

    /**
     * Build a list of {@code Announce} based on the nodes returned by etcd.
     * Depending on the #filterOutElapsedTtl flag, the method can filter out the announces
     * which ttl has elapsed (ttl <= 0).
     * etcd may return announces with negative ttl, in case the cluster quorum is lost
     * (the majority of the etcd peers are down).
     *
     * @param nodes the list of etcd node representing announces without particular order.
     * @param filterOutElapsedTtl {@code true} in order to remove the nodes which ttl is elapsed (ttl <= 0) ;
     *                            {@code false} in order to keep all nodes.
     * @return a list of announces without particular order.
     */
    @Nonnull
    private static List<Announce> buildAnnounces(@Nonnull List<EtcdNode> nodes, boolean filterOutElapsedTtl) {
        List<Announce> announces = new ArrayList<Announce>(nodes.size());
        for (EtcdNode node : nodes) {
            String data = node.value();
            if (data != null) {
                Long ttl = node.ttl();
                if (ttl != null) {
                    if (! filterOutElapsedTtl || (ttl > 0L)) {
                        Announce announce = new Announce(new AnnounceData(data), node.key(), ttl);
                        announces.add(announce);
                    } else {
                        LOG.debug("Discard announce node with elapsed ttl: {}", node);
                    }
                } else {
                    LOG.warn("Received an announce node without ttl: {}", node);
                }
            }
        }
        return announces;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Announces announces1 = (Announces) o;
        if (!announces.equals(announces1.announces)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return announces.hashCode();
    }

    @Override
    public String toString() {
        return "Announces{" +
                "announces=" + announces +
                '}';
    }
}
