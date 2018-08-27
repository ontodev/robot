# Report

The `report` command runs a series of quality control SPARQL queries over the input ontology and generates a TSV or YAML report file based on the results. Each query has a logging level to define the severity of the issue: ERROR, WARN, or INFO.
* `ERROR`: Must be fixed before releasing the ontology. These issues will cause problems for users, such as classes with multiple labels.
* `WARN`: Should be fixed as soon as possible. These will not cause problems for *all* users, but may not be what they expect. For example, a class that is inferred to be equivalent to another named class.
* `INFO`: Should be fixed if possible. These are for consistency and cleanliness, such as definitions that do not start with an uppercase character.
<!-- DO NOT TEST -->
```
robot report --input edit.owl --output report.tsv
```

By default, the output is a TSV file. You can also get a YAML output by including `--format YAML`. A summary of the report is also printed to the console:
```
Violations: 710
-----------------
ERROR:      109
WARN:       572
INFO:       29
```

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

If the report fails, the command will exit with a status of `1`, but a report will still be generated and written to the output file. To always pass (even with errors), change the level to `--fail-on none`.

## Queries

`report` runs a number of queries, each with a default logging level. [See the full list](report_queries/).

Each query retrieves a triple in the form of `?entity ?property ?value`. The `?entity` is the violating entity, the `?property` is the property that is being violated, and `?value` is what is causing the violation (which could be empty).

For example, the query to retrieve references to deprecatd classes:
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

For all default queries, include the query name shown above. If you do not wish to include a default query in your report, simply omit it from your profile. Any queries not named in the profile will not be run. Furthermore, your own queries can be included by providing the desired logging level followed by the path.

This example would create a report with references to deprecated classes as ERROR and the user query violations as INFO:
```
ERROR	deprecated_class
INFO	file:///absolute/path/to/other_query.rq
```

---

## Error Messages

### Fail On Error

Only `info`, `warn`, and `error` are valid inputs for `--fail-on`.

### Missing Entity Binding

All queries must bind `?entity ?property ?value` for correct formatting. If `?entity` is ever `null`, the query cannot be reported on.

### Report Level Error

The logging level defined in a profile must be `ERROR`, `WARN`, or `INFO`.
