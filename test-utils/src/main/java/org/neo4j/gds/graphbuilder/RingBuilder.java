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
package org.neo4j.gds.graphbuilder;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import java.util.Random;

/**
 * RingBuilder creates a ring of nodes where each node is
 * connected to its successor while the last element of the
 * chain connects back to its head.
 */
public class RingBuilder extends GraphBuilder<RingBuilder> {

    RingBuilder(Transaction tx, Label label, RelationshipType relationship, Random random) {
        super(tx, label, relationship, random);
    }

    /**
     * create a ring
     *
     * @param size number of elements (&gt;= 2)
     * @return itself for method chaining
     */
    public RingBuilder createRing(int size) {
        if (size < 2) {
            throw new IllegalArgumentException("size must be >= 2");
        }
        Node head = createNode();
        Node temp = head;
        for (int i = 1; i < size; i++) {
            Node node = createNode();
            createRelationship(temp, node);
            temp = node;
        }
        createRelationship(temp, head);
        return this;
    }

    @Override
    protected RingBuilder me() {
        return this;
    }
}
