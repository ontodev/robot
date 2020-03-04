# Remove

## Contents

1. [Overview](#overview)
2. [Preserving the Structure (`--preserve-structure`)](#preserving-the-structure)
2. [Selectors (`--select`)](#selectors)
3. [Axioms (`--axioms`)](#axioms)
4. [Examples](#examples)

## Overview

The `remove` command allows you to remove selected axioms from an ontology. The [`filter`](/filter) command is the opposite of `remove`, allowing you to keep only selected axioms. To use these powerful commands effectively, some familiarity with OWL and OWLAPI is helpful.

`remove` works in four steps:

1. specify an initial target set of objects to remove
2. use selectors to broaden or narrow the target set of objects
3. specify the types of axioms to consider
4. check each axiom of specified type against the target set of objects

Here is how each step works in more detail:

1. The `--term` and `--term-file` options let you specify the initial target set. You can specify zero or more `--term` options (one term each), and zero or more `--term-file` options (with one term per line). You can specify terms by IRI or CURIE. If no terms are specified, then the initial target set consists of all the objects in the ontology.

    * If you wish to exclude a term or set of terms that would be removed otherwise, you can do so with `--exclude-term <term>` or `--exclude-terms <term-file>`. These terms will **never** be removed.
    * If you wish to *include* a term or set of terms that would be kept otherwise, you can do so with `--include-term <term>` or `--include-terms <term-file>`. These terms will **always** be removed.

2. The `--select` option lets you specify "selectors" that broaden or narrow the target set of objects. Some selectors such as `classes` take the target set and return a subset, i.e. all the classes in the target set. Other selectors such as `parents` return a new target set of related objects. The `parents` selector gets the parents of each object in the target set. Note that the `parents` set does not include the original target set, just the parents. Use the `self` selector to return the same target set. You can use multiple selectors together to get their union, so `--select "self parents"` will return the target set (`self`) and all the parents. You can use `--select` multiple times to modify the target set in several stages, so `--select parents --select children` will get the parents and then all their children, effectively returning the target set objects and all their siblings.

3. The `--axioms` option lets you specify which axiom types to consider. By default, all axioms are considered, but this can be restricted to annotation axioms, logical axioms, more specific subtypes, or any combination.

4. The final step is to take each axiom of the specified types, and compare it to the target set. Two options are important for this step. When `--signature true` is used, the named objects for the axiom (aka the "signature" of the axiom) are compared to the target set. When `--signature false` is used (the default), all named *and* anonymous objects for the axiom are compared to the target set. Named objects, such as classes, have IRIs, while anonymous objects, such as class expressions, do not have IRIs. When using `remove --trim true` (the default), if *any* object for the axiom is in the target set then that axiom is removed. When using `remove --trim false`, if *all* objects for the axiom are in the target set, then that axiom is removed.

Consider an example with this small ontology:

```
ax1: A subClassOf B
ax2: A subClassOf R some C
ax3: D subClassOf E
```

Then `remove --term A --term R --term C --select "self parents" --axioms all --signature true --trim true` works as follows:

1. The initial target set includes all the specified `--term`s: `{A, R, C}`
2. For `self` we get just the initial target set `{A, R, C}`; for `parents` we get `{B, R some C}`; so the new target set is the union `{A, B, R, C, R some C}`
3. `--axioms all` means that we consider all axioms
4. For each axiom we compare the objects for the axiom to the target set. With `--signature true` we only consider the named objects, i.e. objects that have IRIs, not anonymous objects such as `R some C`. With `--trim true` we are checking that *any* object for the axiom is in the target set.
    - the objects for `ax1` are `{A, B}`, and at least one of these is in the target set, so `ax1` is matched and removed
    - the objects for `ax2` are `{A, R, C}` (with `R some C` excluded), and at least one of these is in the target set, so `ax2` is matched and removed
    - the objects for `ax3` are `{D, E}`, and none of these are in the target set, so `ax3` is not matched and is not removed

## Preserving the Structure

The `remove` and `filter` operations maintains structural integrity by default: lineage is maintained, and gaps will be filled where classes have been removed. If you wish to *not* preserve the hierarchy, include `--preserve-structure false`.


## Selectors

You can use the `--select` option zero or more times, providing one or more "selectors" each time. These can be provided in quotes in one select statement (`--select "x y z"`), or in multiple statements (`--select x --select y --select z`). When selects are provided in multiple statements, the output of the first will be passed to the second and so on. If multiple options are provided in one statement, all options will be processed at the same time, resulting in a union target set.

### Subset Selectors

These selectors take a target set and return a subset:

- `classes`
- `properties`
- `object-properties`
- `data-properties`
- `annotation-properties`
- `individuals`
- `named` -- named entities have an IRI
- `anonymous` -- anonymous entities do not have an IRI, e.g. class expressions

### Relation Selectors

These selectors consider each object in the target set, and return all the objects that have some relation

- `self` (default) -- just return the target set, useful when combined with other selectors
- `parents`
- `ancestors`
- `children`
- `descendants`
- `equivalents`
- `types`
- `instances`
- `domains`
- `ranges`
- `complement` -- all objects that are *not* in the target set; this can make `remove` act somewhat like `filter`, and vice versa
- `ontology` -- remove ontology annotations (for [filter](/filter), this returns just the ontology annotations)
- `imports` -- remove import statements and "dangling" references to imported terms (for [filter](/filter), this will copy the import declarations to the output ontology) **Warning:** If you wish to remove import statements but keep dangling references to imported terms, you must use `--trim false`.

### Pattern Subset Selectors

Terms can also be selected from the set based on their annotations. This can be helpful if you want to remove only terms with a specific annotation. When selecting with axioms, the `--select` option must always be quoted.

- `CURIE=CURIE`, e.g. `rdfs:seeAlso=obo:EX_123`
- `CURIE=<IRI>`, e.g. `rdfs:seeAlso=<http://purl.obolibrary.org/obo/EX_123>`
- `CURIE='literal'`, e.g. `rdfs:label='example label'`
- `CURIE='literal'^^datatype`, e.g. `rdfs:label='example label'^^xsd:string`
- `CURIE='literal'@language`, e.g. `rdfs:label='example label'@en`
- `CURIE=^^datatype`, e.g. `rdfs:label=^^xsd:string`
- `CURIE=@language`, e.g. `rdfs:label=@en`
- `CURIE=~'regex pattern'`, e.g. `rdfs:label=~'example.*'`

It is also possible to select terms based on parts of their IRI. You may include an IRI pattern using one or more wildcard characters (`*` and/or `?`) enclosed in angle brackets. This MUST be enclosed in quotes to work on the command line.

- `<IRI-pattern>`, e.g. `<http://purl.obolibrary.org/obo/EX_*>`

If an IRI uses a literal `*` or a `?` character, these must be escaped (e.g., `http://example.com?[WILDCARD]` would become `http://example.com\?[WILDCARD]`).

If you wish to match a more complicated pattern, you may also use a regex pattern here by preceding the pattern with a tilde (`~`):

- `<~IRI-regex>`, e.g. `<~^.+EX_[0-9]{7}$>`

If you do not want to type the full IRI out, you can also pattern match with `*` and `?` by CURIE for any [loaded prefixes](/global#prefixes). For example, to select everything in both the OBI and IAO namespaces:

- `--select "OBI:* IAO:*"`

For regex pattern matching with CURIEs, simply prefix the CURIE pattern with `~`, as we do for IRIs:

- `--select "~EX:[0-9]{7}$`

For CURIEs, the pattern must always come after the prefix and colon.

## Axioms

The `--axioms` option allows you to specify the type of OWLAxiom to remove. More than one type can be provided and these will be processed **in order**. For each axiom in the ontology (not including its imports closure), if the axiom implements one of the specified axiom types AND *any* of the selected terms are in the axiom's signature, then the axiom is removed from the ontology.

Basic axiom selectors select the axiom(s) based on the OWLAPI AxiomType. We have included some special shortcuts to group related axiom types together.
- `all` (default)
- `logical`
- `annotation`
- `subclass`
- `subproperty`
- `equivalent`: classes and properties
- `disjoint`: classes and properties
- `type`: class assertions
- `tbox`: classes and class axioms
- `abox`: instances and instance-level axioms
- `rbox`: object properties, i.e., relations
- [OWLAPI AxiomType](http://owlcs.github.io/owlapi/apidocs_4/org/semanticweb/owlapi/model/AxiomType.html) name (e.g., `ObjectPropertyRange`)

There are also some special axiom selectors that use additional processing to find certain axioms:
- `internal`: all entities that are in one of the `--base-iri` namespaces
- `external`: all entities that are not in one of the `--base-iri` namespaces
- `tautologies`: all axioms that are *always* true; these would be entailed in an empty ontology. WARNING: this may remove more axioms than desired.
- `structural-tautologies`: all axioms that match a set of tautological patterns (e.g., `X SubClassOf owl:Thing`, `owl:Nothing SubClassOf X`, `X SubClassOf X`)

The `--base-iri <namespace>` is a special option for use with `internal` and `external` axioms. It allows you to specify one or more "base namespaces" (e.g., `--base-iri http://purl.obolibrary.org/obo/OBI_`). You can also use any defined prefix (e.g., `--base-iri OBI`) An axiom is considered internal if the subject is in one of the base namespaces.

## Examples

Remove a class ('organ'), all its descendants, and any axioms using these classes:

    robot remove --input uberon_module.owl \
      --term UBERON:0000062 \
      --select "self descendants" \
      --signature true \
      --output results/remove_class.owl

Remove all axioms containing an entity in the BFO namespace from the UBERON module:

    robot remove --input uberon_module.owl \
      --select "<http://purl.obolibrary.org/obo/BFO_*>" \
      --signature true \
      --output results/remove_bfo.owl

Remove all anonymous entities from the UBERON module:

    robot remove --input uberon_module.owl \
      --select anonymous \
      --signature false \
      --output results/remove_anonymous.owl

Remove all individuals from OBI:

```
robot remove --input obi.owl --select individuals
```

Remove all deprecated classes from OBI:

```
robot remove --input obi.owl \
  --select "owl:deprecated='true'^^xsd:boolean"
```

Remove the definitions (may be annotated with `oboInOwl:hasDbXref` or not) from a given set of terms. Note that `IAO:0000115` (definition) and `oboInOwl:hasDbXref` are included in the `--term-file`.
 
    robot remove --input fbcv-module.owl \
      --term-file fbcv-remove.txt \
      --axioms annotation \
      --trim false \
      --signature true \
      --output results/fbcv-removed.owl

Remove structural tautologies (e.g., `owl:Nothing`):

    robot remove --input tautologies.owl \
      --axioms structural-tautologies \
      --output results/no-tautologies.owl

Remove disjoint classes and class assertions:

    robot remove --input template.owl \
      --axioms "DisjointClasses ClassAssertion" \
      --output results/no-class-logic.owl

Create a "base" subset by removing external axioms (alternatively, use `filter --axioms internal`):

    robot remove --input template.owl \
      --base-iri http://example.com \
      --axioms external \
      --exclude-term IAO:0000117 \
      --exclude-term IAO:0000119 \
      --preserve-structure false \
      --trim false \
      --output results/template-base.owl

*Filter* for only desired annotation properties (in this case, label and ID). This works by actually *removing* the opposite set of annotation properties (complement annotation-properties) from the ontology:

    robot remove --input uberon_module.owl \
      --term rdfs:label \
      --term oboInOwl:id  \
      --select complement \
      --select annotation-properties \
      --signature true \
      --output results/filter_annotations.owl
