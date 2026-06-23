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
package com.viaoa.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Network utility helpers for locating local network information and performing
 * simple subnet scans. Provides methods for obtaining the primary non-loopback
 * IPv4 address, the machine's MAC address, the local host name, and for
 * enumerating all IPv4 addresses assigned to active network interfaces. Also
 * includes a diagnostic subnet scanner that probes a fixed address range using
 * {@link InetAddress#isReachable(int)} and reverse DNS lookup. <p>
 *
 * The MAC address lookup is cached after first retrieval, and all methods
 * operate solely on the Java standard networking APIs without any OA-specific
 * dependencies. Exceptions are silently ignored and null results indicate that
 * a value could not be determined. This class is stateless aside from the
 * cached MAC address and is safe for concurrent invocation.
 */
public class OANetwork {

	/**
	 * Scans a fixed range of IP addresses on the local subnet and outputs reachability
	 * and reverse DNS lookup information to standard output.
	 *
	 * @throws Exception if an error occurs while resolving or probing addresses
	 */
	public static void findAllServers() throws Exception {
		InetAddress localhost = InetAddress.getLocalHost();

		byte[] ip = localhost.getAddress();

		for (int i = 210; i <= 254; i++) {
			ip[3] = (byte) i;

			System.out.println(i + ") ");

			InetAddress address = InetAddress.getByAddress(ip);

			String s = address.getHostAddress();
			System.out.println("  " + address);

			if (address.isReachable(250)) {
				// machine is turned on and can be pinged
				System.out.println("  reachable using ping");
				continue;
			}

			System.out.println("  checking reverse DNS lookup");
			String s2 = address.getHostName();
			if (!s.equals(s2)) {
				// machine is known in a DNS lookup
				System.out.println("  reachable as " + address.getHostName());
			} else {
				System.out.println("  not reachable");
				// the host address and host name are equal, meaning the host name could not be resolved
			}
		}
	}

	//return current client mac address
	/**
	 * Cached MAC address of the local network interface.
	 */
	protected static String macAddress;

	/**
	 * Returns the MAC address of the local machine.
	 *
	 * @return the MAC address string
	 * @throws Exception if the MAC address cannot be determined
	 */
	public static String getMACAddress() throws Exception {
		if (macAddress != null) {
			return macAddress;
		}
		InetAddress ip;
		StringBuilder sb = new StringBuilder(32);

		ip = InetAddress.getLocalHost();
		NetworkInterface network = NetworkInterface.getByInetAddress(ip);
		byte[] mac = network.getHardwareAddress();

		for (int i = 0; i < mac.length; i++) {
			sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
		}
		macAddress = sb.toString();
		return macAddress;
	}

	/**
	 * Returns the first non-loopback IPv4 address found on the active network interfaces.
	 *
	 * @return the primary {@link InetAddress}, or null if none is found
	 */
	public static InetAddress getMainInetAddress() {
		try {
			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
			while (e.hasMoreElements()) {
				NetworkInterface n = e.nextElement();
				Enumeration<InetAddress> ee = n.getInetAddresses();
				while (ee.hasMoreElements()) {
					InetAddress i = ee.nextElement();
					String ip = i.getHostAddress();
					if (ip.matches("[0-9]*\\.[0-9]*\\.[0-9]*\\.[0-9]*") && !ip.startsWith("127")) {
						return i;
					}
				}
			}
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Returns the first non-loopback IPv4 address found on the active network interfaces
	 * as a string.
	 *
	 * @return the IP address string, or null if none is found
	 */
	public static String getIPAddress() {
		try {
			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
			while (e.hasMoreElements()) {
				NetworkInterface n = e.nextElement();
				Enumeration<InetAddress> ee = n.getInetAddresses();
				while (ee.hasMoreElements()) {
					InetAddress i = ee.nextElement();
					String ip = i.getHostAddress();
					if (ip.matches("[0-9]*\\.[0-9]*\\.[0-9]*\\.[0-9]*") && !ip.startsWith("127")) {
						return ip;
					}
				}
			}
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Returns a comma-separated list of all non-loopback IPv4 addresses found on the
	 * active network interfaces.
	 *
	 * @return a comma-separated list of IP addresses, or null if none are found
	 */
	public static String getIPAddresses() {
		String ips = null;
		try {
			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
			while (e.hasMoreElements()) {
				NetworkInterface n = e.nextElement();
				Enumeration<InetAddress> ee = n.getInetAddresses();
				while (ee.hasMoreElements()) {
					InetAddress i = ee.nextElement();
					String ip = i.getHostAddress();
					if (ip.matches("[0-9]*\\.[0-9]*\\.[0-9]*\\.[0-9]*") && !ip.startsWith("127")) {
						if (ips == null) {
							ips = ip;
						} else {
							ips += ", " + ip;
						}
					}
				}
			}
		} catch (Exception e) {
		}
		return ips;
	}

	/**
	 * Returns the local host name.
	 *
	 * @return the host name, or null if it cannot be determined
	 */
	public static String getHostName() {
		try {
			InetAddress ia = InetAddress.getLocalHost();
			String hostName = ia.getHostName();
			return hostName;
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Entry point used for diagnostic or test execution.
	 *
	 * @param args command-line arguments
	 * @throws Exception if an error occurs during execution
	 */
	public static void main(String[] args) throws Exception {
		// findAllServers();
		for (int i = -5; i < 5; i++) {
			String sx = Integer.toBinaryString(i);
			String s = showAsBinary(i);
			System.out.println(i + " " + sx + " " + s);
		}
		int i = 4;
		i++;
	}

	/**
	 * Returns a 32-bit binary string representation of the given integer value.
	 *
	 * @param x the integer value to convert
	 * @return the binary string representation
	 */
	public static String showAsBinary(final int x) {
		String s = "";
		for (int i = 0; i < 32; i++) {
			int xx = (x >> (31 - i));
			xx &= 0x01;
			s += (xx == 1 ? "1" : "0");
		}
		return s;
	}

}
