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

import java.util.Deque;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.etcd.testing.condition.Condition;
import org.apache.sling.etcd.testing.tree.Folder;
import org.apache.sling.etcd.testing.tree.Key;
import org.apache.sling.etcd.testing.tree.Node;
import org.apache.sling.etcd.common.ErrorCodes;

/**
 * The {@code Etcd} class allows to apply the etcd operations on a tree kept in memory.
 * The operation mock the etcd service but are not 100% similar to the real etcd implementation.
 */
public class Etcd {

    private long index = 0;

    private final Object lock = new Object();

    private Folder root;

    public Etcd(@Nonnull Folder root) {
        this.root = root;
    }

    public Etcd() {
        root = new Folder("root", null, nextIndex());
    }

    /**
     * Get a node from the tree.
     *
     * @param key the node's key
     * @return the node or {@code null} if no matching node was found.
     */
    public Node getNode(@Nonnull String key) {
        synchronized (lock) {
            return getNode(root, Node.names(key), key);
        }
    }

    /**
     * Put a key in the tree.
     *
     * @param key the key's key
     * @param value the key's value
     * @param ttl the optional time to live in second
     * @param condition the optional pre-condition
     * @return the previous key if it did exist
     * @throws EtcdException in case a exception occurs
     */
    @Nonnull
    public Change<Key> putKey(@Nonnull String key, @Nullable String value, @Nullable Integer ttl, @Nullable Condition condition)
            throws EtcdException {
        synchronized (lock) {

            if ("/".equals(key)) {
                throw new EtcdException(ErrorCodes.ROOT_READ_ONLY, "/", index());
            }

            String parentKey = Node.parent(key);
            Folder parent = getOrCreateFolder(root, Node.names(parentKey));

            // Check the parent if not a key
            if (parent == null) {
                throw new EtcdException(ErrorCodes.NOT_DIR, parentKey, index());
            }

            // check current node
            String name = Node.name(key);
            Node node = parent.child(name);
            checkCondition(key, node, condition);

            if (node != null && node.isFolder()) {
                throw new EtcdException(ErrorCodes.NOT_FILE, node.path(), index());
            }

            // put new key
            Key current = new Key(name, value, ttl, nextIndex());
            parent.putChild(current, nextIndex());

            return new Change<Key>((Key) node, current);
        }
    }

    /**
     * Put a folder in the tree.
     *
     * @param key the folder's key
     * @param ttl the optional folder time to live in second
     * @param condition the optional pre-condition
     * @return the previous node (folder or key) if it existed
     * @throws EtcdException in case an exception occurs
     */
    @Nonnull
    public Change<Node> putFolder(@Nonnull String key, @Nullable Integer ttl, @Nullable Condition condition)
            throws EtcdException {
        synchronized (lock) {

            if ("/".equals(key)) {
                throw new EtcdException(ErrorCodes.ROOT_READ_ONLY, "/", index());
            }

            String parentKey = Node.parent(key);
            Folder parent = getOrCreateFolder(root, Node.names(parentKey));

            // Check the parent if not a key
            if (parent == null) {
                throw new EtcdException(ErrorCodes.NOT_DIR, parentKey, index());
            }

            // check current node
            String name = Node.name(key);
            Node node = parent.child(name);
            checkCondition(key, node, condition);

            // put new folder
            Folder current = new Folder(name, ttl, nextIndex());
            parent.putChild(current, nextIndex());

            return new Change<Node>(node, current);
        }
    }

    /**
     * Create a unique and ordered new key under the given key.
     *
     * @param key the parent key where the key needs to be created
     * @param value the valud of the key to be created
     * @param ttl the ttl of the key to be created
     * @param condition the condition wrt to the key to be created
     * @return the key that has been created
     * @throws EtcdException in case an exception occurs
     */
    @Nonnull
    public Key createKey(@Nonnull String key, @Nullable String value, @Nullable Integer ttl, @Nullable Condition condition)
            throws EtcdException {
        synchronized (lock) {

            long index = nextIndex();
            String createdKey = key + "/" + String.valueOf(index);

            String parentKey = Node.parent(createdKey);
            Folder parent = getOrCreateFolder(root, Node.names(parentKey));

            // Check the parent if not a key
            if (parent == null) {
                throw new EtcdException(ErrorCodes.NOT_DIR, parentKey, index());
            }

            // check current node
            String name = Node.name(createdKey);
            Node node = parent.child(name);
            checkCondition(createdKey, node, condition);

            if (node != null && node.isFolder()) {
                throw new EtcdException(ErrorCodes.NOT_FILE, node.path(), index());
            }

            // put new key
            Key created = new Key(name, value, ttl, nextIndex());
            parent.putChild(created, nextIndex());

            return created;
        }
    }

    /**
     * Delete the given key.
     *
     * @param key the key's key to be deleted
     * @param condition the condition wrt to the key to be deleted
     * @return the deleted key
     * @throws EtcdException in case an exception occurs
     */
    @Nonnull
    public Key deleteKey(@Nonnull String key, @Nullable Condition condition)
            throws EtcdException {
        synchronized (lock) {

            if ("/".equals(key)) {
                throw new EtcdException(ErrorCodes.ROOT_READ_ONLY, "/", index());
            }

            Node node = getNode(key);
            checkCondition(key, node, condition);

            if (node == null) {
                throw new EtcdException(ErrorCodes.KEY_NOT_FOUND, key, index());
            }

            if(node.isFolder()) {
                throw new EtcdException(ErrorCodes.NOT_FILE, node.path(), index());
            }

            String parentKey = Node.parent(key);
            Folder parent = getOrCreateFolder(root, Node.names(parentKey));
            String name = Node.name(key);

            if (parent != null) {
                parent.removeChild(name, nextIndex());
            }

            return (Key)node;
        }
    }

    /**
     * Delete the given folder.
     *
     * @param key the folder's key
     * @param recursive {@code true} in order to require deleting the sub folders as well, {@code false} otherwise
     * @param condition the condition wrt to the folder to be deleted
     * @return the deleted folder
     * @throws EtcdException in case an exception occurs
     */
    @Nonnull
    public Folder deleteFolder(@Nonnull String key, boolean recursive, @Nullable Condition condition)
            throws EtcdException {
        synchronized (lock) {

            if ("/".equals(key)) {
                throw new EtcdException(ErrorCodes.ROOT_READ_ONLY, "/", index());
            }

            Node node = getNode(key);

            if (node == null) {
                throw new EtcdException(ErrorCodes.KEY_NOT_FOUND, key, index());
            }

            // when tries to delete folder

            checkCondition(key, node, condition);

            if(! node.isFolder()) {
                throw new EtcdException(ErrorCodes.NOT_DIR, node.path(), index());
            }

            Folder folder = (Folder) node;

            if (folder.children(false).size() > 0 && ! recursive) {
                throw new EtcdException(ErrorCodes.DIR_NOT_EMPTY, node.path(), index());
            }

            String parentKey = Node.parent(key);
            Folder parent = getOrCreateFolder(root, Node.names(parentKey));
            String name = Node.name(key);

            if (parent != null) {
                parent.removeChild(name, nextIndex());
            }

            return folder;
        }
    }

    /**
     * @return the current etcd index
     */
    public long index() {
        synchronized (lock) {
            return index;
        }
    }

    //

    private void checkCondition(@Nonnull String key, @Nullable Node node, @Nullable Condition condition)
            throws EtcdException {
        if (condition != null) {
            condition.check(key, node, index());
        }
    }

    private long nextIndex() {
        return ++index;
    }

    private Node getNode(@Nonnull Node node, @Nonnull Deque<String> names, @Nonnull String key) {
        if (! node.ttlElapsed()) {
            if (node.path().equals(key)) {
                return node;
            } else if (node.isFolder() && ! names.isEmpty()) {
                Folder folder = (Folder) node;
                String name = names.removeFirst();
                Node child = folder.child(name);
                if (child != null) {
                    return getNode(child, names, key);
                }
            }
        }
        return null;
    }

    private Folder getOrCreateFolder(@Nonnull Folder folder, @Nonnull Deque<String> names) {
        if (names.isEmpty()) {
            return folder;
        } else {
            String name = names.removeFirst();
            Node child = folder.child(name);
            if (child == null) {
                Folder created = new Folder(name, null, nextIndex());
                folder.putChild(created, nextIndex());
                return getOrCreateFolder(created, names);
            } else {
                if (child.isFolder()) {
                    return getOrCreateFolder((Folder)child, names);
                } else {
                    return null;
                }
            }
        }
    }
}
