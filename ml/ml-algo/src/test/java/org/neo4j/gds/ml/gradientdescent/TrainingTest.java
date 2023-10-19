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

import org.junit.jupiter.api.Test;
import org.neo4j.gds.annotation.ValueClass;
import org.neo4j.gds.core.utils.TerminationFlag;
import org.neo4j.gds.core.utils.progress.tasks.LogLevel;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.ml.core.Variable;
import org.neo4j.gds.ml.core.batch.Batch;
import org.neo4j.gds.ml.core.batch.BatchQueue;
import org.neo4j.gds.ml.core.functions.Constant;
import org.neo4j.gds.ml.core.functions.ElementSum;
import org.neo4j.gds.ml.core.functions.Sigmoid;
import org.neo4j.gds.ml.core.functions.Weights;
import org.neo4j.gds.ml.core.tensor.Scalar;
import org.neo4j.gds.ml.core.tensor.Tensor;
import org.neo4j.gds.ml.core.tensor.Vector;
import org.neo4j.graphdb.TransactionTerminatedException;

import java.util.List;
import java.util.PrimitiveIterator;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainingTest {

    @Test
    void parallel() {
        var config = ImmutableTestTrainingConfig
            .builder()
            .patience(10)
            .maxEpochs(10)
            .build();

        var training = new Training(config, ProgressTracker.NULL_TRACKER, LogLevel.INFO, 100L, TerminationFlag.RUNNING_TRUE);
        var singleThreadedTraining = new Training(config, ProgressTracker.NULL_TRACKER, LogLevel.INFO, 100L, TerminationFlag.RUNNING_TRUE);

        var objective = new TestTrainingObjective();
        var singleThreadedObjective = new TestTrainingObjective();

        Supplier<BatchQueue> queueSupplier = () -> BatchQueue.consecutive(100, 10);

        training.train(objective, queueSupplier, 4);
        singleThreadedTraining.train(singleThreadedObjective, queueSupplier, 1);

        assertThat(objective.weights()).usingRecursiveComparison().isEqualTo(singleThreadedObjective.weights());
    }

    @Test
    void checksTerminationFlag() {
        TerminationFlag terminationFlag = () -> false;

        var config = ImmutableTestTrainingConfig
            .builder()
            .patience(10)
            .maxEpochs(10)
            .build();

        var training = new Training(config, ProgressTracker.NULL_TRACKER, LogLevel.INFO, 100L, terminationFlag);

        var objective = new TestTrainingObjective();

        assertThatThrownBy(() -> training.train(objective, () -> BatchQueue.consecutive(100, 10), 4))
            .isInstanceOf(TransactionTerminatedException.class)
            .hasMessageContaining("The transaction has been terminated.");
    }

    public static class TestTrainingObjective implements Objective<Long> {

        private final Weights<Vector> weights;

        private final Scalar expectedValue;

        TestTrainingObjective() {
            this.weights = new Weights<>(Vector.create(0d, 5));
            this.expectedValue = new Scalar(-1D);
        }

        @Override
        public List<Weights<? extends Tensor<?>>> weights() {
            return List.of(weights);
        }

        @Override
        public Variable<Scalar> loss(Batch batch, long trainSize) {
            var nodeIdsInBatch = new double[batch.size()];
            var batchIdx = 0;
            PrimitiveIterator.OfLong iterator = batch.elementIds();
            while (iterator.hasNext()) {
                nodeIdsInBatch[batchIdx++] = iterator.nextLong();
            }

            Vector nodeVector = new Vector(nodeIdsInBatch);

            return new Sigmoid<>(new ElementSum(
                List.of(
                    new Constant<>(weights.data().elementwiseProduct(nodeVector)),
                    new Constant<>(expectedValue),
                    weights
                )
            ));
        }

        @Override
        public Long modelData() {
            return Long.MIN_VALUE;
        }
    }

    @ValueClass
    @SuppressWarnings("immutables:subtype")
    public interface TestTrainingConfig extends GradientDescentConfig {
        static ImmutableTestTrainingConfig.Builder builder() {
            return ImmutableTestTrainingConfig.builder();
        }
    }
}
