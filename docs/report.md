# Report

## Contents

1. [Overview](#overview)
2. [Labels (`--labels`)](#labels)
3. [Failing (`--fail-on`)](#failing)
4. [Queries](#queries)
5. [Profiles (`--profile`)](#profiles)
6. [Executing on Disk (`--tdb`)](#executing-on-disk)
7. [Limiting Results (`--limit`)](#limiting-results)

## Overview

The `report` command runs a series of quality control SPARQL queries over the input ontology and generates a TSV or YAML report file based on the results. Each query has a logging level to define the severity of the issue: ERROR, WARN, or INFO.
* `ERROR`: Must be fixed before releasing the ontology. These issues will cause problems for users, such as classes with multiple labels.
* `WARN`: Should be fixed as soon as possible. These will not cause problems for *all* users, but may not be what they expect. For example, a class that is inferred to be equivalent to another named class.
* `INFO`: Should be fixed if possible. These are for consistency and cleanliness, such as definitions that do not start with an uppercase character.
<!-- DO NOT TEST -->
```
robot report --input edit.owl --output report.tsv
```

A summary of the report is also printed to the console:
```
Violations: 710
-----------------
ERROR:      109
WARN:       572
INFO:       29
```

## Formats

The following formats are supported by `report`:
- TSV
- CSV
- HTML
- YAML
- JSON
- XLSX

The format will be determined by the extension of the `--output` (e.g., if the output is `report.csv`, the format will be CSV). If you'd like to override this, you can include the `--format` option. For example, to force YAML format, include `--format YAML`.

HTML format will either be a "standalone" HTML page, or, if you plan to include this table in an existing HTML page, just the HTML table by including `--standalone false`. All HTML content is based on [Bootstrap](https://getbootstrap.com/).

If you do not provide an `--output` and your format is CSV or TSV, all violations will be printed to the console in that format. An `--output` **must** be provided for HTML, JSON, and YAML formats. You can also choose to print the details of the first `n` number of violations using `--print <n>`. If the format is CSV, it will be printed as comma-separated values, otherwise it will be printed as tab-separated values.
<!-- DO NOT TEST -->
```
robot report --input edit.owl \
 --print 5 \
 --output report.tsv
```

This will print the first five violations and also write the report to `report.tsv`. If you do not provide an `--output` while using `--print <n>`, *only* the first `n` violations will be printed to the terminal.

## Labels

The report output contains a series of CURIEs and/or IRIs. If you wish to include the entity labels, simply add `--labels true`. For large ontologies, this may take a bit longer to complete.

## Failing

By default, `report` will fail if any `ERROR`-level violations are found. This can be changed by setting the `--fail-on` level to a different logging level. For example, if you want your report to fail if there are any `WARN`-level violations:
<!-- DO NOT TEST -->
```
robot report --input edit.owl \
  --fail-on WARN \
  --output report.tsv
```

If the report fails, the command will exit with a status of `1`, but a report will still be generated and written to the output file. To always pass (even with errors), you can set the fail-on level to "none":

    robot report --input edit.owl \
      --fail-on none \
      --output results/report.tsv

## Queries

`report` runs a number of queries, each with a default logging level. [See the full list](report_queries/).

Each query retrieves a triple in the form of `?entity ?property ?value`. The `?entity` is the violating entity, the `?property` is the property that is being violated, and `?value` is what is causing the violation (which could be empty).

For example, the query to retrieve references to deprecated classes:
```
SELECT DISTINCT ?entity ?property ?value WHERE
  {?value owl:deprecated true .
   ?entity a owl:Class .
   ?entity ?property ?value }
```
Here, the `?value` is any deprecated class that is referenced in another entity's axioms.

You can provide your own queries to use in the report (which can be included in the profile, described below). Please make sure to follow this `?entity ?property ?value` pattern when writing these queries.

## Profiles

Each QC query is given the corresponding logging level shown above in the [default profile](/). The profile is a simple text file with a logging level followed by the query name (separated by a tab). Each line represents one query configuration:
```
ERROR	definition_cardinality
INFO	deprecated_class
```

`report` allows the user to define their own profile to configure different logging levels and include their own QC queries with the `--profile` option:
<!-- DO NOT TEST -->
```
robot report --input edit.owl \
  --profile my-profile.txt \
  --output my-report.tsv
```

For all default queries, include the query name shown above. If you do not wish to include a default query in your report, simply omit it from your profile. Any queries not named in the profile will not be run. Furthermore, your own queries can be included by providing the desired logging level followed by the absolute or relative path.

This example would create a report with references to deprecated classes as ERROR and the user query violations as INFO:
```
ERROR   deprecated_class
INFO    file:///absolute/path/to/other_query.rq
INFO    file:./relative/path/to/other_query.rq
```

## Executing on Disk

`report` may fail on some very large ontologies when ROBOT cannot fit the entire ontology into memory. You can either increase the available memory (see [Java Options](global#java-options)), or use the `--tdb true` option to load the ontology as an [Apache Jena TDB Dataset](http://jena.apache.org/documentation/tdb/datasets.html) instead of with OWL API:

```
robot report --input edit.owl \
  --tdb true \
  --output my-report.tsv
```

Please note that this will only work with ontologies in RDF/XML or Turtle syntax, and not with Manchester Syntax. Attempting to load an ontology in a different syntax will result in a [Syntax Error](errors#syntax-error). ROBOT will create a directory to store the ontology as a dataset, which defaults to `.tdb`. You can change the location of the TDB directory by using `--tdb-directory <directory>`.

Once the report is complete, ROBOT will remove the TDB directory. You can include `--keep-tdb-mappings true` to prevent ROBOT from removing the TDB directory (which may be beneficial if you want to reuse it with [query](query#executing-on-disk)). This will greatly reduce the execution time of subsequent TDB-based operations on the ontology.

## Limiting Results

Large numbers of results from the report queries may cause an `OutOfMemoryError`. To prevent this, you can limit the number of results with `--limit <INTEGER>`:

```
robot report --input edit.owl \
  --limit 10000 \
  --output my-report.tsv
```

This example will only include the first 10,000 results for each report query, meaning that some violation counts may be incomplete. Typically, you should not need to include a limit, but when working with large ontologies (using TDB), there may be hundreds of thousands of results.

---

## Error Messages

### Fail On Error

Only `info`, `warn`, and `error` are valid inputs for `--fail-on`.

### Limit Number Error

The argument for the `--limit` option must be a number.

### Missing Entity Binding

All queries must bind `?entity ?property ?value` for correct formatting. If `?entity` is ever `null`, the query cannot be reported on.

### Print Number Error

The argument for the `--print` option must be a number. 

### Report Level Error

The logging level defined in a profile must be `ERROR`, `WARN`, or `INFO`.
