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
package org.neo4j.gds.core.loading;

import org.neo4j.gds.compat.Neo4jProxy;
import org.neo4j.gds.compat.PropertyReference;
import org.neo4j.gds.core.Aggregation;
import org.neo4j.internal.kernel.api.PropertyCursor;
import org.neo4j.kernel.api.KernelTransaction;

import java.util.Arrays;

public interface PropertyReader {
    /**
     * Load the relationship properties for the given batch of relationships.
     * Relationships are represented as two arrays from the {@link RelationshipsBatchBuffer}.
     *
     * @param relationshipReferences   relationship references (IDs)
     * @param propertyReferences       property references (IDs or References)
     * @param numberOfReferences       number of valid entries in the first two arrays
     * @param propertyKeyIds           property key ids to load
     * @param defaultValues            default weight for each property key
     * @param aggregations             the aggregation for each property
     * @param atLeastOnePropertyToLoad true iff there is at least one value in {@code propertyKeyIds} that is not {@link org.neo4j.kernel.api.StatementConstants#NO_SUCH_PROPERTY_KEY} (-1).
     * @return list of property values per relationship property id
     */
    long[][] readProperty(
        long[] relationshipReferences,
        PropertyReference[] propertyReferences,
        int numberOfReferences,
        int[] propertyKeyIds,
        double[] defaultValues,
        Aggregation[] aggregations,
        boolean atLeastOnePropertyToLoad
    );

    static PropertyReader preLoaded() {
        return (relationshipReferences, propertyReferences, numberOfReferences, weightProperty, defaultWeight, aggregations, atLeastOnePropertyToLoad) -> {
            long[] properties = Arrays.copyOf(relationshipReferences, numberOfReferences);
            return new long[][]{properties};
        };
    }

    static PropertyReader storeBacked(KernelTransaction kernelTransaction) {
        return (relationshipReferences, propertyReferences, numberOfReferences, relationshipProperties, defaultPropertyValues, aggregations, atLeastOnePropertyToLoad) -> {
            long[][] properties = new long[relationshipProperties.length][numberOfReferences];
            if (atLeastOnePropertyToLoad) {
                try (PropertyCursor pc = Neo4jProxy.allocatePropertyCursor(kernelTransaction)) {
                    double[] relProps = new double[relationshipProperties.length];
                    for (int i = 0; i < numberOfReferences; i++) {
                        Neo4jProxy.relationshipProperties(
                            kernelTransaction,
                            relationshipReferences[i],
                            propertyReferences[i],
                            pc
                        );
                        ReadHelper.readProperties(
                            pc,
                            relationshipProperties,
                            defaultPropertyValues,
                            aggregations,
                            relProps
                        );
                        for (int j = 0; j < relProps.length; j++) {
                            properties[j][i] = Double.doubleToLongBits(relProps[j]);
                        }
                    }
                }
            } else {
                for (int i = 0; i < numberOfReferences; i++) {
                    for (int j = 0; j < defaultPropertyValues.length; j++) {
                        double value = aggregations[j].normalizePropertyValue(defaultPropertyValues[j]);
                        properties[j][i] = Double.doubleToLongBits(value);
                    }
                }
            }
            return properties;
        };
    }

    static Buffered buffered(int batchSize, int propertyCount) {
        return new Buffered(batchSize, propertyCount);
    }

    class Buffered implements PropertyReader {

        private final long[][] buffer;
        private final int propertyCount;

        Buffered(int batchSize, int propertyCount) {
            this.propertyCount = propertyCount;
            this.buffer = new long[propertyCount][batchSize];
        }

        public void add(int relationshipId, int propertyKeyId, double property) {
            buffer[propertyKeyId][relationshipId] = Double.doubleToLongBits(property);
        }

        @Override
        public long[][] readProperty(
            long[] relationshipReferences,
            PropertyReference[] propertyReferences,
            int numberOfReferences,
            int[] propertyKeyIds,
            double[] defaultValues,
            Aggregation[] aggregations,
            boolean atLeastOnePropertyToLoad
        ) {
            long[][] resultBuffer = new long[propertyCount][numberOfReferences];

            for (int propertyKeyId = 0; propertyKeyId < propertyCount; propertyKeyId++) {
                long[] propertyValues = new long[numberOfReferences];
                for (int relationshipOffset = 0; relationshipOffset < numberOfReferences; relationshipOffset++) {
                    int relationshipId = (int) relationshipReferences[relationshipOffset];
                    // We need to fill this consecutively indexed
                    // in the same order as the relationships are
                    // stored in the batch.
                    propertyValues[relationshipOffset] = buffer[propertyKeyId][relationshipId];
                }
                resultBuffer[propertyKeyId] = propertyValues;
            }

            return resultBuffer;
        }
    }
}
