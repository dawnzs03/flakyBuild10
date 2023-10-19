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

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.extension.TestGraph;
import org.neo4j.gds.ml.core.ComputationContext;
import org.neo4j.gds.ml.core.FiniteDifferenceTest;
import org.neo4j.gds.ml.core.functions.Constant;
import org.neo4j.gds.ml.core.functions.Weights;
import org.neo4j.gds.ml.core.tensor.Matrix;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.neo4j.gds.ml.core.RelationshipWeights.UNWEIGHTED;

class GraphSageLossTest implements FiniteDifferenceTest {

    private static final Offset<Double> LOSS_TOLERANCE = Offset.offset(1e-10);
    protected ComputationContext ctx;

    @BeforeEach
    protected void setup() {
        ctx = new ComputationContext();
    }

    @ParameterizedTest
    @CsvSource({
        "1, 0.5440720306533048",
        "4, 1.9948392942657789",
        "8, 3.9291956457490773",
        "12, 5.863551997232376",
        "20, 9.732264700198973",
        "50, 24.239937336323717"
    })
    void shouldComputeLossBatchSizeOne(int negativeSamplingFactor, double expectedLoss) {
        var combinedEmbeddings = Constant.matrix(
            new double[]{
                1.5, -1, 0.75,  // nodeId
                1, -0.75, 0.7,  // positive nodeId
                -0.1, 0.4, 0.1  // negative nodeId
            },
            3, 3
        );

        var lossVar = new GraphSageLoss(UNWEIGHTED, combinedEmbeddings, new long[]{0, 1, 2}, negativeSamplingFactor);

        var lossData = ctx.forward(lossVar);

        assertThat(lossData.value()).isEqualTo(expectedLoss, LOSS_TOLERANCE);
    }

    @ParameterizedTest
    @CsvSource({
        "1,   0.9105670167",
        "4,   2.42379747276",
        "8,   4.44143808081",
        "12,  6.45907868887",
        "20, 10.49435990499",
        "50, 25.62666446543"
    })
    void shouldComputeLoss(int negativeSamplingFactor, double expectedLoss) {
        var combinedEmbeddings = Constant.matrix(
            new double[]{
                1.5, -1, 0.75,      // nodeId
                0.5, -0.1, 0.7,     // nodeId
                0.23, 0.001, 0.3,   // nodeId
                1, -0.75, 0.7,      // positive nodeId
                0.1, -0.125, 0.45,  // positive nodeId
                0.2, 0.01, 0.24,    // positive nodeId
                -0.1, 0.4, 0.1,     // negative nodeId
                -0.9, 0.7, -0.3,    // negative nodeId
                -0.25, 0.4, -0.2    // negative nodeId
            },
            9, 3
        );

        long[] batchIds = {0, 1, 2, 3, 4, 5, 6, 7, 8};
        var lossVar = new GraphSageLoss(UNWEIGHTED, combinedEmbeddings, batchIds, negativeSamplingFactor);

        assertThat(ctx.forward(lossVar).value()).isEqualTo(expectedLoss, LOSS_TOLERANCE);
    }

    @Test
    void testGradient() {
        var combinedEmbeddings = new Weights<>(new Matrix(
            new double[]{
                1.5, -1, 0.75,  // nodeId
                1, -0.75, 0.7,  // positive nodeId
                -0.1, 0.4, 0.1  // negative nodeId
            },
            3, 3
        ));

        finiteDifferenceShouldApproximateGradient(
            combinedEmbeddings,
            new GraphSageLoss(UNWEIGHTED, combinedEmbeddings, new long[]{0, 1, 2}, 5)
        );
    }

    @Override
    public double epsilon() {
        return 1e-8;
    }

    @Nested
    @GdlExtension
    class Weighted {

        @GdlGraph(orientation = Orientation.UNDIRECTED)
        private static final String DB_CYPHER =
            "CREATE" +
            "  (u1:User { id: 0 })" +
            ", (u2:User { id: 1 })" +
            ", (d1:Dish { id: 2 })" +
            ", (d2:Dish { id: 3 })" +
            ", (d3:Dish { id: 4 })" +
            ", (d4:Dish { id: 5 })" +
            ", (u1)-[:ORDERED {times: 5}]->(d1)" +
            ", (u1)-[:ORDERED {times: 2}]->(d2)" +
            ", (u1)-[:ORDERED {times: 1}]->(d3)" +
            ", (u2)-[:ORDERED {times: 3}]->(d3)" +
            ", (u2)-[:ORDERED {times: 3}]->(d4)";

        @Inject
        private GraphStore graphStore;

        @Inject
        private TestGraph graph;

        @ParameterizedTest
        @CsvSource({
            "1, 0.5440720306533048",
            "4, 1.9948392942657789",
            "8, 3.9291956457490773",
            "12, 5.863551997232376",
            "20, 9.732264700198973",
            "50, 24.239937336323717"
        })
        void shouldComputeLossBatchSizeOne(int negativeSamplingFactor, double expectedLoss) {
            var combinedEmbeddings = Constant.matrix(
                new double[]{
                    1.5, -1, 0.75,  // nodeId
                    1, -0.75, 0.7,  // positive nodeId
                    -0.1, 0.4, 0.1  // negative nodeId
                },
                3, 3
            );

            // node, neighbor, negative node
            long[] batchIds = {graph.toMappedNodeId("u1"), graph.toMappedNodeId("u2"), graph.toMappedNodeId("d1")};
            var lossVar = new GraphSageLoss(
                graph::relationshipProperty,
                combinedEmbeddings,
                batchIds,
                negativeSamplingFactor
            );

            assertThat(ctx.forward(lossVar).value()).isEqualTo(expectedLoss, LOSS_TOLERANCE);
        }

        @ParameterizedTest
        @CsvSource({
            "1,   0.5440720306",
            "4,   1.9948392942",
            "8,   3.9291956457",
            "12,  5.8635519972",
            "20,  9.7322647001",
            "50, 24.2399373363"
        })
        void shouldComputeOnFilteredGraph(int negativeSamplingFactor, double expectedLoss) {
            var combinedEmbeddings = new Weights<>(new Matrix(
                new double[]{
                    1.5, -1, 0.75,  // nodeId
                    1.5, -1, 0.75,  // nodeId
                    1, -0.75, 0.7,  // positive nodeId
                    1, -0.75, 0.7,  // positive nodeId
                    -0.1, 0.4, 0.1,  // negative nodeId
                    -0.1, 0.4, 0.1  // negative nodeId
                },
                6, 3
            ));

            var filteredGraph = graphStore.getGraph(
                Set.of(NodeLabel.of("User")),
                graphStore.relationshipTypes(),
                Optional.of("times")
            );

            var user1 = graph.toMappedNodeId("u1");
            var user2 = graph.toMappedNodeId("u2");

            // node1, node2, neighbor1, neighbor2, negative nodes
            long[] batchIds = {user1, user2, user1, user1, user2, user1};
            var lossVar = new GraphSageLoss(
                filteredGraph::relationshipProperty,
                combinedEmbeddings,
                batchIds,
                negativeSamplingFactor
            );

            var lossData = ctx.forward(lossVar);

            assertThat(lossData.value()).isEqualTo(expectedLoss, LOSS_TOLERANCE);

            finiteDifferenceShouldApproximateGradient(
                combinedEmbeddings,
                lossVar
            );
        }

        @Test
        void testGradient() {
            var combinedEmbeddings = new Weights<>(new Matrix(
                new double[]{
                    1.5, -1, 0.75,  // nodeId
                    1, -0.75, 0.7,  // positive nodeId
                    -0.1, 0.4, 0.1  // negative nodeId
                },
                3, 3
            ));

            // node, neighbor, negative node
            long[] batchIds = {graph.toMappedNodeId("u1"), graph.toMappedNodeId("d2"), graph.toMappedNodeId("d4")};
            finiteDifferenceShouldApproximateGradient(
                combinedEmbeddings,
                new GraphSageLoss(graph::relationshipProperty, combinedEmbeddings, batchIds, 5)
            );

        }
    }
}
