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

import org.jetbrains.annotations.Nullable;
import org.neo4j.gds.annotation.Configuration;

@Configuration("NullableFieldsConfig")
public interface NullableFields {

    @Nullable String nullableRequiredField();

    default @Nullable String nullableDefaultField() {
        return null;
    }

    @Configuration.ConvertWith(method = "emptyToNull")
    default @Nullable String conversionCanReturnNull() {
        return null;
    }

    static String emptyToNull(String input) {
        return input == null || input.isEmpty() ? null : input;
    }
}
