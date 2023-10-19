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
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.neo4j.gds.api.schema.MutableNodeSchema;
import org.neo4j.gds.api.schema.MutableRelationshipSchema;
import org.neo4j.gds.api.schema.PropertySchema;
import org.neo4j.gds.api.schema.RelationshipPropertySchema;
import org.neo4j.gds.compat.CompatPropertySizeCalculator;
import org.neo4j.gds.core.io.GraphStoreInput;
import org.neo4j.gds.core.io.file.FileHeader;
import org.neo4j.gds.core.io.file.FileInput;
import org.neo4j.gds.core.io.file.GraphInfo;
import org.neo4j.gds.core.io.file.GraphPropertyFileHeader;
import org.neo4j.gds.core.io.file.HeaderProperty;
import org.neo4j.gds.core.io.file.MappedListIterator;
import org.neo4j.gds.core.io.file.NodeFileHeader;
import org.neo4j.gds.core.io.file.RelationshipFileHeader;
import org.neo4j.gds.core.loading.Capabilities;
import org.neo4j.internal.batchimport.InputIterable;
import org.neo4j.internal.batchimport.InputIterator;
import org.neo4j.internal.batchimport.input.Collector;
import org.neo4j.internal.batchimport.input.Groups;
import org.neo4j.internal.batchimport.input.IdType;
import org.neo4j.internal.batchimport.input.Input;
import org.neo4j.internal.batchimport.input.InputChunk;
import org.neo4j.internal.batchimport.input.InputEntityVisitor;
import org.neo4j.internal.batchimport.input.ReadableGroups;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

final class CsvFileInput implements FileInput {

    private static final char COLUMN_SEPARATOR = ',';
    private static final String ARRAY_ELEMENT_SEPARATOR = ";";
    private static final CsvMapper CSV_MAPPER = new CsvMapper();
    static final ObjectReader LINE_READER = CSV_MAPPER
        .readerForArrayOf(String.class)
        .with(CsvSchema
            .emptySchema()
            .withColumnSeparator(COLUMN_SEPARATOR)
        )
        .with(CsvParser.Feature.WRAP_AS_ARRAY)
        .with(CsvParser.Feature.SKIP_EMPTY_LINES);
    private static final ObjectReader ARRAY_READER = CSV_MAPPER
        .readerForArrayOf(String.class)
        .with(CsvSchema
            .emptySchema()
            .withArrayElementSeparator(ARRAY_ELEMENT_SEPARATOR)
        );

    private final Path importPath;
    private final String userName;
    private final GraphInfo graphInfo;
    private final MutableNodeSchema nodeSchema;
    private final Optional<HashMap<String, String>> labelMapping;
    private final MutableRelationshipSchema relationshipSchema;
    private final Map<String, PropertySchema> graphPropertySchema;
    private final Capabilities capabilities;

    CsvFileInput(Path importPath) {
        this.importPath = importPath;
        this.userName = new UserInfoLoader(importPath).load();
        this.graphInfo = new GraphInfoLoader(importPath, CSV_MAPPER).load();
        this.nodeSchema = new NodeSchemaLoader(importPath).load();
        this.labelMapping = new NodeLabelMappingLoader(importPath).load();
        this.relationshipSchema = new RelationshipSchemaLoader(importPath).load();
        this.graphPropertySchema = new GraphPropertySchemaLoader(importPath).load();
        this.capabilities = new GraphCapabilitiesLoader(importPath, CSV_MAPPER).load();
    }

    @Override
    public InputIterable nodes(Collector badCollector) {
        Map<Path, List<Path>> pathMapping = CsvImportFileUtil.nodeHeaderToFileMapping(importPath);
        Map<NodeFileHeader, List<Path>> headerToDataFilesMapping = pathMapping.entrySet()
            .stream()
            .collect(Collectors.toMap(
                entry -> CsvImportFileUtil.parseNodeHeader(
                    entry.getKey(),
                    labelMapping.isPresent() ? labelMapping.get()::get : Functions.identity()
                ),
                Map.Entry::getValue
            ));

        return () -> new NodeImporter(headerToDataFilesMapping, nodeSchema);
    }

    @Override
    public InputIterable relationships(Collector badCollector) {
        Map<Path, List<Path>> pathMapping = CsvImportFileUtil.relationshipHeaderToFileMapping(importPath);
        Map<RelationshipFileHeader, List<Path>> headerToDataFilesMapping = pathMapping.entrySet().stream().collect(Collectors.toMap(
            entry -> CsvImportFileUtil.parseRelationshipHeader(entry.getKey()),
            Map.Entry::getValue
        ));

        return () -> new RelationshipImporter(headerToDataFilesMapping, relationshipSchema);
    }

    @Override
    public InputIterable graphProperties() {
        var pathMapping = CsvImportFileUtil.graphPropertyHeaderToFileMapping(importPath);
        var headerToDataFilesMapping = pathMapping.entrySet().stream().collect(Collectors.toMap(
            entry -> CsvImportFileUtil.parseGraphPropertyHeader(entry.getKey()),
            Map.Entry::getValue
        ));

        return () -> new GraphPropertyImporter(headerToDataFilesMapping, graphPropertySchema);
    }

    @Override
    public IdType idType() {
        return IdType.ACTUAL;
    }

    @Override
    public ReadableGroups groups() {
        return Groups.EMPTY;
    }

    @Override
    public Input.Estimates calculateEstimates(CompatPropertySizeCalculator propertySizeCalculator) {
        return null;
    }

    @Override
    public String userName() {
        return userName;
    }

    @Override
    public GraphInfo graphInfo() {
        return graphInfo;
    }

    @Override
    public MutableNodeSchema nodeSchema() {
        return nodeSchema;
    }

    @Override
    public Optional<HashMap<String, String>> labelMapping() {
        return labelMapping;
    }

    @Override
    public MutableRelationshipSchema relationshipSchema() {
        return relationshipSchema;
    }

    @Override
    public Map<String, PropertySchema> graphPropertySchema() {
        return graphPropertySchema;
    }

    @Override
    public Capabilities capabilities() {
        return capabilities;
    }

    abstract static class FileImporter<
        HEADER extends FileHeader<SCHEMA, PROPERTY_SCHEMA>,
        SCHEMA,
        PROPERTY_SCHEMA extends PropertySchema> implements InputIterator {

        private final MappedListIterator<HEADER, Path> entryIterator;
        final SCHEMA elementSchema;

        FileImporter(
            Map<HEADER, List<Path>> headerToDataFilesMapping,
            SCHEMA elementSchema
        ) {
            this.entryIterator = new MappedListIterator<>(headerToDataFilesMapping);
            this.elementSchema = elementSchema;
        }

        @Override
        public synchronized boolean next(InputChunk chunk) throws IOException {
            if (entryIterator.hasNext()) {
                Pair<HEADER, Path> entry = entryIterator.next();

                assert chunk instanceof LineChunk;
                var header = entry.getKey();
                ((LineChunk<HEADER, SCHEMA, PROPERTY_SCHEMA>) chunk).initialize(header, entry.getValue());
                return true;
            }
            return false;
        }

        @Override
        public void close() {
        }
    }

    static class NodeImporter extends FileImporter<NodeFileHeader, MutableNodeSchema, PropertySchema> {

        NodeImporter(
            Map<NodeFileHeader, List<Path>> headerToDataFilesMapping,
            MutableNodeSchema nodeSchema
        ) {
            super(headerToDataFilesMapping, nodeSchema);
        }

        @Override
        public InputChunk newChunk() {
            return new NodeLineChunk(elementSchema);
        }
    }

    static class RelationshipImporter extends FileImporter<RelationshipFileHeader, MutableRelationshipSchema, RelationshipPropertySchema> {

        RelationshipImporter(
            Map<RelationshipFileHeader, List<Path>> headerToDataFilesMapping,
            MutableRelationshipSchema relationshipSchema
        ) {
            super(headerToDataFilesMapping, relationshipSchema);
        }

        @Override
        public InputChunk newChunk() {
            return new RelationshipLineChunk(elementSchema);
        }
    }

    static class GraphPropertyImporter extends FileImporter<GraphPropertyFileHeader, Map<String, PropertySchema>, PropertySchema> {


        GraphPropertyImporter(
            Map<GraphPropertyFileHeader, List<Path>> headerToDataFilesMapping,
            Map<String, PropertySchema> graphPropertySchema
        ) {
            super(headerToDataFilesMapping, graphPropertySchema);
        }

        @Override
        public InputChunk newChunk() {
            return new GraphPropertyLineChunk(elementSchema);
        }
    }

    abstract static class LineChunk<
        HEADER extends FileHeader<SCHEMA, PROPERTY_SCHEMA>,
        SCHEMA,
        PROPERTY_SCHEMA extends PropertySchema> implements InputChunk, GraphStoreInput.LastProgress {

        private final SCHEMA schema;

        HEADER header;
        Map<String, PROPERTY_SCHEMA> propertySchemas;
        MappingIterator<String[]> lineIterator;

        LineChunk(SCHEMA schema) {
            this.schema = schema;
        }

        void initialize(HEADER header, Path path) throws IOException {
            this.header = header;
            this.propertySchemas = header.schemaForIdentifier(schema);
            this.lineIterator = LINE_READER.readValues(path.toFile());
        }

        @Override
        public boolean next(InputEntityVisitor visitor) throws IOException {
            if (lineIterator.hasNext()) {
                String[] lineArray = lineIterator.next();
                // Ignore empty lines
                if (lineArray.length != 0) {
                    visitLine(lineArray, header, visitor);
                }
                return true;
            }
            return false;
        }

        abstract void visitLine(String[] lineArray, HEADER header, InputEntityVisitor visitor) throws IOException;

        @Override
        public void close() throws IOException {
            if (lineIterator != null) {
                lineIterator.close();
            }
        }

        @Override
        public long lastProgress() {
            return 1;
        }
    }

    static class NodeLineChunk extends LineChunk<NodeFileHeader, MutableNodeSchema, PropertySchema> {

        NodeLineChunk(MutableNodeSchema nodeSchema) {
            super(nodeSchema);
        }

        @Override
        void visitLine(String[] lineArray, NodeFileHeader header, InputEntityVisitor visitor) throws IOException {
            visitor.labels(header.nodeLabels());
            visitor.id(CsvImportParsingUtil.parseId(lineArray[0]));

            visitProperties(header, propertySchemas, visitor, lineArray);

            visitor.endOfEntity();
        }
    }

    static class RelationshipLineChunk extends LineChunk<RelationshipFileHeader, MutableRelationshipSchema, RelationshipPropertySchema> {

        RelationshipLineChunk(MutableRelationshipSchema relationshipSchema) {
            super(relationshipSchema);
        }

        @Override
        void visitLine(String[] lineArray, RelationshipFileHeader header, InputEntityVisitor visitor) throws IOException {
            visitor.type(header.relationshipType());
            visitor.startId(CsvImportParsingUtil.parseId(lineArray[0]));
            visitor.endId(CsvImportParsingUtil.parseId(lineArray[1]));

            visitProperties(header, propertySchemas, visitor, lineArray);

            visitor.endOfEntity();
        }
    }

    static class GraphPropertyLineChunk extends LineChunk<GraphPropertyFileHeader, Map<String, PropertySchema>, PropertySchema> {

        GraphPropertyLineChunk(Map<String, PropertySchema> stringPropertySchemaMap) {
            super(stringPropertySchemaMap);
        }

        @Override
        void visitLine(
            String[] lineArray, GraphPropertyFileHeader header, InputEntityVisitor visitor
        ) throws IOException {
            visitProperties(header, propertySchemas, visitor, lineArray);
            visitor.endOfEntity();
        }
    }

    private static <PROPERTY_SCHEMA extends PropertySchema> void visitProperties(
        FileHeader<?, PROPERTY_SCHEMA> header,
        Map<String, PROPERTY_SCHEMA> propertySchemas,
        InputEntityVisitor visitor,
        String[] parsedLine
    ) throws IOException {
        for (HeaderProperty headerProperty : header.propertyMappings()) {
            var stringProperty = parsedLine[headerProperty.position()];
            var propertyKey = headerProperty.propertyKey();
            var defaultValue = propertySchemas.get(propertyKey).defaultValue();
            var value = CsvImportParsingUtil.parseProperty(stringProperty, headerProperty.valueType(), defaultValue, ARRAY_READER);
            visitor.property(propertyKey, value);
        }
    }
}
