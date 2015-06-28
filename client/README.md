org.apache.sling.etcd.client
============================

This module provides a client for CoreOS etcd.
The implementation supports the CoreOS etcd REST API [version 2.0](https://coreos.com/docs/distributed-configuration/etcd-api).

## Features

The current implementation does support

* All operations on keys/folders (GET, PUT, POST, DELETE)
* retrieve etcd version
* retrieve etcd leader statistics
* SSL (client to etcd)

And does not support

* Wait
* Async I/O

## Build

Use the following command to build the artifact.

```
mvn clean install
```

The artifact is an OSGI bundle with coordinates

```
<dependency>
	<groupId>org.apache.sling</groupId>
	<artifactId>org.apache.sling.etcd.client</artifactId>
	<version>x.y.z</version>
</dependency>
```

## API

The service class is ``EtcdClient`` for which the factory ``EtcdClientFactory`` allows to obtain an instance as shown in the snipnet below.

```
CloseableHttpClient httpClient = HttpClients.custom().build();
URI etcdEndpoint = new URI("localhost:4001");
EtcdClient etcdClient = EtcdClientFactory#create(httpClient, etcdEndpoint);

// get keys recursively under '/some/key'
String key = "/some/key";
Map<String, String> params = EtcdParams
    .builder()
    .recursive(true)
    .build(); 
EtcdResponse response = etcdClient.getKey(key, params);
if (response.isAction()) {
	// received an etcd action
	EtcdAction action = response.action();
	// do something with it ..
} else {
    // received an etcd error (I/O errors throw IOException)
    EtcdError error = response.error();
    // handle the error ..
}

```

## Setup

The instructions to install the latest CoreOS etcd release are available [here](https://github.com/coreos/etcd/releases/). The version used for development was 2.0.8.
