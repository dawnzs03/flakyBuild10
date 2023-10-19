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
package org.neo4j.gds.result;

import org.neo4j.gds.config.AlgoBaseConfig;

public abstract class AbstractResultBuilder<RESULT> {

    protected long preProcessingMillis = -1;
    protected long computeMillis = -1;
    protected long writeMillis = -1;
    protected long mutateMillis = -1;
    protected long nodeCount;
    protected long nodePropertiesWritten;
    protected long relationshipsWritten;
    protected AlgoBaseConfig config;

    public AbstractResultBuilder<RESULT> withPreProcessingMillis(long preProcessingMillis) {
        this.preProcessingMillis = preProcessingMillis;
        return this;
    }

    public AbstractResultBuilder<RESULT> withComputeMillis(long computeMillis) {
        this.computeMillis = computeMillis;
        return this;
    }

    public AbstractResultBuilder<RESULT> withWriteMillis(long writeMillis) {
        this.writeMillis = writeMillis;
        return this;
    }

    public AbstractResultBuilder<RESULT> withMutateMillis(long mutateMillis) {
        this.mutateMillis = mutateMillis;
        return this;
    }

    public AbstractResultBuilder<RESULT> withNodeCount(long nodeCount) {
        this.nodeCount = nodeCount;
        return this;
    }

    public AbstractResultBuilder<RESULT> withNodePropertiesWritten(long nodePropertiesWritten) {
        this.nodePropertiesWritten = nodePropertiesWritten;
        return this;
    }

    public AbstractResultBuilder<RESULT> withRelationshipsWritten(long relationshipPropertiesWritten) {
        this.relationshipsWritten = relationshipPropertiesWritten;
        return this;
    }

    public AbstractResultBuilder<RESULT> withConfig(AlgoBaseConfig config) {
        this.config = config;
        return this;
    }

    public abstract RESULT build();
}
