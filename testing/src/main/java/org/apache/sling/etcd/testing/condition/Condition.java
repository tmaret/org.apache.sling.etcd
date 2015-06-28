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
package org.apache.sling.etcd.testing.condition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.etcd.testing.EtcdException;
import org.apache.sling.etcd.testing.tree.Node;

public interface Condition {

    /**
     * Checks whether the condition applies to the give node.
     * If the condition applies, the method simply returns, otherwise
     * a {@code EtcdException} is thrown.
     *
     * @param key the key of the node to be checked
     * @param node the node to be checked (possibly {@code null})
     * @param index the etcd index at which the test is evaluated
     * @throws EtcdException if the test failed
     */
    void check(@Nonnull String key, @Nullable Node node, long index) throws EtcdException;
}