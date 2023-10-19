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
package org.neo4j.gds.pipeline.catalog;

import org.neo4j.gds.ml.pipeline.PipelineCatalog;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.stream.Stream;

import static org.neo4j.procedure.Mode.READ;

public class PipelineExistsProc extends PipelineCatalogProc {

    private static final String DESCRIPTION = "Checks if a given pipeline exists in the pipeline catalog.";

    @Procedure(name = "gds.beta.pipeline.exists", mode = READ)
    @Description(DESCRIPTION)
    public Stream<PipelineExistsResult> exists(@Name(value = "pipelineName") String pipelineName) {
        validatePipelineName(pipelineName);

        String type;
        boolean exists;

        if (PipelineCatalog.exists(username(), pipelineName)) {
            exists = true;
            type = PipelineCatalog.get(username(), pipelineName).type();
        } else {
            exists = false;
            type = "n/a";
        }
        return Stream.of(new PipelineExistsResult(
            pipelineName,
            type,
            exists
        ));
    }

    @SuppressWarnings("unused")
    public static class PipelineExistsResult {
        public final String pipelineName;
        public final String pipelineType;
        public final boolean exists;

        PipelineExistsResult(String pipelineName, String pipelineType, boolean exists) {
            this.pipelineName = pipelineName;
            this.pipelineType = pipelineType;
            this.exists = exists;
        }
    }
}
