# Validate

- [Overview](#overview)
    - [Formats](#formats)
    - [Other configuration](#other-configuration)
- [Input file organisation](#input-file-organisation)
    - [Validation rules](#validation-rules)
    - [Comments](#comments)
    - [Cell data](#cell-data)
    - [Multi-value cells](#multi-value-cells)
- [Validation rule syntax](#validation-rule-syntax)
    - [Rule types](#rule-types)
    - [Wildcards](#wildcards)
    - [When-clauses](#when-clauses)
    - [Compound rule-types](#compound-rule-types)
- [Error Messages](#error-messages)

## Overview

Validates tables (CSV or TSV files) (`--table`) against an input ontology (`--input`) using the sets of rules defined (per table) in the table files, and writes the output to TXT, HTML, or XLSX files in the output directory (`--output-dir`) with the same base filename. If no output format is specified then the output is directed to STDOUT. For example:

    robot validate --input immune_exposures.owl \
      --table immune_exposures.csv \
      --reasoner hermit \
      --no-fail true \
      --format TXT \
      --output-dir results/

In this case the command will generate a single file called `immune_exposures.txt` in the `results/` directory.

One can also specify multiple table files to validate against a single input ontology. In that case there will be multiple output files corresponding to each table in the output directory. For example:

    robot validate --input immune_exposures.owl \
      --table immune_exposures.csv \
      --table immune_exposures_2.csv \
      --reasoner hermit \
      --no-fail true \
      --format HTML \
      --output-dir results/

In this case two files: `immune_exposures.html` and `immune_exposures_2.html` will appear in the `results/` directory.

### Formats

* `txt`: a list of failed validations. E.g.:
```
At immune_exposures.csv row 17, column 2: Cell is empty but rule: "is-required true" does not allow this.
```
* `html`: a [Bootstrap](https://getbootstrap.com/) HTML version of the `--table`. Cells containing bad data (failed validations) are highlighted red. Hovering over the red cells shows a tooltip with the message. These tables use CSS and JavaScript plugins from the BootstrapCDN, therefore they require an internet connection to properly view the table. See [HTML Tables](#html-tables-standalone) for more details.
* `xlsx`: an Excel spreadsheet version of the `--table`. Cells containing bad data are highlighted red and have a Comment on them containing the message.

### Other options

#### Exit Codes (`--no-fail`)

If there are any invalid cells, `validate` will fail by default with exit code `1`. This is good for use in [`Makefile` workflows](/make), as it will stop the workflow when there is a non-zero exit code. You can override this with `--no-fail true` as shown in the above examples if you want to bypass failures and always exit with code `0`.

#### Logging (`--silent`)

`validate` will only print a summary message at the end if there were any failures (unless no `--format` is specified, in which case it will always print to STDOUT). If you would like to print all invalid data messages, include `--silent false`.

#### Output Files (`--write-all`)

`validate` will only write tables with failed validations to the output directory. If you wish to write _all_ tables, including those that did not have any failed validation, specify `--write-all true`.

#### HTML Tables (`--standalone`)

If the output format is HTML, all output tables will be written as "standalone" files. This means that they have a header containing the Bootstrap stylesheet and scripts ([for tooltips](https://getbootstrap.com/docs/4.5/components/tooltips)). If you want to plug the table data into an existing HTML file, you can use `--standalone false` to generate _just_ the table element.

Note that the tooltips and styling will not work until the table is inserted into a file containing the required CSS and JavaScript from [BootstrapCDN](https://getbootstrap.com/docs/4.5/getting-started/introduction/). For offline viewing, you can also [download](https://getbootstrap.com/docs/4.5/getting-started/download/) the required files and provide a local path in the HTML header. The Bootstrap download does not include [jQuery](https://jquery.com/download/) or [Popper.js](https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js), which are required to enable tooltips (as well as a [small piece of JavaScript](https://getbootstrap.com/docs/4.5/components/tooltips/#example-enable-tooltips-everywhere)).

#### Errors Table (`--errors`)

The results that are written to the output directory contain all lines from the input tables, even if those lines don't have errors. You can choose to also output a table containing just the errors by specifying `--errors <path>`:

    robot validate --input immune_exposures.owl \
      --table immune_exposures.csv \
      --reasoner hermit \
      --no-fail true \
      --errors errors.csv

If this path ends with `.csv`, the output will be comma-separated. Otherwise, the output will be tab-separated.

This output will contain the following columns:
* **table**: the name of the table this cell is in
* **cell**: the A1 notation for the location of the cell
* **rule ID**: a combination of `<table name>!<location>` for the cell that the rule is written in
* **message**: text description of the violation

## Input file organisation

### Validation rules

Validation rules are read from the second row of the CSV or TSV file (`--table <path-to-input>`).

If the `--skip-row k` option is used, then the 'second row' is the second of the rows remaining in the table _after_ the kth row has been removed. For example, if you include validation rules in a [template](/robot), you might put the rules in the third row (after the template strings, which must be in the second row) and include `--skip-row 2`.

Below is an example table. One or more validation rule can be specified for each column, separated by a semi-colon (`;`), and these rules are applied only to the data in that column. Each rule in a column will be validated independently, and if any one of those rules is violated, the data in the cell will be considered invalid (i.e., the data must pass all rules). For details on using rules as "OR" statements (i.e., the data must pass at least one rule), see [Compound rule-types](#compound-rule-types).

|header A                        |header B                        |header C                     |
|--------------------------------|--------------------------------|-----------------------------|
|rule A1; rule A2; rule A3 ...   |rule B1; rule B2; rule B3 ...   |rule C1; rule C2; rule C3 ...|
|data                            |data                            |data                         |
|data                            |data                            |data                         |
|...                             |                                |                             |

### Comments

To comment out all of the rules for a given column, the list should be prefixed by '##'. To comment out particular rules from among the rules belonging to a given column, prefix those rules with '#'. For example:

_To comment out all rules:_
```
## rule 1; rule 2; rule 3
```

_To comment out rule 1 but not rule 2:_
```
# rule 1; rule 2
```

_To comment out rule 2 but not rule 1:_
```
rule 1; # rule 2
```

### Cell data

Data cells must either be in the form of a named class, e.g. 'Dengue virus', a named individual, e.g. 'Dr. Smith', or a general class expression, e.g. ('Dengue virus' or 'Dengue virus 2'). IRIs or CURIEs may be be used in lieu of labels if desired.

When using labels within class expressions, the label must be enclosed in single quotes if it contains a space. No quotes should be used for single-word labels (e.g. `virus`). If you are just referring to a named class or individual, you do not need to enclose the label in single quotes.

### Multi-value cells

A data cell can contain more than one logical entity if these are separated using the pipe ('|') character. Such cells are called multi-value cells. When a rule is defined over a multi-value cell, it will be validated for each logical entity in that multi-value cell, and if the rule contains a [wildcard](#wildcards) that refers to a multi-value cell, then all possible interpretations of that rule will be validatd against the current cell (which may itself be a multi-value cell). Consider, for example:

|header 1                        |header 2                        |
|--------------------------------|--------------------------------|
|                                |subclass-of %1                  |
|data1A \| data1B                |data2A \| data2B                |
|...                             |                                |

In this case, the following validations will be performed:

* data2A subclass-of data1A
* data2A subclass-of data1B
* data2B subclass-of data1A
* data2B subclass-of data1B

## Validation rule syntax

Individual rules must be of the form:

	<main-rule-type> <value> [(when <when-subject-expr-1> <when-rule-type-1> <when-value-1> & ...)]

Where:

* `<main-rule-type>` can be one of (or a combination of -- see [Compound rule-types](#compound-rule-types)):

    * is-required
    * is-excluded
    * subclass-of
    * direct-subclass-of
    * not-subclass-of
    * not-direct-subclass-of
    * superclass-of
    * direct-superclass-of
    * not-superclass-of
    * not-direct-superclass-of
    * equivalent-to
    * not-equivalent-to
    * instance-of
    * direct-instance-of
    * not-instance-of

* `<when-rule-type>` can be any of the above rule types (or a combination of -- see [Compound rule-types](#compound-rule-types)) _except_ `is-required` and `is-excluded`

### Rule types

* The following rule types are called _presence_ rule types. They place restrictions on whether a cell in a given column can have data or not, and may take a value of either `true` (equivalently: `t`, `yes`, `y`) or `false` (equivalently: `f`, `no`, `n`). If no truth value is supplied, `true` is assumed.

    * is-required
        * When set to `true` (implicitly or explicitly), this indicates that cells in this column should have data, possibly conditional upon an optional when-clause. E.g. `is-required (when <when-rule-type> <when-value>)` (see [When-clauses](#when-clauses))
    * is-excluded
        * When set to `true` (implicitly or explicitly), this indicates that cells in this column must be empty, possibly conditional upon an optional when-clause. E.g. `is-excluded (when <when-rule-type> <when-value>)` (see [When-clauses](#when-clauses))

* The following rule types are called _query_ rule types. They involve queries to the reasoner. Consider the example rule: `<query-type> 'vaccine'`. Replacing `<query-type>` with each of the below results in the following corresponding reasoner queries:

    * subclass-of
        * queries the reasoner to verify that the class represented in the current cell is a subclass of the class 'vaccine'
    * direct-subclass-of
        * queries the reasoner to verify that the class represented in the current cell is a direct subclass of the class 'vaccine'
    * not-subclass-of
        * queries the reasoner to verify that the class represented in the current cell is **not** a subclass of the class 'vaccine'
    * not-direct-subclass-of
        * queries the reasoner to verify that the class represented in the current cell is **not** a direct subclass of the class 'vaccine'
    * superclass-of
        * queries the reasoner to verify that the class represented in the current cell is a superclass of the class 'vaccine'
    * direct-superclass-of
        * queries the reasoner to verify that the class represented in the current cell is a direct superclass of the class 'vaccine'
    * not-superclass-of
        * queries the reasoner to verify that the class represented in the current cell is **not** a superclass of the class 'vaccine'
    * not-superclass-of
        * queries the reasoner to verify that the class represented in the current cell is **not** a direct superclass of the class 'vaccine'
    * equivalent-to
        * queries the reasoner to verify that the class represented in the current cell is equivalent to the class 'vaccine'
    * not-equivalent-to
        * queries the reasoner to verify that the class represented in the current cell is **not** equivalent to the class 'vaccine'
    * instance-of
        * queries the reasoner to verify that the individual represented in the current cell is an instance of the class 'vaccine'
    * direct-instance-of
        * queries the reasoner to verify that the individual represented in the current cell is a direct instance of the class 'vaccine'
    * not-instance-of
        * queries the reasoner to verify that the individual represented in the current cell is **not** an instance of the class 'vaccine'

#### Further notes on `<value>` and `<when-value>`

* For the rule types: `is-required` and `is-excluded`, `<value>` is _optional_ and if not specified defaults to _true_.

* For other rule types, `<value>` is _mandatory_ and must be in the form of a description logic (DL) expression query, in Manchester syntax.

* `instance-of`, `direct-instance-of`, and `not-instance-of` may only be applied to named individuals.

* `subclass-of`, `direct-subclass-of`, `not-subclass-of`, `superclass-of`, `direct-superclass-of`, `not-superclass-of`, and `equivalent-to` may be applied only to classes or general class expressions.

* `<when-subject-expr>` must describe an individual, a class, or a generalised class expression and can be in the form of an `rdfs:label`, an IRI, an abbreviated IRI, a general DL expression, or a wildcard.

### Wildcards

Wildcards of the form `%n` can be specified within `<value>`, `<when-value>`, and `<when-subject-expr>` clauses, and are used to indicate the entity described by the data in the _nth_ cell of a given row. E.g.:

```
is-required (when %1 equivalent-to ('Dengue virus' or 'Dengue virus 2'))
```

requires data in the current cell whenever the class indicated in column 1 of the current row is either 'Dengue virus' or 'Dengue virus 2'.

```
subclass-of hasBasisIn in some %2 (when %1 subclass-of ('Dengue virus' or 'Dengue virus 2'))
```

requires that, whenever the class indicated in column 1 of the current row is a subclass of the class consisting of the union of `'Dengue virus'` and `'Dengue virus 2'`, the data in the current cell must be a subclass of the set of classes that bear the relation `hasBasisIn` to the class indicated in column 2 of the same row.

### When-clauses

The optional when-clause indicates that the rule given in the main clause should be validated only when the when-clause is satisfied. If multiple when-clauses are specified (separated by `'&'`, then each when-clause must evaluate to _true_ in order for the main validation rule to execute. E.g.:

```
direct-subclass-of %2 (when %5 superclass-of 'exposure process' & %2 superclass-of vaccine)
```

indicates that the validation rule `'direct-subclass-of %2'` should only be run against the current cell when both the cell in column 5 is a superclass of `'exposure process'` and the cell in column 2 is a superclass of `vaccine`.

### Compound rule-types

`<rule-type>` and `<when-rule-type>` can take the form: `rule-type-1|rule-type-2|rule-type-3|...`

E.g.
```
subclass-of|equivalent-to %3 (when %4 subclass-of|equivalent-to %2)
```

requires that, whenever the contents of the cell in column 4 of the given row are either a subclass-of or equivalent-to the contents of the cell in column 2, then the contents of the current cell must be a subclass-of or equivalent-to the contents of the cell in column 3.

## Error Messages

### Malformed Rule Error

The indicated rule could not be parsed. See: [Validation Rule Syntax](#validation-rule-syntax).

### Invalid Presence Rule Error

A rule of the presence type must be in the form of a truth value. If this is ommitted it defaults to 'true'. For example, the following are valid: `is-required true`, `is-excluded`, `is-excluded false`. See: [Presence Types and Query Types](#presence-types-and-query-types).

### Column Out of Range Error

When a wildcard is used as part of a rule, the column number indicated must not be greater than the number of columns that are in the table data provided. See: [Wildcards](#wildcards).

### No Main Error

When a when-clause is specified, a main clause must also be specified, with the latter being evaluated only when the when-clause is satisfied. See: [Validation Rule Syntax](#validation-rule-syntax).

### Malformed When Clause Error

The indicated when-clause could not be parsed. See: [When-Clauses](#when-clauses).

### Invalid When Type Error

The indicated when rule type is not one of the rule types allowed in a when-clause. See: [Validation Rule Syntax](#validation-rule-syntax).

### Unrecognized Query Type Error

The query type indicated is not one of the recognized query types. See: [Presence Types and Query Types](#presence-types-and-query-types).

### Unrecognized Rule Type Error

The rule type indicated is not one of the recognized rule types. See: [Validation Rule Syntax](#validation-rule-syntax).

### Table Not Provided Error

The name of a `.csv` or `.tsv` file containing the table data to validate must be supplied using the `--table` option of the `validate` command. E.g. `robot validate --input myontology.owl --table mytable.csv`.

### Incorrect Table Format Error

The name of the file specified using the `--table` option must end in either `.csv` or `.tsv`.

### Invalid Skip Row Error

The value of the `--skip-row` option must be an integer.
