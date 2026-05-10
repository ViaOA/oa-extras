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
 * Provides lightweight UDP-based service discovery for OA applications.
 *
 * <p>The classes in this package allow servers to advertise their availability
 * and clients to automatically locate those servers on a local network without
 * requiring preconfigured IP addresses or hostnames.</p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.viaoa.comm.discovery.OADiscoveryServer OADiscoveryServer} —
 *       Broadcasts periodic “here I am” messages and listens for client
 *       discovery requests.</li>
 *
 *   <li>{@link com.viaoa.comm.discovery.OADiscoveryClient OADiscoveryClient} —
 *       Sends “where are you” discovery broadcasts and listens for server
 *       replies, maintaining a list of discovered servers and providing
 *       callbacks for extensibility.</li>
 * </ul>
 *
 * <h2>How It Works</h2>
 * <p>The discovery mechanism uses UDP broadcast packets to locate services:
 * clients send a broadcast request, servers respond with identifying messages,
 * and both sides use simple callbacks for customization.  
 * This allows zero-configuration discovery within a LAN, supporting scenarios
 * such as:</p>
 *
 * <ul>
 *   <li>Cluster node discovery</li>
 *   <li>Local service auto-registration</li>
 *   <li>Dynamic endpoint lookup for distributed OA applications</li>
 * </ul>
 *
 * <h2>Threading Model</h2>
 * <p>Both client and server run background threads that manage broadcast
 * sending and packet reception. Start/stop behavior is tracked using atomic
 * generation counters to prevent old threads from continuing after restarts.</p>
 *
 * <h2>Extensibility</h2>
 * <p>Applications may override server and client callback methods such as:</p>
 * <ul>
 *   <li>{@code OADiscoveryClient.onNewServerMessage(String)}</li>
 *   <li>{@code OADiscoveryServer.shouldRespond(String)}</li>
 * </ul>
 * <p>to implement custom filtering, registration workflows, or UI updates.</p>
 *
 * <p>This package is intentionally minimal and dependency-light, making it well
 * suited for embedded systems, microservice discovery, and OA-based LAN
 * applications.</p>
 */
package com.viaoa.comm.discovery;
