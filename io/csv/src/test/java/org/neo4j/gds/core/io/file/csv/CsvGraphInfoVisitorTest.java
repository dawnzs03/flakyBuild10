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

import org.junit.jupiter.api.Test;
import org.neo4j.gds.ElementIdentifier;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.api.DatabaseId;
import org.neo4j.gds.core.io.file.ImmutableGraphInfo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.neo4j.gds.core.io.file.csv.CsvGraphInfoVisitor.GRAPH_INFO_FILE_NAME;

class CsvGraphInfoVisitorTest extends CsvVisitorTest {

    @Test
    void shouldExportGraphInfo() {
        DatabaseId databaseId = DatabaseId.random();
        String idMapBuilderType = "custom";
        CsvGraphInfoVisitor graphInfoVisitor = new CsvGraphInfoVisitor(tempDir);
        var relationshipTypeCounts = Map.of(RelationshipType.of("REL1"), 42L, RelationshipType.of("REL2"), 1337L);
        var inverseIndexedRelTypes = List.of(RelationshipType.of("REL1"),RelationshipType.of("REL2"));
        var graphInfo = ImmutableGraphInfo.builder()
            .databaseId(databaseId)
            .idMapBuilderType(idMapBuilderType)
            .nodeCount(1337L)
            .maxOriginalId(19L)
            .relationshipTypeCounts(relationshipTypeCounts)
            .inverseIndexedRelationshipTypes(inverseIndexedRelTypes)
            .build();
        graphInfoVisitor.export(graphInfo);
        graphInfoVisitor.close();

        assertCsvFiles(List.of(GRAPH_INFO_FILE_NAME));
        assertDataContent(
            GRAPH_INFO_FILE_NAME,
            List.of(
                defaultHeaderColumns(),
                List.of(
                    databaseId.databaseName(),
                    idMapBuilderType,
                    Long.toString(1337L),
                    Long.toString(19L),
                    CsvMapUtil.relationshipCountsToString(relationshipTypeCounts),
                    inverseIndexedRelTypes.stream().map(ElementIdentifier::name).collect(Collectors.joining(";"))
                )
            )
        );
    }

    @Override
    protected List<String> defaultHeaderColumns() {
        return List.of(
            CsvGraphInfoVisitor.DATABASE_NAME_COLUMN_NAME,
            CsvGraphInfoVisitor.ID_MAP_BUILDER_TYPE_COLUMN_NAME,
            CsvGraphInfoVisitor.NODE_COUNT_COLUMN_NAME,
            CsvGraphInfoVisitor.MAX_ORIGINAL_ID_COLUMN_NAME,
            CsvGraphInfoVisitor.REL_TYPE_COUNTS_COLUMN_NAME,
            CsvGraphInfoVisitor.INVERSE_INDEXED_REL_TYPES
        );
    }
}
