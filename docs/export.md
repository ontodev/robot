# Export

## Contents

1. [Formats](#formats)
2. [Columns](#columns)
3. [Including and Excluding Entities](#including-and-excluding-entities)
4. [Rendering Cell Values](#rendering-cell-values)
5. [Preparing the Ontology](#preparing-the-ontology)

ROBOT can export details about ontology entities as a table. At minimum, the `export` command expects an input ontology (`--input`), a set of column headers (`--header`), and a file to write to (`--export`):

```
robot export --input nucleus_part_of.owl \
  --header "ID|LABEL" \
  --export nucleus.csv
```

### Formats

The following formats are currently supported:

* `tsv` - tab-separated file, default format for unknown extensions
* `csv` - comma-separated file
* `html` - HTML table with with [Bootstrap](https://getbootstrap.com/) styling
* `json` - JSON file with values as arrays (except for ID/CURIE and IRI, which are single strings)
* `xlsx` - Excel workbook with contents on first sheet

These can be specified with the `--format` option:

    robot export --input nucleus_part_of.owl \
      --header "LABEL|SubClass Of" \
      --format html --export results/nucleus.html
 
If this option is not included, `export` will predict the format based on the file extension:
 
    robot export --input nucleus_part_of.owl \
      --header "ID|LABEL|SubClass Of" \
      --export results/nucleus.json

### Columns

The `--header` option is a pipe-separated list of special keywords or properties used in the ontology. The columns in the `--header` argument will exactly match the first line of the export file (the column headers).

Various `--header` types are supported:

* **Special Headers**:
	* `IRI`: creates an "IRI" column based on the full unique identifier
	* `ID`: creates an "ID" column based on the short form of the unique identifier (CURIE) - please note that all IRIs must have [defined prefixes](global/prefixes), or the full IRI will be returned.
	* `LABEL`: creates a "Label" column based on `rdfs:label` (`rdfs:label` can also be used in place of this column)
	* `SYNONYMS`: creates a "SYNONYMS" column based on all synonyms (oboInOwl exact, broad, narrow, related, or IAO alternative term)
	* `SubClass Of`: creates a "SubClass Of" column based on `rdfs:subClassOf`
	* `SubClasses`: creates a "SubClasses" column based on direct children of a class
	* `Equivalent Class`: creates an "Equivalent Classes" column based on `owl:equivalentClass`
	* `SubProperty Of`: creates a "SubProperty Of" column based on `rdfs:subPropertyOf`
	* `Equivalent Property`: creates an "Equivalent Properties" column based on `owl:equivalentProperty`
	* `Disjoint With`: creates a "Disjoint With" column based on `owl:disjointWith`
	* `Type`: creates an "Instance Of" column based on `rdf:type` for named individuals or the OWL EntityType for all others (e.g., `Class`)
* **Property CURIES**: you can always reference a property by the short form of the unique identifier (e.g. `oboInOwl:hasDbXref`). Any prefix used [must be defined](global/prefixes).
* **Property Labels**: as long as a property label is defined in the input ontology, you can reference a property by label (e.g. `database_cross_reference`). This label will also be used as the column header.

The first header in the `--header` list is used to sort the rows of the export. You can change the column that is sorted on by including `--sort <header>`. This can either be one header, or a pipe-separated list of headers that will be sorted in-order:

    robot export --input nucleus_part_of.owl \
      --header "ID|LABEL|SubClass Of" \
      --sort "LABEL|SubClass Of" \
      --export results/nucleus-sorted.csv
      
In the example above, the rows are first sorted on the `NAME` field, and then sorted by `SubClass Of`. This means that entities with the same parent will be grouped in alphabetical order.

If the `--sort` header starts with `^`, the column will be sorted in reverse order.

    robot export --input nucleus_part_of.owl \
      --header "ID|LABEL|SubClass Of" \
      --sort "^LABEL" \
      --export results/nucleus-reversed.csv

All special keyword columns will include both named OWL objects (named classes, properties, and individuals) and anonymous expressions (class expressions, property expressions). When using another object or data property, the values will include both individuals and class expressions (from subclass or equivalent statements) in Manchester syntax. When using an annotation property, the literal value will be returned.

By default, multiple values in a cell are separated with a pipe character (`|`). You can update this to anything you'd like with the `--split` option. For example, you could separate with commas:
```
robot export --input nucleus_part_of.owl \
  --header "NAME|SubClass Of" --split ", "
```

The output of any cell with multiple values is sorted in alphabetical order.

### Including and Excluding Entities

By default, the export includes details on the classes and individuals in an ontology. Properties are excluded. You can configure which types of entities you wish to include with the `--include <entity types>` option. The `<entity types>` argument is a space-, comma-, or tab-separated list of one or more of the following entity types:

* `classes`
* `individuals`
* `properties`

For example, to return the details of *individuals only*:

    robot --prefix "example: http://example.com/" \
      export --input template.owl \
      --header "ID|LABEL|Type" \
      --include "individuals" \
      --export results/individuals.csv

To return details of *classes and properties*:

    robot export --input nucleus_part_of.owl \
      --header "ID|LABEL|Type|SubClass Of|SubProperty Of" \
      --include "classes properties" \
      --export results/classes-properties.csv

The `--include` option does not need to be specified if you are getting details on individuals and classes. If you do specify an `--include`, it cannot be an empty string, as no entities will be included in the export.

Finally, the export will include anonymous expressions (subclasses, equivalent classes, property expressions). If you only wish to include *named* entities, add `--exclude-anonymous true`:

    robot export --input nucleus_part_of.owl \
      --header "LABEL|SubClass Of|part of" \
      --exclude-anonymous true \
      --export results/nucleus.csv

Note that in the example above, the first two headers are special keywords and the third is the label of a property used in the ontology.

### Rendering Cell Values

Entities used in cell values are rendered by one of four different strategies:

* `NAME` - render the entity by label (if label does not exist, entity is rendered by CURIE)
* `ID` - render the entity by short form ID/CURIE
* `IRI` - render the entity by full IRI
* `LABEL` - render the entity by label ONLY (if label does not exist, entity is rendered as an empty string)

By default, values are rendered with the `NAME` strategy. To update the strategy globally, you can use the `--entity-format` option and provide one of the above values:
  
    robot export --input nucleus_part_of.owl \
      --header "ID|SubClass Of" \
      --entity-format ID \
      --exclude-anonymous true \
      --export results/nucleus-ids.csv

In the above example, all the "subclass of" values will be rendered by their short form ID.

You can also specify different rendering strategies for different columns by including the strategy name in a square-bracket-enclosed tag after the column name:

    robot export --input nucleus_part_of.owl \
      --header "rdfs:label|SubClass Of [ID]|SubClass Of [IRI]" \
      --exclude-anonymous true \
      --export results/nucleus-iris.csv

These tags should not be used with the following default columns: `LABEL`, `ID`, or `IRI` as they will not change the rendered values.

### Preparing the Ontology

When exporting details on classes using object or data properties, we recommend running [reason](/reason), [relax](/relax), and [reduce](/reduce) first. You can also create a subset of entities using [remove](/remove) or [filter](/filter).

---

## Error Messages

### Include Nothing Error

`--include` cannot be an empty string as there will be no entities to return details on. Note that classes and individuals are *included* by default and properties are *excluded* by default.

### Invalid Column Error

A property cannot be resolved, usually meaning that the label cannot be resolved. Ensure that the property label is defined in the input ontology or the column name provided is one of the special keywords.

### Unknown Format Error

The following formats are currently supported: `tsv`, `csv`, `html`, `json`, and `xlsx`. Please make sure you are using one of these formats.

### Unknown Tag Error

The tag for rendering following a column name must be one of: `[ID]`, `[IRI]`, `[NAME]`.