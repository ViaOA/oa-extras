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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.log.OALogUtil;

/**
 * Server-side discovery component that broadcasts its availability to
 * {@link OADiscoveryClient} instances using UDP broadcast messages.
 *
 * <p>The server listens on a designated client port for "where are you"
 * discovery requests, and upon receiving them, responds by broadcasting its
 * configured message (typically its host address) on the server broadcast port.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Auto-broadcast loop that periodically sends a discovery message.</li>
 *   <li>Listener that waits for client discovery packets and optionally adjusts
 *       broadcast frequency based on {@link #shouldRespond(String)}.</li>
 *   <li>Start/stop coordination using an atomic generation counter.</li>
 *   <li>Extensible response logic through overridable callback methods.</li>
 * </ul>
 *
 * <p>This class is typically used by OA servers or services that must be
 * discoverable on a LAN without preconfigured addresses.</p>
 *
 * @see OADiscoveryClient
 */
public class OADiscoveryServer {
    private static Logger LOG = Logger.getLogger(OADiscoveryServer.class.getName());

    /**
     * UDP port on which the server listens for client discovery requests.
     */
    private int portReceive;
    
    /**
     * UDP broadcast port used to send "here I am" discovery messages to clients.
     */
    private int portSend;
    
    /**
     * UDP sockets used for broadcasting availability ({@code sockSend})
     * and receiving client discovery messages ({@code sockReceive}).
     */
    private volatile DatagramSocket sockSend, sockReceive;
    
    /**
     * Cached broadcast InetAddress used when sending UDP discovery messages.
     */
    private InetAddress iaBroadcast;
    
    /**
     * Message payload sent in broadcast packets. Defaults to the server's
     * host IP address when not explicitly assigned.
     */
    private String msg;
    
    /**
     * Indicates whether the server discovery thread is actively running.
     */
    private volatile boolean bStarted;
    
    /**
     * Tracks start/stop cycles to ensure that obsolete discovery threads
     * terminate when a new cycle begins.
     */
    private AtomicInteger aiStartStop = new AtomicInteger();

    /**
     * Creates a discovery server configured with the ports used for
     * broadcasting and receiving discovery messages.
     *
     * @param serverPort port on which this server will broadcast "here I am" messages
     * @param clientPort port on which this server listens for client discovery requests
     */
    public OADiscoveryServer(int serverPort, int clientPort) {
        LOG.config(String.format("serverPort=%d, clientPort=%d", serverPort, clientPort));
        this.portSend = serverPort;
        this.portReceive = clientPort;
    }

    /**
     * Computes and returns the broadcast InetAddress. This is derived from
     * the local host address by replacing its final byte with 255.
     *
     * @return the InetAddress to use for UDP broadcast messages
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
     * Sets the message payload to broadcast when responding to client discovery
     * requests.
     *
     * @param msg the message to broadcast
     */
    public void setMessage(String msg) {
        this.msg = msg;
    }

    /**
     * Returns the broadcast message. If no message is assigned, this method
     * resolves the local host IP address and caches it for subsequent use.
     *
     * @return the message sent to discovery clients
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
     * Starts the discovery server if it is not already running. This method:
     * <ul>
     *   <li>Marks the server as active</li>
     *   <li>Creates a new start/stop generation token</li>
     *   <li>Launches a background thread to handle broadcasting and listening
     *       for client requests</li>
     * </ul>
     *
     * @throws Exception if thread or socket initialization fails
     */
    public void start() throws Exception {
        if (bStarted) return;
        LOG.fine("starting thread that will send out broadcast messages, and listen for discoveryClient msgs");
        bStarted = true;
        final int iStartStop = aiStartStop.incrementAndGet();
        
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OADiscoveryServer.this.run(iStartStop);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error: " + e);
                }
            }
        }, "Discovery_Server");
        t.start();
    }

    /**
     * Core discovery loop executed by the background thread.
     *
     * <p>Behavior:</p>
     * <ul>
     *   <li>Broadcasts the server message multiple times per cycle</li>
     *   <li>Listens for client discovery requests on the receive port</li>
     *   <li>Logs the received message and consults {@link #shouldRespond(String)}
     *       to decide whether to continue broadcasting</li>
     *   <li>Stops when either the server is stopped or a new start/stop cycle
     *       invalidates this thread</li>
     * </ul>
     *
     * @param iStartStop the start/stop generation token used to detect stale threads
     * @throws Exception if socket operations fail
     */
    protected void run(int iStartStop) throws Exception {
        byte[] bsReceive = new byte[1024];
        int amt = 8;
        for (int i = 0; bStarted && iStartStop == aiStartStop.get(); i++) {
            for (int j = 0; j < amt && bStarted && iStartStop == aiStartStop.get(); j++) {
                send();
                Thread.sleep(250);
            }
            if (sockReceive == null) {
                sockReceive = new DatagramSocket(portReceive);
            }
            DatagramPacket dpReceive = new DatagramPacket(bsReceive, bsReceive.length);
            sockReceive.receive(dpReceive);
            String s = new String(dpReceive.getData());
            LOG.fine("received client message: " + s);
            if (!shouldRespond(s)) amt = 0;
            else amt = 2;
        }
        LOG.config("thread stopped");
    }

    /**
     * Determines whether this server should broadcast a response for the provided
     * client discovery message.
     *
     * <p>Default implementation always returns {@code true}. Subclasses may
     * override to suppress or filter responses.</p>
     *
     * @param msg the discovery request received from a client
     * @return true if the server should broadcast a response
     */
    public boolean shouldRespond(String msg) {
        return true;
    }
    
    /**
     * Stops the discovery server by:
     * <ul>
     *   <li>Clearing the active flag</li>
     *   <li>Incrementing the start/stop generation to invalidate running threads</li>
     *   <li>Logging the shutdown event</li>
     * </ul>
     */
    public void stop() {
        bStarted = false;
        aiStartStop.getAndIncrement();
        LOG.config("stopping");
    }
    
    /**
     * Broadcasts the server message on the configured broadcast port.
     * Lazily creates a broadcast-enabled UDP socket if needed.
     *
     * @throws Exception if packet creation or transmission fails
     */
    public void send() throws Exception {
        LOG.finer("Sending: " + getMessage());
        byte[] bsSend = getMessage().getBytes();
        DatagramPacket sendPacket = new DatagramPacket(bsSend, bsSend.length, getBroadcastInetAddress(), portSend);
        if (sockSend == null) {
            sockSend = new DatagramSocket();
            sockSend.setBroadcast(true);
        }
        synchronized (sockSend) {
            sockSend.send(sendPacket);
        }
    }
    
    /**
     * Diagnostic entry point that enables console logging, starts the
     * discovery server, and runs indefinitely to demonstrate discovery
     * behavior.
     *
     * @param args ignored
     * @throws Exception if server startup fails
     */
    public static void main(String args[]) throws Exception {
        OALogUtil.consoleOnly(Level.FINEST, "com");
        OADiscoveryServer ds = new OADiscoveryServer(9998, 9999);
        ds.start();
        for (;;) Thread.sleep(10000);
    }
}
