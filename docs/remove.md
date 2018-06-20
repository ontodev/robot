# Remove

The `remove` command allows you to remove selected axioms from an ontology. The [`filter`](/filter) command is the opposite of `remove`, allowing you to keep only selected axioms. `remove` works in three steps:

1. `--entity`: specify a set of entities (default: all entities)
2. `--select`: select a new set of entities using one or more relations (zero or more)
3. `--axioms`: specify the axiom types to remove from those entities (default: all axioms)

For example, to remove all descendants of 'assay' from OBI:

```
robot remove --input obi.owl --entity OBI:0000070 --select descendants
```

`remove` also includes a `--trim` option, set to false by default. If `--trim true` is included, ROBOT will remove any dangling entites left behind by removals. By contrast, in `filter`, `--trim` defaults to true.

## Select

`--select` supports multiple options. These can be provided in quotes in one select statement (`--select "x y z"`), or in multiple statements (`--select x --select y --select z`). When selects are provided in multiple statements, the output of the first will be passed to the second and so on. If multiple options are provided in one statement, all options will be processed at the same time.

The following selection options can be combined in any way.

#### General

There are three general select options that give control over the types of axioms that are removed. By default, both `named` and `anonymous` axioms are removed. But, for example, if only `--select anonymous` is provided, the named classes will not be removed.

1. `complement`: remove the complement set of the entities (equivalent to [filter](/filter), except that `remove` creates a new ontology whereas `filter` edits the input ontology).
2. `named`: remove named entities.
3. `anonymous`: remove anonymous entities (e.g. anonymous ancestors).
4. `imports`: remove import statements *

\* The `imports` selection cannot be used with [filter](/filter)

#### Relation Types

Relation type selections provide the ability to select which entities to remove based on their relationship to the entity (or entities) specified by `--entity`/`--entities`. If no relation type `--select` is provided, the default is `self`. This means that the entity set itself will be removed.

1. `self` (default)
2. `parents`
3. `ancestors`
4. `children`
5. `descendants`
6. `equivalents`
7. `types`

#### Entity Types

If an entity type is provided, only the entities of that type will be included in the removal. By default, all types are included.

1. `classes`
2. `properties`
3. `object-properties`
4. `data-properties`
5. `annotation-properties`
6. `individuals`

### Patterns

Entities can also be selected from the set based on axioms. This can be helpful if you want to remove only entities with a specific annotation. When selecting with axioms, the `--select` option must always be quoted.

1. `CURIE=CURIE`
2. `CURIE=<IRI>`
3. `CURIE='literal'`
4. `CURIE='literal'^^datatype`
5. `CURIE=~'regex pattern'`

## Axioms

`--axioms` can be the name of any OWLAPI interface that implements OWLAxiom. Aliases are provided for convenience: `all`, `annotation`, `logical`, `class`, `equivalent-classes`, etc. The default is `all`, e.g. OWLAxiom. More than one type can be provided and order is not significant. For each axiom in the ontology (not including its imports closure), if the axiom implements one of the specified interfaces AND *any* of the selected entities is in the axiom's signature, then the axiom is removed from the ontology.

## Examples

1. Remove 'assay' and all its descendants from OBI:

```
robot remove --input obi.owl --entity OBI:0000070 --select "self descendants"
```

2. Remove all individuals from OBI:

```
robot remove --input obi.owl --select individuals
```

3. Remove all anonymous classes from OBI:

```
robot remove --input obi.owl --select classes --select anonymous
```

4. Remove all deprecated classes from OBI:

```
robot remove --input obi.owl --select "owl:deprecated='true'^^xsd:boolean"
```

5. *Filter* for only desired annotation properties (in this case, label and ID). This works by actually *removing* the opposite set of annotation properties (complement annotation-properties) from the ontology:

```
robot remove --input edit.owl \
  --entity rdfs:label --entity oboInOwl:id \
  --select "complement annotation-properties"
```

---

## Error Messages

### DataType Error

This error message occurs if something other than `boolean`, `integer`, `double`, or `float` is provided as a datatype on an annotation value in `--select`.

### Literal Value Error

This error message occurs when the value provided does not match a given datatype. For example, if you specify the `boolean` datatype, but provide an integer, it will fail.

### Select Error

This error message occurs when an unsupported argument is provided for `--select`. Review the documentation above for valid `--select` statements.