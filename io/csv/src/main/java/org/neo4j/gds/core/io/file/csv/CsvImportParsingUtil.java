/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gds.core.io.file.csv;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import org.neo4j.gds.api.DefaultValue;
import org.neo4j.gds.api.nodeproperties.ValueType;

import java.io.IOException;

final class CsvImportParsingUtil {

    private static final ValueType.Visitor<CsvParsingFunction> PARSING_VISITOR = new ValueType.Visitor<>() {
        @Override
        public CsvParsingFunction visitLong() {
            return CsvImportParsingUtil::parseLongValue;
        }

        @Override
        public CsvParsingFunction visitDouble() {
            return CsvImportParsingUtil::parseDoubleValue;
        }

        @Override
        public CsvParsingFunction visitString() {
            throw new UnsupportedOperationException("String value parsing is not supported");
        }

        @Override
        public CsvParsingFunction visitLongArray() {
            return CsvImportParsingUtil::parseLongArray;
        }

        @Override
        public CsvParsingFunction visitDoubleArray() {
            return CsvImportParsingUtil::parseDoubleArray;
        }

        @Override
        public CsvParsingFunction visitFloatArray() {
            return CsvImportParsingUtil::parseFloatArray;
        }
    };

    @FunctionalInterface
    interface CsvParsingFunction {
        Object parse(String value, DefaultValue defaultValue, ObjectReader arrayReader) throws IOException;
    }

    public static Object parseProperty(
        String value,
        ValueType valueType,
        DefaultValue defaultValue,
        ObjectReader arrayReader
    ) throws IOException {
        return valueType.accept(PARSING_VISITOR).parse(value, defaultValue, arrayReader);
    }

    static long parseId(String value) {
        return Long.parseLong(value);
    }

    private static long parseLongValue(String value, DefaultValue defaultValue, ObjectReader arrayReader) {
        if (value.isBlank()) {
            return defaultValue.longValue();
        }
        return parseId(value);
    }

    private static double parseDoubleValue(String value, DefaultValue defaultValue, ObjectReader arrayReader) {
        if (value.isBlank()) {
            return defaultValue.doubleValue();
        }
        return Double.parseDouble(value);
    }

    private static float[] parseFloatArray(
        String value,
        DefaultValue defaultValue,
        ObjectReader arrayReader
    ) throws IOException {
        try (MappingIterator<String[]> objectMappingIterator = arrayReader.readValues(value)) {
            if (objectMappingIterator.hasNext()) {
                var stringArray = objectMappingIterator.next();
                var parsedArray = new float[stringArray.length];
                for (int i = 0; i < stringArray.length; i++) {
                    String s = stringArray[i];
                    parsedArray[i] = Float.parseFloat(s);
                }
                return parsedArray;
            }
        }
        return defaultValue.floatArrayValue();
    }

    private static double[] parseDoubleArray(
        String value,
        DefaultValue defaultValue,
        ObjectReader arrayReader
    ) throws IOException {
        try (MappingIterator<String[]> objectMappingIterator = arrayReader.readValues(value)) {
            if (objectMappingIterator.hasNext()) {
                var stringArray = objectMappingIterator.next();
                var parsedArray = new double[stringArray.length];
                for (int i = 0; i < stringArray.length; i++) {
                    String s = stringArray[i];
                    parsedArray[i] = (double) parseProperty(s, ValueType.DOUBLE, defaultValue, arrayReader);
                }
                return parsedArray;
            }
        }
        return defaultValue.doubleArrayValue();
    }

    private static long[] parseLongArray(
        String value,
        DefaultValue defaultValue,
        ObjectReader arrayReader
    ) throws IOException {
        try (MappingIterator<String[]> objectMappingIterator = arrayReader.readValues(value)) {
            if (objectMappingIterator.hasNext()) {
                var stringArray = objectMappingIterator.next();
                var parsedArray = new long[stringArray.length];
                for (int i = 0; i < stringArray.length; i++) {
                    String s = stringArray[i];
                    parsedArray[i] = (long) parseProperty(s, ValueType.LONG, defaultValue, arrayReader);
                }
                return parsedArray;
            }
        }
        return defaultValue.longArrayValue();
    }

    private CsvImportParsingUtil() {}
}
