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
import org.apache.sling.etcd.common.ErrorCodes;

public class PrevIndex implements Condition {

    private final long index;

    public PrevIndex(long index) {
        this.index = index;
    }

    public void check(@Nonnull String key, @Nullable Node node, long index)
            throws EtcdException {

        if (node == null) {
            throw new EtcdException(ErrorCodes.KEY_NOT_FOUND, key, index);
        }

        // as per etcd doc
        // the condition prevIndex=0 always passes.
        if ((this.index != 0) && (this.index != node.modifiedIndex())) {
            String cause = String.format("[%s != %s]", this.index, node.modifiedIndex());
            throw new EtcdException(ErrorCodes.TEST_FAILED, cause, index);
        }
    }

    public long index() {
        return index;
    }
}