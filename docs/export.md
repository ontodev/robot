# Export

ROBOT can export details about ontology entities as a table (TSV or CSV). At minimum, the `export` command expects an input ontology (`--input`), a set of column headers (`--header`), and a file to write to (`--export`):

```
robot export --input nucleus.owl \
  --header "CURIE,LABEL" \
  --export nucleus.csv
```

### Columns

The `--header` option is a delimiter-separated (usually comma or tab) list of special keywords or properties used in the ontology. The delimiter is predicted based on the `--export` path file extension (`.csv` is comma-separated, anything else will default to tab-separated). If you want to override the default delimiter, use the `--delimiter <delim>` option. The `--header` argument will exactly match the first line of the export file (the column headers).

Various `--header` types are supported:

* **Special Headers**:
	* `IRI`: creates an "IRI" column based on the full unique identifier
	* `CURIE`: creates a "CURIE" column based on the short form of the unique identifier
	* `LABEL`: creates a "Label" column based on `rdfs:label`
	* `SubClass Of`: creates a "SubClass Of" column based on `rdfs:subClassOf`
	* `Eqivalent Class`: creates an "Equivalent Classes" column based on `owl:equivalentClass`
	* `SubProperty Of`: creates a "SubProperty Of" column based on `rdfs:subPropertyOf`
	* `Equivalent Property`: creates an "Equivalent Properties" column based on `owl:equivalentProperty`
	* `Disjoint With`: creates a "Disjoint With" column based on `owl:disjointWith`
	* `Type`: creates an "Instance Of" column based on `rdf:type` for named individuals
* **Property CURIES**: you can always reference a property by the short form of the unique identifier (e.g. `oboInOwl:hasDbXref`). ROBOT will attempt to find a label for a CURIE to set as the column header, but if the label is not defined in the ontology, the header will be the CURIE. Any prefix used [must be defined](global/prefixes).
* **Property Labels**: as long as a property label is defined in the input ontology, you can reference a property by label enclosed in single quotes (e.g. `database_cross_reference`). This label will also be used as the column header.

The first header in the `--header` list is used to sort the rows of the export. You can change the column that is sorted on by including `--sort <header>`. This can either be one header, or a delimiter-separated list of headers that will be sorted in-order:

    robot export --input nucleus.owl \
      --header "CURIE,LABEL,SubClass Of" \
      --sort "SubClass Of,LABEL" \
      --export results/nucleus-sorted.csv

If the `--sort` header starts with `*`, the column will be sorted in reverse order:

    robot export --input nucleus.owl \
      --header "CURIE,LABEL,SubClass Of" \
      --sort "*LABEL" \
      --export results/nucleus-reversed.csv

All special keyword columns will include both named OWL objects (named classes, properties, and individuals) and anonymous expressions (class expressions, property expressions). When using another object or data property, the values will include both individuals and class expressions (from subclass or equivalent statements) in Manchester syntax. When using an annotation property, the literal value will be returned.

### Including and Excluding Entities

By default, the export includes details on the classes and individuals in an ontology. Properties are excluded. You can configure which types of entities you wish to include with the `--include <entity types>` option. The `<entity types>` argument is a space-, comma-, or tab-separated list of one or more of the following entity types:

* `classes`
* `individuals`
* `properties`

For example, to return the details of *individuals only*:

    robot export --input template.owl \
      --header "CURIE,LABEL,Type" \
      --include "individuals" \
      --export results/individuals.csv

To return details of *classes and properties*:

    robot export --input nucleus.owl \
      --header "CURIE,LABEL,SubClass Of,SubProperty Of" \
      --include "classes properties" \
      --export results/classes-properties.csv

The `--include` option does not need to be specified if you are getting details on individuals and classes. If you do specify an `--include`, it cannot be an empty string, as no entities will be included in the export.

Finally, the export will include anonymous expressions (subclasses, equivalent classes, property expressions). If you only wish to include *named* entities, add `--exclude-anonymous true`:

    robot export --input nucleus.owl \
      --header "LABEL,SubClass Of,part of" \
      --exclude-anonymous true \
      --export results/nucleus.csv

Note that in the example above, the first two headers are special keywords and the third is the label of a property used in the ontology.

### Preparing the Ontology

When exporting details on classes using object or data properties, we recommend running [reason](/reason), [relax](/relax), and [reduce](/reduce) first. You can also create a subset of entities using [remove](/remove) or [filter](/filter).

---

## Error Messages

### Exclude All Error

`--include` cannot be an empty string as there will be no entities to return details on. Note that classes and individuals are *included* by default and properties are *excluded* by default.

### Invalid Column Error

A property cannot be resolved, usually meaning that the label cannot be resolved. Ensure that the property label is defined in the input ontology or the column name provided is one of the special keywords.
