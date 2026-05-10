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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.viaoa.datetime.OADate;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.service.object.OAObjectInfoService;
import com.viaoa.lang.OAString;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.metadata.OAPropertyInfo;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.secure.Base64;


/**
 * Lightweight HTTP client for sending JSON-based GET and POST requests.
 *
 * <p>This class provides convenience wrappers around {@link HttpURLConnection}
 * for interacting with REST-style services. It supports:</p>
 *
 * <ul>
 *   <li>Optional Basic authentication</li>
 *   <li>Automatic cookie handling (Set-Cookie → Cookie)</li>
 *   <li>URL-encoded parameter generation from Maps and OAObjects</li>
 *   <li>Simple JSON request creation for POST operations</li>
 *   <li>Reading full response bodies into a String</li>
 * </ul>
 *
 * <p>Some JSON serialization/deserialization is deferred (commented out) pending
 * integration with OA's JAXB-based JSON mapping. These sections are intentionally
 * left as placeholders.</p>
 *
 * <p>This client is intended as a small, dependency-free HTTP helper for OA-based
 * applications and utilities. It does not manage connection pooling or async IO,
 * and is designed for straightforward request/response usage.</p>
 */
public class HttpJsonClient {

	/**
	 * Username used for optional Basic authentication.
	 */
	private String userId;

	/**
	 * Password used for optional Basic authentication.
	 * Declared transient to avoid accidental serialization.
	 */
	private transient String password;

	/**
	 * Cookie header value sent with HTTP requests. Updated automatically
	 * when a Set-Cookie response header is received.
	 */
	private String cookie;

	/**
	 * Assigns credentials for Basic authentication. When set, outgoing
	 * requests include an Authorization header using Base64 encoding.
	 *
	 * @param userId username to authenticate with
	 * @param password password to authenticate with
	 */
	public void setUserAccess(String userId, String password) {
		this.userId = userId;
		this.password = password;
	}

	/**
	 * Manually assigns the cookie value sent on the "Cookie" request header.
	 *
	 * @param val cookie string to send on subsequent requests
	 */
	public void setCookie(String val) {
		this.cookie = val;
	}

	/**
	 * Performs an HTTP GET request on the given URL and returns the full
	 * response body as a String.
	 *
	 * @param urlStr URL to request
	 * @return response body as text
	 * @throws IOException if the connection fails or returns non-200
	 */
	public String get(String urlStr) throws IOException {
		String json = perform(urlStr, "GET", null);
		return json;
	}

	/**
	 * Performs an HTTP GET and (once implemented) converts the JSON response
	 * into an OAObject instance of the given class.
	 *
	 * <p>Currently returns {@code null}; JSON mapping is not yet implemented.</p>
	 *
	 * @param urlStr URL to request
	 * @param responseClass type to convert JSON into
	 * @return deserialized OAObject or {@code null}
	 */
	public <T extends OAObject> T get(String urlStr, Class<T> responseClass) throws Exception {
		String json = perform(urlStr, "GET", null);

		/*
		OAJaxb<T> jaxb = new OAJaxb(responseClass);
		T obj = (T) jaxb.convertFromJSON(json);
		
		return obj;
		*/
		return null;
	}

	/**
	 * Performs an HTTP GET using URL-encoded query parameters derived from
	 * the provided map.
	 *
	 * @param urlStr base URL for the request
	 * @param mapRequest key/value parameters to encode
	 * @return response body as text
	 */
	public String get(String urlStr, Map<String, String> mapRequest) throws Exception {
		String s = urlEncode(mapRequest);
		String json = perform(urlStr + "?" + s, "GET", null);
		return json;
	}

	/**
	 * GET request with encoded parameters and optional JSON-to-object
	 * conversion. Currently returns {@code null} pending JSON deserialization.
	 *
	 * @param urlStr base URL
	 * @param responseClass type of OAObject to convert into
	 * @param mapRequest parameters to encode into the URL
	 * @return OAObject mapped from JSON or {@code null}
	 */
	public <T extends OAObject> T get(String urlStr, Class<T> responseClass, Map<String, String> mapRequest) throws Exception {
		String json = get(urlStr, mapRequest);

		/*
		OAJaxb<T> jaxb = new OAJaxb(responseClass);
		T obj = (T) jaxb.convertFromJSON(json);
		
		return obj;
		*/
		return null;
	}

	/**
	 * Performs an HTTP GET using query parameters derived from the
	 * properties of the provided OAObject.
	 *
	 * @param urlStr base URL
	 * @param objRequest source of property→value pairs for URL encoding
	 * @return response body as text
	 */
	public String get(String urlStr, OAObject objRequest) throws Exception {
		String s = urlEncode(objRequest);
		String json = perform(urlStr + "?" + s, "GET", null);
		return json;
	}

	/**
	 * GET request using URL parameters derived from an OAObject, with
	 * placeholder support for deserializing JSON into a typed OAObject.
	 *
	 * @param urlStr base URL
	 * @param responseClass desired return object type
	 * @param objRequest OAObject whose properties form the query parameters
	 * @return deserialized OAObject or {@code null}
	 */
	public <T extends OAObject> T get(String urlStr, Class<T> responseClass, OAObject objRequest) throws Exception {
		String json = get(urlStr, objRequest);

		/*
		OAJaxb<T> jaxb = new OAJaxb(responseClass);
		T obj = (T) jaxb.convertFromJSON(json);
		
		return obj;
		*/
		return null;
	}

	/**
	 * Sends an HTTP POST request with no body and returns the response as text.
	 *
	 * @param urlStr target URL
	 * @return response body
	 */
	public String post(String urlStr) throws IOException {
		String json = perform(urlStr, "POST", null);
		return json;
	}

	/**
	 * Sends an HTTP POST containing a JSON request body.
	 *
	 * @param urlStr target URL
	 * @param jsonRequest JSON text to send, or {@code null}
	 * @return response body
	 */
	public String post(String urlStr, String jsonRequest) throws IOException {
		String json = perform(urlStr, "POST", jsonRequest);
		return json;
	}

	/**
	 * POST request with optional JSON→OAObject conversion.
	 * Currently returns {@code null}.
	 *
	 * @param urlStr target URL
	 * @param responseClass expected return type
	 * @return OAObject or {@code null}
	 */
	public <T extends OAObject> T post(String urlStr, Class<T> responseClass) throws Exception {
		String json = perform(urlStr, "POST", null);
		/*
		OAJaxb<T> jaxb = new OAJaxb(responseClass);
		T obj = (T) jaxb.convertFromJSON(json);
		
		return obj;
		*/
		return null;
	}

	/**
	 * Constructs a simple JSON object from key/value pairs and POSTs it.
	 *
	 * @param urlStr target URL
	 * @param mapRequest name/value pairs to include in the JSON body
	 * @return response body text
	 */
	public String post(String urlStr, Map<String, String> mapRequest) throws Exception {
		String jsonRequest = "";
		if (mapRequest != null) {
			boolean bFirst = true;
			for (Entry<String, String> entry : mapRequest.entrySet()) {
				String key = entry.getKey();
				String val = entry.getValue();

				if (jsonRequest.length() != 0) {
					jsonRequest += ", ";
				}
				jsonRequest += "\"" + key + "\": \"" + val + "\"";
			}
		}
		String json = perform(urlStr, "POST", "{" + jsonRequest + "}");
		return json;
	}

	/**
	 * POSTs a JSON body created from a map, with placeholder support for
	 * JSON-to-object conversion.
	 *
	 * @param urlStr target URL
	 * @param responseClass expected deserialization type
	 * @param mapRequest key/value pairs to encode into JSON
	 * @return OAObject or {@code null}
	 */
	public <T extends OAObject> T post(String urlStr, Class<T> responseClass, Map<String, String> mapRequest) throws Exception {
		String jsonRequest = "";

		if (mapRequest != null) {
			boolean bFirst = true;
			for (Entry<String, String> entry : mapRequest.entrySet()) {
				String key = entry.getKey();
				String val = entry.getValue();

				if (jsonRequest.length() == 0) {
					jsonRequest += ", ";
				}
				jsonRequest += "\"" + key + "\": \"" + val + "\"";
			}
		}

		String json = perform(urlStr, "POST", "{" + jsonRequest + "}");

		/*
		OAJaxb<T> jaxb = new OAJaxb(responseClass);
		T obj = (T) jaxb.convertFromJSON(json);
		
		return obj;
		*/
		return null;
	}

	/**
	 * POSTs a JSON representation of the given OAObject.
	 * JSON serialization is not yet implemented.
	 *
	 * @param urlStr target URL
	 * @param reqObject object to serialize into JSON
	 * @return response body text
	 */
	public String post(String urlStr, OAObject reqObject) throws Exception {
		String jsonRequest;
		if (reqObject == null) {
			jsonRequest = null;
		} else {
			/*
			OAJaxb jaxb = new OAJaxb<>(reqObject.getClass());
			jsonRequest = jaxb.convertToJSON(reqObject);
			*/
			jsonRequest = null;
		}

		String json = perform(urlStr, "POST", jsonRequest);

		return json;
	}

	/**
	 * POSTs a JSON body and (eventually) converts the JSON response into
	 * an OAObject of the given type.
	 *
	 * @param urlStr target URL
	 * @param responseClass desired return type
	 * @param jsonRequest JSON request body
	 * @return OAObject or {@code null}
	 */
	public <T extends OAObject> T post(String urlStr, Class<T> responseClass, String jsonRequest) throws Exception {
		String json = perform(urlStr, "POST", jsonRequest);

		/*
		OAJaxb<T> jaxb = new OAJaxb(responseClass);
		T obj = (T) jaxb.convertFromJSON(json);
		
		return obj;
		*/
		return null;
	}

	/**
	 * POSTs an OAObject (once JSON serialization is implemented) and returns
	 * a typed response object.
	 *
	 * @param urlStr target URL
	 * @param responseClass runtime type of returned object
	 * @param reqObject request object to encode
	 * @return OAObject or {@code null}
	 */
	public <T extends OAObject> T post(String urlStr, Class<T> responseClass, OAObject reqObject) throws Exception {
		String jsonRequest;
		if (reqObject == null) {
			jsonRequest = null;
		} else {
			/*
			OAJaxb jaxb = new OAJaxb<>(reqObject.getClass());
			jsonRequest = jaxb.convertToJSON(reqObject);
			*/
			jsonRequest = null;
		}

		String json = perform(urlStr, "POST", jsonRequest);

		/*
		OAJaxb<T> jaxb = new OAJaxb(responseClass);
		T obj = (T) jaxb.convertFromJSON(json);
		
		return obj;
		*/
		return null;
	}

	/**
	 * Opens an {@link HttpURLConnection}, configures request headers,
	 * sends an optional JSON body, handles cookies and Basic authentication,
	 * validates the HTTP status, and reads the full response stream.
	 *
	 * @param urlStr target URL
	 * @param methodName HTTP method ("GET", "POST")
	 * @param jsonRequest JSON body or {@code null}
	 * @return full response body text
	 * @throws IOException if the HTTP status is non-200 or communication fails
	 */
	public String perform(String urlStr, String methodName, String jsonRequest) throws IOException {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setRequestProperty("User-Agent", "HttpJsonClient");
		conn.setRequestMethod(methodName);
		conn.setDoOutput(true);

		conn.setDoInput(true);
		conn.setUseCaches(false);
		conn.setAllowUserInteraction(false);
		if (OAString.isNotEmpty(jsonRequest)) {
			conn.setRequestProperty("Content-Type", "application/json");
		}
		conn.setRequestProperty("charset", "utf-8");
		conn.setRequestProperty("Accept", "application/json"); // "application/json, text/*;q=0.7");

		if (OAString.isNotEmpty(cookie)) {
			conn.addRequestProperty("cookie", cookie);
		}

		if (OAString.isNotEmpty(userId)) {
			String s = userId + ":" + password;
			conn.setRequestProperty("Authorization", "Basic " + Base64.encode(s));
		}

		if (OAString.isNotEmpty(jsonRequest)) {
			OutputStream out = conn.getOutputStream();
			Writer writer = new OutputStreamWriter(out, "UTF-8");
			try {
				writer.write(jsonRequest);
			}
			finally {
				writer.close();
				out.close();
			}
		}

		String setcookie = conn.getHeaderField("Set-Cookie");
		if (OAString.isNotEmpty(setcookie)) {
			this.cookie = OAString.field(setcookie, ";", 1);
		}

		// https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
		int responseCode = conn.getResponseCode();

		if (responseCode != 200) {
			throw new IOException("Error non 200 Response code:" + responseCode + ", msg=" + conn.getResponseMessage());
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		for (;;) {
			int ch = br.read();
			if (ch < 0) {
				break;
			}
			sb.append((char) ch);
		}

		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		br.close();

		conn.disconnect();
		return sb.toString();
	}

	/**
	 * Converts a map of key/value pairs into a URL-encoded query string
	 * (key=value&key=value).
	 *
	 * @param map key/value pairs to encode
	 * @return URL-encoded text
	 * @throws Exception if UTF-8 encoding fails
	 */
	protected String urlEncode(Map<String, String> map) throws Exception {
		StringBuilder sb = new StringBuilder();
		if (map != null) {
			boolean bFirst = true;
			for (Entry<String, String> entry : map.entrySet()) {
				String key = entry.getKey();
				String val = entry.getValue();

				if (!bFirst) {
					sb.append("&");
				}
				bFirst = false;
				sb.append(key);
				sb.append("=");
				sb.append(URLEncoder.encode(val, "UTF-8"));
				// https://www.jmarshall.com/easy/http/http_footnotes.html#urlencoding
			}
		}

		return sb.toString();
	}

	/**
	 * URL-encodes all property values of the given OAObject using
	 * {@link OAObjectInfo} metadata.
	 *
	 * @param obj OAObject to encode
	 * @return URL-encoded query string
	 * @throws Exception if reflection or encoding fails
	 */
	public String urlEncode(OAObject obj) throws Exception {
		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(obj.getClass());
		OAObjectInfo oi = og.objectsInternal().callObjectInfoGetOAObjectInfo(obj.getClass());
		Map<String, String> map = new HashMap<>();

		for (OAPropertyInfo pi : oi.getPropertyInfos()) {
			String val = pi.getValue(obj) + "";
			map.put(pi.getName(), val);
		}

		String result = urlEncode(map);

		return result;
	}

	/**
	 * Prints all HTTP response headers to stdout, one per line, in
	 * "Key: Value" format.
	 *
	 * @param httpURLConnection connection whose headers should be displayed
	 * @throws IOException if reading headers fails
	 */
	public static void displayHeaderFields(final HttpURLConnection httpURLConnection) throws IOException {
		StringBuilder builder = new StringBuilder();
		Map<String, List<String>> map = httpURLConnection.getHeaderFields();
		for (Map.Entry<String, List<String>> entry : map.entrySet()) {
			if (entry.getKey() == null) {
				continue;
			}
			builder.append(entry.getKey()).append(": ");

			List<String> headerValues = entry.getValue();
			Iterator<String> it = headerValues.iterator();
			if (it.hasNext()) {
				builder.append(it.next());
				while (it.hasNext()) {
					builder.append(", ").append(it.next());
				}
			}
			builder.append("\n");
		}
		System.out.println(builder);
	}

	/**
	 * Demonstration entry point showing sample GET and POST usage with
	 * parameter maps. Not intended for production use.
	 *
	 * @param args ignored
	 * @throws Exception if a sample request fails
	 */
	public static void main(String[] args) throws Exception {
		String s;
		// s = httpGet("http://localhost:8082/servlet/oarest/salesorder/23548?pp=salesorderitems.item.mold");

		// s = httpPost("http://localhost:8082/servlet/oarest/salesorder/23548", null, null);

		// s = httpPost("http://localhost:8082/servlet/oarest/salesorder/23548", null, null);

		HttpJsonClient client = new HttpJsonClient();

		// ?line=fRE&productLineCode=0&productLineSubcode=123&item=R134A-30&storeId=12345&zipcode=44260&state=wI&county=Oranga");
		Map<String, String> map = new HashMap<String, String>();
		map.put("line", "fRE");
		map.put("productLineCode", "0");
		map.put("productLineSubcode", "123");
		map.put("item", "R134A-30");
		map.put("storeId", "12345");
		map.put("zipcode", "20108");
		map.put("state", "VA");
		map.put("county", "FAIRFAX");
		map.put("stocking", "true");
		/*
				s = client.get("http://localhost:18080/retail-products/itemRestriction", map);

				s = client.post("http://localhost:18080/retail-products/iseries/itemRestriction/get", map);
		*/

		// s = client.post("http://localhost:8081/retail-products/iseries/itemRestriction/get", map);

		map = new HashMap<String, String>();
		map.put("itemRuleType", "LINE_ITEM"); // LINE, PRODUCT_LINE_CODE, PRODUCT_LINE_SUBCODE, LINE_ITEM;
		map.put("changeType", "SALES_RESTRICTED"); // SALES_RESTRICTED, FLIGHT_RESTRICTED, CAUSTIC, HYBRID_ELECTRIC, FREON_RESTRICTED;
		map.put("updateType", "ADD"); // CHANGE, ADD, DELETE, CLEAR
		// node.set("locationRuleType", ""); // NOT_USED, STORE_ID, ZIPCODE, STATE, COUNTY
		map.put("newValue", "");
		map.put("line", "WIX");
		map.put("productLineCode", "-1");
		map.put("productLineSubcode", "-1");
		map.put("item", "");
		map.put("storeId", "1234");
		map.put("zipcode", "54321");
		map.put("state", "MO");
		map.put("county", "GREENE");
		map.put("salesRestrictedEffectiveDate", new OADate().toString(OADate.JsonFormat));
		s = client.post("http://localhost:8081/retail-products/iseries/itemRestriction/put", map);

		/*
		OAJsonRootNode node = new OAJsonObjectNode();
		node.set("itemRuleType", "LINE_ITEM"); // LINE, PRODUCT_LINE_CODE, PRODUCT_LINE_SUBCODE, LINE_ITEM;
		node.set("changeType", "SALES_RESTRICTED"); // SALES_RESTRICTED, FLIGHT_RESTRICTED, CAUSTIC, HYBRID_ELECTRIC, FREON_RESTRICTED;
		node.set("updateType", "ADD"); // CHANGE, ADD, DELETE, CLEAR
		// node.set("locationRuleType", ""); // NOT_USED, STORE_ID, ZIPCODE, STATE, COUNTY
		node.set("newValue", "");
		node.set("line", "WIX");
		node.set("productLineCode", -1);
		node.set("productLineSubcode", -1);
		node.set("item", "");
		node.set("storeId", 1234);
		node.set("zipcode", "54321");
		node.set("state", "MO");
		node.set("county", "GREENE");
		node.set("salesRestrictedEffectiveDate", new OADate());
		String json = node.toJson();
		s = client.post("http://localhost:8081/retail-products/iseries/itemRestriction/put", json);
		*/

		//		s = client.post("http://localhost:8081/retail-products/iseries/items/getSalesRestrictedItemsByLocation", map);

		//qqqqqqqqq put json into a Map qqqqqqqqqqq

		// localhost:18080/retail-products/itemRestriction?line=14&productLineCode=0&productLineSubcode=0&item=2343&storeId=4&zipcode=12345&state=GA&county=Cobb

		// s = client.get("http://localhost:8081/retail-products/iseries/itemRestriction/get?line=fRE&productLineCode=0&productLineSubcode=123&item=R134A-30&storeId=12345&zipcode=44260&state=wI&county=Oranga");

		/*
		String json = "{'line'='fRE'&'productLineCode'=0&'productLineSubcode'=123&'item'='R134A-30'&'storeId'=12345&'zipcode'='44260'&'state'='wI'&'county'='Oranga'&'restrictedEffectiveDate'='2020-01-15'}";
		json = json.replace("&", ",\n");
		json = json.replace('=', ':');
		json = json.replace('\'', '\"');

		s = OAHttpClient
				.httpPost("http://localhost:8081/retail-products/iseries/itemRestriction/getRestriction", json);
		*/

		int xx = 4;
		xx++;
	}

}
