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
package org.neo4j.gds.extension;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.gds.QueryRunner;
import org.neo4j.gds.TestSupport;
import org.neo4j.gds.compat.GraphDatabaseApiProxy;
import org.neo4j.gds.compat.Neo4jProxy;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.internal.id.IdGeneratorFactory;
import org.neo4j.kernel.impl.core.NodeEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static org.neo4j.gds.extension.ExtensionUtil.injectInstance;
import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

public class Neo4jSupportExtension implements BeforeEachCallback {

    private static final String RETURN_STATEMENT = "RETURN *";

    // taken from org.neo4j.test.extension.DbmsSupportController
    private static final ExtensionContext.Namespace DBMS_NAMESPACE = ExtensionContext.Namespace.create(
        "org",
        "neo4j",
        "dbms"
    );

    // taken from org.neo4j.test.extension.DbmsSupportController
    private static final String DBMS_KEY = "service";

    @Override
    public void beforeEach(ExtensionContext context) {
        GraphDatabaseService db = getDbms(context)
            .map(dbms -> dbms.database(GraphDatabaseSettings.DEFAULT_DATABASE_NAME))
            .orElseThrow(() -> new IllegalStateException("No database was found."));

        Class<?> requiredTestClass = context.getRequiredTestClass();
        Optional<Pair<String, Boolean>> createQuery = createQueryAndIdOffset(requiredTestClass);
        var idFunctions = neo4jGraphSetup(db, createQuery);
        injectFields(context, db, idFunctions);
    }

    private Optional<DatabaseManagementService> getDbms(ExtensionContext context) {
        return Optional.ofNullable(context.getStore(DBMS_NAMESPACE).get(DBMS_KEY, DatabaseManagementService.class));
    }

    private Optional<Pair<String, Boolean>> createQueryAndIdOffset(Class<?> testClass) {
        return Stream.<Class<?>>iterate(testClass, c -> c.getSuperclass() != null, Class::getSuperclass)
            .flatMap(clazz -> stream(clazz.getDeclaredFields()))
            .filter(field -> field.isAnnotationPresent(Neo4jGraph.class))
            .findFirst()
            .map(field -> Pair.of(
                ExtensionUtil.getStringValueOfField(field),
                field.getAnnotation(Neo4jGraph.class).offsetIds()
            ));
    }

    private IdFunctions neo4jGraphSetup(GraphDatabaseService db, Optional<Pair<String, Boolean>> createQueryAndOffset) {
        offsetNodeIds(db, createQueryAndOffset.map(Pair::getRight).orElse(false));

        return createQueryAndOffset
            .map(Pair::getLeft)
            .map(query -> formatWithLocale("%s %s", query, RETURN_STATEMENT))
            .map(query -> QueryRunner.runQuery(db, query, Neo4jSupportExtension::extractVariableIds))
            .orElse(IdFunctions.EMPTY);
    }

    private static IdFunctions extractVariableIds(Result result) {
        if (!result.hasNext()) {
            throw new IllegalArgumentException("Result of create query was empty");
        }
        List<String> columns = result.columns();
        Map<String, Object> row = result.next();

        Map<String, Node> idMap = new HashMap<>();
        Map<Long, String> variableToIddMap = new HashMap<>();
        columns.forEach(column -> {
            Object value = row.get(column);
            if (value instanceof NodeEntity) {
                idMap.put(column, (NodeEntity) value);
                variableToIddMap.put(((NodeEntity) value).getId(), column);
            }
        });

        return new IdFunctions(variableToIddMap, idMap);
    }

    private void offsetNodeIds(GraphDatabaseService db, boolean offsetIds) {
        if (!offsetIds) {
            return;
        }

        // try to convince the db that `idOffset` number of nodes have already been allocated
        var idGeneratorFactory = GraphDatabaseApiProxy.resolveDependency(db, IdGeneratorFactory.class);
        TestSupport.fullAccessTransaction(db).accept((tx, ktx) -> Neo4jProxy.reserveNeo4jIds(idGeneratorFactory, 42, ktx.cursorContext()));
    }

    private void injectFields(ExtensionContext context, GraphDatabaseService db, IdFunctions idFunctions) {
        NodeFunction nodeFunction = idFunctions.variableToId::get;
        IdFunction idFunction = variable -> nodeFunction.of(variable).getId();
        context.getRequiredTestInstances().getAllInstances().forEach(testInstance -> {
            injectInstance(testInstance, nodeFunction, NodeFunction.class);
            injectInstance(testInstance, idFunction, IdFunction.class);
            injectInstance(testInstance, idFunctions.idToVariable::get, IdToVariable.class);
            injectInstance(testInstance, db, GraphDatabaseService.class);
        });
    }

    // Inverse Id mapping
    private static class IdFunctions {

        static final IdFunctions EMPTY = new IdFunctions(Map.of(), Map.of());
        final Map<Long, String> idToVariable;
        final Map<String, Node> variableToId;

        IdFunctions(Map<Long, String> idToVariable, Map<String, Node> variableToId) {
            this.idToVariable = idToVariable;
            this.variableToId = variableToId;
        }
    }
}
