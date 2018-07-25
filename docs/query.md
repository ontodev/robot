# Query

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

## Imports

By default, `query` ignores import statements (`--imports ignore`). To query over the union of all imports, simply include `--imports union`. `query` also supports imports as named graphs. To load imports as named graphs, include `--imports graphs`. The graphs will be loaded with the respective ontology IRI as their names. For example, to find all the classes from an import (graph specified in [`named_graph.sparql`](/examples/named_graph.sparql)):

    robot query --input imports.owl \
      --imports graphs --catalog catalog.xml \
      --query named_graph.sparql results/named_graph.csv
      
The example above also uses the [global](/global)  `--catalog` option to specify the catalog file for the import mapping.

---

## Error Messages

### Missing Query Error

You must specify a query to execute with `--query` or `--queries`.

### Query Type Error

Each SPARQL query should be a SELECT, ASK, DESCRIBE, or CONSTRUCT.
