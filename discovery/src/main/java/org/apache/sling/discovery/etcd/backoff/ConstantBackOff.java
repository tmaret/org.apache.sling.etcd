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
package org.apache.sling.discovery.etcd.backoff;

/**
 * Constant function which returns the same value independently of the index.
 */
public class ConstantBackOff implements BackOff {

    private final long value;

    public ConstantBackOff(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be greater or equal to 0");
        }
        this.value = value;
    }

    public long value() {
        return value;
    }

    public long value(int index) {
        return value;
    }

    public long increment() {
        return value;
    }

    public long reset() {
        return value;
    }

    public long max() {
        return value;
    }

    @Override
    public String toString() {
        return "ConstantBackOff{" +
                "value=" + value +
                '}';
    }
}
