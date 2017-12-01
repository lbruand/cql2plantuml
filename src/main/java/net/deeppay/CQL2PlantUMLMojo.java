package net.deeppay;

/*
 * Copyright 2016 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import net.deeppay.cqlAST2puml.CQL2Puml;
import net.deeppay.cqlparser.CQLSchemaParser;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import scala.collection.Seq;
import scala.io.Source;
import scala.util.parsing.combinator.Parsers;
import troy.cql.ast.DataDefinition;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Goal which touches a timestamp file.
 *
 * @goal touch
 * 
 * @phase process-sources
 */
public class CQL2PlantUMLMojo
    extends AbstractMojo
{
    /**
     * Location of the file.
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * Location of the input file
     * @parameter
     * @throws MojoExecutionException
     */
    private File inputFile;

    /**
     * Output model name
     * @throws MojoExecutionException
     */
    private String outputModelName = "output.puml";

    public void execute()
        throws MojoExecutionException
    {
        File f = outputDirectory;

        if ( !f.exists() )
        {
            f.mkdirs();
        }

        File outputFile = new File( f, outputModelName );


        FileWriter fileWriter = null;

        Source source = null;

        try {
            fileWriter = new FileWriter(outputFile);
            source = Source.fromFile(inputFile, "UTF-8");
            String input = source.mkString();
            Parsers.ParseResult<Seq<DataDefinition>> seqParseResult = CQLSchemaParser.parseSchema(input);
            CQL2Puml cql2Puml = new CQL2Puml();
            if (seqParseResult.successful()) {
                cql2Puml.output(seqParseResult.get(), fileWriter);
            }
        } catch (IOException e) {
            getLog().error("error", e);
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    getLog().info("IOException", e);
                }
            }
            if (source != null) {
                source.close();
            }
        }

    }
}
