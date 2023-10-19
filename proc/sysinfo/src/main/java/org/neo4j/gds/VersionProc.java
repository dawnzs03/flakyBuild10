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

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Procedure;

import java.util.stream.Stream;

public class VersionProc {

    @Procedure(value = "gds.version", mode = Mode.READ)
    @Description("CALL gds.version() | Return the installed graph data science library version.")
    public Stream<GdsVersion> version() {
        return Stream.of(new GdsVersion(BuildInfoProperties.require().gdsVersion()));
    }

    public static class GdsVersion {

        public String gdsVersion;

        GdsVersion(String gdsVersion) {
            this.gdsVersion = gdsVersion;
        }
    }
}
