CQL2Plantuml
============


Turn any CQL ( [Cassandra](http://cassandra.apache.org/)) schema into a graphical representation.

A small scala tool to transform a schema to a [plantuml](http://plantuml.com/) puml file that can be in turn transformed into a graphical representation of your CQL schema.


Gallery
=======

## System auth from Cassandra

![system auth](examples/system_auth/system_auth.png)

Downloads
=========

Download a jar file from [release](https://github.com/lbruand/cql2plantuml/releases).

run the jar file using :

```
java -jar cql2plantuml-assembly-1.0.jar <input file.cql>
```

This creates a .puml file from your input cql file.
This .puml file does not contain any link.
You can edit it to add links.
You can then run plantuml to obtain a .png or .svg file:

```
plantuml -T <input file.puml>
```


Derivative work warning
=======================

This work includes classes (CQL AST Parser) originally written by Tamer AbdulRadi distributed under the Apache Licence 2.0 at (https://github.com/schemasafe/troy). These classes were then modified.



