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
package com.viaoa.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import com.viaoa.lang.OAString;

/**
 * Utility class for reading a delimited text file line by line and converting
 * each row into an array of column values. Subclasses implement
 * {@link #process(String[], int)} to receive parsed rows. <p>
 *
 * The {@link #read(File, String, boolean)} method streams the file using a
 * {@link java.io.BufferedReader}, parses each line using
 * {@link com.viaoa.lang.OAString#parseLine(String, char, boolean)}, and
 * invokes the process callback for each successfully parsed row. Column
 * parsing supports optional quoted fields and single-character delimiters. <p>
 *
 * This class does not perform trimming or type conversion and does not manage
 * multi-character delimiters. It is intended for simple CSV/TSV-style formats
 * where each line corresponds to a logical record.
 */
public abstract class LoadDelimitedFile {
    
	/**
	 * Reads a delimited text file line-by-line and processes each parsed row.
	 * <p>
	 * Each line is read using a {@link java.io.BufferedReader} and parsed into
	 * columns using {@link #parse(String, char, boolean, int)}. If parsing returns
	 * a non-null column array, {@link #process(String[], int)} is invoked for that
	 * line.
	 * <p>
	 * This method does not trim fields, perform type conversion, or apply
	 * multi-line/record aggregation; each input line is treated as one record.
	 *
	 * @param file the delimited text file to read
	 * @param sep the single-character delimiter used to separate columns (for example ',' or '\t')
	 * @param bQuoted {@code true} if fields may be wrapped in quotes and should be parsed accordingly
	 * @throws Exception if an I/O error occurs or a subclass throws an exception during processing
	 */
    public void read(File file, char sep, boolean bQuoted) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        for (int i=1;;i++) {
            String line = reader.readLine();
            if (line == null) break;
            String[] ss = parse(line, sep, bQuoted, i);
            if (ss != null) process(ss, i);
        }
        reader.close();
    }
    
    /**
     * Parses a single input line into an array of column values.
     * <p>
     * This default implementation delegates to
     * {@link com.viaoa.lang.OAString#parseLine(String, char, boolean)} to split
     * the line into fields using the supplied delimiter and quoted-field rules.
     * Subclasses may override to customize parsing behavior.
     *
     * @param line the raw input line to parse
     * @param sep the single-character delimiter used to separate columns
     * @param bQuoted {@code true} if fields may be wrapped in quotes and should be parsed accordingly
     * @param lineNumber the 1-based line number for the input line being parsed
     * @return an array of parsed column values, or {@code null} to skip processing for this line
     */
    public String[] parse(String line, char sep, boolean bQuoted, int lineNumber) {
        String[] flds = OAString.parseLine(line, sep, bQuoted);
        return flds;
    }
        
    /**
     * Callback invoked for each successfully parsed record.
     * <p>
     * Implementations define how the parsed column values should be handled,
     * such as validation, transformation, persistence, or aggregation.
     *
     * @param columns the parsed column values for the current line
     * @param lineNumber the 1-based line number of the record in the input file
     */
    public abstract void process(String[] columns, int lineNumber);
}
    
    


