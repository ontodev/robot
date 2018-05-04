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

`--select` supports multiple options. These can be provided in quotes in one select statement (`--select "1 2 3"`), or in multiple statements (`--select 1 --select 2 --select 3`). When selects are provided in multiple statements, the output of the first will be passed to the second and so on. If multiple options are provided in one statement, all options will be processed at the same time.

The following selection options can be combined in any way.

#### General

There are three general select options that give control over the types of axioms that are removed. By default, both `named` and `anonymous` axioms are removed. But, for example, if only `--select anonymous` is provided, the named classes will not be removed.

1. `complement`: remove the complement set of the entities (equivalent to `filter`, except that `remove` creates a new ontology whereas `filter` edits the input ontology).
2. `named`: remove named entities.
3. `anonymous`: remove anonymous entities (e.g. anonymous ancestors).

#### Relation Types

Relation type selections provide the ability to select which entities to remove based on their relationship to the entity (or entities) specified by `--entity`/`--entities`. If no `--select` is provided, the default is `self`. This means that the entity set itself will be removed.

1. `self` (default)
2. `parents`
3. `ancestors`
4. `children`
5. `descendants`
6. `equivalents`
7. `types`

#### Entity Types

If an entity type is provided, only the entities of that type will be included in the removal.

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

## Axioms

`--axioms` can be the name of any OWLAPI interface that implements OWLAxiom. Aliases are provided for convenience: `all`, `annotation`, `logical`, `class`, `equivalent-classes`, etc. The default is `all`, e.g. OWLAxiom. More than one type can be provided and order is not significant. For each axiom in the ontology (not including its imports closure), if the axiom implements one of the specified interfaces AND *any* of the selected entities is in the axiom's signature, then the axiom is removed from the ontology.

## Examples

1. Remove 'assay' and all its descendants from OBI:

```
robot remove --input obi.owl --entity OBI:0000070 --select "self descendants"
```

2. Remove everything from OBI except 'assay' and its descendants (`filter` is preferred):

```
robot remove --input obi.owl --entity OBI:0000070 --select "self descendants" --select complement
```

3. Remove all individuals from OBI:

```
robot remove --input obi.owl --select individuals
```

4. Remove all anonymous classes from OBI:

```
robot remove --input obi.owl --select classes --select anonymous
```

5. Remove a subset:

```
robot remove --input edit.owl --select "oboInOwl:inSubset='subset name'"
```
