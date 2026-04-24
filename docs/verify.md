# Verify

## Contents

1. [Overview](#overview)
2. [Fail on Violation (`--fail-on-violation`)](#fail-on-violation)
3. [Executing on Disk (`--tdb`)](#executing-on-disk)

## Overview

The `verify` command is used to check an ontology for violations of rules. Each rule is expressed as a SPARQL SELECT query that matches violations of the rule. If the query produces any results, `verify` will exit with an error message that reports the violations. If the ontology conforms to the rule, the query should find no violations, and the `verify` command will succeed.

In the `verify` command, you specify a set of queries using the `--queries` parameter and
specify an output directory for the result using the `--output-dir` parameter,
e.g. `--queries` query-file-1 query-file-2 ... `--output-dir` some-directory

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


## Fail on Violation

By default, this command will fail with a non-zero exit code when any violations are found. If you wish the command to succeed (e.g., for use for warnings in workflows), you can include `--fail-on-violation false`. Note that it will still log `FAIL Rule [rule name]` on the command line.

	robot verify --input asserted-equiv.owl \
	  --queries equivalent.sparql \
	  --fail-on-violation false \
	  --output-dir results/

## Executing on Disk

For very large ontologies, it may be beneficial to load the ontology to a mapping file on disk rather than loading it into memory. This is supported by an [Apache Jena TDB Dataset](http://jena.apache.org/documentation/tdb/datasets.html). To execute `verify` with TDB, use `--tdb true`:

    robot verify --input asserted-equiv.owl \
      --tdb true \
      --queries equivalent.sparql \
      --output-dir results/

Please note that this will only work with ontologies in RDF/XML or Turtle syntax, and not with Manchester Syntax. Attempting to load an ontology in a different syntax will result in a [Syntax Error](errors#syntax-error). ROBOT will create a directory to store the ontology as a dataset, which defaults to `.tdb`. You can change the location of the TDB directory by using `--tdb-directory <directory>`. If a `--tdb-directory` is specified, you do not need to include `--tdb true`. If you've already created a TDB directory, you can verify from the TDB dataset without needing to specify an `--input` - just include the `--tdb-directory`.

Once the verify operation is complete, ROBOT will remove the TDB directory. If you are performing many verify commands on one ontology, you can include `--keep-tdb-mappings true` to prevent ROBOT from removing the TDB directory. This will greatly reduce the execution time of subsequent TDB-based operations on the ontology.

---

## Error Messages

### Verification Failed

At least one of the query you specifies returned results. The number of failures for each rule will be printed. A CSV file will be generated with the results that matched the rule.

### Missing Query Error

You must specify at least one query to execute with `--queries`.
