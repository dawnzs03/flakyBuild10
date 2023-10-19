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
package org.neo4j.gds.doc;

import org.junit.jupiter.api.AfterAll;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.gds.catalog.GraphStreamRelationshipPropertiesProc;
import org.neo4j.gds.functions.AsNodeFunc;
import org.neo4j.gds.ml.linkmodels.pipeline.LinkPredictionPipelineAddStepProcs;
import org.neo4j.gds.ml.linkmodels.pipeline.LinkPredictionPipelineAddTrainerMethodProcs;
import org.neo4j.gds.ml.linkmodels.pipeline.LinkPredictionPipelineConfigureAutoTuningProc;
import org.neo4j.gds.ml.linkmodels.pipeline.LinkPredictionPipelineConfigureSplitProc;
import org.neo4j.gds.ml.linkmodels.pipeline.LinkPredictionPipelineCreateProc;
import org.neo4j.gds.ml.linkmodels.pipeline.predict.LinkPredictionPipelineMutateProc;
import org.neo4j.gds.ml.linkmodels.pipeline.predict.LinkPredictionPipelineStreamProc;
import org.neo4j.gds.ml.linkmodels.pipeline.train.LinkPredictionPipelineTrainProc;
import org.neo4j.gds.ml.pipeline.PipelineCatalog;

import java.util.List;

class LinkPredictionPipelineDocTest extends MultiFileDocTestBase {

    @AfterAll
    static void tearDown() {
        PipelineCatalog.removeAll();
    }

    @Override
    protected List<Class<?>> functions() {
        return List.of(AsNodeFunc.class);
    }

    @Override
    protected List<Class<?>> procedures() {
        return List.of(
            LinkPredictionPipelineCreateProc.class,
            LinkPredictionPipelineMutateProc.class,
            LinkPredictionPipelineStreamProc.class,
            LinkPredictionPipelineTrainProc.class,
            LinkPredictionPipelineAddStepProcs.class,
            LinkPredictionPipelineConfigureSplitProc.class,
            LinkPredictionPipelineConfigureAutoTuningProc.class,
            LinkPredictionPipelineAddTrainerMethodProcs.class,
            GraphStreamRelationshipPropertiesProc.class,
            GraphProjectProc.class
        );
    }

    @Override
    protected List<String> adocPaths() {
        return List.of(
            "pages/machine-learning/linkprediction-pipelines/config.adoc",
            "pages/machine-learning/linkprediction-pipelines/training.adoc",
            "pages/machine-learning/linkprediction-pipelines/predict.adoc"
        );
    }
}
