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
package org.neo4j.gds.ml.linkmodels.pipeline;

import org.neo4j.gds.ml.pipeline.ExecutableNodePropertyStep;
import org.neo4j.gds.ml.pipeline.TrainingPipeline;
import org.neo4j.gds.ml.pipeline.linkPipeline.LinkFeatureStep;
import org.neo4j.gds.ml.pipeline.linkPipeline.LinkPredictionTrainingPipeline;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PipelineInfoResult {
    public final String name;
    public final List<Map<String, Object>> nodePropertySteps;
    public final List<Map<String, Object>> featureSteps;
    public final Map<String, Object> splitConfig;
    public final Map<String, Object> autoTuningConfig;
    public final Object parameterSpace;

    PipelineInfoResult(String pipelineName, LinkPredictionTrainingPipeline pipeline) {
        this.name = pipelineName;
        this.nodePropertySteps = pipeline
            .nodePropertySteps()
            .stream()
            .map(ExecutableNodePropertyStep::toMap)
            .collect(Collectors.toList());
        this.featureSteps = pipeline.featureSteps().stream().map(LinkFeatureStep::toMap).collect(Collectors.toList());
        this.splitConfig = pipeline.splitConfig().toMap();
        this.autoTuningConfig = pipeline.autoTuningConfig().toMap();
        this.parameterSpace = TrainingPipeline.toMapParameterSpace(pipeline.trainingParameterSpace());
    }
}
