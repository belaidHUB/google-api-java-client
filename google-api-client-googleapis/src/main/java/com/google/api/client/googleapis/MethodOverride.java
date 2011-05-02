/*
 * Copyright (c) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.client.googleapis;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpMethod;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;

import java.io.IOException;
import java.util.EnumSet;

/**
 * HTTP request execute interceptor for Google API's that wraps HTTP requests -- other than GET or
 * POST -- inside of a POST request and uses {@code "X-HTTP-Method-Override"} header to specify the
 * actual HTTP method.
 * <p>
 * Use this for an HTTP transport that doesn't support PATCH like {@code NetHttpTransport} or {@code
 * UrlFetchTransport}. By default, only the methods not supported by the transport will be
 * overridden. When running behind a firewall that does not support certain verbs like PATCH, use
 * the {@link MethodOverride(EnumSet<HttpMethod>)} constructor instead to specify additional methods
 * to override.
 * </p>
 * <p>
 * Sample usage, taking advantage that this class implements {@link HttpRequestInitializer}:
 * </p>
 *
 * <pre>
  public static HttpRequestFactory createRequestFactory(HttpTransport transport) {
    return transport.createRequestFactory(new MethodOverride());
  }
 * </pre>
 *
 * <p>
 * If you have a custom request initializer, take a look at the sample usage for
 * {@link HttpExecuteInterceptor}, which this class also implements.
 * </p>
 *
 * @since 1.4
 * @author Yaniv Inbar
 */
public final class MethodOverride implements HttpExecuteInterceptor, HttpRequestInitializer {

  /**
   * HTTP methods supported by the HTTP transport that nevertheless need to be overridden.
   * <p>
   * Any HTTP method not supported by the HTTP transport is automatically overridden, so it doesn't
   * matter if those HTTP methods are specified here. By default, the methods DELETE, HEAD, PATCH,
   * and PUT are all overridden. GET and POST are never overridden.
   * </p>
   */
  private final EnumSet<HttpMethod> override;

  /**
   * Assumes not override HTTP methods unless the transport doesn't support them.
   */
  public MethodOverride() {
    override = EnumSet.noneOf(HttpMethod.class);
  }

  /**
   * @param override HTTP methods to override (in addition to the ones the transport doesn't
   *        support).
   */
  public MethodOverride(EnumSet<HttpMethod> override) {
    this.override = override.clone();
  }

  public void initialize(HttpRequest request) {
    request.interceptor = this;
  }

  public void intercept(HttpRequest request) throws IOException {
    if (overrideThisMethod(request)) {
      HttpMethod method = request.method;
      request.method = HttpMethod.POST;
      request.headers.set("X-HTTP-Method-Override", method.name());
      // Google servers will fail to process a POST unless the Content-Length header >= 1
      if (request.content == null || request.content.getLength() == 0) {
        request.content = new ByteArrayContent(" ");
      }
    }
  }

  private boolean overrideThisMethod(HttpRequest request) {
    HttpMethod method = request.method;
    if (method != HttpMethod.GET && method != HttpMethod.POST && override.contains(method)) {
      return true;
    }
    switch (method) {
      case PATCH:
        return !request.transport.supportsPatch();
      case HEAD:
        return !request.transport.supportsHead();
    }
    return false;
  }
}
