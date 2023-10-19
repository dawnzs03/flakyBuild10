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
import org.neo4j.gds.core.model.ModelCatalog;
import org.neo4j.gds.degree.DegreeCentralityMutateProc;
import org.neo4j.gds.embeddings.graphsage.GraphSageMutateProc;
import org.neo4j.gds.embeddings.graphsage.GraphSageStreamProc;
import org.neo4j.gds.embeddings.graphsage.GraphSageTrainProc;
import org.neo4j.gds.embeddings.graphsage.GraphSageWriteProc;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.extension.Neo4jModelCatalogExtension;

import java.util.List;

@Neo4jModelCatalogExtension
class GraphSageDocTest extends SingleFileDocTestBase {

    @Inject
    ModelCatalog modelCatalog;

    @Override
    protected List<Class<?>> procedures() {
        return List.of(
            GraphProjectProc.class,
            GraphSageTrainProc.class,
            GraphSageStreamProc.class,
            GraphSageMutateProc.class,
            GraphSageWriteProc.class,
            DegreeCentralityMutateProc.class
        );
    }

    @Override
    protected String adocFile() {
        return "pages/machine-learning/node-embeddings/graph-sage.adoc";
    }

    @Override
    protected Runnable cleanup() {
        return () -> {
            GraphStoreCatalog.removeAllLoadedGraphs();
            modelCatalog.removeAllLoadedModels();
        };
    }
}
