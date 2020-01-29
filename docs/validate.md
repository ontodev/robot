# Validate

Validates a table (CSV or TSV file) against an ontology using the set of rules defined in the table file, and write the output to a TXT, HTML, or XLSX file. (If no output format is specified then the output is directed to STDOUT.) For example:

    robot validate --input immune_exposures.owl \
      --table immune_exposures.csv \
      --reasoner hermit \
      --format TXT \
      --output-dir results/

Multiple table files can be specified as input. In that case there will be multiple output files corresponding to each table in the output directory. Note that each input table must define its own set of validation rules. For example:

    robot validate --input immune_exposures.owl \
      --table immune_exposures.csv \
      --table immune_exposures_2.csv \
      --reasoner hermit \
      --format HTML \
      --output-dir results/

In this case, since an output format of HTML has been specified, two files: immune_exposures.html and immune_exposures_2.html will appear in the results/ directory.

## Validation rules

### Data file organisation

Validation rules are read from the second row of the CSV file as follows:

|header A                        |header B                        |header C                     |
|--------------------------------|--------------------------------|-----------------------------|
|rule A1; rule A2; rule A3 ...   |rule B1; rule B2; rule B3 ...   |rule C1; rule C2; rule C3 ...|
|data                            |data                            |data                         |
|data                            |data                            |data                         |
|...                             |                                |                             |

* Data must be either a named class (e.g. 'Dengue virus'), a named individual (e.g. 'Dr. Smith'), or a general class expression (e.g. ('Dengue virus' or 'Dengue virus 2')). IRIs or short-form IRIs may be be used in lieu of labels if desired.

* Rules for a given column must be separated by semicolons. To comment out all of the rules for a given column, the list should be prefixed by '##'. To comment out individual rules from the rules belonging to a given column, prefix the rule with '#'. For example:

_To comment out all rules:_

	## rule 1; rule 2; rule 3

_To comment out rule 1 but not rule 2:_

	# rule 1; rule 2

_To comment out rule 2 but not rule 1:_

	rule 1; # rule 2

### Validation rule syntax

Individual rules must be of the form:

	<main-rule-type> <rule> [(when <when-subject-expr-1> <when-rule-type-1> <when-rule-1> & ...)]

Where:

* `<main-rule-type>` can be one of:

    * is-required
    * is-excluded
    * subclass-of
    * direct-subclass-of
    * superclass-of
    * direct-superclass-of
    * equivalent-to
    * instance-of
    * direct-instance-of

* `<when-rule-type>` can be one of:

    * subclass-of
    * direct-subclass-of
    * superclass-of
    * direct-superclass-of
    * equivalent-to
    * instance-of
    * direct-instance-of

#### Presence types and Query types

* The following rule types are called _presence_ rule types. They place restrictions on whether a cell in a given column can have data or not, and may take a value of either `true` (equivalently: `t`, `yes`, `y`) or `false` (equivalently: `f`, `no`, `n`). If no truth value is supplied, `true` is assumed.

    * is-required
        * When set to `true`, this indicates that cells in this column should have data, possibly conditional upon an optional when-clause. E.g. `is-required (when 'Crotalus atrox' subclass-of 'vaccine')`
    * is-excluded
        * When set to `true`, this indicates that cells in this column must be empty, possibly conditional upon an optional when-clause. E.g. `is-excluded (when 'Crotalus atrox' subclass-of 'vaccine')`

* The following rule types are called _query_ rule types. They involve queries to the reasoner. Consider the example rule `<query-type> 'vaccine'`. Given the query types below, we have the following corresponding reasoner queries:

    * subclass-of
        * queries the reasoner to verify that the class represented in the current cell is a subclass of the class 'vaccine'
    * direct-subclass-of
        * queries the reasoner to verify that the class represented in the current cell is a direct subclass of the class 'vaccine'
    * superclass-of
        * queries the reasoner to verify that the class represented in the current cell is a superclass of the class 'vaccine'
    * direct-superclass-of
        * queries the reasoner to verify that the class represented in the current cell is a direct superclass of the class 'vaccine'
    * equivalent-to
        * queries the reasoner to verify that the class represented in the current cell is equivalent to the class 'vaccine'
    * instance-of
        * queries the reasoner to verify that the individual represented in the current cell is an instance of the class 'vaccine'
    * direct-instance-of
        * queries the reasoner to verify that the individual represented in the current cell is a direct instance of the class 'vaccine'

#### Further notes on `<rule>` and `<when-rule>`

* For the rule types: `is-required` and `is-excluded`, `<rule>` is _optional_ and if not specified defaults to _true_.

* For other rules types, `<rule>` is _mandatory_ and must be in the form of a description logic (DL) expression query, in Manchester syntax.

* `instance-of` and `direct-instance-of` may only be applied to named individuals. `subclass-of`, `direct-subclass-of`, `superclass-of`, `direct-superclass-of`, and `equivalent-to` may be applied only to classes or general class expressions.

* `<when-subject-expr>` must describe an individual, a class, or generalised class expression and can be in the form of an `rdfs:label`, an IRI, an abbreviated IRI, a general DL expression, or a wildcard.

#### Wildcards

Wildcards of the form `%n` can be specified within `<rule>`, `<when-rule>`, and `<when-subject-expr>` clauses, and are used to indicate the entity described by the data in the _nth_ cell of a given row. E.g. the rule:

    subclass-of hasBasisIn in some %2 (when %1 subclass-of ('Dengue virus' or 'Dengue virus 2'))

requires that, whenever the class indicated in column 1 of the current row is a subclass of the class consisting of the union of `'Dengue virus'` and `'Dengue virus 2'`, the data in the current cell must be a subclass of the set of classes that bear the relation `hasBasisIn` to the class indicated in column 2 of the same row.

#### When-clauses

The optional when-clause indicates that the rule given in the main clause should be validated only when the when-clause is satisfied. If multiple when-clauses are specified (separated by `'&'`, then each when-clause must evaluate to _true_ in order for the main validation rule to execute. E.g.:

	direct-subclass-of %2 (when %5 superclass-of 'exposure process' & %2 superclass-of vaccine)

indicates that the validation rule `'direct-subclass-of %2'` should only be run against the current cell when both the cell in column 5 is a superclass of `'exposure process'` and the cell in column 2 is a superclass of `vaccine`.

#### Compound rule-types

`<rule-type>` and `<when-rule-type>` can take the form: `rule-type-1|rule-type-2|rule-type-3|...`

E.g.

	subclass-of|equivalent-to %3 (when %4 subclass-of|equivalent-to %2)

requires that, whenever the contents of the cell in column 4 of the given row are either a subclass-of or equivalent-to the contents of the cell in column 2, then the contents of the current cell must be a subclass-of or equivalent-to the contents of the cell in column 3.

#### Literals

Literal `rdfs:label` expressions must be enclosed in single quotes if they contain spaces.
E.g. in the first example below, single quotes are required around `Dengue virus` but in the second example no single quotes are necessary around `vaccination`.

	subclass-of 'Dengue virus'
	hasBasisIn some vaccination

_Note that double quotes are not allowed._

#### `is-required` and `is-excluded`

`is-required` indicates that the given cell must be non-empty, while `is-excluded` requires that it be empty. These rules can be used with an optional when-clause. E.g.:

	is-required (when %1 subclass-of 'exposure process)

indicates that the current cell must be non-empty whenever the cell in column 1 of the current
row is a subclass of `'exposure process'`.

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
