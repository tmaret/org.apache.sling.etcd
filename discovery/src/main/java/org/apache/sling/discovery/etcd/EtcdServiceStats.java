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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Keep track of the {@code EtcdService} error statistics (IO errors, Etcd errors).
 */
public class EtcdServiceStats {

    /**
     * Stores the mapping between the etcd error codes and the number of consecutive occurrences.
     */
    private final Map<Integer, Integer> etcdErrorCodes = new HashMap<Integer, Integer>();

    /**
     * The sum of consecutive I/O errors.
     */
    private volatile int ioErrors;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Return the sum of consecutive occurrences of etcd error in a given range.
     *
     * @param minErrorCode minimum error code (inclusive)
     * @param maxErrorCode maximum error code (exclusive)
     * @return the sum of consecutive occurrences of the errors in the given range.
     */
    public int consecutiveEtcdError(int minErrorCode, int maxErrorCode) {
        lock.readLock().lock();
        try {
            int sum = 0;
            for (Map.Entry<Integer, Integer> entry : etcdErrorCodes.entrySet()) {
                int code = entry.getKey();
                if ((code >= minErrorCode) && (code < maxErrorCode)) {
                    sum = Math.min(sum + entry.getValue(), Integer.MAX_VALUE);
                }
            }
            return sum;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @return the sum of consecutive I/O errors.
     */
    public int consecutiveIoError() {
        lock.readLock().lock();
        try {
            return ioErrors;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Increase the amount of occurrences of the etcd error with the given code.
     *
     * @param errorCode the etcd error code to increase the number of occurrences
     */
    public void increaseEtcdError(int errorCode) {
        lock.writeLock().lock();
        try {
            Integer was = etcdErrorCodes.get(errorCode);
            int is = (was != null) ? Math.min(was + 1, Integer.MAX_VALUE) : 1;
            etcdErrorCodes.put(errorCode, is);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Increase the number of occurrences of an I/O errors.
     */
    public void increaseIoError() {
        lock.writeLock().lock();
        try {
            ioErrors++;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Set the number of I/O occurrences to zero.
     */
    public void resetIoError() {
        lock.writeLock().lock();
        try {
            ioErrors = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * For any etcd error code, set the number of occurrences to 0.
     */
    public void resetEtcdError() {
        lock.writeLock().lock();
        try {
            etcdErrorCodes.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

}
