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
package org.neo4j.gds.ml.gradientdescent;

import org.neo4j.gds.ml.core.Variable;
import org.neo4j.gds.ml.core.batch.Batch;
import org.neo4j.gds.ml.core.functions.Constant;
import org.neo4j.gds.ml.core.functions.Weights;
import org.neo4j.gds.ml.core.tensor.Matrix;
import org.neo4j.gds.ml.core.tensor.Scalar;
import org.neo4j.gds.ml.core.tensor.Tensor;
import org.neo4j.gds.ml.models.Features;

import java.util.List;

/**
 * A training objective that computes a loss over a batch of nodes
 */
public interface Objective<DATA> {
    List<Weights<? extends Tensor<?>>> weights();
    Variable<Scalar> loss(Batch batch, long trainSize);

    /**
     * Returns the data, such as weights, needed to store or load the model
     * @return the data
     */
    DATA modelData();

    static Constant<Matrix> batchFeatureMatrix(Batch batch, Features features) {
        var batchFeatures = new Matrix(batch.size(), features.featureDimension());
        var batchFeaturesOffset = 0;
        var batchIterator = batch.elementIds();

        while (batchIterator.hasNext()) {
            var elementId = batchIterator.nextLong();
            batchFeatures.setRow(batchFeaturesOffset++, features.get(elementId));
        }

        return new Constant<>(batchFeatures);
    }
}
