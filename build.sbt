
lazy val commonSettings = Seq(
  version := "1.0",
  name := "cql2plantuml",
  scalaVersion := "2.12.4"
)

lazy val root = (project in file(".")).
	settings(commonSettings: _*).
  settings(
    mainClass in Compile := Some("net.deeppay.cqlAST2puml.Main")        
  )




libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6"
libraryDependencies += "org.scalatest" % "scalatest_2.12" % "3.0.4" % "test"


// META-INF discarding
/*mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
   {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case x => MergeStrategy.first
   }
}*/
