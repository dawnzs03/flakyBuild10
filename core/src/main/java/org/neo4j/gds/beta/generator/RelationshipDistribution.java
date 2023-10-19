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
package org.neo4j.gds.beta.generator;

import org.neo4j.gds.utils.StringJoining;
import org.neo4j.gds.core.utils.statistics.DistributionHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.LongUnaryOperator;
import java.util.stream.Collectors;

import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;
import static org.neo4j.gds.utils.StringFormatting.toUpperCaseWithLocale;

public enum RelationshipDistribution {
    UNIFORM {
        @Override
        public LongUnaryOperator degreeProducer(long nodeCount, long averageDegree, Random random) {
            return (ignore) -> averageDegree;
        }

        @Override
        public LongUnaryOperator relationshipProducer(long nodeCount, long averageDegree, Random random) {
            return (ignore) -> DistributionHelper.uniformSample(nodeCount, random);
        }
    },
    RANDOM {
        @Override
        public LongUnaryOperator degreeProducer(long nodeCount, long averageDegree, Random random) {
            long stdDev = averageDegree / 2;
            return (ignore) -> DistributionHelper.gaussianSample(nodeCount, averageDegree, stdDev, random);
        }

        @Override
        public LongUnaryOperator relationshipProducer(long nodeCount, long averageDegree, Random random) {
            return (ignore) -> DistributionHelper.uniformSample(nodeCount, random);

        }
    },
    POWER_LAW {
        @Override
        public LongUnaryOperator degreeProducer(long nodeCount, long averageDegree, Random random) {
            long stdDev = averageDegree / 2;
            return (ignore) -> DistributionHelper.gaussianSample(nodeCount, averageDegree, stdDev, random);
        }

        @Override
        public LongUnaryOperator relationshipProducer(long nodeCount, long averageDegree, Random random) {
            long min = 1;
            double gamma = 1 + 1.0 / averageDegree;
            return (ignore) -> DistributionHelper.powerLawSample(min, nodeCount - 1, gamma, random);
        }
    };

    private static final List<String> VALUES = Arrays
        .stream(RelationshipDistribution.values())
        .map(RelationshipDistribution::name)
        .collect(Collectors.toList());

    public static RelationshipDistribution parse(Object object) {
        if (object instanceof String) {
            var inputString = toUpperCaseWithLocale((String) object);
            if(!VALUES.contains(inputString)) {
                throw new IllegalArgumentException(formatWithLocale(
                    "RelationshipDistribution `%s` is not supported. Must be one of: %s.",
                    object,
                    StringJoining.join(VALUES)
                ));
            }
            return RelationshipDistribution.valueOf(inputString);
        } else if (object instanceof RelationshipDistribution) {
            return (RelationshipDistribution) object;
        }

        throw new IllegalArgumentException(formatWithLocale(
            "Expected RelationshipDistribution or String. Got %s.",
            object.getClass().getSimpleName()
        ));
    }

    public static String toString(RelationshipDistribution distribution) {
        return distribution.toString();
    }


    /**
     * Produces a unary function which accepts a node id parameter and returns the number of outgoing relationships
     * that should be generated for this node.
     *
     * @param nodeCount Expected number of nodes in the generated graph
     * @param averageDegree Expected average degree in the generated graph
     * @param random Random instance to be used to generate the number of outgoing relationships
     * @return A unary function that accepts a node id and returns that nodes out degree
     */
    public abstract LongUnaryOperator degreeProducer(long nodeCount, long averageDegree, Random random);

    /**
     * Produces a unary function which accepts a node id parameter and returns another node id to wich the node will
     * be connected.
     *
     * @param nodeCount Expected number of nodes in the generated graph
     * @param averageDegree Expected average degree in the generated graph
     * @param random Random instance to be used to generate the other node id
     * @return A unary function that accepts a node id and returns another node id to wich a relationship will be created.
     */
    public abstract LongUnaryOperator relationshipProducer(long nodeCount, long averageDegree, Random random);
}
