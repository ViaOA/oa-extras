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
package com.viaoa.serialize.csv;

import java.util.ArrayList;

import com.viaoa.hub.Hub;
import com.viaoa.lang.OAString;
import com.viaoa.object.OAObject;
import com.viaoa.path.OAPath;

/**
 * Utility for exporting the contents of a {@link Hub} to CSV using a list of
 * property paths. Callers register properties with a column title and a
 * property-path expression, and then invoke {@link #download()} to iterate the
 * hub and produce one CSV line per object. <p>
 *
 * Property values are retrieved using {@link OAPath#getValue(Object)},
 * and fields are encoded using {@link OAString#csv(String, Object)} to ensure
 * proper quoting and delimiter handling. Subclasses implement
 * {@link #onWriteLine(String)} to direct each generated CSV line to the desired
 * output destination (file, servlet stream, buffer, etc.). <p>
 *
 * This class supports only simple one-line-per-object exports; paths that
 * traverse many-valued relationships are not expanded into multiple rows.
 *
 * @param <F> the OAObject type contained in the hub.
 */
public abstract class OADownloadCsv<F extends OAObject> {
    /**
     * The hub containing the objects to be exported.
     */
    protected Hub<F> hub;

    /**
     * Collection of registered properties defining CSV columns.
     */
    private ArrayList<MyProperty> alProperty = new ArrayList<>();
    
    /**
     * Creates a new CSV download utility for the given hub.
     *
     * @param hub the hub containing objects to export
     */
    public OADownloadCsv(Hub<F> hub) {
        this.hub = hub;
    }

    /**
     * Container for a CSV column definition.
     */
    protected static class MyProperty {
    	/**
    	 * The column title used in the CSV header.
    	 */
        String title;

        /**
         * The property-path expression used to retrieve values.
         */
        String propPath;

        /**
         * Resolved property-path helper used to extract values from objects.
         */
        OAPath pp;
    }

    /**
     * Registers a property path to be exported as a CSV column.
     *
     * @param title the column title
     * @param propPath the property-path expression
     */
    public void addProperty(String title, String propPath) {
        MyProperty mp = new MyProperty();
        mp.title = title;
        mp.propPath = propPath;
        mp.pp = new OAPath(hub.getObjectClass(), propPath);
        
        mp.pp.getLinkInfos();
        alProperty.add(mp);
    }
    
    /**
     * Writes the CSV header and one data line for each object in the hub.
     */
    public void download() {
        writeHeading();
        for (F obj : hub) {
            writeData(obj);
        }
    }
    
    /**
     * Builds and writes the CSV header line using registered column titles.
     */
    protected void writeHeading() {
        String txt = "";
        for (MyProperty mp : alProperty) {
            txt = OAString.csv(txt, mp.title);
        }
        onWriteLine(txt);
    }
    
    /**
     * Builds and writes a CSV data line for the given object.
     *
     * @param obj the object whose property values are written
     */
    protected void writeData(F obj) {
        String txt = "";
        for (MyProperty mp : alProperty) {
            Object val = mp.pp.getValue(obj);
            txt = OAString.csv(txt, val);
        }
        onWriteLine(txt);
    }
    
    /**
     * Writes a single CSV line to the output destination.
     *
     * @param txt the CSV-formatted line
     */
    protected abstract void onWriteLine(String txt);
    
}
