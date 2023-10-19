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
package org.neo4j.gds.core;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Pattern;

public final class StringIdentifierValidations {
    private static final Pattern LEADING_OR_TRAILING_WHITESPACES_PATTERN = Pattern.compile("(^\\s)|(\\s$)");

    private StringIdentifierValidations() {}

    public static @Nullable String validateNoWhiteCharacter(@Nullable String input, String parameterName) {
        if (input != null && LEADING_OR_TRAILING_WHITESPACES_PATTERN.matcher(input).find()) {
            throw new IllegalArgumentException(String.format(Locale.ENGLISH, "`%s` must not end or begin with whitespace characters, but got `%s`.", parameterName, input));
        }

        return input;
    }

    public static String emptyToNull(String input) {
        return input == null || input.isEmpty() ? null : input;
    }
}
