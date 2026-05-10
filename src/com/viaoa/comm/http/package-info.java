/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Provides lightweight HTTP and HTTPS communication utilities for OA-based
 * applications. This package contains helper classes for issuing JSON-based
 * GET/POST requests, handling authentication and cookies, and enabling HTTPS
 * access for environments that require relaxed certificate validation.
 *
 * <h2>Current Components</h2>
 * <ul>
 *   <li>{@link com.viaoa.comm.http.HttpJsonClient HttpJsonClient} –
 *       Simple HTTP client for sending JSON requests and reading JSON
 *       responses using {@link java.net.HttpURLConnection}.</li>
 *
 *   <li>{@link com.viaoa.comm.http.OAHttpsUtil OAHttpsUtil} –
 *       Optional HTTPS helper that installs permissive SSL settings
 *       (trust-all certificates and hostnames), useful in development
 *       or controlled environments.</li>
 * </ul>
 *
 * <h2>Intended Use</h2>
 * <p>The classes in this package are designed for:</p>
 * <ul>
 *   <li>REST-style service calls from OA clients</li>
 *   <li>Server-to-server communication using simple JSON payloads</li>
 *   <li>Lightweight integration with OA objects via URL-encoding helpers</li>
 *   <li>Interacting with OA REST endpoints or external services</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li>Minimal dependencies (pure JDK networking APIs)</li>
 *   <li>Predictable, synchronous request/response behavior</li>
 *   <li>Simple debugging and diagnostics, friendly to tools and logs</li>
 *   <li>Compatibility with the OA 4.0 communication model</li>
 * </ul>
 *
 * <h2>Work in Progress</h2>
 * <p>This package is evolving and will expand to include:</p>
 * <ul>
 *   <li>Higher-level HTTP/REST wrappers for OA services</li>
 *   <li>Modern JSON serialization/deserialization utilities</li>
 *   <li>Support for PUT/DELETE and streaming uploads/downloads</li>
 *   <li>Optional integration with {@code java.net.http.HttpClient}</li>
 *   <li>Enhanced HTTPS configuration and security options</li>
 * </ul>
 *
 * <p>Until these features are complete, the current utilities provide a stable,
 * minimal foundation for performing HTTP communication in OA applications.</p>
 */
package com.viaoa.comm.http;
