package troy.cql.ast.ddl

import troy.cql.ast.DataType

object Table {
  final case class Column(name: String, dataType: DataType, isStatic: Boolean, isPrimaryKey: Boolean)
  final case class PrimaryKey(partitionKeys: Seq[String], clusteringColumns: Seq[String])
  object PrimaryKey {
    def simple(c: String) = PrimaryKey(Seq(c), Seq.empty)
  }

  sealed trait CreateTableOption
  final case class Property(val key : String, val value : String) extends CreateTableOption
  case object CompactStorage extends CreateTableOption
  case object ClusteringOrderOption extends CreateTableOption
}
