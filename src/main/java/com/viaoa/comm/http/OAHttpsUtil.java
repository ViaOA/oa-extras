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
package com.viaoa.comm.http;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Utility class that configures the JVM-wide HTTPS settings to trust all SSL
 * certificates and all hostnames. This allows OA HTTP clients to connect to
 * HTTPS endpoints without requiring valid certificates or hostname matching.
 *
 * <p>Usage:</p>
 * <pre>
 *     OAHttpsUtil.setupHttpsAccess();
 * </pre>
 *
 * <p>This is primarily intended for development, testing, and controlled
 * environments where certificate validation is undesirable or unnecessary.
 * It should not be relied on for production security.</p>
 *
 * <p>Internally, the class installs:</p>
 * <ul>
 *   <li>A permissive {@link X509TrustManager} that accepts all certificates</li>
 *   <li>A permissive {@link HostnameVerifier} that approves all hostnames</li>
 *   <li>An {@link SSLContext} configured with the above trust manager</li>
 * </ul>
 *
 * <p>Note: This logic has also been integrated into OARestClient.</p>
 */
public class OAHttpsUtil {

	/**
	 * Tracks whether HTTPS access has already been configured. Ensures that the
	 * trust-all SSL configuration is installed only once.
	 */
	private static boolean bSetupHttpsAccess;

	/**
	 * Ensures that JVM-wide HTTPS behavior is configured to trust all certificates
	 * and hostnames. Safe to call multiple times; configuration is applied once.
	 *
	 * <p>If configuration fails, the method wraps the exception in a runtime
	 * exception for easier reporting.</p>
	 *
	 * @throws Exception if underlying SSL initialization fails
	 */
	public static void setupHttpsAccess() throws Exception {
		if (bSetupHttpsAccess) {
			return;
		}
		try {
			_setupHttpsAccess();
			bSetupHttpsAccess = true;
		} catch (Exception e) {
			throw new RuntimeException("OAWebUti.setupHttpsAccess failed", e);
		}
	}

	/**
	 * Installs permissive SSL infrastructure, including:
	 * <ul>
	 *   <li>An {@code X509TrustManager} that performs no certificate validation</li>
	 *   <li>An {@code SSLContext} initialized with this trust manager</li>
	 *   <li>A hostname verifier that always returns {@code true}</li>
	 * </ul>
	 *
	 * <p>Once applied, all {@link HttpsURLConnection} instances in the JVM will:</p>
	 * <ul>
	 *   <li>Accept any server certificate</li>
	 *   <li>Ignore hostname mismatches</li>
	 * </ul>
	 *
	 * <p>This method is intended for development/testing and should be used with
	 * caution in production environments.</p>
	 *
	 * @throws Exception if SSLContext initialization fails
	 */
	protected static void _setupHttpsAccess() throws Exception {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}
		} };

		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	}

}
