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
package org.neo4j.gds.paths.steiner;

import org.neo4j.gds.BaseProc;
import org.neo4j.gds.executor.ProcedureExecutor;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Internal;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.gds.paths.steiner.Constants.DESCRIPTION;
import static org.neo4j.procedure.Mode.READ;

public class SteinerTreeStreamProc extends BaseProc {

    @Procedure(value = "gds.steinerTree.stream", mode = READ)
    @Description(DESCRIPTION)
    public Stream<StreamResult> spanningTree(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return new ProcedureExecutor<>(
            new SteinerTreeStreamSpec(),
            executionContext()
        ).compute(graphName, configuration);
    }

    @Deprecated
    @Procedure(value = "gds.beta.steinerTree.stream", mode = READ, deprecatedBy = "gds.steinerTree.stream")
    @Description(DESCRIPTION)
    @Internal
    public Stream<StreamResult> spanningTreeBeta(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        executionContext()
            .log()
            .warn("Procedure `gds.beta.steinerTree.stream` has been deprecated, please use `gds.steinerTree.stream`.");
        return spanningTree(graphName, configuration);
    }

}
