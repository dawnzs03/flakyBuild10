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

public interface Inheritance {
    public interface BaseConfig {
        double inheritedValue();

        default short inheritedDefaultValue() {
            return 42;
        }

        int overriddenValue();

        default long overwrittenValue() {
            return 42;
        }

        @Configuration.Ignore
        default long ignoredValue() {
            return 88;
        }

        @Configuration.Ignore
        default int ignoredInBaseValue() {
            return 111;
        }
    }

    @Configuration("MyConfigImpl")
    public interface MyConfig extends BaseConfig {
        String baseValue();

        @Override
        default int overriddenValue() {
            return 1337;
        }


        @Override
        default long overwrittenValue() {
            return 1337;
        }

        @Override
        @Configuration.Ignore
        default long ignoredValue() {
            return 1337;
        }

        @Override
        @Configuration.Key("myKey")
        int ignoredInBaseValue();
    }
}
