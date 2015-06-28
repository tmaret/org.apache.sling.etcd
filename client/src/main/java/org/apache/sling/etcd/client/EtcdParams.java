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

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Utility class that allows to instantiate a {@link EtcdParamsBuilder} or get
 * an empty set of parameters.
 */
public final class EtcdParams {

    /**
     * @return a new {@link EtcdParamsBuilder} instance.
     */
    @Nonnull
    public static EtcdParamsBuilder builder() {
        return new EtcdParamsBuilder();
    }

    /**
     * @return an immutable empty map of parameters.
     */
    @Nonnull
    public static Map<String, String> noParams() {
        return Collections.emptyMap();
    }

}
