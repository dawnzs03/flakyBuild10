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
package org.neo4j.gds.ml.pipeline.node.classification;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.GdsCypher;
import org.neo4j.gds.api.DefaultValue;
import org.neo4j.gds.api.IdMap;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.gds.catalog.GraphStreamNodePropertiesProc;
import org.neo4j.gds.core.loading.GraphStoreCatalog;
import org.neo4j.gds.extension.IdFunction;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.extension.Neo4jGraph;
import org.neo4j.gds.extension.Neo4jModelCatalogExtension;
import org.neo4j.gds.ml.pipeline.PipelineCatalog;
import org.neo4j.gds.ml.pipeline.node.classification.predict.NodeClassificationPipelineMutateProc;
import org.neo4j.gds.ml.pipeline.node.classification.predict.NodeClassificationPipelineStreamProc;
import org.neo4j.gds.ml.pipeline.node.classification.predict.NodeClassificationPipelineTrainProc;

import java.util.List;
import java.util.Map;

@Neo4jModelCatalogExtension
public class NodeClassificationPipelineFilteredTest extends BaseProcTest {

    private static final String GRAPH_NAME = "g";

    @Neo4jGraph
    private static final String DB_QUERY =
        "CREATE " +
        "  (x1:X {class: 0})" +
        ", (x2:X {class: 5})" +
        ", (x3:X {class: 0})" +
        ", (x4:X {class: 5})" +
        ", (x5:X {class: 0})" +
        ", (x6:X {class: 5})" +
        ", (x7:X {class: 0})" +
        ", (x8:X {class: 5})" +
        ", (x9:X {class: 0})" +
        ", (x10:X {class: 5})" +

        ", (y1:Y {})" +
        ", (y2:Y {})" +

        ", (x2)-[:R]->(y1)" +
        ", (x4)-[:R]->(y1)" +
        ", (x6)-[:R]->(y1)" +
        ", (x8)-[:R]->(y1)" +
        ", (x10)-[:R]->(y1)" +
        ", (x2)-[:R]->(y2)" +
        ", (x4)-[:R]->(y2)" +
        ", (x6)-[:R]->(y2)" +
        ", (x8)-[:R]->(y2)" +
        ", (x10)-[:R]->(y2)";

    @Inject
    IdFunction idFunction;

    @BeforeEach
    void setup() throws Exception {
        registerProcedures(
            GraphProjectProc.class,
            GraphStreamNodePropertiesProc.class,
            NodeClassificationPipelineStreamProc.class,
            NodeClassificationPipelineMutateProc.class,
            NodeClassificationPipelineCreateProc.class,
            NodeClassificationPipelineAddStepProcs.class,
            NodeClassificationPipelineAddTrainerMethodProcs.class,
            NodeClassificationPipelineConfigureSplitProc.class,
            NodeClassificationPipelineTrainProc.class
        );

        String createQuery = GdsCypher.call(GRAPH_NAME)
            .graphProject()
            .withNodeLabels("X", "Y")
            .withRelationshipType("R")
            .withNodeProperties(List.of("class"), DefaultValue.DEFAULT)
            .yields();

        runQuery(createQuery);
    }

    @AfterEach
    void tearDown() {
        PipelineCatalog.removeAll();
    }

    @Test
    void trainAndPredictWithTargetAndContextNodeLabels() {
        runQuery("CALL gds.beta.pipeline.nodeClassification.create('p')");

        runQuery("CALL gds.beta.pipeline.nodeClassification.addNodeProperty('p', 'degree', {" +
                 "mutateProperty: 'degree', " +
                 "contextNodeLabels: ['Y']" +
                 "})");

        runQuery("CALL gds.beta.pipeline.nodeClassification.selectFeatures('p', 'degree')");

        runQuery("CALL gds.beta.pipeline.nodeClassification.configureSplit('p', {" +
                 "  testFraction: 0.2, " +
                 "  validationFolds: 2" +
                 "})");

        runQuery("CALL gds.beta.pipeline.nodeClassification.addLogisticRegression('p', {penalty: 0, maxEpochs: 100})");

        assertCypherResult("CALL gds.beta.pipeline.nodeClassification.train('g', {" +
                 " targetNodeLabels: ['X']," +
                 " pipeline: 'p'," +
                 " modelName: 'model'," +
                 " targetProperty: 'class'," +
                 " metrics: ['ACCURACY']," +
                 " randomSeed: 2" +
                 "}) " +
                 "YIELD modelInfo " +
                           "RETURN" +
                           "  modelInfo.metrics.ACCURACY.validation.avg AS avgValidScore," +
                           "  modelInfo.metrics.ACCURACY.train.avg AS avgTrainScore," +
                           "  modelInfo.metrics.ACCURACY.outerTrain AS outerTrainScore," +
                           "  modelInfo.metrics.ACCURACY.test AS testScore",
            List.of(Map.of("avgTrainScore", 1.0, "avgValidScore", 1.0, "outerTrainScore", 1.0, "testScore", 1.0))
        );
        //Score = 1 means the model is able to recover the perfect correlation between degree-class.
        //This implies the correct contextNodes have been used (for degree to be correct)

        IdMap nodes = GraphStoreCatalog.get(getUsername(), db.databaseName(), GRAPH_NAME).graphStore().nodes();
        IdFunction mappedId = name -> nodes.toMappedNodeId(idFunction.of(name));

        // this test identified a bug in the stream path.
        // we need to make sure that the graph consisting of the targetNodeLabels (taken from the train or predict config) is used to map the ids to the id space of the graph store
        assertCypherResult(
            "CALL gds.beta.pipeline.nodeClassification.predict.stream('g', {" +
            " modelName: 'model'," +
            " includePredictedProbabilities: true" +
            " })" +
            "  YIELD nodeId, predictedClass",
            List.of(
                Map.of("nodeId", idFunction.of("x1"), "predictedClass", 0L),
                Map.of("nodeId", idFunction.of("x2"), "predictedClass", 5L),
                Map.of("nodeId", idFunction.of("x3"), "predictedClass", 0L),
                Map.of("nodeId", idFunction.of("x4"), "predictedClass", 5L),
                Map.of("nodeId", idFunction.of("x5"), "predictedClass", 0L),
                Map.of("nodeId", idFunction.of("x6"), "predictedClass", 5L),
                Map.of("nodeId", idFunction.of("x7"), "predictedClass", 0L),
                Map.of("nodeId", idFunction.of("x8"), "predictedClass", 5L),
                Map.of("nodeId", idFunction.of("x9"), "predictedClass", 0L),
                Map.of("nodeId", idFunction.of("x10"), "predictedClass", 5L)
            )
        );
        // this test identified a bug in the mutation path.
        // generic code was doing computationResult.getGraph which gives incorrect graph during the updating of the graphStore
        runQuery(
            "CALL gds.beta.pipeline.nodeClassification.predict.mutate('g', {" +
            " modelName: 'model'," +
            " mutateProperty: 'predictedClass'" +
            " })");
        assertCypherResult(
            "CALL gds.graph.nodeProperties.stream(" +
            "   'g', " +
            "   ['predictedClass']" +
            ") YIELD nodeId, propertyValue AS predictedClass",
            List.of(
                Map.of("nodeId", idFunction.of("x1"), "predictedClass", 0L),
                Map.of("nodeId", idFunction.of("x2"), "predictedClass", 5L),
                Map.of("nodeId", idFunction.of("x3"), "predictedClass", 0L),
                Map.of("nodeId", idFunction.of("x4"), "predictedClass", 5L),
                Map.of("nodeId", idFunction.of("x5"), "predictedClass", 0L),
                Map.of("nodeId", idFunction.of("x6"), "predictedClass", 5L),
                Map.of("nodeId", idFunction.of("x7"), "predictedClass", 0L),
                Map.of("nodeId", idFunction.of("x8"), "predictedClass", 5L),
                Map.of("nodeId", idFunction.of("x9"), "predictedClass", 0L),
                Map.of("nodeId", idFunction.of("x10"), "predictedClass", 5L)
            )
        );
    }
}
