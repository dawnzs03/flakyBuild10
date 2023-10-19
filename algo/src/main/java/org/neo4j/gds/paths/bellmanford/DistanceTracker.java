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
package org.neo4j.gds.paths.bellmanford;

import org.neo4j.gds.collections.haa.HugeAtomicDoubleArray;
import org.neo4j.gds.collections.haa.HugeAtomicLongArray;
import org.neo4j.gds.core.utils.mem.MemoryEstimation;
import org.neo4j.gds.core.utils.mem.MemoryEstimations;
import org.neo4j.gds.core.utils.paged.ParalleLongPageCreator;
import org.neo4j.gds.core.utils.paged.ParallelDoublePageCreator;

final class DistanceTracker {

    private static final double DIST_INF = Double.MAX_VALUE;
    private static final long NO_PREDECESSOR = Long.MAX_VALUE;
    private static final long NO_LENGTH = Long.MAX_VALUE;

    static DistanceTracker create(
        long size,
        int concurrency
    ) {
        var distances = HugeAtomicDoubleArray.of(
            size,
            ParallelDoublePageCreator.of(concurrency, index -> DIST_INF)
        );
        var predecessors = HugeAtomicLongArray.of(
            size,
            ParalleLongPageCreator.of(concurrency, index -> NO_PREDECESSOR)
        );
        var lengths = HugeAtomicLongArray.of(
            size,
            ParalleLongPageCreator.of(concurrency, index -> NO_LENGTH)
        );

        return new DistanceTracker(predecessors, distances, lengths, size);
    }

    private final HugeAtomicLongArray predecessors;
    // Use atomic array since it get/set methods are volatile
    private final HugeAtomicDoubleArray distances;

    private final HugeAtomicLongArray lengths;
    private final long size;

    private DistanceTracker(
        HugeAtomicLongArray predecessors,
        HugeAtomicDoubleArray distances,
        HugeAtomicLongArray lengths,
        long size
    ) {
        this.predecessors = predecessors;
        this.distances = distances;
        this.lengths = lengths;
        this.size = size;
    }

    static MemoryEstimation memoryEstimation() {
        return MemoryEstimations.builder(DistanceTracker.class)
            .perNode("distances", HugeAtomicDoubleArray::memoryEstimation)
            .perNode("predecessors", HugeAtomicLongArray::memoryEstimation)
            .perNode("lengths", HugeAtomicLongArray::memoryEstimation)
            .build();
    }

    public double distance(long nodeId) {
        return distances.get(nodeId);
    }

    public long predecessor(long nodeId) {
        return predecessors.get(nodeId);
    }

    public long length(long nodeId) {return lengths.get(nodeId);}

    HugeAtomicDoubleArray distances() {
        return distances;
    }

    long size() {
        return size;
    }

    public void set(long nodeId, long predecessor, double distance, long length) {
        distances.set(nodeId, distance);
        predecessors.set(nodeId, predecessor);
        lengths.set(nodeId, length);
    }

    double compareAndExchange(
        long nodeId,
        double expectedDistance,
        double newDistance,
        long predecessor,
        long length
    ) {
        long currentLength = lengths.get(nodeId);


        // locked by another thread
        if (currentLength < 0) { //the length is always positive
            //we  should signal failure
            // for that we must be sure not to return the 'expectedDistance' by accident!
            //we hence return its negation (or -1 if ==0)
            return (Double.compare(expectedDistance, 0.0) == 0) ? -1.0 : -expectedDistance;
        }

        //we obtain a lock on nodeId, if we negate it's length to a negative side
        //if simultaneously nodeId is being relaxed, we can obtain its most recent correct value by negating
        //if negative
        var witness = lengths.compareAndExchange(nodeId, currentLength, -currentLength);

        // CAX failed
        if (witness != currentLength) {
            //we  should signal failure
            // for that we must be sure not to return the 'expectedDistance' by accident!
            return (Double.compare(expectedDistance, 0.0) == 0) ? -1.0 : -expectedDistance;
        }

        double oldDistance = distances.get(nodeId);
        // we have the lock; no-one else can write on nodeId at the moment.
        // Let us do a check if it makes sense to update

        if (oldDistance > newDistance) {
            distances.set(nodeId, newDistance);
            predecessors.set(nodeId, predecessor);
            // unlock
            lengths.set(nodeId, length);
            // return previous distance to signal successful CAX

            return expectedDistance;
        }
        lengths.set(nodeId, currentLength);
        //signal unsuccesful update
        //note that this unsuccesful update will be the last attempt
        return (Double.compare(expectedDistance, 0.0) == 0.0) ? -1.0 : -expectedDistance;
    }
}
