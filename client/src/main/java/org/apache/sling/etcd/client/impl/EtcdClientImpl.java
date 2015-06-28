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
package org.apache.sling.etcd.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.etcd.client.EtcdException;
import org.apache.sling.etcd.client.EtcdClient;
import org.apache.sling.etcd.client.KeyResponse;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.etcd.client.LeaderStatsResponse;
import org.apache.sling.etcd.client.MembersResponse;
import org.apache.sling.etcd.client.MemberStatsResponse;
import org.apache.sling.etcd.client.VersionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>The current {@code EtcdClient} implementation requires a single etcd endpoint.</p>
 *
 * <p>If the client is communicating with an etcd cluster, then the endpoint should reference
 * an etcd instance running in readwrite
 * <a href="https://github.com/coreos/etcd/blob/release-2.0/Documentation/proxy.md">proxy mode</a>.</p>
 *
 * <p>If the client is communicating with a single etcd node (no cluster), the endpoint could
 * reference it directly.</p>
 *
 * <p>The etcd proxy is aware of the etcd peers in a cluster and takes care of dispatching the requests
 * to the most relevant etcd peer.</p>
 */
/*
 * The implementation may be extended in order to avoid the use of an etcd proxy.
 * In this case, the client should figure out the members in the etcd cluster and
 * based on failures, RTT or other characteristics select the most relevant endpoint.
 *
 * In order to know the list of etcd peer members, the client could either require a
 * fixed list of etcd endpoints URIs or leverage the list-member API
 * https://github.com/coreos/etcd/blob/master/Documentation/other_apis.md#list-members.
 */
public class EtcdClientImpl implements EtcdClient {

    private static final String API_KEYS_FORMAT = "/v2/keys%s";

    private static final String PATH_VERSION = "/version";

    private static final String PATH_LEADER_STATS = "/v2/stats/leader";

    private static final String PATH_MEMBERS = "/v2/members";

    private static final String PATH_SELF_STATS = "/v2/stats/self";

    private static final String UTF8 = "UTF-8";

    protected static final ContentType FORM_URLENCODED = ContentType.create(
            "application/x-www-form-urlencoded", UTF8);

    private static final Logger LOG = LoggerFactory.getLogger(EtcdClientImpl.class);

    private final CloseableHttpClient httpClient;

    private final URI endpoint;

    /**
     * @param httpClient The client used for communicating with etcd.
     *                   The client must enable redirect handling (default).
     * @param endpoint The uri to access the etcd peers (e.g. http://localhost:4001)
     */
    public EtcdClientImpl(@Nonnull CloseableHttpClient httpClient, @Nonnull URI endpoint) {
        this.httpClient = Check.nonNull(httpClient, "httpClient");
        this.endpoint = Check.nonNull(endpoint, "endpoint");
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public KeyResponse getKey(@Nonnull String key,
                               @Nonnull Map<String, String> parameters)
            throws IOException {
        Check.nonNull(parameters, "parameters");
        Check.nonNull(key, "key");
        return execKey(new HttpGet(
                buildUri(String.format(API_KEYS_FORMAT,
                        key), parameters)));
    }

    /**
     * The request is sent with {@code application/x-www-form-urlencoded} content type.
     * The "value" is sent in the request body, the other parameters are sent as query parameters.
     */
    @Nonnull
    public KeyResponse putKey(@Nonnull String key,
                               @Nullable String value,
                               @Nonnull Map<String, String> parameters)
            throws IOException {
        Check.nonNull(parameters, "parameters");
        Check.nonNull(key, "key");
        return execKey(entity(new HttpPut(
                buildUri(String.format(API_KEYS_FORMAT,
                        key), parameters)), value));
    }

    /**
     * The request is sent with {@code multipart/form-data} content type.
     * The "value" is sent in the request body, the other parameters are sent as query parameters.
     */
    @Nonnull
    public KeyResponse putKey(@Nonnull String key,
                               @Nonnull InputStream value,
                               @Nonnull Map<String, String> parameters)
            throws IOException {
        Check.nonNull(parameters, "parameters");
        Check.nonNull(value, "value");
        Check.nonNull(key, "key");
        return execKey(entity(new HttpPut(
                buildUri(String.format(API_KEYS_FORMAT,
                        key), parameters)), value));
    }

    /**
     * The request is sent with {@code application/x-www-form-urlencoded} content type.
     * The "value" is sent in the request body, the other parameters are sent as query parameters.
     */
    @Nonnull
    public KeyResponse postKey(@Nonnull String key,
                                @Nullable String value,
                                @Nonnull Map<String, String> parameters)
            throws IOException {
        Check.nonNull(parameters, "parameters");
        Check.nonNull(key, "key");
        return execKey(entity(new HttpPost(
                buildUri(String.format(API_KEYS_FORMAT,
                        key), parameters)), value));
    }

    /**
     * The request is sent with {@code multipart/form-data} content type.
     * The "value" is sent in the request body, the other parameters are sent as query parameters.
     */
    @Nonnull
    public KeyResponse postKey(@Nonnull String key,
                                @Nonnull InputStream value,
                                @Nonnull Map<String, String> parameters)
            throws IOException {
        Check.nonNull(parameters, "parameters");
        Check.nonNull(value, "value");
        Check.nonNull(key, "key");
        return execKey(entity(new HttpPost(
                buildUri(String.format(API_KEYS_FORMAT,
                        key), parameters)), value));
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public KeyResponse deleteKey(@Nonnull String key,
                                  @Nonnull Map<String, String> parameters)
            throws IOException {
        Check.nonNull(parameters, "parameters");
        Check.nonNull(key, "key");
        return execKey(new HttpDelete(
                buildUri(String.format(API_KEYS_FORMAT,
                        key), parameters)));
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public MembersResponse getMembers() throws IOException {
        Response res = exec(new HttpGet(
                buildUri(endpoint,
                        PATH_MEMBERS, Collections.<String, String>emptyMap())), 200);
        return new MembersResponseImpl(res.status, res.reasonPhrase, res.headers, toJson(res.body));
    }

    /**
     * {@inheritDoc}
     */
    /*
     * In previous versions of the etcd API, this call was redirected automatically to the leader
     * https://github.com/coreos/etcd/issues/2806
     */
    @Nonnull
    public LeaderStatsResponse getLeaderStats(@Nonnull URI leaderPeerEndpoint) throws IOException {
        Response res = exec(new HttpGet(
                buildUri(leaderPeerEndpoint,
                        PATH_LEADER_STATS, Collections.<String, String>emptyMap())), 200);
        return new LeaderStatsResponseImpl(res.status, res.reasonPhrase, res.headers, toJson(res.body));
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public MemberStatsResponse getMemberStats(@Nonnull URI peerEndpoint) throws IOException {
        Check.nonNull(peerEndpoint, "peerEndpoint");
        Response res = exec(new HttpGet(
                buildUri(peerEndpoint,
                        PATH_SELF_STATS, Collections.<String, String>emptyMap())), 200);
        return new MemberStatsResponseImpl(res.status, res.reasonPhrase, res.headers, toJson(res.body));
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    public VersionResponse getVersion(@Nonnull URI peerEndpoint) throws IOException {
        Check.nonNull(peerEndpoint, "peerEndpoint");
        Response res = exec(new HttpGet(buildUri(
                peerEndpoint, PATH_VERSION, Collections.<String, String>emptyMap())), 200);
        return new VersionResponseImpl(res.status, res.reasonPhrase, res.headers, res.body);
    }

    //

    @Nonnull
    private KeyResponse execKey(@Nonnull HttpUriRequest method) throws IOException {
        Response res = exec(method, 200, 201, 400, 404);
        return new KeyResponseImpl(res.status, res.reasonPhrase, res.headers, toJson(res.body));
    }

    @Nonnull
    private Response exec(@Nonnull HttpUriRequest method, int ... expected) throws IOException {
        CloseableHttpResponse response = httpClient.execute(logMethod(method));
        try {
            StatusLine statusLine = response.getStatusLine();
            Map<String, List<String>> headers = extractHeaders(response.getAllHeaders());
            String body = (response.getEntity() != null)
                    ? EntityUtils.toString(response.getEntity(), UTF8)
                    : null;
            logResponse(statusLine, body, headers);
            checkStatus(statusLine, expected);
            if (body == null) {
                throw new IOException(String.format("No entity found in response %s", formatStatusLine(response.getStatusLine())));
            }
            return new Response(statusLine.getStatusCode(), statusLine.getReasonPhrase(), headers, body);
        } finally {
            IOUtils.closeQuietly(response);
        }
    }

    @Nonnull
    private URI buildUri(@Nonnull String path, @Nonnull Map<String, String> parameters) {
        return buildUri(endpoint, path, parameters);
    }

    @Nonnull
    private URI buildUri(@Nonnull URI endpoint, @Nonnull String path, @Nonnull Map<String, String> parameters) {
        try {
            URIBuilder builder = new URIBuilder();
            for (Map.Entry<String, String> p : parameters.entrySet()) {
                builder.setParameter(p.getKey(), p.getValue());
            }
            return builder.setScheme(endpoint.getScheme())
                    .setHost(endpoint.getHost())
                    .setPort(endpoint.getPort())
                    .setPath(path)
                    .build();
        } catch (URISyntaxException e) {
            throw new EtcdException(e.getMessage(), e);
        }
    }

    @Nonnull
    private HttpUriRequest entity(@Nonnull HttpEntityEnclosingRequestBase method, @Nullable String value) {
        if (value != null) {
            method.setEntity(EntityBuilder
                    .create()
                    .setParameters(new BasicNameValuePair("value", value))
                    .setContentType(FORM_URLENCODED)
                    .build());
        }
        return method;
    }

    @Nonnull
    private HttpUriRequest entity(@Nonnull HttpEntityEnclosingRequestBase method, @Nonnull InputStream value) {
        method.setEntity(MultipartEntityBuilder
                .create()
                .addBinaryBody("value", value)
                .build());
        return method;
    }

    @Nonnull
    private Map<String, List<String>> extractHeaders(@Nonnull Header[] headers) {
        final Map<String, List<String>> all = new HashMap<String, List<String>>();
        for (Header header : headers) {
            List<String> values = all.get(header.getName());
            if (values == null) {
                values = new ArrayList<String>();
                all.put(header.getName(), values);
            }
            values.add(header.getValue());
        }
        return all;
    }

    private void logResponse(@Nonnull StatusLine statusLine, @Nullable String body, @Nonnull Map<String, List<String>> headers) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Response status: {} body: {} headers: {}", new Object[]{formatStatusLine(statusLine), body, headers});
        }
    }

    private void checkStatus(StatusLine statusLine, int... expected) throws IOException {
        int status = statusLine.getStatusCode();
        for (int exp : expected) {
            if (exp == status) {
                return;
            }
        }
        throw new IOException(String.format("Unexpected status: %s", formatStatusLine(statusLine)));
    }

    @Nonnull
    private HttpUriRequest logMethod(@Nonnull HttpUriRequest method) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Execute {} request for uri {}", new Object[]{method.getMethod(), method.getURI()});
        }
        return method;
    }

    @Nonnull
    private String formatStatusLine(@Nonnull StatusLine line) {
        return String.format("code: %s reason: %s", line.getStatusCode(), line.getReasonPhrase());
    }

    @Nonnull
    private JSONObject toJson(@Nonnull String data) throws IOException {
        try {
            return new JSONObject(data);
        } catch (JSONException e) {
            throw new IOException(String.format("No json response: %s", e.getMessage()));
        }
    }

    private static class Response {

        final String reasonPhrase;
        final int status;
        final String body;
        final Map<String, List<String>> headers;

        private Response(int status, @Nonnull String reasonPhrase, @Nonnull Map<String, List<String>> headers, @Nonnull String body) {
            this.reasonPhrase = reasonPhrase;
            this.headers = headers;
            this.body = body;
            this.status = status;
        }
    }
}
