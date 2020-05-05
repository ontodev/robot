# Query

## Contents

1. [Overview](#overview)
2. [Handling Imports (`--use-graphs`)](#handling-imports)
3. [SPARQL UPDATE (`--update`)](#sparql-update)
4. [Executing on Disk (`--tdb`)](#executing-on-disk)

## Overview

ROBOT can execute <a href="https://www.w3.org/TR/rdf-sparql-query/" target="_blank">SPARQL</a>
queries against an ontology. The [verify](/verify) command is similar, but is used to test that an ontology conforms to the specified rules.

The `query` command can execute SPARQL ASK, SELECT, and CONSTRUCT queries by using the `--query` option with two arguments: a query file and an output file. The output file will only be written if the query returns some results. The output format will be inferred from the output file extension, or you can use the `--format` option.

ASK always produces `true` or `false`.

SELECT produces a table, if there are any results, defaulting to CSV format. For example:

    robot query --input nucleus.owl \
      --query cell_part.sparql results/cell_part.csv

This produces <a href="/examples/cell_part.csv" target="_blank">`cell_part.csv`</a>.

CONSTRUCT produces RDF data, if there are any results, defaulting to Turtle format:

    robot query --format ttl \
      --input nucleus.owl \
      --query part_of.sparql results/part_of.ttl

This produces <a href="/examples/part_of.ttl" target="_blank">`part_of.ttl`</a>.

Instead of specifying one or more pairs (query file, output file), you can specify a single `--output-dir` and use the `--queries` option to provide one or more queries of any type. Each output file will be written to the output directory with the same base name as the query file that produced it. For example the `foo.sparql` query file will produce the `foo.csv` file. The output directory must exist.

    robot query --input nucleus.owl \
      --queries cell_part_ask.sparql \
      --output-dir results/

## Handling Imports

By default, `query` ignores import statements. To include all imports as named graphs, add `--use-graphs true`. 

    robot query --input imports.owl \
      --use-graphs true --catalog catalog.xml \
      --query named_graph.sparql results/named_graph.csv
      
The example above also uses the [global](/global)  `--catalog` option to specify the catalog file for the import mapping. The default graph is the union of all graphs, which allows querying over an ontology and all its imports.

The names of the graphs correspond to the ontology IRIs of the imports. If the import does not have an ontology IRI, one will be automatically generated. Running `query` with the `-vv` flag will print the names of all graphs as they are added.

## SPARQL UPDATE

The `query` command also supports [SPARQL UPDATE](https://www.w3.org/TR/sparql11-update/) to insert and delete triples.

    robot query --input nucleus.owl \
      --update update.ru \
      --output results/nucleus_update.owl

When using SPARQL update, you can either provide an `--output` for the updated ontology, or [chain](/chaining) it into another command.

You can perform multiple updates in one command to reduce time spent loading and saving the ontology. Updates are processed in the order that they are input.

    robot query --input nucleus.owl \
     --update update.ru \
     --update revert.ru \
     --output results/nucleus.owl

The `--update` option only updates the ontology itself, not any of the imports.

**Warning:** The output of SPARQL updates will not include `xsd:string` datatypes, because `xsd:string` is considered implicit in RDF version 1.1. This behaviour differs from other ROBOT commands, where `xsd:string` datatypes from the input are maintained in the output.

## Executing on Disk

For very large ontologies, it may be beneficial to load the ontology to a mapping file on disk rather than loading it into memory. This is supported by [Jena TDB Datasets](http://jena.apache.org/documentation/tdb/datasets.html). To execute a query with TDB, use `--tdb true`:
 
    robot query --input nucleus.ttl --tdb true \
     --query cell_part.sparql results/cell_part.csv
 
Please note that this will only work with ontologies in RDF/XML or Turtle syntax, and not with Manchester Syntax. Attempting to load an ontology in a different syntax will result in a [Syntax Error](errors#syntax-error). ROBOT will create a directory to store the ontology as a dataset, which defaults to `.tdb`. You can change the location of the TDB directory by using `--tdb-directory <directory>`.

Once the query operation is complete, ROBOT will remove the TDB directory. If you are performing many query commands on one ontology, you can include `--keep-tdb-mappings true` to prevent ROBOT from removing the TDB directory. This will greatly reduce the execution time of subsequent queries.

The ontology is never loaded as an `OWLOntology` object, since doing so loads the whole ontology into memory. Therefore, TDB cannot be used while chaining commands or with the `--update` option.

### Creating a TDB Directory

You can also choose to just create a TDB directory without running a query using the `--create-tdb` option. This is useful for workflows were a TDB directory may need to be initiated in one step and queried mulitple times in another.

```
robot query --input nucleus.ttl --create-tdb true
```

---

## Error Messages

### Missing File Error

The file provided for `--update` does not exist. Check the path and try again.

### Missing Output Error

The `--query`, `--select`, and `--construct` options require two arguments: a query file and an output file (`--query <query> <output>`). 

### Missing Query Error

You must specify a query to execute with `--query` or `--queries`.

### Query Parse Error

The query was not able to be parsed. Often, this is as a result of an undefined prefix in the query. See the error message for more details.

### Query Type Error

Each SPARQL query should be a SELECT, ASK, DESCRIBE, or CONSTRUCT.
