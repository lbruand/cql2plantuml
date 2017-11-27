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

import java.io.Writer

import troy.cql.ast.DataType.{Custom, Tuple, UserDefined}
import troy.cql.ast.{DataType, _}

class CQL2Puml {

  final def toStrDatatype(dt : DataType) : String = {
    dt match {
      case UserDefined(ks, identifier) => s"${identifier}"
      case Custom(javaClass : String) => javaClass
      case Tuple(ts) => ts.mkString("tuple<", ", ", ">")
      case DataType.Set(t) => "set<" + toStrDatatype(t) + ">"
      case DataType.List(t) => "list<" + toStrDatatype(t) + ">"
      case DataType.Frozen(t) => "frozen<" + toStrDatatype(t) + ">"
      case DataType.Map(k, v) => "map<" + toStrDatatype(k) + ", "+ toStrDatatype(v) +">"
      case _ => dt.toString
    }
  }

  def output(c: CreateTable, out: Writer): Unit = {
    out.write(s"""Table(${c.tableName.table}, "${c.tableName.table}") {\n""")
    c.primaryKey match {
      case Some(s) => {
          s.partitionKeys.foreach(s => {
            out.write(s"  partition_key(${s} : ${obtainDataTypeForColumnName(c, s)})\n")
          })
          s.clusteringColumns.foreach(s => {
            out.write(s"  clustering_key(${s} : ${obtainDataTypeForColumnName(c, s)})\n")
          })
          out.write("  ---\n")
      }
      case None => // Nothing
    }

    val pks : Set[String] = c.primaryKey.map(pk => (pk.partitionKeys ++ pk.clusteringColumns)).getOrElse(List.empty[String]).toSet

    c.columns.filter(x => !pks.contains(x.name)).foreach(c => {
      if (c.isPrimaryKey) {
        out.write(s"  partition_key(${c.name} : ${toStrDatatype(c.dataType)})\n")
      } else {
        out.write(s"  ${c.name} : ${toStrDatatype(c.dataType)}\n")
      }

    })
    out.write("}\n\n")
  }

  private def obtainDataTypeForColumnName(c: CreateTable, s: String) :String =
    c.columns.find(_.name.equals(s)).map(x => toStrDatatype(x.dataType)).getOrElse("")


  def output(k: CreateKeyspace, out: Writer): Unit = {
    // Nothing
  }

  def output(i: CreateIndex, out: Writer): Unit = {
    // Nothing
  }

  def output(t: CreateType, out: Writer): Unit = {
    out.write(s"""UDT(${t.typeName.name}, "${t.typeName.name}") {\n""")

    t.fields.foreach(f => {
      out.write(s"  ${f.fieldName} : ${toStrDatatype(f.fieldType)}\n")
    })
    out.write("}\n\n")
  }

  def output(comment: PassThruComment, out: Writer): Unit = {
    out.write(comment.comment)
    out.write("\n")
  }

  def output(seq : Seq[DataDefinition], out : Writer): Unit = {
    out.write("""
      |@startuml
      |' uncomment the line below if you're using computer with a retina display
      |' skinparam dpi 300
      |!define Table(name,desc) class name as "desc" << (T,#FFAAAA) >>
      |!define UDT(name,desc) class name as "desc" << (U,#AAFFAA) >>
      |' we use bold for primary key
      |' green color for unique
      |' and underscore for not_null
      |!define primary_key(x) <b>x</b>
      |!define partition_key(x) <b>x</b>
      |!define clustering_key(x) <i>x</i>
      |!define primary_key(x) <b>x</b>
      |!define unique(x) <color:green>x</color>
      |!define not_null(x) <u>x</u>
      |' other tags available:
      |' <i></i>
      |' <back:COLOR></color>, where color is a color name or html color code
      |' (#FFAACC)
      |' see: http://plantuml.com/classes.html#More
      |hide methods
      |hide stereotypes
      |
      |' entities
      |""".stripMargin)
     seq.foreach( _ match {
         case c: CreateTable => output(c, out)
         case k: CreateKeyspace => output(k, out)
         case i: CreateIndex => output(i, out)
         case t: CreateType => output(t, out)
         case comment : PassThruComment => output(comment, out)
         case _ => //nothing
     })//output(_, out))

    out.write("""
                |@enduml
              """.stripMargin)
  }
}
