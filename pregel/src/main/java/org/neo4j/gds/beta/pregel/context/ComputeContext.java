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
package org.neo4j.gds.beta.pregel.context;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.beta.pregel.BasePregelComputation;
import org.neo4j.gds.beta.pregel.Messenger;
import org.neo4j.gds.beta.pregel.NodeValue;
import org.neo4j.gds.beta.pregel.PregelConfig;
import org.neo4j.gds.core.utils.paged.HugeAtomicBitSet;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;

import java.util.Optional;

/**
 * A context that is used during the computation. It allows an implementation
 * to send messages to other nodes and change the state of the currently
 * processed node.
 */
public class ComputeContext<CONFIG extends PregelConfig> extends NodeCentricContext<CONFIG> {

    private final HugeAtomicBitSet voteBits;

    private final Messenger<?> messenger;
    private final MutableInt iteration;
    private final MutableBoolean hasSendMessage;

    protected BasePregelComputation<CONFIG> computation;

    public ComputeContext(Graph graph,
                          CONFIG config,
                          BasePregelComputation<CONFIG> computation,
                          NodeValue nodeValue,
                          Messenger<?> messenger,
                          HugeAtomicBitSet voteBits,
                          MutableInt iteration,
                          Optional<MutableBoolean> hasSendMessage,
                          ProgressTracker progressTracker) {
        super(graph, config, nodeValue, progressTracker);
        this.computation = computation;
        this.sendMessagesFunction = config.hasRelationshipWeightProperty()
            ? this::sendToNeighborsWeighted
            : this::sendToNeighbors;
        this.messenger = messenger;
        this.voteBits = voteBits;
        this.iteration = iteration;
        this.hasSendMessage = hasSendMessage.orElse(new MutableBoolean(false));
    }

    private final SendMessagesFunction sendMessagesFunction;

    /**
     * Returns the node value for the given node schema key.
     *
     * @throws IllegalArgumentException if the key does not exist or the value is not a double
     */
    public double doubleNodeValue(String key) {
        return nodeValue.doubleValue(key, nodeId);
    }

    /**
     * Returns the node value for the given node schema key.
     *
     * @throws IllegalArgumentException if the key does not exist or the value is not a long
     */
    public long longNodeValue(String key) {
        return nodeValue.longValue(key, nodeId);
    }

    /**
     * Returns the node value for the given node schema key.
     *
     * @throws IllegalArgumentException if the key does not exist or the value is not a long array
     */
    public long[] longArrayNodeValue(String key) {
        return nodeValue.longArrayValue(key, nodeId);
    }

    /**
     * Returns the node value for the given node-id and node schema key.
     *
     * @throws IllegalArgumentException if the key does not exist or the value is not a long array
     */
    public long[] longArrayNodeValue(String key, long id) {
        return nodeValue.longArrayValue(key, id);
    }

    /**
     * Returns the node value for the given node schema key.
     *
     * @throws IllegalArgumentException if the key does not exist or the value is not a double array
     */
    public double[] doubleArrayNodeValue(String key) {
        return nodeValue.doubleArrayValue(key, nodeId);
    }

    /**
     * Notify the execution framework that this node intends
     * to stop the computation. If the node voted to halt
     * and has not received new messages in the next superstep,
     * the compute method will not be called for that node.
     * If a node receives messages, the vote to halt flag will
     * be ignored.
     */
    public void voteToHalt() {
        voteBits.set(nodeId);
    }

    /**
     * Indicates if the current superstep is the first superstep.
     */
    public boolean isInitialSuperstep() {
        return superstep() == 0;
    }

    /**
     * Returns the current superstep (0-based).
     */
    public int superstep() {
        return iteration.getValue();
    }

    /**
     * Sends the given message to all neighbors of the node.
     */
    public void sendToNeighbors(double message) {
        sendMessagesFunction.sendToNeighbors(nodeId, message);
    }

    /**
     * Sends the given message to the target node. The target
     * node can be any existing node id in the graph.
     *
     * @throws ArrayIndexOutOfBoundsException if the node is in the not in id space
     */
    public void sendTo(long targetNodeId, double message) {
        messenger.sendTo(targetNodeId, message);
        this.hasSendMessage.setValue(true);
    }

    private void sendToNeighbors(long sourceNodeId, double message) {
        graph.forEachRelationship(sourceNodeId, (ignored, targetNodeId) -> {
            sendTo(targetNodeId, message);
            return true;
        });
    }

    private void sendToNeighborsWeighted(long sourceNodeId, double message) {
        graph.forEachRelationship(sourceNodeId, 1.0, (ignored, targetNodeId, weight) -> {
            sendTo(targetNodeId, computation.applyRelationshipWeight(message, weight));
            return true;
        });
    }

    public boolean hasSentMessage() {
        return hasSendMessage.getValue();
    }

    @FunctionalInterface
    interface SendMessagesFunction {
        void sendToNeighbors(long sourceNodeId, double message);
    }

    public static final class BidirectionalComputeContext<CONFIG extends PregelConfig> extends ComputeContext<CONFIG> implements BidirectionalNodeCentricContext {

        private final SendMessagesIncomingFunction sendMessagesIncomingFunction;

        public BidirectionalComputeContext(
            Graph graph,
            CONFIG config,
            BasePregelComputation<CONFIG> computation,
            NodeValue nodeValue,
            Messenger<?> messenger,
            HugeAtomicBitSet voteBits,
            MutableInt iteration,
            Optional<MutableBoolean> hasSendMessage,
            ProgressTracker progressTracker
        ) {
            super(
                graph,
                config,
                computation,
                nodeValue,
                messenger,
                voteBits,
                iteration,
                hasSendMessage,
                progressTracker
            );

            this.sendMessagesIncomingFunction = config.hasRelationshipWeightProperty()
                ? this::sendToIncomingNeighborsWeighted
                : this::sendToIncomingNeighbors;
        }

        /**
         * Sends the given message to all neighbors of the node.
         */
        public void sendToIncomingNeighbors(double message) {
            sendMessagesIncomingFunction.sendToIncomingNeighbors(nodeId, message);
        }

        private void sendToIncomingNeighbors(long sourceNodeId, double message) {
            graph.forEachInverseRelationship(sourceNodeId, (ignored, targetNodeId) -> {
                sendTo(targetNodeId, message);
                return true;
            });
        }

        private void sendToIncomingNeighborsWeighted(long sourceNodeId, double message) {
            graph.forEachInverseRelationship(sourceNodeId, 1.0, (ignored, targetNodeId, weight) -> {
                sendTo(targetNodeId, computation.applyRelationshipWeight(message, weight));
                return true;
            });
        }

        @Override
        public Graph graph() {
            return graph;
        }

        @FunctionalInterface
        interface SendMessagesIncomingFunction {
            void sendToIncomingNeighbors(long sourceNodeId, double message);
        }
    }
}
