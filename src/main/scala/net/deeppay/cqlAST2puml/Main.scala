/*
 * Copyright 2016 Deeppay
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.deeppay.cqlAST2puml

import java.io.{File, FileWriter}

import net.deeppay.cqlparser.CQLSchemaParser
import troy.cql.ast.DataDefinition

object Main extends App {
  def autoClose[A <: AutoCloseable,B](closeable: A)(fun: (A) ⇒ B): B = {
    try {
      fun(closeable)
    } finally {
      closeable.close()
    }
  }
  override def main(args: Array[String]) {

    // TODO use scopt here.
    if (args.length == 0) {
      usage()
    } else {
      val inputFilename : String = args(0)
      val outputFilename : String = if (args.length > 1) {
        args(1)
      } else {
        if (inputFilename.contains(".")) {
          inputFilename.split("\\.").dropRight(1).mkString(".") + ".puml"
        } else {
          inputFilename + ".puml"
        }
      }
      println(s" $inputFilename $outputFilename\n")
      val source = scala.io.Source.fromFile(inputFilename)
      val input  = source.mkString
      val result: CQLSchemaParser.ParseResult[Seq[DataDefinition]] = CQLSchemaParser.parseSchema(input)

      val cQL2Puml = new CQL2Puml()
      autoClose(new FileWriter(new File(outputFilename))) (writer =>{//new StringWriter()
        result match {
          case CQLSchemaParser.Success(seq, _) => {
            cQL2Puml.output(seq, writer)
          }
          case _ => {
            println("Error while parsing: " + result)
          }
        }
      })

    }
  }

  private def usage() = {
    println("CQLAST2puml <input file.cql> <outputfile.puml>")
  }
}
