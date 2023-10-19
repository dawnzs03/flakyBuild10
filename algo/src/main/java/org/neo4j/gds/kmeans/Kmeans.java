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

import org.jetbrains.annotations.NotNull;
import org.neo4j.gds.Algorithm;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.nodeproperties.ValueType;
import org.neo4j.gds.api.properties.nodes.NodePropertyValues;
import org.neo4j.gds.core.concurrency.ParallelUtil;
import org.neo4j.gds.core.concurrency.RunWithConcurrency;
import org.neo4j.gds.core.utils.TerminationFlag;
import org.neo4j.gds.core.utils.paged.HugeDoubleArray;
import org.neo4j.gds.core.utils.paged.HugeIntArray;
import org.neo4j.gds.core.utils.partition.PartitionUtils;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;

import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;

public class Kmeans extends Algorithm<KmeansResult> {

    static final String KMEANS_DESCRIPTION =
        "The Kmeans  algorithm clusters nodes into different communities based on Euclidean distance";
    private static final int UNASSIGNED = -1;
    private final String nodeWeightProperty;
    private HugeIntArray bestCommunities;
    private final Graph graph;
    private final int k;
    private final int concurrency;
    private final ExecutorService executorService;
    private final SplittableRandom random;
    private final NodePropertyValues nodePropertyValues;
    private final int dimensions;

    private final boolean computeSilhouette;
    private double[][] bestCentroids;

    private HugeDoubleArray distanceFromCentroid;
    private final KmeansIterationStopper kmeansIterationStopper;

    private final int maximumNumberOfRestarts;

    private HugeDoubleArray silhouette;

    private final KmeansSampler.SamplerType samplerType;

    private double averageSilhouette;

    private double bestDistance;

    private long[] nodesInCluster;

    private final List<List<Double>> seededCentroids;


    public static Kmeans createKmeans(Graph graph, KmeansBaseConfig config, KmeansContext context) {
        String nodeWeightProperty = config.nodeProperty();
        NodePropertyValues nodeProperties = graph.nodeProperties(nodeWeightProperty);
        if (nodeProperties == null) {
            throw new IllegalArgumentException("Property '" + nodeWeightProperty + "' does not exist for all nodes");
        }
        return new Kmeans(
            context.progressTracker(),
            context.executor(),
            graph,
            config.k(),
            config.concurrency(),
            config.maxIterations(),
            config.numberOfRestarts(),
            config.deltaThreshold(),
            nodeProperties,
            config.computeSilhouette(),
            config.initialSampler(),
            config.seedCentroids(),
            nodeWeightProperty,
            getSplittableRandom(config.randomSeed())
        );
    }


    Kmeans(
        ProgressTracker progressTracker,
        ExecutorService executorService,
        Graph graph,
        int k,
        int concurrency,
        int maxIterations,
        int maximumNumberOfRestarts,
        double deltaThreshold,
        NodePropertyValues nodePropertyValues,
        boolean computeSilhouette,
        KmeansSampler.SamplerType initialSampler,
        List<List<Double>> seededCentroids,
        String nodeWeightProperty,
        SplittableRandom random
    ) {
        super(progressTracker);
        this.nodeWeightProperty = nodeWeightProperty;
        this.executorService = executorService;
        this.graph = graph;
        this.k = k;
        this.concurrency = concurrency;
        this.random = random;
        this.bestCommunities = HugeIntArray.newArray(graph.nodeCount());
        this.nodePropertyValues = nodePropertyValues;
        this.dimensions = nodePropertyValues.doubleArrayValue(0).length;
        this.kmeansIterationStopper = new KmeansIterationStopper(
            deltaThreshold,
            maxIterations,
            graph.nodeCount()
        );
        this.maximumNumberOfRestarts = maximumNumberOfRestarts;
        this.distanceFromCentroid = HugeDoubleArray.newArray(graph.nodeCount());
        this.computeSilhouette = computeSilhouette;
        this.samplerType = initialSampler;
        this.seededCentroids = seededCentroids;
        this.nodesInCluster = new long[k];
    }

    @Override
    public KmeansResult compute() {
        progressTracker.beginSubTask(); // KMeans start

        checkInputValidity();

        if (k > graph.nodeCount()) {
            // Every node in its own community. Warn and return early.
            progressTracker.logWarning("Number of requested clusters is larger than the number of nodes.");
            bestCommunities.setAll(v -> (int) v);
            distanceFromCentroid.setAll(v -> 0d);
            progressTracker.endSubTask(); // KMeans end --> conditional!!!
            bestCentroids = new double[(int) graph.nodeCount()][dimensions];
            for (int i = 0; i < (int) graph.nodeCount(); ++i) {
                bestCentroids[i] = nodePropertyValues.doubleArrayValue(i);
            }
            return ImmutableKmeansResult.of(bestCommunities, distanceFromCentroid, bestCentroids, 0.0, silhouette, 0.0);
        }
        long nodeCount = graph.nodeCount();

        var currentCommunities = HugeIntArray.newArray(nodeCount);
        var currentDistanceFromCentroid = HugeDoubleArray.newArray(nodeCount);

        bestDistance = Double.POSITIVE_INFINITY;
        bestCommunities.setAll(v -> UNASSIGNED);

        // We need this `if` because the task tree is different if the number of restart is > 1.
        if(maximumNumberOfRestarts == 1) {
            kMeans(nodeCount, currentCommunities, currentDistanceFromCentroid, 0);
        } else {
            for (int restartIteration = 0; restartIteration < maximumNumberOfRestarts; ++restartIteration) {
                progressTracker.beginSubTask(); // KMeans Iteration - start
                kMeans(nodeCount, currentCommunities, currentDistanceFromCentroid, restartIteration);
                progressTracker.endSubTask(); // KMeans Iteration - end
            }
        }

        if (computeSilhouette) {
            calculateSilhouette();
        }
        progressTracker.endSubTask(); // KMeans end
        return ImmutableKmeansResult.of(
            bestCommunities,
            distanceFromCentroid,
            bestCentroids,
            bestDistance,
            silhouette,
            averageSilhouette
        );
    }

    private void kMeans(
        long nodeCount,
        HugeIntArray currentCommunities,
        HugeDoubleArray currentDistanceFromCentroid,
        int restartIteration
    ) {

        //note: currentDistanceFromCentroid is not reset to a [0,...,0] distance array but it does not have to
        // it's used only in K-Means++ (where it is essentially reset; see func distanceFromLastSampledCentroid in KmeansTask)
        // or during final distance calculation where it is reset as well (see calculateFinalDistance in KmeansTask)

        ClusterManager clusterManager = ClusterManager.createClusterManager(nodePropertyValues, dimensions, k);

        currentCommunities.setAll(v -> UNASSIGNED);

        var tasks = PartitionUtils.rangePartition(
            concurrency,
            nodeCount,
            partition -> KmeansTask.createTask(
                samplerType,
                clusterManager,
                nodePropertyValues,
                currentCommunities,
                currentDistanceFromCentroid,
                k,
                dimensions,
                partition
            ),
            Optional.of((int) nodeCount / concurrency)
        );
        int numberOfTasks = tasks.size();

        KmeansSampler sampler = KmeansSampler.createSampler(
            samplerType,
            random,
            nodePropertyValues,
            clusterManager,
            nodeCount,
            k,
            concurrency,
            currentDistanceFromCentroid,
            executorService,
            tasks,
            progressTracker
        );

        assert numberOfTasks <= concurrency;

        //Initialization do initial centroid computation and assignment
        initializeCentroids(clusterManager, sampler);

        int iteration = 0;
        progressTracker.beginSubTask(); // Main - start
        while (true) {
            progressTracker.beginSubTask(); // Iteration - start

            long numberOfSwaps = 0;
            //assign each node to a centroid
            boolean shouldComputeDistance = (iteration > 0)
                                            || (samplerType == KmeansSampler.SamplerType.UNIFORM);
            if (shouldComputeDistance) {
                RunWithConcurrency.builder()
                    .concurrency(concurrency)
                    .tasks(tasks)
                    .executor(executorService)
                    .run();

                for (KmeansTask task : tasks) {
                    numberOfSwaps += task.getSwaps();
                }
            }
            recomputeCentroids(clusterManager, tasks);
            progressTracker.endSubTask(); // Iteration - end
            if (kmeansIterationStopper.shouldQuit(numberOfSwaps, ++iteration)) {
                break;
            }

        }
        progressTracker.endSubTask(); // Main - end

        double averageDistanceFromCentroid = calculatedistancePhase(tasks);
        updateBestSolution(
            restartIteration,
            clusterManager,
            averageDistanceFromCentroid,
            currentCommunities,
            currentDistanceFromCentroid
        );
    }

    private void initializeCentroids(ClusterManager clusterManager, KmeansSampler sampler) {
        progressTracker.beginSubTask(); // Initialization - start
        if (!seededCentroids.isEmpty()) {
            clusterManager.assignSeededCentroids(seededCentroids);
        } else {
            sampler.performInitialSampling();
        }
        progressTracker.endSubTask(); // Initialization - end
    }

    private void recomputeCentroids(ClusterManager clusterManager, List<KmeansTask> tasks) {
        clusterManager.reset();

        for (KmeansTask task : tasks) {
            clusterManager.updateFromTask(task);
        }
        clusterManager.normalizeClusters();
    }

    @NotNull
    private static SplittableRandom getSplittableRandom(Optional<Long> randomSeed) {
        return randomSeed.map(SplittableRandom::new).orElseGet(SplittableRandom::new);
    }

    private void checkInputValidity() {
        if (!seededCentroids.isEmpty()) {
            for (List<Double> centroid : seededCentroids) {
                if (centroid.size() != dimensions) {
                    throw new IllegalStateException(
                        "All property arrays for K-Means should have the same number of dimensions");
                } else {
                    for (double value : centroid) {
                        if (Double.isNaN(value)) {
                            throw new IllegalArgumentException("Input for K-Means should not contain any NaN values");
                        }
                    }
                }

            }
        }
        ParallelUtil.parallelForEachNode(graph.nodeCount(), concurrency, TerminationFlag.RUNNING_TRUE, nodeId -> {
            if (nodePropertyValues.valueType() == ValueType.FLOAT_ARRAY) {
                var value = nodePropertyValues.floatArrayValue(nodeId);
                if (value == null) {
                    throw new IllegalArgumentException("Property '" + nodeWeightProperty + "' does not exist for all nodes");
                }
                if (value.length != dimensions) {
                    throw new IllegalStateException(
                        "All property arrays for K-Means should have the same number of dimensions");
                } else {
                    for (int dimension = 0; dimension < dimensions; ++dimension) {
                        if (Float.isNaN(value[dimension])) {
                            throw new IllegalArgumentException("Input for K-Means should not contain any NaN values");
                        }
                    }
                }
            } else {
                var value = nodePropertyValues.doubleArrayValue(nodeId);
                if (value == null) {
                    throw new IllegalArgumentException("Property '" + nodeWeightProperty + "' does not exist for all nodes");
                }
                if (value.length != dimensions) {
                    throw new IllegalStateException(
                        "All property arrays for K-Means should have the same number of dimensions");
                } else {
                    for (int dimension = 0; dimension < dimensions; ++dimension) {
                        if (Double.isNaN(value[dimension])) {
                            throw new IllegalArgumentException("Input for K-Means should not contain any NaN values");

                        }
                    }
                }
            }
        });
    }


    private void calculateSilhouette() {
        var nodeCount = graph.nodeCount();
        progressTracker.beginSubTask();
        this.silhouette = HugeDoubleArray.newArray(nodeCount);
        var tasks = PartitionUtils.rangePartition(
            concurrency,
            nodeCount,
            partition -> SilhouetteTask.createTask(
                nodePropertyValues,
                bestCommunities,
                silhouette,
                k,
                dimensions,
                nodesInCluster,
                partition,
                progressTracker
            ),
            Optional.of((int) nodeCount / concurrency)
        );
        RunWithConcurrency.builder()
            .concurrency(concurrency)
            .tasks(tasks)
            .executor(executorService)
            .run();

        for (var task : tasks) {
            averageSilhouette += task.getAverageSilhouette();
        }
        progressTracker.endSubTask();

    }

    private double calculatedistancePhase(List<KmeansTask> tasks) {
        for (KmeansTask task : tasks) {
            task.switchToPhase(TaskPhase.DISTANCE);
        }
        RunWithConcurrency.builder()
            .concurrency(concurrency)
            .tasks(tasks)
            .executor(executorService)
            .run();
        double averageDistanceFromCentroid = 0;
        for (KmeansTask task : tasks) {
            averageDistanceFromCentroid += task.getDistanceFromCentroidNormalized();
        }
        return averageDistanceFromCentroid;
    }

    private void updateBestSolution(
        int restartIteration,
        ClusterManager clusterManager,
        double averageDistanceFromCentroid,
        HugeIntArray currentCommunities,
        HugeDoubleArray currentDistanceFromCentroid
    ) {
        if (restartIteration >= 1) {
            if (averageDistanceFromCentroid < bestDistance) {
                bestDistance = averageDistanceFromCentroid;
                ParallelUtil.parallelForEachNode(
                    graph.nodeCount(),
                    concurrency,
                    terminationFlag,
                    v -> {
                        bestCommunities.set(v, currentCommunities.get(v));
                        distanceFromCentroid.set(v, currentDistanceFromCentroid.get(v));
                    }
                );
                bestCentroids = clusterManager.getCentroids();
                if (computeSilhouette) {
                    nodesInCluster = clusterManager.getNodesInCluster();
                }
            }
        } else {
            bestCommunities = currentCommunities;
            distanceFromCentroid = currentDistanceFromCentroid;
            bestCentroids = clusterManager.getCentroids();
            bestDistance = averageDistanceFromCentroid;
            if (computeSilhouette) {
                nodesInCluster = clusterManager.getNodesInCluster();
            }

        }
    }

}
