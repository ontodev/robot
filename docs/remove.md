# Remove

The `remove` command allows you to remove selected axioms from an ontology. The [`filter`](/filter) command is the opposite of `remove`, allowing you to keep only selected axioms. `remove` works in three steps:

1. `--term`: specify a set of terms (default: all terms, use `--term-file` for a file with terms line by line)
2. `--select`: select a new set of terms using one or more relations (zero or more)
3. `--axioms`: specify the axiom types to remove from those terms (default: all axioms)

This operation maintains structural integrity; lineage is maintained, and gaps will be filled where classes have been removed. If you wish to *not* preserve the hierarchy, include `--preserve-structure false`.

For example, to remove all descendants of 'assay' from OBI:

```
robot remove --input obi.owl --term OBI:0000070 --select descendants
```

`remove` also includes a `--trim` option, set to `true` by default. For an axiom to be removed, *one or more* of the entities in that axiom must be in the removal set. If `--trim false` is specified, *all* entities in the axiom must be in the selected set of terms. Dangling entities (entities without any axioms *about* them) will also be removed when `--trim` is `true`.

If you are removing import statements and wish to keep references to imported terms, use `--trim false`. Otherwise, the dangling entities left over from removing the imports will also be removed.

## Select

`--select` supports multiple options. These can be provided in quotes in one select statement (`--select "x y z"`), or in multiple statements (`--select x --select y --select z`). When selects are provided in multiple statements, the output of the first will be passed to the second and so on. If multiple options are provided in one statement, all options will be processed at the same time.

The following selection options can be combined in any way.

#### General

There are three general select options that give control over the types of axioms that are removed. By default, both `named` and `anonymous` entities are removed. But, for example, if only `--select anonymous` is provided, the named classes will not be removed.

1. `complement`: remove the complement set of the terms (equivalent to [filter](/filter), except that `remove` creates a new ontology whereas `filter` edits the input ontology).
2. `named`: remove named entities.
3. `anonymous`: remove anonymous entities (e.g. anonymous ancestors).
4. `imports`: remove import statements *

\* The `imports` selection cannot be used with [filter](/filter)

#### Relation Types

Relation type selections provide the ability to select which terms to remove based on their relationship to the term (or terms) specified by `--term`/`--term-file`. If no relation type `--select` is provided, the default is `self`. This means that the term set itself will be removed.

1. `self` (default)
2. `parents`
3. `ancestors`
4. `children`
5. `descendants`
6. `equivalents`
7. `types`
8. `instances`

#### Entity Types

If an entity type is provided, only the terms of that type will be included in the removal. By default, all types are included.

1. `classes`
2. `properties`
3. `object-properties`
4. `data-properties`
5. `annotation-properties`
6. `individuals`

### Patterns

Terms can also be selected from the set based on axioms. This can be helpful if you want to remove only terms with a specific annotation. When selecting with axioms, the `--select` option must always be quoted.

1. `CURIE=CURIE`
2. `CURIE=<IRI>`
3. `CURIE='literal'`
4. `CURIE='literal'^^datatype`
5. `CURIE=~'regex pattern'`

## Axioms

The `--axioms` option allows you to specify the type of OWLAxiom to remove. More than one type can be provided and the order is not significant. For each axiom in the ontology (not including its imports closure), if the axiom implements one of the specified axiom types AND *any* of the selected terms are in the axiom's signature, then the axiom is removed from the ontology.

1. `all` (default)
2. `logical`
3. `annotation`
4. `subclass`
5. `subproperty`
6. `equivalent` (classes and properties)
7. `disjoint` (classes and properties)
8. `type` (class assertions)
9. `tbox` (classes and class axioms)
10. `abox` (instances and instance-level axioms)
11. `rbox` (object properties, aka relations)

## Examples

Remove a class ('organ') and and all its descendants:

    robot remove --input uberon_module.owl \
      --term UBERON:0000062 \
      --select "self descendants" \
      --output results/remove_class.owl

Remove all individuals from OBI:

```
robot remove --input obi.owl --select individuals 
```

Remove all anonymous entities from the UBERON module:

    robot remove --input uberon_module.owl \
      --select anonymous \
      --output results/remove_anonymous.owl

Remove all deprecated classes from OBI:

```
robot remove --input obi.owl \
  --select "owl:deprecated='true'^^xsd:boolean" 
```

*Filter* for only desired annotation properties (in this case, label and ID). This works by actually *removing* the opposite set of annotation properties (complement annotation-properties) from the ontology:

    robot remove --input uberon_module.owl \
      --term rdfs:label --term oboInOwl:id  \
      --select complement --select annotation-properties \
      --output results/filter_annotations.owl
