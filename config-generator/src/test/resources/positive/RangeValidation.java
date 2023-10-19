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
package positive;

import org.neo4j.gds.annotation.Configuration;
import java.util.List;
import java.util.Optional;

@Configuration("RangeValidationConfig")
public interface RangeValidation {

    @Configuration.IntegerRange(min = 21, max = 42, minInclusive = false, maxInclusive = true)
    int integerWithinRange();

    @Configuration.LongRange(min = 21, max = 42, minInclusive = false, maxInclusive = true)
    long longWithinRange();

    @Configuration.DoubleRange(min = 21.0, max = 42.0, minInclusive = false, maxInclusive = true)
    double doubleWithinRange();

    @Configuration.DoubleRange(min = 21.0, max = 42.0, minInclusive = false, maxInclusive = true)
    Optional<Double> maybeDoubleWithinRange();

    @Configuration.DoubleRange(min = 21.0, max = 42.0, minInclusive = false, maxInclusive = true)
    List<Double> listDoubleWithinRange();
}
