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
package org.neo4j.gds.embeddings.fastrp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.GdsCypher;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.api.DefaultValue;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.gds.extension.Neo4jGraph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.FLOAT_ARRAY;
import static org.neo4j.gds.TestSupport.crossArguments;
import static org.neo4j.gds.ml.core.tensor.operations.FloatVectorOperations.anyMatch;
import static org.neo4j.gds.ml.core.tensor.operations.FloatVectorOperations.scale;

class FastRPWriteProcTest extends BaseProcTest {

    @Neo4jGraph
    private static final String DB_CYPHER =
        "CREATE" +
        "  (a:Node {name: 'a', f1: 0.4, f2: 1.3})" +
        ", (b:Node {name: 'b', f1: 2.1, f2: 0.5})" +
        ", (e:Node2 {name: 'e'})" +
        ", (c:Isolated {name: 'c'})" +
        ", (d:Isolated {name: 'd'})" +
        ", (a)-[:REL]->(b)" +

        ", (a)<-[:REL2 {weight: 2.0}]-(b)" +
        ", (a)<-[:REL2 {weight: 1.0}]-(e)";

    private static final String FAST_RP_GRAPH = "myGraph";

    @BeforeEach
    void setUp() throws Exception {
        registerProcedures(
            FastRPWriteProc.class,
            GraphProjectProc.class
        );

        runQuery(GdsCypher.call(FAST_RP_GRAPH)
            .graphProject()
            .withNodeLabel("Node")
            .withRelationshipType("REL", Orientation.UNDIRECTED)
            .withNodeProperties(List.of("f1","f2"), DefaultValue.of(0.0f))
            .yields());
    }

    @ParameterizedTest
    @MethodSource("weights")
    void shouldComputeNonZeroEmbeddings(Collection<Float> weights, double propertyRatio) {
        List<String> featureProperties = List.of("f1", "f2");
        int embeddingDimension = 128;
        GdsCypher.ParametersBuildStage queryBuilder = GdsCypher.call(FAST_RP_GRAPH)
            .algo("fastRP")
            .writeMode()
            .addParameter("embeddingDimension", embeddingDimension)
            .addParameter("propertyRatio", propertyRatio)
            .addParameter("featureProperties", featureProperties)
            .addParameter("writeProperty", "embedding");

        if (!weights.isEmpty()) {
            queryBuilder.addParameter("iterationWeights", weights);
        }
        String writeQuery = queryBuilder.yields();

        runQuery(writeQuery);

        runQueryWithRowConsumer("MATCH (n:Node) RETURN n.embedding as embedding", row -> {
            assertThat(row.get("embedding"))
                .asInstanceOf(FLOAT_ARRAY)
                .hasSize(embeddingDimension)
                .matches(vector -> anyMatch(vector, v -> v != 0.0));
        });
    }

    @Test
    void shouldComputeAndWriteWithWeight() {
        var createQuery = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .graphProject()
            .withNodeLabel("Node")
            .withNodeLabel("Node2")
            .withNodeProperties(List.of("f1", "f2"), DefaultValue.of(0.0f))
            .withRelationshipType("REL2")
            .withRelationshipProperty("weight")
            .yields();
        runQuery(createQuery);

        List<String> featureProperties = List.of("f1", "f2");
        var propertyRatio = 0.5;
        int embeddingDimension = 128;

        String query = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .algo("fastRP")
            .writeMode()
            .addParameter("embeddingDimension", embeddingDimension)
            .addParameter("propertyRatio", propertyRatio)
            .addParameter("featureProperties", featureProperties)
            .addParameter("relationshipWeightProperty", "weight")
            .addParameter("writeProperty", "embedding")
            .yields();

        runQuery(query);

        String retrieveQuery = "MATCH (n) WHERE n:Node OR n:Node2 RETURN n.name as name, n.embedding as embedding";
        Map<String, float[]> embeddings = new HashMap<>(3);
        runQueryWithRowConsumer(retrieveQuery, row -> {
            embeddings.put(row.getString("name"), (float[]) row.get("embedding"));
        });

        float[] embeddingOfE = embeddings.get("e");
        scale(embeddingOfE, 2);
        assertThat(embeddings.get("b")).containsExactly(embeddingOfE);
    }

    private static Stream<Arguments> weights() {
        return crossArguments(
            () -> Stream.of(
                Arguments.of(Collections.emptyList()),
                Arguments.of(List.of(1.0f, 1.0f, 2.0f, 4.0f))
            ),
            () -> Stream.of(
                Arguments.of(0f),
                Arguments.of(0.5f),
                Arguments.of(1f)
            )
        );
    }
}
