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
import org.neo4j.gds.core.io.file.GraphPropertyFileHeader;
import org.neo4j.gds.core.io.file.NodeFileHeader;
import org.neo4j.gds.core.io.file.RelationshipFileHeader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.neo4j.gds.core.io.file.csv.CsvFileInput.LINE_READER;

public final class CsvImportFileUtil {

    private static final ObjectReader HEADER_FILE_READER = LINE_READER;

    private CsvImportFileUtil() {}

    public static NodeFileHeader parseNodeHeader(Path headerFile, Function<String, String> labelMapping) {
        try (MappingIterator<String[]> iterator = HEADER_FILE_READER.readValues(headerFile.toFile())) {
            var headerLine = iterator.next();
            if (headerLine == null) {
                throw new UncheckedIOException(new IOException("Header line was null"));
            }
            return NodeFileHeader.of(
                headerLine,
                Arrays.stream(inferNodeLabels(headerFile))
                    .map(label -> labelMapping.apply(label))
                    .toArray(String[]::new)
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static RelationshipFileHeader parseRelationshipHeader(Path headerFile) {
        try (MappingIterator<String[]> iterator = HEADER_FILE_READER.readValues(headerFile.toFile())) {
            var headerLine = iterator.next();
            if (headerLine == null) {
                throw new UncheckedIOException(new IOException("Header line was null"));
            }
            return RelationshipFileHeader.of(headerLine, inferRelationshipType(headerFile));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static GraphPropertyFileHeader parseGraphPropertyHeader(Path headerFile) {
        try (MappingIterator<String[]> iterator = HEADER_FILE_READER.readValues(headerFile.toFile())) {
            var headerLine = iterator.next();
            if (headerLine == null) {
                throw new UncheckedIOException(new IOException("Header line was null"));
            }
            return GraphPropertyFileHeader.of(headerLine);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Map<Path, List<Path>> nodeHeaderToFileMapping(Path csvDirectory) {
        return headerToFileMapping(csvDirectory, CsvImportFileUtil::getNodeHeaderFiles);
    }

    public static Map<Path, List<Path>> relationshipHeaderToFileMapping(Path csvDirectory) {
        return headerToFileMapping(csvDirectory, CsvImportFileUtil::getRelationshipHeaderFiles);
    }

    public static Map<Path, List<Path>> graphPropertyHeaderToFileMapping(Path csvDirectory) {
        return headerToFileMapping(csvDirectory, CsvImportFileUtil::getGraphPropertyHeaderFiles);
    }

    public static List<Path> getNodeHeaderFiles(Path csvDirectory) {
        String nodeFilesPattern = "^nodes(_\\w+)*_header.csv";
        return getFilesByRegex(csvDirectory, nodeFilesPattern);
    }

    static List<Path> getRelationshipHeaderFiles(Path csvDirectory) {
        String nodeFilesPattern = "^relationships(_\\w+)+_header.csv";
        return getFilesByRegex(csvDirectory, nodeFilesPattern);
    }

    static List<Path> getGraphPropertyHeaderFiles(Path csvDirectory) {
        var graphPropertyFilesPattern = "^graph_property(_\\w+)+_header.csv";
        return getFilesByRegex(csvDirectory, graphPropertyFilesPattern);
    }

    private static Map<Path, List<Path>> headerToFileMapping(
        Path csvDirectory,
        Function<Path, Collection<Path>> headerPaths
    ) {
        Map<Path, List<Path>> headerToDataFileMapping = new HashMap<>();
        for (Path headerFile : headerPaths.apply(csvDirectory)) {
            String dataFilePattern = headerFile.getFileName().toString().replace("_header", "(_\\d+)");
            List<Path> dataPaths = headerToDataFileMapping.computeIfAbsent(
                headerFile,
                path -> new ArrayList<>()
            );
            dataPaths.addAll(getFilesByRegex(csvDirectory, dataFilePattern));
        }
        return headerToDataFileMapping;
    }

    private static List<Path> getFilesByRegex(Path csvDirectory, String pattern) {
        var matcher = csvDirectory.getFileSystem().getPathMatcher("regex:" + pattern);
        try (var fileStream = Files.newDirectoryStream(csvDirectory, entry -> matcher.matches(entry.getFileName()))) {
            var files = new ArrayList<Path>();
            fileStream.forEach(files::add);
            return files;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String[] inferNodeLabels(Path headerFile) {
        return inferNodeLabels(headerFile.getFileName().toString());
    }

    static String[] inferNodeLabels(String headerFileName) {
        var nodeLabels = headerFileName.replaceAll("nodes_|_?header.csv", "").split("_");
        return noLabelFound(nodeLabels) ? new String[0] : nodeLabels;
    }

    private static boolean noLabelFound(String[] nodeLabels) {
        return nodeLabels.length == 1 && nodeLabels[0].isEmpty();
    }

    private static String inferRelationshipType(Path headerFile) {
        var headerFileName = headerFile.getFileName().toString();
        return headerFileName.replaceAll("relationships_|_header.csv", "");
    }
}
