package troy.cql.ast.ddl

import troy.cql.ast.Identifier

object Keyspace {
  sealed trait KeyspaceOption
  final case class Replication(options: Seq[(String, String)]) extends KeyspaceOption // TODO
  final case class OtherKeyspaceOption(key : Identifier, value: String) extends KeyspaceOption
}
