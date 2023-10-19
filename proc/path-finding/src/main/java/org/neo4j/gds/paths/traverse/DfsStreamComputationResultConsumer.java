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
package org.neo4j.gds.paths.traverse;

import org.neo4j.gds.core.utils.paged.HugeLongArray;
import org.neo4j.gds.executor.ComputationResult;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;

import java.util.stream.Stream;

import static org.neo4j.gds.LoggingUtil.runWithExceptionLogging;

class DfsStreamComputationResultConsumer implements ComputationResultConsumer<DFS, HugeLongArray, DfsStreamConfig, Stream<DfsStreamResult>> {

    private final PathFactoryFacade pathFactoryFacade;

    DfsStreamComputationResultConsumer(PathFactoryFacade pathFactoryFacade) {
        this.pathFactoryFacade = pathFactoryFacade;
    }

    @Override
    public Stream<DfsStreamResult> consume(
        ComputationResult<DFS, HugeLongArray, DfsStreamConfig> computationResult,
        ExecutionContext executionContext
    ) {
        return runWithExceptionLogging(
            "Result streaming failed",
            executionContext.log(),
            () -> computationResult.result()
                .map(result -> TraverseStreamComputationResultConsumer.consume(
                    computationResult.config().sourceNode(),
                    result,
                    computationResult.graph()::toOriginalNodeId,
                    DfsStreamResult::new,
                    executionContext.returnColumns().contains("path"),
                    pathFactoryFacade,
                    DfsStreamProc.NEXT,
                    executionContext.nodeLookup()
                )).orElseGet(Stream::empty)
        );
    }
}
