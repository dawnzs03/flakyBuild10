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
package org.neo4j.gds.doc.syntax;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.neo4j.gds.doc.PartialsIncludeProcessor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.asciidoctor.Asciidoctor.Factory.create;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
public abstract class SyntaxTestBase {

    private static final Path ASCIIDOC_PATH = Paths.get("build/doc-sources/modules/ROOT");

    private Asciidoctor asciidoctor;

    @BeforeEach
    void setUp() {
        asciidoctor = create();
    }

    @AfterEach
    void tearDown() {
        asciidoctor.shutdown();
    }

    @Test
    void runSyntaxTest(SoftAssertions softAssertions, @TempDir File outputDirectory) {
        asciidoctor.javaExtensionRegistry()
            .includeProcessor(new PartialsIncludeProcessor())
            .postprocessor(syntaxPostProcessor(softAssertions));

        var docFile = adocPath().toFile();
        assertThat(docFile).exists().canRead();

        var options = OptionsBuilder.options()
            .baseDir(ASCIIDOC_PATH.toFile()) // Make sure to set the `baseDir` it is read in `PartialsIncludeProcessor`
            .toDir(outputDirectory) // Make sure we don't write anything in the project.
            .safe(SafeMode.UNSAFE); // By default we are forced to use relative path which we don't want.

        asciidoctor.convertFile(docFile, options);
    }

    protected Iterable<SyntaxModeMeta> syntaxModes() {
        return List.of(
            SyntaxModeMeta.of(SyntaxMode.STREAM),
            SyntaxModeMeta.of(SyntaxMode.STATS),
            SyntaxModeMeta.of(SyntaxMode.MUTATE),
            SyntaxModeMeta.of(SyntaxMode.WRITE)
        );
    }

    Path adocPath() {
        return ASCIIDOC_PATH.resolve(adocFile());
    }

    protected abstract String adocFile();

    protected List<String> procedurePackages() {
        return List.of("org.neo4j.gds");
    }

    private ProcedureSyntaxAutoChecker syntaxPostProcessor(SoftAssertions syntaxAssertions) {
        var procedureLookup = ProcedureLookup.forPackages(procedurePackages());
        return new ProcedureSyntaxAutoChecker(syntaxModes(), syntaxAssertions, procedureLookup);
    }
}
