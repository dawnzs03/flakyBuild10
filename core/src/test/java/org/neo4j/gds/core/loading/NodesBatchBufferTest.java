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

import com.carrotsearch.hppc.LongHashSet;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.compat.Neo4jProxy;
import org.neo4j.gds.compat.PropertyReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NodesBatchBufferTest {

    @Test
    void shouldIgnoreNodesThatAreOutOfBoundsOnOffer() {
        var nodesBatchBuffer = new NodesBatchBufferBuilder()
            .capacity(3)
            .highestPossibleNodeCount(43)
            .build();

        // within range
        nodesBatchBuffer.offer(new TestNode(21));
        // end of range
        nodesBatchBuffer.offer(new TestNode(42));
        // out of range
        nodesBatchBuffer.offer(new TestNode(84));

        assertThat(nodesBatchBuffer)
            .returns(2, RecordsBatchBuffer::length)
            .returns(new long[]{21, 42, 0}, RecordsBatchBuffer::batch);
    }

    @Test
    void shouldIgnoreNodesThatAreOutOfBoundsOnOfferWithLabelInformation() {
        var nodesBatchBuffer = new NodesBatchBufferBuilder()
            .capacity(3)
            .highestPossibleNodeCount(43)
            .hasLabelInformation(true)
            .nodeLabelIds(LongHashSet.from(0))
            .build();

        // within range
        nodesBatchBuffer.offer(new TestNode(21, 0));
        // end of range
        nodesBatchBuffer.offer(new TestNode(42, 0));
        // out of range
        nodesBatchBuffer.offer(new TestNode(84, 0));

        assertThat(nodesBatchBuffer)
            .returns(2, RecordsBatchBuffer::length)
            .returns(new long[]{21, 42, 0}, RecordsBatchBuffer::batch);
    }

    @Test
    void shouldNotThrowOnCheckedBuffer() {
        var nodesBatchBuffer = new NodesBatchBufferBuilder()
            .capacity(2)
            .highestPossibleNodeCount(42)
            .useCheckedBuffer(true)
            .build();

        assertThat(nodesBatchBuffer.offer(new TestNode(0))).isTrue();
        assertThat(nodesBatchBuffer.offer(new TestNode(1))).isFalse();
        assertThat(nodesBatchBuffer.offer(new TestNode(2))).isFalse();
        assertThat(nodesBatchBuffer.isFull()).isTrue();
    }

    @Test
    void shouldThrowOnUncheckedBuffer() {
        var nodesBatchBuffer = new NodesBatchBufferBuilder()
            .capacity(2)
            .highestPossibleNodeCount(42)
            .useCheckedBuffer(false)
            .build();

        assertThat(nodesBatchBuffer.offer(new TestNode(0))).isTrue();
        assertThat(nodesBatchBuffer.offer(new TestNode(1))).isTrue();

        assertThatThrownBy(() -> nodesBatchBuffer.offer(new TestNode(2)))
            .isInstanceOf(ArrayIndexOutOfBoundsException.class);

        assertThat(nodesBatchBuffer.isFull()).isTrue();
    }

    private static final class TestNode implements NodeReference {
        private final long nodeId;
        private final long[] labels;

        private TestNode(long nodeId, long... labels) {
            this.nodeId = nodeId;
            this.labels = labels;
        }

        @Override
        public long nodeId() {
            return this.nodeId;
        }

        @Override
        public long[] labels() {
            return labels;
        }

        @Override
        public long relationshipReference() {
            return -1L;
        }

        @Override
        public PropertyReference propertiesReference() {
            return Neo4jProxy.noPropertyReference();
        }
    }
}
