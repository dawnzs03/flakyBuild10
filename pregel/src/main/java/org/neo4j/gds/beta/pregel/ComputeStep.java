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
package org.neo4j.gds.beta.pregel;

import org.neo4j.gds.beta.pregel.context.ComputeContext;
import org.neo4j.gds.beta.pregel.context.InitContext;
import org.neo4j.gds.core.utils.paged.HugeAtomicBitSet;
import org.neo4j.gds.core.utils.partition.Partition;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;

public interface ComputeStep<
    CONFIG extends PregelConfig,
    ITERATOR extends Messages.MessageIterator,
    INIT_CONTEXT extends InitContext<CONFIG>,
    COMPUTE_CONTEXT extends ComputeContext<CONFIG>
    > {

    HugeAtomicBitSet voteBits();

    InitFunction<CONFIG, INIT_CONTEXT> initFunction();

    ComputeFunction<CONFIG, COMPUTE_CONTEXT> computeFunction();

    NodeValue nodeValue();

    Messenger<ITERATOR> messenger();

    Partition nodeBatch();

    INIT_CONTEXT initContext();

    COMPUTE_CONTEXT computeContext();

    ProgressTracker progressTracker();

    default void computeBatch() {
        var messenger = messenger();
        var messageIterator = messenger.messageIterator();
        var messages = new Messages(messageIterator);

        var nodeBatch = nodeBatch();
        var initContext = initContext();
        var computeContext = computeContext();
        var voteBits = voteBits();

        nodeBatch.consume(nodeId -> {
            if (computeContext.isInitialSuperstep()) {
                initContext.setNodeId(nodeId);
                initFunction().init(initContext);
            }

            messenger.initMessageIterator(messageIterator, nodeId, computeContext.isInitialSuperstep());

            if (!messages.isEmpty() || !voteBits.get(nodeId)) {
                voteBits.clear(nodeId);
                computeContext.setNodeId(nodeId);
                computeFunction().compute(computeContext, messages);
            }
        });
        progressTracker().logProgress(nodeBatch.nodeCount());
    }

    @FunctionalInterface
    interface InitFunction<CONFIG extends PregelConfig, INIT_CONTEXT extends InitContext<CONFIG>> {
        void init(INIT_CONTEXT computeContext);
    }

    @FunctionalInterface
    interface ComputeFunction<CONFIG extends PregelConfig, COMPUTE_CONTEXT extends ComputeContext<CONFIG>> {
        void compute(COMPUTE_CONTEXT computeContext, Messages messages);
    }
}
