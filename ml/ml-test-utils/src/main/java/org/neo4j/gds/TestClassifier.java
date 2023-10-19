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
package org.neo4j.gds;

import org.neo4j.gds.ml.core.batch.Batch;
import org.neo4j.gds.ml.core.tensor.Matrix;
import org.neo4j.gds.ml.models.Classifier;
import org.neo4j.gds.ml.models.Features;

public abstract class TestClassifier implements Classifier {
    public Matrix predictProbabilities(Batch batch, Features features) {
        var predictedProbabilities = new Matrix(batch.size(), numberOfClasses());
        var offset = 0;
        var batchIterator = batch.elementIds();

        while (batchIterator.hasNext()) {
            var id = batchIterator.nextLong();
            double[] predictionsForNode = predictProbabilities(features.get(id));
            predictedProbabilities.setRow(offset++, predictionsForNode);
        }
        return predictedProbabilities;
    }
}
