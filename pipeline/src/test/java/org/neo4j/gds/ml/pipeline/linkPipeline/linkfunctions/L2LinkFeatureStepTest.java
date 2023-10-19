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
package org.neo4j.gds.ml.pipeline.linkPipeline.linkfunctions;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.utils.TerminationFlag;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.ml.pipeline.linkPipeline.LinkFeatureExtractor;
import org.neo4j.gds.ml.pipeline.linkPipeline.LinkFeatureStepFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@GdlExtension
final class L2LinkFeatureStepTest {

    @GdlGraph(orientation = Orientation.UNDIRECTED)
    static String GRAPH = "(a:N {noise: 42, z: 13, array: [3.0,2.0], zeros: [.0, .0], invalidValue: NaN}), " +
                          "(b:N {noise: 1337, z: 0, array: [1.0,1.0], zeros: [.0, .0], invalidValue: 1.0}), " +
                          "(c:N {noise: 42, z: 2, array: [8.0,2.3], zeros: [.0, .0], invalidValue: 3.0}), " +
                          "(d:N {noise: 42, z: 9, array: [0.1,91.0], zeros: [.0, .0], invalidValue: 4.0}), " +

                          "(a)-[:REL]->(b), " +
                          "(a)-[:REL]->(c), " +
                          "(a)-[:REL]->(d)";

    @Inject
    Graph graph;

    @Test
    void runL2LinkFeatureStep() {
        var step = LinkFeatureStepFactory.create(
            "L2",
            LinkFeatureStepConfigurationImpl.builder().nodeProperties(List.of("noise", "z", "array")).build()
        );

        var linkFeatures = LinkFeatureExtractor.extractFeatures(
            graph,
            List.of(step),
            4,
            ProgressTracker.NULL_TRACKER,
            TerminationFlag.RUNNING_TRUE
        );

        var delta = 0.0001D;

        assertArrayEquals(new double[]{Math.pow(42 - 1337, 2), Math.pow(13 - 0D, 2), Math.pow(3 - 1D, 2), Math.pow(2 - 1D, 2)}, linkFeatures.get(0), delta);
        assertArrayEquals(new double[]{Math.pow(42 - 42, 2), Math.pow(13 - 2, 2), Math.pow(3 - 8, 2), Math.pow(2 - 2.3D, 2)}, linkFeatures.get(1), delta);
        assertArrayEquals(new double[]{Math.pow(42 - 42, 2), Math.pow(13 - 9, 2), Math.pow(3 - 0.1D, 2), Math.pow(2 - 91.0D, 2)}, linkFeatures.get(2), delta);
    }

    @Test
    void handlesZeroVectors() {
        var step = new L2FeatureStep(List.of("zeros"));

        var linkFeatures = LinkFeatureExtractor.extractFeatures(
            graph,
            List.of(step),
            4,
            ProgressTracker.NULL_TRACKER,
            TerminationFlag.RUNNING_TRUE
        );

        for (long i = 0; i < linkFeatures.size(); i++) {
            assertThat(linkFeatures.get(i)).containsOnly(0.0);
        }
    }

    @Test
    void failsOnNaNValues() {
        var step = new L2FeatureStep(List.of("invalidValue", "z"));

        assertThatThrownBy(() -> LinkFeatureExtractor.extractFeatures(
            graph,
            List.of(step),
            4,
            ProgressTracker.NULL_TRACKER,
            TerminationFlag.RUNNING_TRUE
        ))
            .hasMessage("Encountered NaN when combining the nodeProperties ['invalidValue', 'z'] for the node pair (0, 1) when computing the L2 feature vector. " +
                        "Either define a default value if its a stored property or check the nodePropertyStep.");
    }
}
