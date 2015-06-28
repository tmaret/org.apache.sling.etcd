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
package org.apache.sling.etcd.client;

import java.util.Calendar;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import aQute.bnd.annotation.ProviderType;

/**
 * Represent an etcd node (Key or Folder) with its associated properties.
 */
@ProviderType
public interface EtcdNode {

    /**
     * @return the created index ; or {@code -1} if the value is not defined.
     */
    long createdIndex();

    /**
     * @return the node key.
     */
    @Nonnull
    String key();

    /**
     * @return the modified index ; or {@code -1} if the value is not defined.
     */
    long modifiedIndex();

    /**
     * @return the value ; or {@code null} if the value is not defined.
     */
    @Nullable
    String value();

    /**
     * @return the node time to live in seconds ; or {@code null} if the value is not defined.
     * The ttl can be a negative value in case the etcd cluster quorum is lost (more than half
     * of the etcd peers are down).
     */
    @Nullable
    Long ttl();

    /**
     * @return the expiration date ; or {@code null} if the value is not defined.
     */
    @Nullable
    Calendar expiration();

    /**
     * @return {@code true} if the node is a folder ; {@code false} if the node is a key or the value is not defined.
     */
    boolean dir();

    /**
     * @return the list of child nodes.
     */
    @Nonnull
    List<EtcdNode> nodes();

    /**
     * @return the node JSON representation.
     */
    @Nonnull
    String toJson();

}
