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
package com.viaoa.comm.discovery;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.log.OALogUtil;

// see this for broadcast addresses
// https://en.wikipedia.org/wiki/IPv4#Addresses_ending_in_0_or_255

/**
 * Client-side discovery utility used to locate active {@code OADiscoveryServer}
 * instances on the network via UDP broadcast messages.
 *
 * <p>The client broadcasts a "where are you" message to a known server port,
 * then listens for responses from servers broadcasting their presence.</p>
 *
 * <p>Behavioral highlights:</p>
 * <ul>
 *   <li>Uses UDP broadcast to locate servers without preconfigured addresses.</li>
 *   <li>Maintains a set of server messages (typically host identifiers).</li>
 *   <li>Runs a background thread to send discovery messages and listen for replies.</li>
 *   <li>Provides an overridable callback {@link #onNewServerMessage(String)}
 *       for reacting to newly discovered servers.</li>
 * </ul>
 *
 * <p>This class is typically used by OA-based distributed apps that support
 * auto-discovery across local networks.</p>
 *
 * @see OADiscoveryServer
 */
public class OADiscoveryClient {
    private static Logger LOG = Logger.getLogger(OADiscoveryClient.class.getName());
    
    /**
     * UDP port on which this client listens for discovery server responses.
     */
    private int portReceive;
    
    /**
     * UDP port to which this client sends broadcast discovery messages.
     */
    private int portSend;
    
    /**
     * Sockets used for sending broadcast packets ({@code sockSend}) and
     * receiving server responses ({@code sockReceive}).
     */
    private DatagramSocket sockSend, sockReceive;
    
    /**
     * Cached broadcast InetAddress used for sending discovery messages.
     */
    private InetAddress iaBroadcast;
    
    /**
     * Tracks unique server messages that have been received, preventing
     * duplicate server notifications.
     */
    private HashSet<String> hsServer = new HashSet<String>();
    
    /**
     * Indicates whether the discovery thread is currently running.
     */
    private volatile boolean bStarted;
    
    /**
     * Counter used to coordinate start/stop cycles and ensure that a
     * stopped discovery thread does not continue processing.
     */
    private AtomicInteger aiStartStop = new AtomicInteger();
    
    /**
     * Message sent in UDP discovery packets. Defaults to the local host
     * IP address when not explicitly set.
     */
    private String msg;

    /**
     * Creates a discovery client configured with the server's broadcast port
     * and the client's send port.
     *
     * @param serverPort the port on which discovery servers broadcast messages
     * @param clientPort the port to which this client will send discovery messages
     */
    public OADiscoveryClient(int serverPort, int clientPort) {
        LOG.config(String.format("serverPort=%d, clientPort=%d", serverPort, clientPort));
        this.portSend = clientPort;
        this.portReceive = serverPort;
    }

    /**
     * Assigns the message payload that this client will broadcast when
     * initiating discovery.
     *
     * @param msg the message to broadcast
     */
    public void setMessage(String msg) {
        this.msg = msg;
    }

    /**
     * Returns the broadcast message. If no message has been set, this method
     * resolves the local host address and uses it as the default payload.
     *
     * @return the discovery broadcast message
     */
    public String getMessage() {
        if (msg == null) {
            try {
                InetAddress ia = InetAddress.getLocalHost();
                this.msg = ia.getHostAddress();
            }
            catch (Exception e) {
            }
        }
        return this.msg;
    }

    /**
     * Computes and returns the broadcast InetAddress used for discovery.
     * The method derives the local host address and replaces the final
     * byte with 255, forming a standard broadcast address.
     *
     * @return the broadcast InetAddress
     */
    protected InetAddress getBroadcastInetAddress() {
        if (iaBroadcast == null) {
            try {
                iaBroadcast = InetAddress.getLocalHost();
                byte[] bs = iaBroadcast.getAddress();
                bs[3] = (byte) 255;
                iaBroadcast = InetAddress.getByAddress(bs);
            }
            catch (Exception e) {
                LOG.log(Level.WARNING, "error getting broadcast InetAddress", e);
            }
        }
        return iaBroadcast;
    }
    
    /**
     * Begins the discovery process by:
     * <ul>
     *   <li>Flagging the discovery loop as active</li>
     *   <li>Creating a broadcast-enabled UDP socket</li>
     *   <li>Starting a background thread that handles sending discovery
     *       packets and listening for server responses</li>
     * </ul>
     *
     * @throws Exception if socket creation or thread startup fails
     */
    public void start() throws Exception {
        LOG.fine("starting thread that will send out broadcast message, and listen for discoveryServer broadcast msgs");
        bStarted = true;
        final int iStartStop = aiStartStop.incrementAndGet();
        sockSend = new DatagramSocket();
        sockSend.setBroadcast(true);
        
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OADiscoveryClient.this.runReceive(iStartStop);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error: " + e);
                }
            }
        }, "Discovery_Client");
        t.start();
    }

    /**
     * Core discovery loop executed in the background thread.
     *
     * <p>Behavior:</p>
     * <ul>
     *   <li>Sends the broadcast discovery message several times</li>
     *   <li>Creates and maintains a receiving socket bound to the server's reply port</li>
     *   <li>Processes incoming UDP packets from discovery servers</li>
     *   <li>Invokes {@link #onNewServerMessage(String)} when a previously unseen
     *       server message is received</li>
     *   <li>Stops when the client is stopped or a new start/stop cycle begins</li>
     * </ul>
     *
     * @param iStartStop the start/stop generation used to detect stale threads
     * @throws Exception if socket operations fail
     */
    protected void runReceive(int iStartStop) throws Exception {
        byte[] bsSend = getMessage().getBytes();
        DatagramPacket sendPacket = new DatagramPacket(bsSend, bsSend.length, getBroadcastInetAddress(), portSend);
        for (int j = 0; j < 4 && bStarted; j++) {
            sockSend.send(sendPacket);
            Thread.sleep(250);
        }

        byte[] bsReceive = new byte[1024];
        for (; bStarted && iStartStop == aiStartStop.get();) {
            if (sockReceive == null) {
                LOG.finer("Sending: "+getMessage());
                sockReceive = new DatagramSocket(portReceive);
            }
            DatagramPacket dpReceive = new DatagramPacket(bsReceive, bsReceive.length);
            sockReceive.receive(dpReceive);

            String serverMsg = new String(dpReceive.getData());
            LOG.finer("Received: " + serverMsg);

            if (!hsServer.contains(serverMsg)) {
                hsServer.add(serverMsg);
                onNewServerMessage(serverMsg);
            }

            /*
             * InetAddress ia = dpReceive.getAddress(); String s = ia.getHostAddress();
             * System.out.println("  hostAddress="+s+", port="+dpReceive.getPort());
             */
        }
        LOG.config("thread stopped");
    }

    /**
     * Stops the discovery process by:
     * <ul>
     *   <li>Clearing the active flag</li>
     *   <li>Incrementing the start/stop counter to invalidate any running threads</li>
     *   <li>Logging lifecycle information</li>
     * </ul>
     */
    public void stop() {
        bStarted = false;
        aiStartStop.getAndIncrement();
        LOG.config("stopping");
    }

    /**
     * Callback invoked whenever a new discovery server message is received.
     * Subclasses may override this method to implement custom behavior
     * (e.g., updating a UI or connecting to the discovered server).
     *
     * @param serverMessage the message received from the discovery server
     */
    public void onNewServerMessage(String serverMessage) {
        System.out.println("New Server Message: " + serverMessage);
    }

    /**
     * Diagnostic entry point. Configures console logging, launches a discovery
     * client, and runs indefinitely to display discovered servers.
     *
     * @param args ignored
     * @throws Exception if startup fails
     */
    public static void main(String args[]) throws Exception {
        OALogUtil.consoleOnly(Level.FINE, "com");
        OADiscoveryClient ds = new OADiscoveryClient(9998, 9999);
        ds.start();
        for (;;) Thread.sleep(10000);
    }
}
