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

import org.neo4j.values.AnyValue;
import org.neo4j.values.SequenceValue;
import org.neo4j.values.ValueMapper;
import org.neo4j.values.storable.BooleanValue;
import org.neo4j.values.storable.DateTimeValue;
import org.neo4j.values.storable.DateValue;
import org.neo4j.values.storable.DurationValue;
import org.neo4j.values.storable.LocalDateTimeValue;
import org.neo4j.values.storable.LocalTimeValue;
import org.neo4j.values.storable.NoValue;
import org.neo4j.values.storable.NumberValue;
import org.neo4j.values.storable.PointValue;
import org.neo4j.values.storable.TextValue;
import org.neo4j.values.storable.TimeValue;
import org.neo4j.values.virtual.MapValue;
import org.neo4j.values.virtual.VirtualNodeValue;
import org.neo4j.values.virtual.VirtualPathValue;
import org.neo4j.values.virtual.VirtualRelationshipValue;

interface PartialValueMapper<R> extends ValueMapper<R> {
    R unsupported(AnyValue value);

    @Override
    R mapSequence(SequenceValue value);

    @Override
    default R mapPath(VirtualPathValue value) {
        return unsupported(value);
    }

    @Override
    default R mapNode(VirtualNodeValue value) {
        return unsupported(value);
    }

    @Override
    default R mapRelationship(VirtualRelationshipValue value) {
        return unsupported(value);
    }

    @Override
    default R mapMap(MapValue value) {
        return unsupported(value);
    }

    @Override
    default R mapNoValue() {
        return unsupported(NoValue.NO_VALUE);
    }

    @Override
    default R mapText(TextValue value) {
        return unsupported(value);
    }

    @Override
    default R mapBoolean(BooleanValue value) {
        return unsupported(value);
    }

    @Override
    default R mapNumber(NumberValue value) {
        return unsupported(value);
    }

    @Override
    default R mapDateTime(DateTimeValue value) {
        return unsupported(value);
    }

    @Override
    default R mapLocalDateTime(LocalDateTimeValue value) {
        return unsupported(value);
    }

    @Override
    default R mapDate(DateValue value) {
        return unsupported(value);
    }

    @Override
    default R mapTime(TimeValue value) {
        return unsupported(value);
    }

    @Override
    default R mapLocalTime(LocalTimeValue value) {
        return unsupported(value);
    }

    @Override
    default R mapDuration(DurationValue value) {
        return unsupported(value);
    }

    @Override
    default R mapPoint(PointValue value) {
        return unsupported(value);
    }
}
