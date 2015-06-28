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
package org.apache.sling.discovery.etcd.fsm;

public abstract class BaseRunner implements Runner {

    /**
     * Object to wait on
     */
    private final Object wait = new Object();

    /**
     * Flag whether the state runner should run ({@code true}) or not ({@code false}).
     */
    protected boolean running = true;

    @Override
    public void stop() {
        synchronized (wait) {
            running = false;
            wait.notifyAll();
        }
    }

    protected void sleep(long ms) {
        synchronized (wait) {
            long timeout = System.currentTimeMillis() + ms;
            long remaining;
            for ( ; running && (remaining = timeout - System.currentTimeMillis()) > 0 ; ) {
                try {
                    wait.wait(remaining);
                } catch (InterruptedException ignore) {
                    // ignore
                }
            }
        }
    }
}
