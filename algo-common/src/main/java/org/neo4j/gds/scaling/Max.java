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

public final class Max extends ScalarScaler {

    public static final String TYPE = "max";
    final double maxAbs;

    private Max(NodePropertyValues properties, Map<String, List<Double>> statistics, double maxAbs) {
        super(properties, statistics);
        this.maxAbs = maxAbs;
    }

    @Override
    public double scaleProperty(long nodeId) {
        return properties.doubleValue(nodeId) / maxAbs;
    }

    public static ScalerFactory buildFrom(CypherMapWrapper mapWrapper) {
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
                    partition -> new ComputeAbsMax(partition, properties, progressTracker),
                    Optional.empty()
                );
                RunWithConcurrency.builder()
                    .concurrency(concurrency)
                    .tasks(tasks)
                    .executor(executor)
                    .run();

                var absMax = tasks.stream().mapToDouble(ComputeAbsMax::absMax).max().orElse(0);

                var statistics = Map.of("absMax", List.of(absMax));

                if (absMax < CLOSE_TO_ZERO) {
                    return new StatsOnly(statistics);
                } else {
                    return new Max(properties, statistics, absMax);
                }
            }
        };
    }

    static class ComputeAbsMax extends AggregatesComputer {

        private double absMax;

        ComputeAbsMax(Partition partition, NodePropertyValues property, ProgressTracker progressTracker) {
            super(partition, property, progressTracker);
            this.absMax = 0;
        }

        @Override
        void compute(double propertyValue) {
            var absoluteValue = Math.abs(propertyValue);
            if (absoluteValue > absMax) {
                absMax = absoluteValue;
            }
        }

        double absMax() {
            return absMax;
        }
    }

}
