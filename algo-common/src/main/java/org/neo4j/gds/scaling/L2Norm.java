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
package org.neo4j.gds.scaling;

import org.neo4j.gds.api.properties.nodes.NodePropertyValues;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.core.concurrency.RunWithConcurrency;
import org.neo4j.gds.core.utils.partition.Partition;
import org.neo4j.gds.core.utils.partition.PartitionUtils;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * @deprecated Does not scale very well when node count is large. Maintained for legacy `alpha.scaleProperties` procs.
 */
@Deprecated(forRemoval = true, since = "2.4.0")
public final class L2Norm extends ScalarScaler {

    public static final String TYPE = "l2norm";
    final double euclideanLength;

    private L2Norm(NodePropertyValues properties, double euclideanLength) {
        super(properties, Map.of());
        this.euclideanLength = euclideanLength;
    }

    @Override
    public double scaleProperty(long nodeId) {
        return properties.doubleValue(nodeId) / euclideanLength;
    }

    static ScalerFactory buildFrom(CypherMapWrapper mapWrapper) {
        mapWrapper.requireOnlyKeysFrom(List.of());
        return new ScalerFactory() {
            @Override
            public String type() {
                return TYPE;
            }

            @Override
            public ScalarScaler create(
                NodePropertyValues properties,
                long nodeCount,
                int concurrency,
                ProgressTracker progressTracker,
                ExecutorService executor
            ) {
                var tasks = PartitionUtils.rangePartition(
                    concurrency,
                    nodeCount,
                    partition -> new ComputeSquaredSum(partition, properties, progressTracker),
                    Optional.empty()
                );
                RunWithConcurrency.builder()
                    .concurrency(concurrency)
                    .tasks(tasks)
                    .executor(executor)
                    .run();

                var squaredSum = tasks.stream().mapToDouble(ComputeSquaredSum::squaredSum).sum();
                var euclideanLength = Math.sqrt(squaredSum);

                if (euclideanLength < CLOSE_TO_ZERO) {
                    return ZERO;
                } else {
                    return new L2Norm(properties, euclideanLength);
                }
            }
        };
    }

    static class ComputeSquaredSum extends AggregatesComputer {

        private double squaredSum;

        ComputeSquaredSum(Partition partition, NodePropertyValues property, ProgressTracker progressTracker) {
            super(partition, property, progressTracker);
            this.squaredSum = 0D;
        }

        @Override
        void compute(double propertyValue) {
            squaredSum += propertyValue * propertyValue;
        }

        double squaredSum() {
            return squaredSum;
        }
    }

}
