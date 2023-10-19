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
package org.neo4j.gds.projection;

import com.neo4j.gds.internal.CustomProceduresUtil;
import org.neo4j.gds.annotation.CustomProcedure;
import org.neo4j.gds.api.DatabaseId;
import org.neo4j.gds.compat.CompatUserAggregationFunction;
import org.neo4j.gds.compat.CompatUserAggregator;
import org.neo4j.gds.compat.Neo4jProxy;
import org.neo4j.gds.core.Username;
import org.neo4j.gds.core.loading.Capabilities.WriteMode;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.internal.kernel.api.exceptions.ProcedureException;
import org.neo4j.internal.kernel.api.procs.FieldSignature;
import org.neo4j.internal.kernel.api.procs.Neo4jTypes;
import org.neo4j.internal.kernel.api.procs.QualifiedName;
import org.neo4j.internal.kernel.api.procs.UserFunctionSignature;
import org.neo4j.kernel.api.procedure.Context;
import org.neo4j.procedure.Name;
import org.neo4j.values.AnyValue;
import org.neo4j.values.storable.TextValue;

import java.util.List;
import java.util.Optional;

import static org.neo4j.internal.kernel.api.procs.DefaultParameterValue.nullValue;

public class CypherAggregation implements CompatUserAggregationFunction {

    // NOTE: keep in sync with `procedureSyntax`
    static final QualifiedName FUNCTION_NAME = new QualifiedName(
        new String[]{"gds", "graph"},
        "project"
    );

    // NOTE: keep in sync with `procedureSyntax`
    @Override
    public UserFunctionSignature signature() {
        return Neo4jProxy.userFunctionSignature(
            FUNCTION_NAME,
            // input signature:
            List.of(
                // @Name("graphName") TextValue graphName
                FieldSignature.inputField("graphName", Neo4jTypes.NTString),
                // @Name("sourceNode") AnyValue sourceNode
                FieldSignature.inputField("sourceNode", Neo4jTypes.NTAny),
                // @Name(value = "targetNode", defaultValue = "null") AnyValue targetNode
                FieldSignature.inputField("targetNode", Neo4jTypes.NTAny, nullValue(Neo4jTypes.NTAny)),
                // @Name(value = "dataConfig", defaultValue = "null") AnyValue dataConfig
                FieldSignature.inputField("dataConfig", Neo4jTypes.NTAny, nullValue(Neo4jTypes.NTAny)),
                // @Name(value = "configuration", defaultValue = "null") AnyValue config
                FieldSignature.inputField("configuration", Neo4jTypes.NTAny, nullValue(Neo4jTypes.NTAny)),
                // @Name(value = "alphaMigrationConfig", defaultValue = "null") AnyValue alphaMigrationConfig
                FieldSignature.inputField("alphaMigrationConfig", Neo4jTypes.NTAny, nullValue(Neo4jTypes.NTAny))
            ),
            // output type: Map
            Neo4jTypes.NTMap,
            // function description
            "Creates a named graph in the catalog for use by algorithms.",
            // not internal
            false,
            // thread-safe, yes please
            true,
            // not deprecated
            Optional.empty()
        );
    }

    // NOTE: keep in sync with `FUNCTION_NAME` and `signature`
    @CustomProcedure(value = "gds.graph.project", namespace = CustomProcedure.Namespace.AGGREGATION_FUNCTION)
    public AggregationResult procedureSyntax(
        @Name("graphName") TextValue graphName,
        @Name("sourceNode") AnyValue sourceNode,
        @Name("targetNode") AnyValue targetNode,
        @Name("dataConfig") AnyValue dataConfig,
        @Name("configuration") AnyValue config
    ) {
        throw new UnsupportedOperationException("This method is only used to document the procedure syntax.");
    }

    public static CompatUserAggregationFunction newInstance() {
        return new CypherAggregation();
    }

    @Override
    public CompatUserAggregator create(Context ctx) throws ProcedureException {
        var databaseService = CustomProceduresUtil.lookupSafeComponentProvider(ctx, GraphDatabaseService.class);
        var username = CustomProceduresUtil.lookupSafeComponentProvider(ctx, Username.class);

        var runsOnCompositeDatabase = Neo4jProxy.isCompositeDatabase(databaseService);
        var writeMode = runsOnCompositeDatabase
            ? WriteMode.NONE
            : WriteMode.LOCAL;

        return new ProductGraphAggregator(
            DatabaseId.of(databaseService),
            username.username(),
            writeMode
        );
    }
}
