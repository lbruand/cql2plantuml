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
import net.sourceforge.plantuml.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import scala.collection.Seq;
import scala.io.BufferedSource;
import scala.io.Source;
import scala.util.parsing.combinator.Parsers;
import troy.cql.ast.DataDefinition;

import java.io.*;

import java.util.Iterator;
import java.util.List;

import net.sourceforge.plantuml.preproc.Defines;

import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;

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
    private final Option option = new Option();

    /**
     * Fileset to search plantuml diagrams in.
     * @parameter property="plantuml.sourceFiles"
     * @required
     * @since 7232
     */
    private FileSet sourceFiles;

    /**
     * Directory where generated images are generated.
     * @parameter property="plantuml.outputDirectory" default-value="${basedir}/target/plantuml"
     * @required
     */
    private File outputDirectory;

    /**
     * Whether or not to generate images in same directory as the source file.
     * This is useful for using PlantUML diagrams in Javadoc,
     * as described here:
     * <a href="http://plantuml.sourceforge.net/javadoc.html">http://plantuml.sourceforge.net/javadoc.html</a>.
     *
     * If this is set to true then outputDirectory is ignored.
     * @parameter property="plantuml.outputInSourceDirectory" default-value="false"
     */
    private boolean outputInSourceDirectory;

    /**
     * Charset used during generation.
     * @parameter property="plantuml.charset"
     */
    private String charset;

    /**
     * External configuration file location.
     * @parameter property="plantuml.config"
     */
    private String config;

    /**
     * Wether or not to keep tmp files after generation.
     * @parameter property="plantuml.keepTmpFiles" default-value="false"
     */
    private boolean keepTmpFiles;

    /**
     * Specify output format. Supported values: xmi, xmi:argo, xmi:start, eps,  pdf, eps:txt, svg, png, dot, txt and utxt.
     * @parameter property="plantuml.format"
     */
    private String format;

    /**
     * Fully qualified path to Graphviz home directory.
     * @parameter property="plantuml.graphvizDot"
     */
    private String graphvizDot;

    /**
     * Wether or not to output details during generation.
     * @parameter property="plantuml.verbose" default-value="false"
     */
    private boolean verbose;

    /**
     * Specify to include metadata in the output files.
     * @parameter property="plantuml.withMetadata"
     * @since 1.3
     */
    private boolean withMetadata = false;

    /**
     * Specify to overwrite any output file, also if the target file is newer as the input file.
     * @parameter property="plantuml.overwrite"
     * @since 1.3
     */
    private boolean overwrite = false;

    /**
     * Specify whether to also write the output puml file
     * @parameter property="plantuml.outputPuml"
     */
    private boolean outputPuml = false;

    protected final void setFormat(final String format) {
        if ("xmi".equalsIgnoreCase(format)) {
            this.option.setFileFormat(FileFormat.XMI_STANDARD);
        } else if ("xmi:argo".equalsIgnoreCase(format)) {
            this.option.setFileFormat(FileFormat.XMI_ARGO);
        } else if ("xmi:start".equalsIgnoreCase(format)) {
            this.option.setFileFormat(FileFormat.XMI_STAR);
        } else if ("eps".equalsIgnoreCase(format)) {
            this.option.setFileFormat(FileFormat.EPS);
        } else if ("eps:txt".equalsIgnoreCase(format)) {
            this.option.setFileFormat(FileFormat.EPS_TEXT);
        } else if ("svg".equalsIgnoreCase(format)) {
            this.option.setFileFormat(FileFormat.SVG);
        } else if ("txt".equalsIgnoreCase(format)) {
            this.option.setFileFormat(FileFormat.ATXT);
        } else if ("utxt".equalsIgnoreCase(format)) {
            this.option.setFileFormat(FileFormat.UTXT);
        } else if ("png".equalsIgnoreCase(format)) {
            this.option.setFileFormat(FileFormat.PNG);
        } else if ("pdf".equalsIgnoreCase(format)) {
            this.option.setFileFormat(FileFormat.PDF);
        } else {
            throw new IllegalArgumentException("Unrecognized format <"+format+">");
        }
    }

    @Override
    public void execute() throws MojoExecutionException {
        // early exit if sourceFiles directory is not available
        final String invalidSourceFilesDirectoryWarnMsg = this.sourceFiles.getDirectory() + " is not a valid path";
        if( null == this.sourceFiles.getDirectory() || this.sourceFiles.getDirectory().isEmpty()) {
            getLog().warn(invalidSourceFilesDirectoryWarnMsg);
            return;
        }
        File baseDir = null;
        try {
            baseDir = new File(this.sourceFiles.getDirectory());
        } catch (Exception e) {
            getLog().debug(invalidSourceFilesDirectoryWarnMsg, e);
        }
        if( null == baseDir || !baseDir.exists() || !baseDir.isDirectory()) {
            getLog().warn(invalidSourceFilesDirectoryWarnMsg);
            return;
        }
        if (!this.outputInSourceDirectory) {
            if (!this.outputDirectory.exists()) {
                // If output directoy does not exist yet create it.
                this.outputDirectory.mkdirs();
            }
            if (!this.outputDirectory.isDirectory()) {
                throw new IllegalArgumentException("<" + this.outputDirectory + "> is not a valid directory.");
            }
        }

        try {
            if (!this.outputInSourceDirectory) {
                this.option.setOutputDir(this.outputDirectory);
            }
            if (this.charset != null) {
                this.option.setCharset(this.charset);
            }
            if (this.config != null) {
                this.option.initConfig(this.config);
            }
            /*if (this.keepTmpFiles) {
                //OptionFlags.getInstance().setKeepTmpFiles(this.keepTmpFiles);
            }*/
            if (this.graphvizDot != null) {
                OptionFlags.getInstance().setDotExecutable(this.graphvizDot);
            }
            if (this.format != null) {
                setFormat(this.format);
            }
            if (this.verbose) {
                OptionFlags.getInstance().setVerbose(true);
            }

            final List<File> files = FileUtils.getFiles(
                    baseDir,
                    getCommaSeparatedList(this.sourceFiles.getIncludes()),
                    getCommaSeparatedList(this.sourceFiles.getExcludes())
            );
            for(final File file : files) {
                File outDir;
                if (this.outputInSourceDirectory) {
                    outDir = file.getParentFile();
                } else {
                    outDir = outputDirectory.toPath().resolve(
                            baseDir.toPath().relativize(file.toPath().getParent())).toFile();
                }
                this.option.setOutputDir(outDir);

                FileFormatOption fileFormatOption = getFileFormatOption();
                if (!overwrite){
                    String newName = fileFormatOption.getFileFormat().changeName(file.getName(), 0);
                    File targetFile = new File(outDir, newName);
                    if (targetFile.exists() && targetFile.lastModified() > file.lastModified()){
                        getLog().debug("Skip file <"+file+"> because target <"+targetFile+"> is newer");
                        continue;
                    }
                }

                getLog().info("Processing file <"+file+">");
                List<GeneratedImage> generatedImages;
                if (file.getName().endsWith(".cql")) {
                    getLog().info("cqlfile : ["+file.getName()+"]");
                    StringWriter stringWriter = new StringWriter();
                    BufferedSource bufferedSource = null;
                    bufferedSource = Source.fromFile(file, "UTF-8");
                    String cql = bufferedSource.mkString();
                    Parsers.ParseResult<Seq<DataDefinition>> seqParseResult = CQLSchemaParser.parseSchema(cql);
                    CQL2Puml cql2Puml = new CQL2Puml();
                    if (seqParseResult.successful()) {
                        cql2Puml.output(seqParseResult.get(), stringWriter);
                    }
                    String puml = stringWriter.toString();
                    if (outputPuml) {
                        String fileName = file.getName();
                        String replacement = ".puml";
                        String pumlFilename = fileName.replaceAll("\\.\\w+$", replacement);
                        if (pumlFilename.equals(fileName)) {
                            pumlFilename = fileName + replacement;
                        }
                        File targetFile = new File(outDir, pumlFilename);
                        FileWriter fileWriter = null;
                        try {
                            fileWriter = new FileWriter(targetFile);
                            fileWriter.write(puml);
                        } finally {
                            try {
                                fileWriter.close();
                            } catch (IOException e) {
                                getLog().info("IOException on close", e);
                            }
                        }
                    }
                    SourceStringReader sourceStringReader = new SourceStringReader(new Defines(), puml, this.option.getCharset(), this.option.getConfig());
                    String newName = fileFormatOption.getFileFormat().changeName(file.getName(), 0);
                    File targetFile = new File(outDir, newName);
                    FileOutputStream fileOutputStream = null;
                    try {
                        fileOutputStream = new FileOutputStream(targetFile);
                        String result = sourceStringReader.generateImage(fileOutputStream, fileFormatOption);
                        getLog().debug(" result =["+result+"]");
                    } finally {
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                            } catch (IOException e) {
                                getLog().info("IOException on close", e);
                            }
                        }
                    }
                } else {
                    final SourceFileReader sourceFileReader =
                            new SourceFileReader(
                                    new Defines(), file, this.option.getOutputDir(),
                                    this.option.getConfig(), this.option.getCharset(),
                                    fileFormatOption);
                    generatedImages = sourceFileReader.getGeneratedImages();
                    for (final GeneratedImage image : generatedImages) {
                        getLog().debug(image + " " + image.getDescription());
                    }
                }

            }
        } catch (Exception e) {
            throw new MojoExecutionException("Exception during plantuml process", e);
        }
    }

    protected String getCommaSeparatedList(final List<String> list) {
        final StringBuilder builder = new StringBuilder();
        final Iterator it = list.iterator();
        while(it.hasNext()) {
            final Object object = it.next();
            builder.append(object.toString());
            if (it.hasNext()) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    private FileFormatOption getFileFormatOption() {
        FileFormatOption formatOptions = new FileFormatOption(this.option.getFileFormat(), this.withMetadata);
        if (formatOptions.isWithMetadata() != withMetadata){
            // Workarround to error in plantUML where the withMetadata flag is not correctly applied.
            return new FileFormatOption(this.option.getFileFormat());
        }
        return formatOptions;
    }

}