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
package org.neo4j.gds.k1coloring;

import com.carrotsearch.hppc.BitSet;
import org.neo4j.gds.annotation.ValueClass;
import org.neo4j.gds.core.utils.paged.HugeLongArray;


@ValueClass
@SuppressWarnings("immutables:subtype")
public interface K1ColoringResult {

        HugeLongArray colors();

        long ranIterations();

        boolean didConverge();

        default BitSet usedColors(){

                var colors=colors();
                var nodeCount=colors.size();
                var usedColors = new BitSet(nodeCount);

                for (long u=0;u<nodeCount;++u) {

                        usedColors.set(colors.get(u));
                }

                return usedColors;

        }

        static K1ColoringResult of(
            HugeLongArray color,
            long ranIterations,
            boolean didConverge
        ) {

            return ImmutableK1ColoringResult.of(
                color,
                ranIterations,
                didConverge
            );
        }

    }

