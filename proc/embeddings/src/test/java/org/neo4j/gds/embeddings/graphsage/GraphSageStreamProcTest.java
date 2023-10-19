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
package org.neo4j.gds.embeddings.graphsage;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.GdsCypher;
import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.NodeProjection;
import org.neo4j.gds.NodeProjections;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.PropertyMapping;
import org.neo4j.gds.RelationshipProjection;
import org.neo4j.gds.RelationshipProjections;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.gds.config.GraphProjectFromStoreConfig;
import org.neo4j.gds.config.ImmutableGraphProjectFromStoreConfig;
import org.neo4j.gds.extension.Neo4jGraph;
import org.neo4j.gds.extension.Neo4jModelCatalogExtension;
import org.neo4j.gds.utils.StringJoining;
import org.neo4j.graphdb.QueryExecutionException;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.neo4j.gds.ElementProjection.PROJECT_ALL;

@Neo4jModelCatalogExtension
class GraphSageStreamProcTest extends BaseProcTest {

    @Neo4jGraph
    private static final String DB_CYPHER =
        "CREATE" +
        "  (a:King{ name: 'A', age: 20, birth_year: 200, death_year: 300 })" +
        ", (b:King{ name: 'B', age: 12, birth_year: 232, death_year: 300 })" +
        ", (c:King{ name: 'C', age: 67, birth_year: 212, death_year: 300 })" +
        ", (d:King{ name: 'D', age: 78, birth_year: 245, death_year: 300 })" +
        ", (e:King{ name: 'E', age: 32, birth_year: 256, death_year: 300 })" +
        ", (f:King{ name: 'F', age: 32, birth_year: 214, death_year: 300 })" +
        ", (g:King{ name: 'G', age: 35, birth_year: 214, death_year: 300 })" +
        ", (h:King{ name: 'H', age: 56, birth_year: 253, death_year: 300 })" +
        ", (i:King{ name: 'I', age: 62, birth_year: 267, death_year: 300 })" +
        ", (j:King{ name: 'J', age: 44, birth_year: 289, death_year: 300 })" +
        ", (k:King{ name: 'K', age: 89, birth_year: 211, death_year: 300 })" +
        ", (l:King{ name: 'L', age: 99, birth_year: 201, death_year: 300 })" +
        ", (m:King{ name: 'M', age: 99, birth_year: 201, death_year: 300 })" +
        ", (n:King{ name: 'N', age: 99, birth_year: 201, death_year: 300 })" +
        ", (o:King{ name: 'O', age: 99, birth_year: 201, death_year: 300 })" +
        ", (a)-[:REL {weight: 1.0}]->(b)" +
        ", (a)-[:REL {weight: 5.0}]->(c)" +
        ", (b)-[:REL {weight: 42.0}]->(c)" +
        ", (b)-[:REL {weight: 10.0}]->(d)" +
        ", (c)-[:REL {weight: 62.0}]->(e)" +
        ", (d)-[:REL {weight: 1.0}]->(e)" +
        ", (d)-[:REL {weight: 1.0}]->(f)" +
        ", (e)-[:REL {weight: 1.0}]->(f)" +
        ", (e)-[:REL {weight: 4.0}]->(g)" +
        ", (h)-[:REL {weight: 1.0}]->(i)" +
        ", (i)-[:REL {weight: -1.0}]->(j)" +
        ", (j)-[:REL {weight: 1.0}]->(k)" +
        ", (j)-[:REL {weight: -10.0}]->(l)" +
        ", (k)-[:REL {weight: 1.0}]->(l)";

    static String graphName = "embeddingsGraph";

    static String modelName = "graphSageModel";

    @BeforeEach
    void setup() throws Exception {
        registerProcedures(
            GraphProjectProc.class,
            GraphSageStreamProc.class,
            GraphSageTrainProc.class
        );

        String query = GdsCypher.call(graphName)
            .graphProject()
            .withNodeLabel("King")
            .withNodeProperty(PropertyMapping.of("age", 1.0))
            .withNodeProperty(PropertyMapping.of("birth_year", 1.0))
            .withNodeProperty(PropertyMapping.of("death_year", 1.0))
            .withRelationshipType(
                "R",
                RelationshipProjection.of(
                    "*",
                    Orientation.UNDIRECTED
                )
            )
            .withRelationshipProperty("weight")
            .yields();

        runQuery(query);
    }

    @Test
    void testStreaming() {
        String trainQuery = GdsCypher.call(graphName)
            .algo("gds.beta.graphSage")
            .trainMode()
            .addParameter("sampleSizes", List.of(2, 4))
            .addParameter("featureProperties", List.of("age", "birth_year", "death_year"))
            .addParameter("embeddingDimension", 16)
            .addParameter("activationFunction", "sigmoid")
            .addParameter("aggregator", "mean")
            .addParameter("modelName", modelName)
            .yields();

        runQuery(trainQuery);
        String query = GdsCypher.call("embeddingsGraph")
            .algo("gds.beta.graphSage")
            .streamMode()
            .addParameter("concurrency", 1)
            .addParameter("modelName", modelName)
            .yields();

        runQueryWithRowConsumer(query, Map.of("embeddingDimension", 16), row -> {
            assertThat(row.getNumber("nodeId"))
                .isNotNull();

            assertThat(row.get("embedding"))
                .asList()
                .hasSize(16);
        });
    }

    @Test
    void weightedGraphSage() {
        var trainQuery = GdsCypher.call(graphName)
            .algo("gds.beta.graphSage")
            .trainMode()
            .addParameter("sampleSizes", List.of(1))
            .addParameter("maxIterations", 1)
            .addParameter("featureProperties", List.of("age"))
            .addParameter("relationshipWeightProperty", "weight")
            .addParameter("embeddingDimension", 1)
            .addParameter("activationFunction", "RELU")
            .addParameter("aggregator", "MEAN")
            .addParameter("randomSeed", 42L)
            .addParameter("modelName", modelName)
            .yields();

        runQuery(trainQuery);

        String streamQuery = GdsCypher.call(graphName)
            .algo("gds.beta.graphSage")
            .streamMode()
            .addParameter("concurrency", 1)
            .addParameter("modelName", modelName)
            .yields();

        assertCypherResult(streamQuery, List.of(
            Map.of("nodeId", idFunction.of("a"), "embedding", Matchers.iterableWithSize(1)),
            Map.of("nodeId", idFunction.of("b"), "embedding", Matchers.iterableWithSize(1)),
            Map.of("nodeId", idFunction.of("c"), "embedding", Matchers.iterableWithSize(1)),
            Map.of("nodeId", idFunction.of("d"), "embedding", Matchers.iterableWithSize(1)),
            Map.of("nodeId", idFunction.of("e"), "embedding", Matchers.iterableWithSize(1)),
            Map.of("nodeId", idFunction.of("f"), "embedding", Matchers.iterableWithSize(1)),
            Map.of("nodeId", idFunction.of("g"), "embedding", Matchers.iterableWithSize(1)),
            Map.of("nodeId", idFunction.of("h"), "embedding", Matchers.iterableWithSize(1)),
            Map.of("nodeId", idFunction.of("i"), "embedding", Matchers.iterableWithSize(1)),
            Map.of("nodeId", idFunction.of("j"), "embedding", Matchers.iterableWithSize(1)),
            Map.of("nodeId", idFunction.of("k"), "embedding", Matchers.iterableWithSize(1)),
            Map.of("nodeId", idFunction.of("l"), "embedding", Matchers.iterableWithSize(1)),
            Map.of("nodeId", idFunction.of("m"), "embedding", Matchers.iterableWithSize(1)),
            Map.of("nodeId", idFunction.of("n"), "embedding", Matchers.iterableWithSize(1)),
            Map.of("nodeId", idFunction.of("o"), "embedding", Matchers.iterableWithSize(1))
        ));
    }

    @ParameterizedTest(name = "Graph Properties: {2} - Algo Properties: {1}")
    @MethodSource("missingNodeProperties")
    void shouldFailOnMissingNodeProperties(
        GraphProjectFromStoreConfig config,
        List<String> nodeProperties,
        List<String> graphProperties,
        List<String> label
    ) {
        String trainQuery = GdsCypher.call(graphName)
            .algo("gds.beta.graphSage")
            .trainMode()
            .addParameter("sampleSizes", List.of(2, 4))
            .addParameter("featureProperties", List.of("age", "birth_year", "death_year"))
            .addParameter("embeddingDimension", 16)
            .addParameter("activationFunction", ActivationFunction.SIGMOID)
            .addParameter("aggregator", "mean")
            .addParameter("modelName", modelName)
            .yields();
        runQuery(trainQuery);

        runQuery(GdsCypher.call(DEFAULT_GRAPH_NAME)
            .graphProject()
            .withGraphProjectConfig(config)
            .yields()
        );

        String query = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .algo("gds.beta.graphSage")
            .streamMode()
            .addParameter("concurrency", 1)
            .addParameter("modelName", modelName)
            .yields();

        assertThatThrownBy(() -> runQuery(query))
            .isInstanceOf(QueryExecutionException.class)
            .hasRootCauseInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("The feature properties %s are not present for all requested labels.", StringJoining.join(nodeProperties))
            .hasMessageContaining("Requested labels: %s", StringJoining.join(label))
            .hasMessageContaining("Properties available on all requested labels: %s", StringJoining.join(graphProperties));
    }

    static Stream<Arguments> missingNodeProperties() {
        return Stream.of(
            Arguments.of(
                ImmutableGraphProjectFromStoreConfig.builder()
                    .graphName("implicitWeightedGraph")
                    .nodeProjections(NodeProjections.single(
                        NodeLabel.of("King"),
                        NodeProjection.builder()
                            .label("King")
                            .addProperty(
                                PropertyMapping.of("age")
                            ).build()
                    ))
                    .relationshipProjections(RelationshipProjections.fromString("REL")
                    ).build(),
                List.of("birth_year", "death_year"),
                List.of("age"),
                List.of("King")
            ),
            Arguments.of(
                ImmutableGraphProjectFromStoreConfig.builder()
                    .graphName("implicitWeightedGraph")
                    .nodeProjections(NodeProjections.single(
                        NodeLabel.of("King"),
                        NodeProjection.builder()
                            .label("King")
                            .addProperties(
                                PropertyMapping.of("age"),
                                PropertyMapping.of("birth_year")
                            ).build()
                    ))
                    .relationshipProjections(RelationshipProjections.fromString("REL")
                    ).build(),
                List.of("death_year"),
                List.of("age", "birth_year"),
                List.of("King")
            ),
            Arguments.of(
                ImmutableGraphProjectFromStoreConfig.of(
                    "",
                    "",
                    NodeProjections.fromString(PROJECT_ALL),
                    RelationshipProjections.fromString(PROJECT_ALL)
                ),
                List.of("age", "birth_year", "death_year"),
                List.of(),
                List.of("__ALL__")
            )
        );
    }
}
