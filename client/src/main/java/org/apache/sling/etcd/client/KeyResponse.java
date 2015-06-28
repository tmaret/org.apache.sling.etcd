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

import aQute.bnd.annotation.ProviderType;

/**
 * Represent the response returned by the etcd key API.
 * The response body can contain either an action or an error encoded in JSON format.
 * The #isAction method allows to know which type of body content is returned and the
 * corresponding data will be available through either the #action or #error methods.
 */
@ProviderType
public interface KeyResponse extends EtcdResponse {

    /**
     * @return {@code true} if the response is an {@link KeyAction} action ;
     *         {@code false} if the response is an {@link KeyError} error.
     */
    boolean isAction();

    /**
     * @return the {@link KeyError} error if the response is an error ; {@code null} otherwise.
     */
    KeyError error();

    /**
     * @return the {@link KeyAction} action if the response is an action ; {@code null} otherwise.
     */
    KeyAction action();

}
