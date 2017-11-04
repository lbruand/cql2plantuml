package net.deeppay.cqlparser




import java.io.StringWriter

import net.deeppay.cqlAST2puml.CQL2Puml
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import troy.cql.ast.DataDefinition




class CreateTableSpec extends FlatSpec {
  "A parser" should "parse successfully a create table statement" in {
    val result = CQLSchemaParser.parse(CQLSchemaParser.createTable, "CREATE TABLE mytable ( hello TEXT PRIMARY KEY);")
    result shouldBe a [CQLSchemaParser.Success[_]]
  }

  "A parser" should "fail on drop table statement" in {
    val result = CQLSchemaParser.parse(CQLSchemaParser.createTable, "DROP TABLE mytable;")
    result shouldBe a [CQLSchemaParser.Failure]
  }

  "A parser" should "parse successfully a create table statement with a keyspace" in {
    val result = CQLSchemaParser.parse(CQLSchemaParser.createTable, "CREATE TABLE mykeyspace.mytable ( hello TEXT PRIMARY KEY);")
    result shouldBe a [CQLSchemaParser.Success[_]]
    result.get.tableName.keyspace.get.name shouldBe "mykeyspace"
    result.get.tableName.table shouldBe "mytable"
  }

  "A schema" should "result in many statments" in {
    val result = CQLSchemaParser.parseSchema("CREATE TABLE mykeyspace.mytable ( hello TEXT PRIMARY KEY);\nCREATE TABLE mykeyspace.mytable ( hello TEXT PRIMARY KEY);")
    result shouldBe a [CQLSchemaParser.Success[_]]

  }

  val lines :String = scala.io.Source.fromURL(getClass.getResource("/testSchema.cql")).mkString

  "Test Schema System" should "be parsed by schema parser" in {

    val result: CQLSchemaParser.ParseResult[Seq[DataDefinition]] = CQLSchemaParser.parseSchema(lines)
    result shouldBe a [CQLSchemaParser.Success[_]]
    print(result)
  }

  "Schema" should "be outputed ok" in {
    val result: CQLSchemaParser.ParseResult[Seq[DataDefinition]] = CQLSchemaParser.parseSchema(lines)

    val cQL2Puml = new CQL2Puml()
    val writer = new StringWriter()
    val tuple = result match {
      case CQLSchemaParser.Success(seq, _) => seq
    }
    cQL2Puml.output(tuple, writer)
    print(writer.toString)
  }

}