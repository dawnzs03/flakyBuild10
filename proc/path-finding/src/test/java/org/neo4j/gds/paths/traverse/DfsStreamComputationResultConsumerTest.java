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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.NodeLookup;
import org.neo4j.gds.api.ProcedureReturnColumns;
import org.neo4j.gds.core.utils.paged.HugeLongArray;
import org.neo4j.gds.executor.ComputationResult;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.graphdb.Path;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DfsStreamComputationResultConsumerTest {

    @Mock ComputationResult<DFS, HugeLongArray, DfsStreamConfig> computationResultMock;
    @Mock ExecutionContext executionContextMock;
    @Mock DfsStreamConfig configMock;
    @Mock Graph graphMock;
    @Mock PathFactoryFacade pathFactoryFacadeMock;

    @Test
    void shouldNotComputePath() {
        when(graphMock.toOriginalNodeId(anyLong())).then(returnsFirstArg());

        when(computationResultMock.graph()).thenReturn(graphMock);
        when(computationResultMock.result()).thenReturn(Optional.of(HugeLongArray.of(1L, 2L)));

        when(configMock.sourceNode()).thenReturn(0L);
        when(computationResultMock.config()).thenReturn(configMock);

        doReturn(ProcedureReturnColumns.EMPTY).when(executionContextMock).returnColumns();
        doReturn(NodeLookup.EMPTY).when(executionContextMock).nodeLookup();

        var consumer = new DfsStreamComputationResultConsumer(pathFactoryFacadeMock);

        var actual = consumer.consume(computationResultMock, executionContextMock);

        verifyNoInteractions(pathFactoryFacadeMock);

        assertThat(actual)
            .hasSize(1)
            .satisfiesExactly(resultRow -> {
                assertThat(resultRow.path).isNull();
                assertThat(resultRow.sourceNode).isEqualTo(0L);
                assertThat(resultRow.nodeIds).containsExactly(1L, 2L);
            });
    }

    @Test
    void shouldComputePath() {
        when(graphMock.toOriginalNodeId(anyLong())).then(returnsFirstArg());

        when(computationResultMock.graph()).thenReturn(graphMock);
        when(computationResultMock.result()).thenReturn(Optional.of(HugeLongArray.of(1L, 2L)));

        when(configMock.sourceNode()).thenReturn(0L);
        when(computationResultMock.config()).thenReturn(configMock);

        when(executionContextMock.returnColumns()).thenReturn(mock(ProcedureReturnColumns.class));
        when(executionContextMock.returnColumns().contains("path")).thenReturn(true);
        doReturn(NodeLookup.EMPTY).when(executionContextMock).nodeLookup();

        doReturn(mock(Path.class)).when(pathFactoryFacadeMock).createPath(any(), any(), any());

        var consumer = new DfsStreamComputationResultConsumer(pathFactoryFacadeMock);

        var actual = consumer.consume(computationResultMock, executionContextMock);
        assertThat(actual)
            .hasSize(1)
            .satisfiesExactly(resultRow -> {
                assertThat(resultRow.path).isNotNull();
                assertThat(resultRow.sourceNode).isEqualTo(0L);
                assertThat(resultRow.nodeIds).containsExactly(1L, 2L);
            });
    }
}
