# Verify

The `verify` command is used to check an ontology for violations of rules. Each rule is expressed as a SPARQL SELECT query that matches violations of the rule. If the query produces any results, `verify` will exit with an error message that reports the violations. If the ontology conforms to the rule, the query should find no violations, and the `verify` command will succeed.

You can use `verify` in two ways:

1. `--query` query-file output-file
2. `--queries` query-file-1 query-file-2 ... `--output-dir` some-directory

For example:

```
robot verify --input asserted-equiv.owl --queries equivalent.sparql --output-dir results/
```

Should output as a response:

    Rule /ontodev/robot/examples/equivalent.sparql: 1 violation(s)
    first,second,firstLabel,secondLabel
    http://purl.obolibrary.org/obo/TEST_A,http://purl.obolibrary.org/obo/TEST_B,,

And the CSV file `results/equivalent.csv` should have:

    first,second,firstLabel,secondLabel
    http://purl.obolibrary.org/obo/TEST_A,http://purl.obolibrary.org/obo/TEST_B,,