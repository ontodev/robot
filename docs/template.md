
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

### Template Options

The `template` command accepts an optional input ontology, either using the `--input` option or from the previous command in a chain. If an input ontology is given, its RDFS labels will be used when parsing the template. The `--template` or `-t` option specified the CSV or TSV template file. Multiple templates are allowed, and the order of templates is significant. You can also specify the normal `--prefix` options, the `--output-iri` and `--version-iri`, and the usual `--output` options. See [Merging](/template#merging) for the three different merge options, and details on how they control the output of the command.

A template may have multiple errors in different rows and columns. By default, `template` will fail on the first error encountered. If you wish to proceed with errors, use `--force true`. This will log all row parse errors to STDERR and attempt to create an ontology anyway. Be aware that the output ontology may be missing axioms.

## Template Strings

### Generic Template Strings

- `ID`: Every term must have an IRI to identify it. This can be specified with an `ID` column. Usually this will be a prefixed ID like `GO:12345`. See the `--prefix` options for details.
    - If an entity already exists in the `--input` ontology, you can refer to it by `LABEL` instead (see below).
    - Rows with no `ID` or `LABEL` will be skipped.
- `LABEL`: If a term exists in an ontology, or its ID has been defined elsewhere (perhaps in a previous template), then the `LABEL` column can specify an `rdfs:label` that uniquely identifies the target term. This can be easier than the numeric IDs for human readers. 
    - The `LABEL` column DOES NOT create an `rdfs:label` annotation for an entity. 
    - If you are creating new entities using `LABEL`, be sure to include an `ID` column as well (and an `A rdfs:label` column if you would like to annotate the new entity with a label).
- `TYPE`: this is the `rdf:type` for the row. Because ROBOT is focused on ontology development, the default value is `owl:Class` and this column is optional. When creating an OWLIndividual, specify the class to which it belongs in this column.
    - `class` or `owl:Class`
    - `object property` or `owl:ObjectProperty`
    - `data property` or `owl:DataProperty`
    - `annotation property` or `owl:AnnotationProperty`
    - `datatype` or `owl:Datatype`
    - `individual`, `named individual`, `owl:Individual`, `owl:NamedIndividual`, or a defined class ID or label
- **annotations**: ROBOT can attach annotations to your class. There are four options:
    - `A` string annotation: If the template string starts with an `A` and a space then it will be interpreted as a string annotation. The rest of the template string should be the label or compact IRI of an annotation property, e.g. `label` or `rdfs:label`. The cell value will be the literal value of the annotation with type `xsd:string`.
    - `AT` typed annotation: If the template string starts with an `AT` and a space then it will be interpreted as a typed annotation. The `^^` characters must be used to separate the annotation property from the datatype, e.g. `rdfs:comment^^xsd:integer`. The cell value will be the typed literal value of the annotation.
    - `AL` language annotation: If the template string starts with an `AL` and a space then it will be interpreted as a language annotation. The `@` character must be used to separate the annotation property from the language code, e.g. `rdfs:comment@en`.
    - `AI` annotation IRI: If the template string starts with an `AI` and a space, then the annotation will be made as with a string annotation, except that the cell value will be interpreted as an IRI.
- `>A` (**axiom annotations**): ROBOT can also annotate logical and annotation axioms. The axiom annotation will be on the axiom created on the cell to the left of the `>A*` template string. The `>` symbol can be used in front of any valid annotation character (`>A`, `>AT`, `>AL`, `>AI`)
   
Sometimes you want to include zero or more values in a single spreadsheet cell, for example when you want to allow for multiple annotations or have seperate logical axioms. If a template string also contains `SPLIT=|`, then ROBOT will use the `|` character to split the contents of a cell in that column and add an annotation for each result (if there are any). Instead of `|` you can specify a string of characters of your choice - other than pure whitespace - to split on (e.g. `SPLIT=, `).

### Class Template Strings

- `CLASS_TYPE`: ROBOT creates a class for each row of data with a that has a `TYPE` of `class` or `owl:Class`. The class type can be ONLY ONE OF:
    - `subclass`: the created class will be asserted to be a subclass of each templated class expression (default)
    - `disjoint`: the created class will be disjoint from each templated class expression, meaning the classes cannot share subclasses
    - `equivalent`: the created class will be asserted to be equivalent to the intersection of all the templated class expressions
- `C` **class expression**: If the template string starts with a `C` and a space then it will be interpreted as a class expression. The value of the current cell will be substituted into the template, replacing all occurrences of the percent `%` character. Then the result will be parsed into an OWL class expression. 
    - ROBOT uses the same syntax for class expressions as Protégé: [Manchester Syntax](http://www.w3.org/2007/OWL/wiki/ManchesterSyntax). This means that an entity can be referred to by its `rdfs:label` (enclosing in single quotes if it has a space in it). 
    - If it does not recognize a label, ROBOT will assume that you're trying to refer to a class by its IRI (or compact IRI). This can lead to unexpected behaviour, but it allows you to refer to classes (by IRI) without loading them into the input ontology. This is particularly useful when the input ontology would be too large, such as the NCBI Taxonomy.
    - Properties in class expression **must** be referred to by label in order to be parsed.

#### Example of Class Template Strings

| Label | Entity Type | Class Type | Related classes | Other axioms |
| --- | --- | --- | --- | --- |
| LABEL | TYPE | CLASS_TYPE | C % | C part_of some % |
| Class 2 | class | | Class 1 | |
| Class 3 | class | disjoint | Class 2 | |
| Class 4 | class | equivalent | | Class 3 |

Class 2 will be a subclass of Class 1, as there is no included `CLASS_TYPE`. Class 3 will be disjoint with Class 2, and Class 4 will be equivalent to `part_of some 'Class 3'`.

Manchester expressions can also be used within the cells, as long as they are enclosed in parentheses:

| Label | Parent |
| --- | --- |
| LABEL | C % |
| Class 4 | |
| Class 5 | (part_of some 'Class 4') |

In this template, Class 5 would be a subclass of `part_of some 'Class 4'`.

### Property Template Strings

- `PROPERTY_TYPE`: ROBOT creates a property for each row of data that has a `TYPE` of either an object or data property. The property type can be (any type followed by a \* can ONLY be used for object properties):
    - **logical types**: these types link the created property to other properties (annotation properties can *only* use `subproperty`). You may only include ONE of these in each `PROPERTY_TYPE`.
      - `subproperty`: the created property will be a subproperty of each templated property expression (default)
      - `equivalent`: the created property will be equivalent to all of the templated property expressions
      - `disjoint`: the created property will be disjoint from each templated property expression and the values cannot be the same
      - `inverse`\*: the created object property will be the inverse of each templated property expression
    - **property types**: these types define the type of the created property, and will not work with annotation properties. You may include any number of these.
      - `functional`: the created property will be functional, meaning each entity (subject) can have at most one value
      - `inverse functional`\*: the created object property will be inverse functional, meaning each value can have at most one subject
      - `irreflexive`\*: the created object property will be irreflexive, meaning the subject cannot also be the value
      - `reflexive`\*: the created object property will be reflexive, meaning each subject is also a value
      - `symmetric`\*: the created object property will be symmetric, meaning the subject and value can be reversed
      - `asymmetric`\*: the created object property will be asymmetric, meaning the subject and value cannot be reversed
      - `transitive`\*: the created object property will be transitive, meaning the property can be chained
- `P` **property expression**: If the template string starts with a `P` and a space then it will be interpreted as a property expression. The value of the current cell will be substituted into the template, replacing all occurrences of the `%` character. Then the result will be parsed into an OWL property expression. ROBOT uses the same syntax for property expressions as Protégé: [Manchester Syntax](http://www.w3.org/2007/OWL/wiki/ManchesterSyntax). If it does not recognize a name, ROBOT will assume that you're trying to refer to an entity by its IRI or CURIE. This can lead to unexpected behavior, but it allows you to refer to entities without loading them into the input ontology.
    - **object properties**: the only supported object property expression is the inverse object property expression. The template string is `P inverse(%)`. A single object property for a value can be specified by `P %`.
    - **data properties**: data property expressions are not yet supported by OWL. A data property for a value (e.g. for a parent property) can be specified by `P %`.
    - **annotation properties**: annotation property expressions are not possible. An annotation property for a value (e.g. for a parent property) can be specified by `P %`.
- `DOMAIN`: The domain to a property is a class expression in [Manchester Syntax](http://www.w3.org/2007/OWL/wiki/ManchesterSyntax) (for object and data properties). For annotation properties, the domain must be a single class specified by label, CURIE, or IRI.
- `RANGE`: The range to a property is either a class expression in [Manchester Syntax](http://www.w3.org/2007/OWL/wiki/ManchesterSyntax) (for object properties) or the name, CURIE, or IRI of a datatype (for annotation and data properties).

#### Example of Property Template Strings

| ID | Entity Type | Property Type | Related Property | Domain | Range |
| --- | --- | --- | --- | --- | --- |
| ID | TYPE | PROPERTY_TYPE | P % | DOMAIN | RANGE |
| OP:1 | owl:ObjectProperty | subproperty | Property 1 | Class 1 | Class 2 |
| DP:1 | owl:DataProperty | functional | Property 2 | Class 2 | xsd:string |

The `functional` data property will still default to a `subproperty` logical axiom for the `P %` template string, unless a different logical property type (`equivalent`, `disjoint`) is provided. Property type can be split, e.g. `PROPERTY_TYPE SPLIT=|`.

### Individual Template Strings

If the `TYPE` is a defined class, `owl:Individual`, or `owl:NamedIndividual`, an instance will be created. If the `TYPE` does not include a defined class, that instance will have no class assertions. You may include a `SPLIT=` in `TYPE` if you wish to provide more than one class assertion for an individual.

- `INDIVIDUAL_TYPE`: ROBOT creates an individual for each or of data that has a `TYPE` of another class. The individual type can be:
    - `named`: the created individual will be a default named individual. When the `INDIVIDUAL_TYPE` is left blank, this is the default. This should be used when adding object property or data property assertions
    - `same`: the created individual will be asserted to be the same individual as each templated individual in the row
    - `different`: the created individual will be asserted to be a different individual than any of the templated individuals in the row
- `I` **individual assertion**:
    - `I <property>`: when creating a `named` individual, replace property with an object property or data property to add assertions (either by label or CURIE). The value of each axiom will be the value of the cell in this column. For object property assertions, this is another individual. For data property assertions, this is a literal value.
    - `I %`: when creating a `same` or `different` individual, this template string is used to specify which individual will be the value of the same or different individual axiom.

#### Example of Individual Template Strings

| Label | Entity Type | Individual Type | Other Axioms | Related Individuals |
| --- | --- | --- | --- | --- |
| LABEL | TYPE | INDIVIDUAL_TYPE | I part_of | I % |
| Individual 1 | Class 1 | named | Individual 2 | |
| Individual 2 | Class 1 | different | | Individual 1 |

<!-- ### Datatype Template Strings -->

## Merging

The `template` command has three options for merging, which are especially useful when chaining commands. First some terminology:

- "input ontology": the ontology from the previous command or specified by the `--input` option, used to resolve terms in the template
- "result ontology": the new ontology created from the template, distinct from the input ontology
- "merged ontology": the result ontology merged into the input ontology

The three options can differ in which ontology is saved for the `--output` option and which is sent to the next command in the chain:

option       | `--output` | output
-------------|------------|-------
no merge     | result     | result
merge before | merged     | merged
merge after  | result     | merged

- no merge (default behaviour): only the result ontology will be output
- `merge-before`: the result ontology is merged into the input ontology immediately, so only the merged ontology will be output
- `merge-after`: any `--output` options apply to the result ontology, then result ontology is merged into the input ontology, and the output of the command is the merged ontology

These three options are particularly useful when chaining commands. For instance, the `merge-after` option lets you save the result ontology separately, then send the merged ontology to the next command. See [merge](/merge) for more information on merge options, including `--collapse-import-closure` and `--include-annotations`.

If the command includes `--ancestors`, the result ontology will include the ancestors (from the input ontology) of the result ontology terms. Only the labels of the ancestors will be included.

## Examples

Create an output ontology that includes the input ontology and the terms defined in the template:
<!-- DO NOT TEST-->
```
robot template --merge-before --input edit.owl \
  --template template.csv --output results/template.owl
```

Create two outputs - the templated terms ([`uberon_template.owl`](/examples/uberon_template.owl)) and the input ontology merged with the output ontology with an annotation ([`uberon_v2.owl`](/examples/uberon_v2)):
<!-- DO NOT TEST -->
```
robot template --merge-after \
  --input edit.owl \
  --template uberon_template.csv \
  --output results/uberon_template.owl \
  annotate --annotation rdfs:comment "UBERON with new terms" \
  --output results/uberon_v2.owl
```

Create an output ontology that consists of the template terms plus their dependencies ([`uberon_template_2.owl`](/examples/uberon_template_2.owl)):
<!-- DO NOT TEST -->
```
robot template --ancestors --input edit.owl \
  --template uberon_template.csv \
  --ontology-iri "https://github.com/ontodev/robot/examples/uberon_template_2.owl" \
  --output results/uberon_template_2.owl
```

Create an output ontology that includes the input ontology and the terms defined in the template, but keep the import statements* ([`test_template.owl`](/examples/test_template.owl)):
<!-- DO NOT TEST -->
```
robot template --merge-before \
  --input test.owl \
  --collapse-import-closure false \
  --template uberon_template.csv \
  --output results/test_template.owl
```

\* NOTE: the imports would be merged into the output if `--collapse-import-closure true` is included instead.

Further examples can be found [in the OBI repository](https://github.com/obi-ontology/obi/tree/master/src/ontology/templates)

---

## Error Messages

### Annotation Property Error

The annotation property provided could not be resolved. Check your template to ensure the provided annotation property is in a correct IRI or CURIE format. For legibility, using CURIEs is recommended, but you must ensure that the prefix is defined.
```
A rdfs:label
A http://www.w3.org/2000/01/rdf-schema#label
```

### Annotation Property Type Error

The only valid `PROPERTY_TYPE` for an annotation property is `subproperty`. Other types of logic for annotation properties are not supported in OWL. If this column is left blank, it will default to `subproperty`.

### Axiom Annotation Error

An axiom annotation is an annotation on an axiom, either a class axiom or another annotation. Because of this, any time `>A` is used, an annotation must be in the previous column. Any time `>C` is used, a class expression must be in the previous column.
```
A rdfs:label,>A rdfs:comment
C %,>C rdfs:comment
```

### Class Type Error

The valid `CLASS_TYPE` values are: `subclass`, `equivalent`, and `disjoint`.

### Class Type Split Error

A class row may only use one of: `subclass`, `equivalent`, and `disjoint`. To add other types of axioms on an OWL class, use a separate row.

### Column Mismatch Error

There number of header columns (first row) must be equal to the number of template string columns (second row).

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

### Individual Type Error

The valid `INDIVIDUAL_TYPE` values are: `named`, `same`, and `different`.

### Individual Type Split Error

An individual row may only use one of: `named`, `same`, and `different`. To add other types of axioms on an OWL individual, use a separate row.

### IRI Error

The IRI provided as the value (in a row) to an `AI` template string could not be resolved as an IRI. Check your template to ensure the provided value is in a correct IRI or CURIE format. If using CURIEs, remember to ensure the prefix is defined.

### Language Format Error

The template string for an `AL` annotation must always include `@`.
```
AL rdfs:label@en
```

### Manchester Parse Error

The provided value cannot be parsed and may not be in proper Manchester syntax. See [Manchester Syntax](http://www.w3.org/2007/OWL/wiki/ManchesterSyntax) for more details. If you are using labels, make sure the labels are defined in the `--input` ontology or using the `LABEL` column. Also ensure that all properties use a label instead of a CURIE or IRI.

### Merge Error

`--merge-before` and `--merge-after` cannot be used simultaneously.

### Missing Template Error

You must specify at least one template with `--template` to proceed.

### Missing Type Error

If no `CLASS_TYPE` column is included, ROBOT will default to using `subclass`. If a `CLASS_TYPE` column is included, though, each row must include a specified class type. If the `CLASS_TYPE` is left empty, this error message will be returned.

### Multiple Property Type Error

While the `PROPERTY_TYPE` column may include multiple types, only one of the logical types is allowed in each column: `subproperty`, `equivalent`, `disjoint`, or (for object properties only) `inverse`. To add other types of axioms on an OWL property, use a separate row.

### Null ID Error

An IRI cannot be created from the provided ID. This is most likely because the ID is not formatted properly, as an IRI or a CURIE.

### Property Type Error

The valid `PROPERTY_TYPE` values are: `subproperty`, `equivalent`, `disjoint`, and (for object properties only) `inverse`.

### Template File Error

The template cannot be found in the current directory. Make sure the file exists and your path is correct.

### Typed Format Error

The template string for an `AT` annotation must always include `^^`.
```
AT rdfs:label^^xsd:string
```

### Unknown Template Error

Valid template strings are limited to the <a href="#template-strings">described above</a>. If a different template string is provided, this error message will be returned.
