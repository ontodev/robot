# Relax

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




