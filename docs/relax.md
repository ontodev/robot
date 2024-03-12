# Relax

## Contents

1. [Overview](#overview)
1. [Exclude named classes from relax (`--exclude-named-classes`)](#exclude-named-classes)
1. [Relax subclass of axioms in addition to equivalents (`--include-subclass-of`)](#include-subclass-of)
1. [Enforce OBO format (`--enforce-obo-format`)](#enforce-obo-format)

## Overview

Robot can be used to relax Equivalence Axioms to weaker SubClassOf axioms. The resulting axioms will be redundant with the stronger equivalence axioms, but may be useful for applications that only consume SubClassOf axioms

Example:

    robot relax  --input ribosome.owl --output results/relaxed.owl
    
## Motivation

Many ontology make use of OWL EquivalenceAxioms, particularly during the development cycle. These are required for being able to use the [reason](/reason) command to classify an ontology. However, many downstream applications are not equipped to use these. A common scenario is to treat the ontology as a graph, and this graph is typically formed from the SubClassOf axioms in an ontology (both those connecting two named classes, and subClasses of "some values from" restrictions). The relax command allows us to capture some of the information in a form that is accessible to basic downstream applications.

For example, given an ontology with:

```
finger EquivalentTo digit and 'part of' some hand
```

Applications that cannot consume equivalence axioms may still wish to know that fingers are parts of hands. The relax command will add two axioms:

```
finger SubClassOf digit
finger SubClassOf 'part of' some hand
```

## Combination with other commands

A common sequence is [reason](/reason) [relax](/relax) [reduce](/reduce), with the last step removing any redundancies in the SubClassOf graph introduced by the relax step.

For example, given an ontology with the following axioms:

```
1. 'cerebellar neuron' EquivalentTo neuron and 'part of' some cerebellum
2. 'hindbrain neuron' EquivalentTo neuron and 'part of' some brain
3. cerebellum SubClassOf 'part of' some brain
4. Transitive('part of')
```

Running `relax` will yield the following axioms about cerebellar neurons:

```
5. 'cerebellar neuron' SubClassOf neuron
6. 'cerebellar neuron' SubClassOf 'part of' some cerebellum
```

Running `reason` will yield the following axioms about cerebellar neurons:

```
7. 'cerebellar neuron' SubClassOf 'hindbrain neuron`
```

Running `reduce` will remove the redundant axiom (5), leaving the following axioms about cerebellar neurons:

```
6. 'cerebellar neuron' SubClassOf 'part of' some cerebellum
7. 'cerebellar neuron' SubClassOf 'hindbrain neuron'
```

This SubClassOf graph is complete and non-redundant, and can be used for intuitive visualization and browsing of the ontology

## Note about normalization of Qualified Number restrictions

For convenience of downstream processing, `relax` rewrites expressions of the kind `:R min 1 :A` or `:R min 2 :A` to `:R some :A`. This is safe, because `:R some :A` is implied by any cardinality restriction > 0.

<a id="exclude-named-classes"></a>

## Exclude named classes (`--exclude-named-classes/-x`)

By default, axioms of the form:

```
:A EquivalentTo :B
```

where `:A` and `:B` are named classes are not relaxed to be:

```
:A SubClassOf :B
:B SubClassOf :A
```

In some cases, this may be desired; in these cases, the `-x/--exclude-named-classes` can be set to `false`.

Example to ensure that named classes are not relaxed (this is the default):

    robot relax --input relaxed2.owl --exclude-named-classes true --output results/relaxed-exclude-named.owl

<a id="include-subclass-of"></a>

## Relax subclass of axioms (`--include-subclass-of/-s`)

By default, relax is only concerned with relaxing `EquivalentClasses` axioms. However, some of the magic of the `relax` commmand is the simplification of complex conjunctive expressions, for example, as described above:

```
finger EquivalentTo digit and 'part of' some hand
```

is relaxed to:

```
finger SubClassOf digit
finger SubClassOf 'part of' some hand
```

In many cases it makes sense to also relax `SubClassOf` axioms this way. For example:

```
finger SubClassOf: digit and 'part of' some hand
```

can be relaxed to:

```
finger SubClassOf digit
finger SubClassOf 'part of' some hand
```

This can be achieved by setting the `--include-subclass-of` option to `true`.

Example:

    robot relax --input relaxed2.owl --include-subclass-of true --output results/relaxed-include-subclass.owl

<a id="enforce-obo-format"></a>

## Enforce OBO format (`--enforce-obo-format/-x`)

OBO format is a widely used representation for OBO ontologies. Its "graphy" nature lends itself as a simple intermediate towards graph-like representation of ontologies, such as obo-graphs JSON. For many use cases, we do not wish to assert non-graphy expressions such as `:R only :B` or `:A or :B`, expressions with nested sub-expressions such as `:R some (:S some B)` or similar. In cases where we only want to assert expressions that are simple existential restrictions, we can use the `--enforce-obo-format` option. This will process complex expressions (that potentially include _some_ complex and some _simple_ sub-expression), but only assert relaxed statements if they correspind to simple subexpression.

Example:

    robot relax --input relaxed2.owl --enforce-obo-format true --output results/relaxed-enforced-obo.owl
