/*
This work is derivative work originally written by Tamer AbdulRadi, licensed under the Apache license.
It has been modified under the terms of the Apache license.

Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */
package net.deeppay.cqlparser

import java.util.UUID

import troy.cql.ast._
import troy.cql.ast.ddl.{Field, Index}
import troy.cql.ast.ddl.Index.{IndexIdentifier, Keys, Using}
import troy.cql.ast.ddl.Keyspace.{KeyspaceOption, OtherKeyspaceOption, Replication}
import troy.cql.ast.ddl.Table._
import troy.cql.ast.dml.Operator._
import troy.cql.ast.dml._

import scala.util.parsing.combinator.JavaTokenParsers


object CQLSchemaParser extends JavaTokenParsers with Helpers {

  implicit class MyRichString(val str: String) extends AnyVal {
    // Ignore case
    def i: Parser[String] = ("""(?i)\Q""" + str + """\E""").r

    def flag: Parser[Boolean] = (str.i ^^^ true) orElse false
  }


  def semicolon: Parser[Unit] = ";".? ^^^ ((): Unit)

  /*
   * <identifier> ::= any quoted or unquoted identifier, excluding reserved keywords
   */
  def constant: Parser[Constant] = {
    import Constants._

    def hex = "[0-9a-fA-F]".r

    def float = """[+-]?[0-9]*((\.[0-9]+([eE][+-]?[0-9]+)?[fF]?)|([fF])|([eE][+‌​-]?[0-9]+))\b""".r ^^ { s =>
      new FloatConstant(s.toFloat)
    }
    def nan = "NaN".r ^^^ NaN
    def infinity = "Infinity".r ^^^ Infinity
    def floats: Parser[FloatNum] = float | nan | infinity

    def str = string ^^ StringConstant
    def int = integer ^^ { s => new IntegerConstant(s.toInt) }
    def uuid = s"$hex{8}-$hex{4}-$hex{4}-$hex{4}-$hex{12}".r ^^ { s => new UuidConstant(UUID.fromString(s)) }
    def boolean = ("true".i | "false".i) ^^ { s => new BooleanConstant(s.toBoolean) }
    def blob = s"0(x|X)$hex+".r ^^ { s => new BlobConstant(s.toString) }
    def nullConst = "null".i ^^^ NullConstant

    str | blob | uuid | floats | int | boolean | nullConst
  }

  // Ignores comments
  protected override val whiteSpace = """(\s|//.*|--.*|(?m)/\*(\*(?!/)|[^*])*\*/)+""".r

  def identifier: Parser[Identifier] = "[a-zA-Z0-9_]+".r.filter(k => !Keywords.contains(k.toUpperCase))



  object Constants {

    def string = "'".r ~> """([^']|'')*""".r <~ "'" ^^ { _.replace("''", "'") }

    def integer = wholeNumber

  }

  def keyspaceName: Parser[KeyspaceName] = identifier ^^ KeyspaceName
  def functionName: Parser[FunctionName] = (keyspaceName <~ ".").? ~ identifier ^^^^ FunctionName

  /*
   * <tablename> ::= (<identifier> '.')? <identifier>
   */
  def tableName: Parser[TableName] = (keyspaceName <~ ".").? ~ identifier ^^^^ TableName

  def typeName: Parser[TypeName] = (keyspaceName <~ ".").? ~ identifier ^^^^ TypeName

  def ifNotExists: Parser[Boolean] = "if not exists".flag






  def operator: Parser[Operator] = {
    def eq = "=".r ^^^ Equals
    def lt = "<".r ^^^ LessThan
    def gt = ">".r ^^^ GreaterThan
    def lte = "<=".r ^^^ LessThanOrEqual
    def gte = ">=".r ^^^ GreaterThanOrEqual
    def noteq = "!=".r ^^^ NotEquals
    def in = "IN".r ^^^ In
    def contains = "CONTAINS".i ^^^ Contains
    def containsKey = "CONTAINS KEY".i ^^^ ContainsKey
    def like = "LIKE".r ^^^ Like

    lte | gte | eq | lt | gt | noteq | in | containsKey | contains | like
  }



  def dataType: Parser[DataType] = {
    def ascii = "ascii".i ^^^ DataType.Ascii
    def bigint = "bigint".i ^^^ DataType.BigInt
    def blob = "blob".i ^^^ DataType.Blob
    def boolean = "boolean".i ^^^ DataType.Boolean
    def counter = "counter".i ^^^ DataType.Counter
    def date = "date".i ^^^ DataType.Date
    def decimal = "decimal".i ^^^ DataType.Decimal
    def double = "double".i ^^^ DataType.Double
    def float = "float".i ^^^ DataType.Float
    def inet = "inet".i ^^^ DataType.Inet
    def int = "int".i ^^^ DataType.Int
    def smallint = "smallint".i ^^^ DataType.Smallint
    def text = "text".i ^^^ DataType.Text
    def time = "time".i ^^^ DataType.Time
    def timestamp = "timestamp".i ^^^ DataType.Timestamp
    def timeuuid = "timeuuid".i ^^^ DataType.Timeuuid
    def tinyint = "tinyint".i ^^^ DataType.Tinyint
    def uuid = "uuid".i ^^^ DataType.Uuid
    def varchar = "varchar".i ^^^ DataType.Varchar
    def varint = "varint".i ^^^ DataType.Varint

    def native: Parser[DataType.Native] =
      ascii | bigint | blob | boolean | counter | date |
        decimal | double | float | inet | int | smallint |
        text | timestamp | timeuuid | time | tinyint |
        uuid | varchar | varint

    def userDefined : Parser[DataType.UserDefined] = identifier ^^ (id => DataType.UserDefined(KeyspaceName(""), id))

    def nativeOrUserDefined : Parser[DataType] = native | userDefined
    def list = "list".i ~> '<' ~> nativeOrUserDefined <~ '>' ^^ DataType.List
    def frozen = "frozen".i ~> '<' ~> (collection | userDefined) <~ '>' ^^ DataType.Frozen
    def set = "set".i ~> '<' ~> nativeOrUserDefined <~ '>' ^^ DataType.Set
    def map = "map".i ~> '<' ~> native ~ (',' ~> (nativeOrUserDefined | frozen)) <~ '>' ^^ {
      case k ~ v => DataType.Map(k, v)
    }
    def collection: Parser[DataType.Collection] = list | set | map | frozen

    def tuple: Parser[DataType.Tuple] = "tuple".i ~> '<' ~> rep1sep(native, ",") <~ '>' ^^ DataType.Tuple

    native | collection | userDefined | tuple
  }

  def staticFlag = "STATIC".flag



  val Keywords = Set(
    "ADD",
    "ALLOW",
    "ALTER",
    "AND",
    "APPLY",
    "ASC",
    "AUTHORIZE",
    "BATCH",
    "BEGIN",
    "BY",
    "COLUMNFAMILY",
    "CREATE",
    "DELETE",
    "DESC",
    "DESCRIBE",
    "DROP",
    "ENTRIES",
    "EXECUTE",
    "FROM",
    "FROZEN",
    "FULL",
    "GRANT",
    "IF",
    "IN",
    "INDEX",
    "INFINITY",
    "INSERT",
    "INTO",
    "KEYSPACE",
    "LIMIT",
    "MODIFY",
    "NAN",
    "NORECURSIVE",
    "NOT",
    "NULL",
    "OF",
    "ON",
    "OR",
    "ORDER",
    "PRIMARY",
    "RENAME",
    "REPLACE",
    "REVOKE",
    "SCHEMA",
    "SELECT",
    "SET",
    "TABLE",
    "TO",
    // "TOKEN", // FIXME: https://github.com/cassandra-scala/troy/issues/132
    "TRUNCATE",
    "UNLOGGED",
    "UPDATE",
    "USE",
    "USING",
    "WHERE",
    "WITH"
  )


  def createTable: Parser[CreateTable] = {
    def createTable = "create".i ~> ("table".i | "columnfamily".i)

    def columnDefinition: Parser[Column] = identifier ~ dataType ~ staticFlag ~ "PRIMARY KEY".flag ^^^^ Column

    def primaryKeyDefinition: Parser[PrimaryKey] = {
      def simplePartitionKey = identifier.asSeq
      def compositePartitionKey = parenthesis(rep1sep(identifier, ","))
      def partitionKeys: Parser[Seq[String]] = simplePartitionKey | compositePartitionKey
      def clusteringColumns: Parser[Seq[String]] = ("," ~> rep1sep(identifier, ",")) orEmpty

      "PRIMARY KEY".i ~> parenthesis(partitionKeys ~ clusteringColumns) ^^^^ PrimaryKey.apply
    }

    def bindMarker: Parser[BindMarker] = {
      import BindMarker._
      def anonymous = """\?""".r ^^^ Anonymous
      def named = ":".r ~> identifier ^^ Named

      anonymous | named
    }

    def term: Parser[Term] = {
      def functionCall: Parser[FunctionCall] =
        identifier ~ parenthesis(repsep(term, ",")) ^^^^ FunctionCall

      def typeHint: Parser[TypeHint] =
        parenthesis(dataType) ~ term ^^^^ TypeHint

      constant | functionCall | typeHint | bindMarker
    }

    def mapLiteral: Parser[MapLiteral] = {
      val pair = term ~ (':' ~> term) ^^ { case key ~ value => key -> value }
      val mapBody = repsep(pair, ",") ^^ MapLiteral
      curlyBraces(mapBody)
    }
    def standardOption: Parser[CreateTableOption] = ( identifier ~ ("=" ~> (constant | mapLiteral)) ^^ { case k ~ v => Property(k, v.toString)})  // <property> | COMPACT STORAGE | CLUSTERING ORDER
    def clusteringOrderDefinition = identifier ~ ("ASC".i | "DESC".i) //^^ {case _ => ClusteringOrderOption }
    def optionClusteringOrder : Parser[CreateTableOption] = ("CLUSTERING".i ~ "ORDER".i ~ "BY".i ~ "(".i ~ rep1sep(clusteringOrderDefinition, ",".i) ~ ")") ^^ {case _ => ClusteringOrderOption }
    def optionCompactStorage :  Parser[CreateTableOption] = "COMPACT".i ~ "STORAGE".i ^^ {case _ => CompactStorage}
    def option: Parser[CreateTableOption] = optionClusteringOrder | optionCompactStorage | standardOption
    def withOptions: Parser[Seq[CreateTableOption]] = ("WITH".i ~> rep1sep(option, "AND".i)) orEmpty

    createTable ~>
      ifNotExists ~
      tableName ~
      ("(" ~> rep1sep(columnDefinition, ",")) ~
      ("," ~> primaryKeyDefinition).? ~
      (")" ~> withOptions) ^^^^ CreateTable.apply
  }
  def createKeyspace: Parser[CreateKeyspace] = {
    val mapKey: Parser[String] = "'" ~> identifier <~ "'"
    val mapValue: Parser[String] = "'" ~> identifier <~ "'"
    val mapKeyValue = mapKey ~ (":" ~> mapValue) ^^ { case k ~ v => k -> v }
    val map: Parser[Seq[(String, String)]] = curlyBraces(repsep(mapKeyValue, ","))
    def optionReplication: Parser[KeyspaceOption] = ("replication".i ~> "=" ~> map) ^^ Replication
    def option : Parser[KeyspaceOption] = optionReplication | ( identifier ~ ("=" ~> identifier) ^^ {case k ~ v => OtherKeyspaceOption(k, v) })
    def withOptions: Parser[Seq[KeyspaceOption]] = ("WITH".i ~> rep1sep(option, "AND".i)) orEmpty

    "CREATE KEYSPACE".i ~>
      ifNotExists ~
      keyspaceName ~
      withOptions ^^^^ CreateKeyspace.apply // TODO: with properties   // <create-keyspace-stmt> ::= CREATE KEYSPACE (IF NOT EXISTS)? <identifier> WITH <properties>
  }

  def createTypeStatement: Parser[CreateType] = {
    def fields = {
      val fieldParser = identifier ~ dataType ^^^^ Field
      parenthesis(rep1sep(fieldParser, ","))
    }

    "CREATE TYPE".i ~>
      ifNotExists ~
      typeName ~
      fields ^^^^ CreateType.apply
  }

  def createIndex: Parser[CreateIndex] = {
    def indexName = identifier.?
    def onTable = "ON".i ~> tableName
    def indexIdentifier: Parser[IndexIdentifier] = {
      val keys = "KEYS".i ~> parenthesis(identifier) ^^ Keys
      val ident = identifier ^^ Index.Identifier
      parenthesis(((keys | ident)))
    }

    def bindMarker: Parser[BindMarker] = {
      import BindMarker._
      def anonymous = """\?""".r ^^^ Anonymous
      def named = ":".r ~> identifier ^^ Named

      anonymous | named
    }

    def term: Parser[Term] = {
      def functionCall: Parser[FunctionCall] =
        identifier ~ parenthesis(repsep(term, ",")) ^^^^ FunctionCall

      def typeHint: Parser[TypeHint] =
        parenthesis(dataType) ~ term ^^^^ TypeHint

      constant | functionCall | typeHint | bindMarker
    }

    def mapLiteral: Parser[MapLiteral] = {
      val pair = term ~ (':' ~> term) ^^ { case key ~ value => key -> value }
      val mapBody = repsep(pair, ",") ^^ MapLiteral
      curlyBraces(mapBody)
    }

    def using = {
      def withOptions =
        "WITH".i ~> "OPTIONS".i ~> "=" ~> mapLiteral

      "using".i ~> Constants.string ~ withOptions.? ^^^^ Using
    }.?

    "CREATE".i ~>
      ("CUSTOM".flag <~ "INDEX".i) ~
      ifNotExists ~
      indexName ~
      onTable ~
      indexIdentifier ~
      using ^^^^ CreateIndex.apply
  }

  def dataDefinition: Parser[DataDefinition] =
    createKeyspace | createTable | createIndex | createTypeStatement // | alterTableStatement


  def parseSchema(input: String): ParseResult[Seq[DataDefinition]] =
    parse(phrase(rep(dataDefinition <~ semicolon)), input)



}
