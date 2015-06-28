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
package org.apache.sling.etcd.testing;

import org.apache.sling.etcd.testing.tree.Folder;
import org.apache.sling.etcd.testing.tree.Key;

public class TestContent {

    /**
     * Build a tree containing the following structure.
     * keys start with the letter 'k'. Non keys nodes are folders.
     * <pre>
     *     /a/k1
     *     /b/b1/k2
     *     /b/b1/k3
     *     /b/b1/b2/k4
     *     /c
     * </pre>
     * @return a root folder pointing to a new instance of the structure.
     */
    public static Folder build() {

        final Key k1 = new Key("k1", "value-k1", null, 1);
        final Key k2 = new Key("k2", "value-k2", null, 2);
        final Key k3 = new Key("k3", "value-k3", null, 3);
        final Key k4 = new Key("k4", "value-k4", null, 4);

        final Folder a = new Folder("a", null, 5);
        a.putChild(k1, 6);

        final Folder b2 = new Folder("b2", null, 7);
        b2.putChild(k4, 8);

        final Folder b1 = new Folder("b1", null, 9);
        b1.putChild(k2, 10);
        b1.putChild(k3, 11);
        b1.putChild(b2, 12);

        final Folder b = new Folder("b", null, 2);
        b.putChild(b1, 13);

        final Folder c = new Folder("c", null, 4);

        final Folder root = new Folder("root", null, 0);
        root.putChild(a, 16);
        root.putChild(b, 16);
        root.putChild(c, 16);

        return root;
    }
}
