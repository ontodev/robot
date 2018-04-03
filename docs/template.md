# Template

ROBOT can convert tables to OWL format using templates. See <a href="/examples/template.csv" target="_blank">`template.csv`</a> for an example. The approach extends the QTT method described in <a href="http://dx.doi.org/10.3233/AO-2011-0086" target="_blank">Overcoming the ontology enrichment bottleneck with Quick Term Templates</a>.

ROBOT can read comma-separated values (`.csv`) or tab-separated values (`.tsv` or `.tab`):

    robot template --template template.csv \
      --prefix "ex: http://example.com/" \
      --ontology-iri "https://github.com/ontodev/robot/examples/template.owl" \
      --output results/template.owl

Each template file must be set up in the following format:

1. **Headers**: ROBOT expects the first row to contain column names for every column used in the data. These are used to make error messages more helpful.
2. **Templates**: ROBOT expects the second row to contain template strings for each column that will be used in the OWL conversion. See below for details on template strings.
3. **Data**: ROBOT expects each of the remaining rows to correspond to an OWLClass or OWLIndividual. (In the future we may add support for other sorts of OWL entities). Rows with a blank "ID" column will be skipped.

The `template` command accepts an optional input ontology, either using the `--input` option or from the previous command in a chain. If an input ontology is given, its RDFS labels will be used when parsing the template. The `--template` or `-t` option specified the CSV or TSV template file. Multiple templates are allowed, and the order of templates is significant. You can also specify the normal `--prefix` options, the `--output-iri` and `--version-iri`, and the usual `--output` options. See below for the three different merge options, and details on how they control the output of the command.

## Template Strings

- `ID`: Every term must have an IRI to identify it. This can be specified with an `ID` column. Usually this will be a prefixed ID like `GO:12345`. See the `--prefix` options for details. Rows with an empty ID cell will be skipped.
- `LABEL`: If a term exists in an ontology, or its ID has been defined elsewhere (perhaps in a previous template), then the `LABEL` column can specify an `rdfs:label` that uniquely identifies the target term. This can be easier the numeric IDs for human readers.
- `TYPE`: this is the `rdf:type` for the row. Because ROBOT is focused on ontology development, the default value is `owl:Class` and this column is optional. When creating an OWLIndividual, specify the class to which it belongs in this column.
- `CLASS_TYPE`: ROBOT creates a class for each row of data. You must specify a CLASS_TYPE, which can be either:
    - `subclass`: the created class will be asserted to be a subclass of each templated class expression
    - `equivalent`: the created class will be asserted to be equivalent to the intersection of all the templated class expressions
- **annotations**: ROBOT can attach annotations to your class. There are four options:
    - `A` string annotation: If the template string starts with an `A` and a space then it will be interpreted as a string annotation. The rest of the template string should be the label or compact IRI of an annotation property, e.g. `label` or `rdfs:label`. The cell value will be the literal value of the annotation with type `xsd:string`.
    - `AT` typed annotation: If the template string starts with an `AT` and a space then it will be interpreted as a typed annotation. The `^^` characters must be used to separate the annotation property from the datatype, e.g. `rdfs:comment^^xsd:integer`. The cell value will be the typed literal value of the annotation.
    - `AL` language annotation: If the template string starts with an `AL` and a space then it will be interpreted as a language annotation. The `@` character must be used to separate the annotation property from the language code, e.g. `rdfs:comment@en`.
    - `AI` annotation IRI: If the template string starts with an `AI` and a space, then the annotation will be made as with a string annotation, except that the cell value will be interpreted as an IRI.
- `C` **class expression**: If the template string starts with a `C` and a space then it will be interpreted as a class expression. The value of the current cell will be substituted into the template, replacing all occurrences of the percent `%` character. Then the result will be parsed into an OWL class expression. ROBOT uses the same syntax for class expressions as Protégé: [Manchester Syntax](http://www.w3.org/2007/OWL/wiki/ManchesterSyntax). If it does not recognize a name, ROBOT will assume that you're trying to refer to a class by its IRI (or compact IRI). This can lead to unexpected behaviour, but it allows you to refer to classes (by IRI) without loading them into the input ontology. This is particularly useful when the input ontology would be too large, such as the NCBI Taxonomy.
- **axiom annotations**: ROBOT can also annotate logical and annotation axioms.
    - `>A` annotation on annotation: Annotates the annotation axiom created from the cell to the left with the cell value. The column to the left must be an `A*` template string.
    - `>C` annotation on class expression: Annotates the class expression axiom created from the cell to the left with the cell value. The column to the left must be a `C` template string.

Sometimes you want to include zero or more values in a single spreadsheet cell, for example when you want to allow for multiple annotations. If an annotation template string also contains `SPLIT=|`, then ROBOT will use the `|` character to split the contents of a cell in that column and add an annotation for each result (if there are any). Instead of `|` you can specify a string of non-whitespace characters of your choice to split on.


## Merging

The `template` command has three options for merging, which are especially useful when chaining commands. First some terminology:

- "input ontology": the ontology from the previous command or specified by the `--input` option, used to resolve terms in the template
- "result ontology": the new ontology created from the template, distinct from the input ontology
- "merged ontology": the result ontology merged into the input ontology

The three options can differ in which ontology is saved for the `--output` option and which is sent to the next command in the chain:

option             | `--output` | output
-------------------|------------|-------
no merge (default) | result     | result
`--merge-before`   | merged     | merged
`--merge-after`    | result     | merged

- no merge (default behaviour): only the result ontology will be output
- `merge-before`: the result ontology is merged into the input ontology immediately, so only the merged ontology will be output
- `merge-after`: any `--output` options apply to the result ontology, then result ontology is merged into the input ontology, and the output of the command is the merged ontology

These three options are particularly useful when chaining commands. For instance, the `merge-after` option lets you save the result ontology separately, then send the merged ontology to the next command. See [merge](/merge) for more information on merge options, including `--collapse-import-closure` and `--include-annotations`.

---

## Error Messages

### Missing Template Error

You must specify at least one template with `--template` to proceed.

### Annotation Property Error

The annotation property provided could not be resolved. Check your template to ensure the provided annotation property is in a correct IRI or CURIE format. For legibility, using CURIEs is recommended, but you must ensure that the prefix is defined.
```
A rdfs:label
A http://www.w3.org/2000/01/rdf-schema#label
```

### Datatype Error

The datatype provided in an `AT` template string could not be resolved. Check your template to ensure the provided datatype is in a correct IRI or CURIE format. For legibility, using CURIEs is recommended, but you must ensure that the prefix is defined.
```
AT rdfs:label^^xsd:string
AT rdfs:label^^http://www.w3.org/2001/XMLSchema#string
```

### File Type Error

The `--template` option accepts the following file types: CSV, TSV, or TAB.

### ID Error

Each template must have an ID column. Keep in mind that if the template has an ID column, but it is not filled in for a row, that row will be skipped.

### IRI Error

The IRI provided as the value (in a row) to an `AI` template string could not be resolved as an IRI. Check your template to ensure the provided value is in a correct IRI or CURIE format. If using CURIEs, remember to ensure the prefix is defined.

### Language Format Error

The template string for an `AL` annotation must always include `@`.
```
AL rdfs:label@en
```

### Template File Error

The template cannot be found in the current directory. Make sure the file exists and your path is correct.

### Typed Format Error

The template string for an `AT` annotation must always include `^^`.
```
AT rdfs:label^^xsd:string
```

### Axiom Annotation Error

An axiom annotation is an annotation on an axiom, either a class axiom or another annotation. Because of this, any time `>A` is used, an annotation must be in the previous column. Any time `>C` is used, a class expression must be in the previous column.
```
A rdfs:label,>A rdfs:comment
C %,>C rdfs:comment
```

### Column Mismatch Error

There number of header columns (first row) must be equal to the number of template string columns (second row).

### Missing Type Error

If no `CLASS_TYPE` column is included, ROBOT will default to using `subclass`. If a `CLASS_TYPE` column is included, though, each row must include a specified class type. If the `CLASS_TYPE` is left empty, this error message will be returned.

### Null ID Error

An IRI cannot be created from the provided ID. This is most likely because the ID is not formatted properly, as an IRI or a CURIE.

### Parse Error

The content of a class expression cell (`C`) was not able to be parsed by OWLAPI. Check the formatting of the cell and the template string to ensure you are using valid CURIEs and/or IRIs.

### Unknown Template Error

Valid template strings are limited to the <a href="#template-strings">described above</a>. If a different template string is provided, this error message will be returned.

### Unknown Type Error

The `CLASS_TYPE` values are limited to `subclass` or `equivalent`. Anything else placed in that column will result in this error message.
