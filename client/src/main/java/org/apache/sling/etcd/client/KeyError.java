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

import javax.annotation.Nonnull;

import aQute.bnd.annotation.ProviderType;

/**
 * Represent the etcd response body returned from the key API in case of error.
 */
@ProviderType
public interface KeyError {

    /**
     * @return the cause of the error.
     */
    @Nonnull
    String cause();

    /**
     * @return the error code.
     */
    int errorCode();

    /**
     * @return the index.
     */
    long index();

    /**
     * @return the error message.
     */
    @Nonnull
    String message();

}
