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
package org.neo4j.gds.kmeans;

import org.neo4j.gds.api.properties.nodes.NodePropertyValues;
import org.neo4j.gds.core.utils.mem.MemoryEstimation;
import org.neo4j.gds.core.utils.mem.MemoryEstimations;
import org.neo4j.gds.core.utils.mem.MemoryRange;
import org.neo4j.gds.core.utils.paged.HugeDoubleArray;
import org.neo4j.gds.core.utils.paged.HugeIntArray;
import org.neo4j.gds.core.utils.partition.Partition;
import org.neo4j.gds.mem.MemoryUsage;


public abstract class KmeansTask implements Runnable {
    private final ClusterManager clusterManager;
    private final Partition partition;
    final NodePropertyValues nodePropertyValues;

    private final HugeDoubleArray distanceFromCentroid;

    final HugeIntArray communities;
    final long[] communitySizes;
    final int k;
    final int dimensions;
    private long swaps;

    private double distance;

    private double squaredDistance = 0;

    private TaskPhase phase;

    long getNumAssignedAtCluster(int ith) {
        return communitySizes[ith];
    }

    long getSwaps() {
        return swaps;
    }

    static MemoryEstimation memoryEstimation(int k, int fakeDimensions) {
        var builder = MemoryEstimations.builder(KmeansTask.class);
        builder
            .fixed("communitySizes", MemoryUsage.sizeOfLongArray(k))
            .add("communityCoordinateSums", MemoryEstimations.of("communityCoordinateSums", MemoryRange.of(
                k * MemoryUsage.sizeOfFloatArray(fakeDimensions),
                k * MemoryUsage.sizeOfDoubleArray(fakeDimensions)
            )));
        return builder.build();
    }

    abstract void reset();

    abstract void updateAfterAssignmentToCentroid(long nodeId, int community);

    KmeansTask(
        KmeansSampler.SamplerType samplerType,
        ClusterManager clusterManager,
        NodePropertyValues nodePropertyValues,
        HugeIntArray communities,
        HugeDoubleArray distanceFromCentroid,
        int k,
        int dimensions,
        Partition partition
    ) {
        this.clusterManager = clusterManager;
        this.nodePropertyValues = nodePropertyValues;
        this.communities = communities;
        this.distanceFromCentroid = distanceFromCentroid;
        this.k = k;
        this.dimensions = dimensions;
        this.partition = partition;
        this.communitySizes = new long[k];
        if (samplerType == KmeansSampler.SamplerType.UNIFORM) {
            this.phase = TaskPhase.ITERATION;
        } else {
            this.phase = TaskPhase.INITIAL;
        }
        this.distance = 0d;
    }

    static KmeansTask createTask(
        KmeansSampler.SamplerType samplerType,
        ClusterManager clusterManager,
        NodePropertyValues nodePropertyValues,
        HugeIntArray communities,
        HugeDoubleArray distanceFromCentroid,
        int k,
        int dimensions,
        Partition partition
    ) {
        if (clusterManager instanceof DoubleClusterManager) {
            return new DoubleKmeansTask(
                samplerType,
                clusterManager,
                nodePropertyValues,
                communities,
                distanceFromCentroid,
                k,
                dimensions,
                partition
            );
        }
        return new FloatKmeansTask(
            samplerType,
            clusterManager,
            nodePropertyValues,
            communities,
            distanceFromCentroid,
            k,
            dimensions,
            partition
        );
    }

    void switchToPhase(TaskPhase newPhase) {
        phase = newPhase;
    }

    private void assignNodeToCentroid(long startNode, long endNode) {
        swaps = 0;

        reset();

        for (long nodeId = startNode; nodeId < endNode; nodeId++) {
            int closestCommunity = clusterManager.findClosestCentroid(nodeId);
            communitySizes[closestCommunity]++;
            int previousCommunity = communities.get(nodeId);
            if (closestCommunity != previousCommunity) {
                swaps++;
            }
            communities.set(nodeId, closestCommunity);
            //Note for potential improvement : This is potentially costly when clusters have somewhat stabilized.
            //Because we keep adding the same nodes to the same clusters. Perhaps instead of making the sum 0
            //we keep as is and do a subtraction when a node changes its cluster.
            //On that note,  maybe we can skip stable communities (i.e., communities that did not change between one iteration to another)
            // or avoid calculating their distance from other nodes etc...
            updateAfterAssignmentToCentroid(nodeId, closestCommunity);
        }
    }

    public double getDistanceFromCentroidNormalized() {
        return distance / communities.size();
    }

    public double getSquaredDistance() {
        return squaredDistance;
    }

    private void calculateFinalDistance(long startNode, long endNode) {


        for (long nodeId = startNode; nodeId < endNode; nodeId++) {
            double nodeCentroidDistance = clusterManager.euclidean(nodeId, communities.get(nodeId));
            distance += nodeCentroidDistance;
            distanceFromCentroid.set(nodeId, nodeCentroidDistance);

        }
    }

    private void distanceFromLastSampledCentroid(long startNode, long endNode, int numAssigned) {
        squaredDistance = 0;
        for (long nodeId = startNode; nodeId < endNode; nodeId++) {
            if (distanceFromCentroid.get(nodeId) > -1) {
                double nodeCentroidDistance = clusterManager.euclidean(nodeId, numAssigned - 1);
                if (numAssigned == 1) {
                    distanceFromCentroid.set(nodeId, nodeCentroidDistance);
                    squaredDistance += nodeCentroidDistance * nodeCentroidDistance;
                    communities.set(nodeId, 0);

                } else if (distanceFromCentroid.get(nodeId) > nodeCentroidDistance) {
                    distanceFromCentroid.set(nodeId, nodeCentroidDistance);
                    squaredDistance += nodeCentroidDistance * nodeCentroidDistance;
                    communities.set(nodeId, numAssigned - 1);
                } else {
                    squaredDistance += distanceFromCentroid.get(nodeId) * distanceFromCentroid.get(nodeId);
                }
            }
            if (numAssigned == k) {

                if (distanceFromCentroid.get(nodeId) <= -1) {
                    communities.set(nodeId, (int) -distanceFromCentroid.get(nodeId) - 1);
                    distanceFromCentroid.set(nodeId, 0);
                }
                int communityId = communities.get(nodeId);
                communitySizes[communityId]++;
                updateAfterAssignmentToCentroid(nodeId, communityId);
            }
        }
    }

    @Override
    public void run() {
        var startNode = partition.startNode();
        long endNode = startNode + partition.nodeCount();
        if (phase == TaskPhase.ITERATION) {
            assignNodeToCentroid(startNode, endNode);
        } else if (phase == TaskPhase.DISTANCE) {
            calculateFinalDistance(startNode, endNode);
        } else {
            distanceFromLastSampledCentroid(startNode, endNode, clusterManager.getCurrentlyAssigned());
        }
    }
}

