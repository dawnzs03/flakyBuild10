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
package org.neo4j.gds;

import org.neo4j.gds.api.NodeLookup;
import org.neo4j.graphdb.Node;
import org.neo4j.kernel.api.KernelTransaction;

public class TransactionNodeLookup implements NodeLookup {

    private final KernelTransaction kernelTransaction;

    public TransactionNodeLookup(KernelTransaction kernelTransaction) {
        this.kernelTransaction = kernelTransaction;
    }

    @Override
    public Node getNodeById(long id) {
        return kernelTransaction.internalTransaction().getNodeById(id);
    }
}
