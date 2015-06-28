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
package org.apache.sling.discovery.etcd.gzip;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.protocol.HttpContext;

/**
 * Decompress gzip response body if needed.
 */
public class GzipResponseInterceptor implements HttpResponseInterceptor {

    public void process(HttpResponse res, HttpContext context)
            throws HttpException, IOException {
        HttpEntity entity = res.getEntity();
        if (entity != null) {
            Header contentEncoding = entity.getContentEncoding();
            if (contentEncoding != null) {
                for (HeaderElement element : contentEncoding.getElements()) {
                    if ("gzip".equalsIgnoreCase(element.getName())) {
                        res.setEntity(new GzipDecompressingEntity(entity));
                        return;
                    }
                }
            }
        }
    }
}
