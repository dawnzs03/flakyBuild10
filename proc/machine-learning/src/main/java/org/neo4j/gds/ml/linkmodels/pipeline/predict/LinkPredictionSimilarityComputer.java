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
package org.neo4j.gds.ml.linkmodels.pipeline.predict;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.ml.models.Classifier;
import org.neo4j.gds.ml.pipeline.linkPipeline.LinkFeatureExtractor;
import org.neo4j.gds.ml.splitting.EdgeSplitter;
import org.neo4j.gds.similarity.knn.NeighborFilter;
import org.neo4j.gds.similarity.knn.NeighborFilterFactory;
import org.neo4j.gds.similarity.knn.metrics.SimilarityComputer;

class LinkPredictionSimilarityComputer implements SimilarityComputer {
    private static final int POSITIVE_CLASS_INDEX = (int) EdgeSplitter.POSITIVE;
    private final LinkFeatureExtractor linkFeatureExtractor;
    private final Classifier classifier;

    LinkPredictionSimilarityComputer(
        LinkFeatureExtractor linkFeatureExtractor,
        Classifier classifier
    ) {
        this.linkFeatureExtractor = linkFeatureExtractor;
        this.classifier = classifier;
    }

    @Override
    public double similarity(long sourceId, long targetId) {
        var features = linkFeatureExtractor.extractFeatures(sourceId, targetId);
        return classifier.predictProbabilities(features)[POSITIVE_CLASS_INDEX];
    }

    @Override
    public boolean isSymmetric() {
        return linkFeatureExtractor.isSymmetric();
    }

    static final class LinkFilter implements NeighborFilter {

        private final LPNodeFilter sourceNodeFilter;
        private final LPNodeFilter targetNodeFilter;
        private final Graph graph;

        private LinkFilter(Graph graph, LPNodeFilter sourceNodeFilter, LPNodeFilter targetNodeFilter) {
            this.graph = graph;
            this.sourceNodeFilter = sourceNodeFilter;
            this.targetNodeFilter = targetNodeFilter;
        }

        @Override
        public boolean excludeNodePair(long firstNodeId, long secondNodeId) {
            if (firstNodeId == secondNodeId) {
                return true;
            }

            var matchesFilter = sourceNodeFilter.test(firstNodeId) && targetNodeFilter.test(secondNodeId) || sourceNodeFilter.test(secondNodeId) && targetNodeFilter.test(firstNodeId);

            // graph.exists a slower but memory-efficient approach (could be replaced by a dedicated data structure)
            return !matchesFilter || graph.exists(firstNodeId, secondNodeId);
        }

        @Override
        public long lowerBoundOfPotentialNeighbours(long node) {
            if (sourceNodeFilter.test(node)) {
                return Math.max(targetNodeFilter.validNodeCount() - 1 - graph.degree(node), 0L);
            } else {
                return Math.max(sourceNodeFilter.validNodeCount() - 1 - graph.degree(node), 0L);
            }
        }
    }

    static class LinkFilterFactory implements NeighborFilterFactory {

        private final Graph graph;

        private final LPNodeFilter sourceNodeFilter;

        private final LPNodeFilter targetNodeFilter;

        LinkFilterFactory(Graph graph, LPNodeFilter sourceNodeFilter, LPNodeFilter targetNodeFilter) {
            this.graph = graph;
            this.sourceNodeFilter = sourceNodeFilter;
            this.targetNodeFilter = targetNodeFilter;
        }

        @Override
        public NeighborFilter create() {
            return new LinkPredictionSimilarityComputer.LinkFilter(graph.concurrentCopy(), sourceNodeFilter, targetNodeFilter);
        }
    }
}
