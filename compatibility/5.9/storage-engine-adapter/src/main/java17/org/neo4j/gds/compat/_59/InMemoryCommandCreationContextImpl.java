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
package org.neo4j.gds.compat._59;

import org.neo4j.configuration.Config;
import org.neo4j.io.pagecache.context.CursorContext;
import org.neo4j.kernel.KernelVersion;
import org.neo4j.kernel.KernelVersionProvider;
import org.neo4j.lock.LockTracer;
import org.neo4j.lock.ResourceLocker;
import org.neo4j.storageengine.api.CommandCreationContext;
import org.neo4j.storageengine.api.cursor.StoreCursors;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class InMemoryCommandCreationContextImpl implements CommandCreationContext {

    private final AtomicLong schemaTokens;
    private final AtomicInteger propertyTokens;
    private final AtomicInteger labelTokens;
    private final AtomicInteger typeTokens;

    InMemoryCommandCreationContextImpl() {
        this.schemaTokens = new AtomicLong(0);
        this.propertyTokens = new AtomicInteger(0);
        this.labelTokens = new AtomicInteger(0);
        this.typeTokens = new AtomicInteger(0);
    }

    @Override
    public long reserveNode() {
        throw new UnsupportedOperationException("Creating nodes is not supported");
    }

    @Override
    public long reserveRelationship(
        long sourceNode,
        long targetNode,
        int relationshipType,
        boolean sourceNodeAddedInTx,
        boolean targetNodeAddedInTx
    ) {
        throw new UnsupportedOperationException("Creating relationships is not supported");
    }

    @Override
    public long reserveSchema() {
        return schemaTokens.getAndIncrement();
    }

    @Override
    public int reserveLabelTokenId() {
        return labelTokens.getAndIncrement();
    }

    @Override
    public int reservePropertyKeyTokenId() {
        return propertyTokens.getAndIncrement();
    }

    @Override
    public int reserveRelationshipTypeTokenId() {
        return typeTokens.getAndIncrement();
    }

    @Override
    public void close() {

    }

    @Override
    public void initialize(
        KernelVersionProvider kernelVersionProvider,
        CursorContext cursorContext,
        StoreCursors storeCursors,
        Supplier<Long> oldestActiveTransactionSequenceNumber,
        ResourceLocker locks,
        Supplier<LockTracer> lockTracer
    ) {

    }

    @Override
    public KernelVersion kernelVersion() {
        // NOTE: Double-check if this is still correct when you copy this into a new compat layer
        return KernelVersion.getLatestVersion(Config.newBuilder().build());
    }
}
