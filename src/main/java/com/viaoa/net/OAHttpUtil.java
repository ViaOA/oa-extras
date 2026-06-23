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

import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import com.viaoa.converter.OAConv;
import com.viaoa.lang.OAString;

/**
 * Utility methods for constructing URL-encoded name/value pairs and for
 * normalizing leading and trailing slashes in URL path fragments. The encoding
 * helpers support single values, arrays, and {@link List} instances, expanding
 * multi-valued inputs into repeated {@code name=value} segments joined with
 * ampersands. Values are converted to strings using {@link OAConv#toString}
 * and encoded using UTF-8 via {@link URLEncoder}. <p>
 *
 * The {@link #updateSlashes(String, boolean, boolean)} method adjusts leading
 * and trailing slashes according to the supplied flags and is useful when
 * constructing relative or absolute URL paths. All methods are static and the
 * class is thread-safe. Exceptions during URL encoding are silently ignored,
 * and callers should ensure that input names are non-empty.
 */
public class OAHttpUtil {

	/**
	 * Builds a URL-encoded query string from a map of name/value pairs.
	 *
	 * @param mapNameValue the map of parameter names to values
	 * @return the URL-encoded name/value string, or null if the map is null
	 */
	public static String getUrlEncodedNameValues(Map<String, Object> mapNameValue) {
		if (mapNameValue == null) {
			return null;
		}
		String result = null;
		for (Map.Entry<String, Object> me : mapNameValue.entrySet()) {
			if (result == null) {
				result = "";
			} else {
				result += "&";
			}
			String s = getUrlEncodedNameValues(me.getKey(), me.getValue(), null);
			result += s;
		}
		return result;
	}

	/**
	 * Builds a URL-encoded name/value string for the given parameter.
	 * Supports single values, arrays, and lists.
	 *
	 * @param name the parameter name
	 * @param value the value, array, or list of values
	 * @param format optional format passed to value conversion
	 * @return the URL-encoded name/value string
	 */
	public static String getUrlEncodedNameValues(final String name, final Object value, final String format) {
		if (OAString.isEmpty(name)) {
			throw new RuntimeException("name can not be null");
		}
		String result = null;

		if (value == null) {
			return null;
		}

		if (value.getClass().isArray()) {
			int x = Array.getLength(value);
			for (int i = 0; i < x; i++) {
				Object obj = Array.get(value, i);
				String val = OAConv.toString(obj, format);
				if (val == null) {
					val = "";
				} else {
					try {
						val = URLEncoder.encode(val, "UTF-8");
					} catch (Exception e) {
					}
				}
				if (result == null) {
					result = "";
				} else {
					result += "&";
				}
				result += name + "=" + val;
			}
		} else if (value instanceof List) {
			List list = (List) value;
			for (Object obj : list) {
				String val = OAConv.toString(obj, format);
				if (val == null) {
					val = "";
				} else {
					try {
						val = URLEncoder.encode(val, "UTF-8");
					} catch (Exception e) {
					}
				}
				if (result == null) {
					result = "";
				} else {
					result += "&";
				}
				result += name + "=" + val;
			}
		} else {
			String val = OAConv.toString(value, format);
			if (val == null) {
				val = "";
			} else {
				try {
					val = URLEncoder.encode(val, "UTF-8");
				} catch (Exception e) {
				}
			}
			result = name + "=" + val;
		}

		return result;
	}

	/**
	 * Normalizes leading and trailing slashes for a URL path fragment.
	 *
	 * @param urlValue the URL path value to normalize
	 * @param bLeadingSlash true to ensure a leading slash
	 * @param bTrailingSlash true to ensure a trailing slash
	 * @return the normalized URL path
	 */
	public static String updateSlashes(String urlValue, boolean bLeadingSlash, boolean bTrailingSlash) {
		if (urlValue == null) {
			return "";
		}
		int x = urlValue.length();
		if (x > 0) {
			char c1 = urlValue.charAt(0);
			if (bLeadingSlash) {
				if (c1 != '/') {
					urlValue = '/' + urlValue;
					x++;
				}
			} else {
				if (c1 == '/') {
					urlValue = urlValue.substring(1);
					x--;
				}
			}

			if (x > 1) {
				char c2 = urlValue.charAt(x - 1);
				if (bTrailingSlash) {
					if (c2 != '/') {
						urlValue += "/";
					}
				} else {
					if (c2 == '/') {
						urlValue = urlValue.substring(0, x - 1);
					}
				}
			}
		} else {
			if (bLeadingSlash || bTrailingSlash) {
				urlValue = "/";
			}
		}
		return urlValue;
	}

}
