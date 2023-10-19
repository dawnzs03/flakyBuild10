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
package org.neo4j.gds.ml.pipeline.nodePipeline.regression;

import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.ml.api.TrainingMethod;
import org.neo4j.gds.ml.pipeline.nodePipeline.NodePropertyTrainingPipeline;

public class NodeRegressionTrainingPipeline extends NodePropertyTrainingPipeline {

    public static final String PIPELINE_TYPE = "Node regression training pipeline";
    public static final String MODEL_TYPE = "NodeRegression";

    public NodeRegressionTrainingPipeline() {
        super(TrainingType.REGRESSION);
    }

    @Override
    public String type() {
        return PIPELINE_TYPE;
    }

    @Override
    public void specificValidateBeforeExecution(GraphStore graphStore) {

    }

    @Override
    public boolean requireEagerFeatures() {
        return !trainingParameterSpace.get(TrainingMethod.RandomForestRegression).isEmpty();
    }
}
