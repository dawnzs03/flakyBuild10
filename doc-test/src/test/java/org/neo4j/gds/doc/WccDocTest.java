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
package org.neo4j.gds.doc;

import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.gds.core.loading.GraphStoreCatalog;
import org.neo4j.gds.functions.AsNodeFunc;
import org.neo4j.gds.wcc.WccMutateProc;
import org.neo4j.gds.wcc.WccStatsProc;
import org.neo4j.gds.wcc.WccStreamProc;
import org.neo4j.gds.wcc.WccWriteProc;

import java.util.List;

class WccDocTest extends SingleFileDocTestBase {

    @Override
    protected List<Class<?>> functions() {
        return List.of(AsNodeFunc.class);
    }

    @Override
    protected List<Class<?>> procedures() {
        return List.of(
            WccStreamProc.class,
            WccWriteProc.class,
            WccMutateProc.class,
            WccStatsProc.class,
            GraphProjectProc.class
        );
    }

    @Override
    boolean setupNeo4jGraphPerTest() {
        return true;
    }

    @Override
    Runnable cleanup() {
        return () -> {
            GraphStoreCatalog.removeAllLoadedGraphs();
            db.executeTransactionally("MATCH (n) DETACH DELETE n");
        };
    }

    @Override
    protected String adocFile() {
        return "pages/algorithms/wcc.adoc";
    }

}
