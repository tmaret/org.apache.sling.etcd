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
package org.apache.sling.etcd.common;

/**
 * CoreOS etcd error codes.
 */
public final class ErrorCodes {

    // Command Related Error

    /**
     * Key not found
     */
    public static final int KEY_NOT_FOUND = 100;

    /**
     * Compare failed
     */
    public static final int TEST_FAILED = 101;

    /**
     * Not a file
     */
    public static final int NOT_FILE = 102;

    /**
     * Not a directory
     */
    public static final int NOT_DIR = 104;

    /**
     * Key already exists
     */
    public static final int NODE_EXITS = 105;

    /**
     * Root is read only
     */
    public static final int ROOT_READ_ONLY = 107;

    /**
     * Directory not empty
     */
    public static final int DIR_NOT_EMPTY = 108;

    // Post Form Related Error

    /**
     * PrevValue is Required in POST form
     */
    public static final int PREV_VALUE_REQUIRED = 201;

    /**
     * The given TTL in POST form is not a number
     */
    public static final int TTL_NOT_NUMBER = 202;

    /**
     * The given index in POST form is not a number
     */
    public static final int INDEX_NOT_NUMBER = 203;

    /**
     * Invalid field
     */
    public static final int INVALID_FIELD = 209;

    /**
     * Invalid POST form
     */
    public static final int INVALID_FORM = 210;

    // Raft Related Error

    /**
     * Raft Internal Error
     */
    public static final int RAFT_INTERNAL_ERROR = 300;

    /**
     * During Leader Election
     */
    public static final int LEADER_ELECTION_ERROR = 301;

    // Etcd Related Error

    /**
     * watcher is cleared due to etcd recovery
     */
    public static final int WATCHER_CLEARED = 400;

    /**
     * The event in requested index is outdated and cleared
     */
    public static final int EVENT_INDEX_CLEARED = 401;
}
