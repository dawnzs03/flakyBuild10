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
package org.neo4j.gds.core.compression.common;

import org.neo4j.gds.api.AdjacencyList;
import org.neo4j.gds.api.AdjacencyProperties;
import org.neo4j.gds.api.compress.AdjacencyCompressor;
import org.neo4j.gds.api.compress.AdjacencyCompressorFactory;
import org.neo4j.gds.api.compress.AdjacencyListBuilder;
import org.neo4j.gds.api.compress.AdjacencyListsWithProperties;
import org.neo4j.gds.api.compress.ImmutableAdjacencyListsWithProperties;
import org.neo4j.gds.core.Aggregation;
import org.neo4j.gds.core.utils.paged.HugeIntArray;
import org.neo4j.gds.core.utils.paged.HugeLongArray;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongSupplier;

public abstract class AbstractAdjacencyCompressorFactory<TARGET_PAGE, PROPERTY_PAGE> implements AdjacencyCompressorFactory {

    private final LongSupplier nodeCountSupplier;
    private final AdjacencyListBuilder<TARGET_PAGE, ? extends AdjacencyList> adjacencyBuilder;
    private final AdjacencyListBuilder<PROPERTY_PAGE, ? extends AdjacencyProperties>[] propertyBuilders;
    private final boolean noAggregation;
    private final Aggregation[] aggregations;
    private final LongAdder relationshipCounter;

    private HugeIntArray adjacencyDegrees;
    private HugeLongArray adjacencyOffsets;
    private HugeLongArray propertyOffsets;

    public AbstractAdjacencyCompressorFactory(
        LongSupplier nodeCountSupplier,
        AdjacencyListBuilder<TARGET_PAGE, ? extends AdjacencyList> adjacencyBuilder,
        AdjacencyListBuilder<PROPERTY_PAGE, ? extends AdjacencyProperties>[] propertyBuilders,
        boolean noAggregation,
        Aggregation[] aggregations
    ) {
        this.adjacencyBuilder = adjacencyBuilder;
        this.propertyBuilders = propertyBuilders;
        this.nodeCountSupplier = nodeCountSupplier;
        this.noAggregation = noAggregation;
        this.aggregations = aggregations;
        this.relationshipCounter = new LongAdder();
    }

    @Override
    public void init() {
        var nodeCount = this.nodeCountSupplier.getAsLong();
        this.adjacencyDegrees = HugeIntArray.newArray(nodeCount);
        this.adjacencyOffsets = HugeLongArray.newArray(nodeCount);
        this.propertyOffsets = HugeLongArray.newArray(nodeCount);
    }

    @Override
    public LongAdder relationshipCounter() {
        return relationshipCounter;
    }

    @Override
    public AdjacencyListsWithProperties build() {
        var builder = ImmutableAdjacencyListsWithProperties
            .builder()
            .adjacency(adjacencyBuilder.build(this.adjacencyDegrees, this.adjacencyOffsets));

        var propertyBuilders = this.propertyBuilders;
        var propertyOffsets = this.propertyOffsets;
        for (var propertyBuilder : propertyBuilders) {
            var properties = propertyBuilder.build(this.adjacencyDegrees, propertyOffsets);
            builder.addProperty(properties);
        }

        return builder.relationshipCount(relationshipCounter.longValue()).build();
    }

    protected abstract AdjacencyCompressor createCompressorFromInternalState(
        AdjacencyListBuilder<TARGET_PAGE, ? extends AdjacencyList> adjacencyBuilder,
        AdjacencyListBuilder<PROPERTY_PAGE, ? extends AdjacencyProperties>[] propertyBuilders,
        boolean noAggregation,
        Aggregation[] aggregations,
        HugeIntArray adjacencyDegrees,
        HugeLongArray adjacencyOffsets,
        HugeLongArray propertyOffsets
    );

    @Override
    public AdjacencyCompressor createCompressor() {
        return this.createCompressorFromInternalState(
            this.adjacencyBuilder,
            this.propertyBuilders,
            this.noAggregation,
            this.aggregations,
            this.adjacencyDegrees,
            this.adjacencyOffsets,
            this.propertyOffsets
        );
    }
}
