# Explain

When reviewing inferred axioms, such as after applying the [`reason`](/reason) command,
you may like more information about why a particular axiom was inferred:

    robot explain --input uvula.ofn --reasoner ELK \
      --axiom "'uvular muscle' SubClassOf 'muscle organ' and 'part of' some 'jaw region'" \
      --explanation results/uvular_muscle.md

Here we provide an axiom in Manchester syntax. If that relationship is entailed by the
axioms asserted in [uvula.ofn](/examples/uvula.ofn), we can use the selected reasoner (here, ELK)
to compute an _explanation_—a minimal set of axioms required to derive that inference. Reviewing
inference explanations is an extremely useful tool for debugging ontology classification issues.
The explanation is printed in Markdown format, which can be easily pasted and rendered within
GitHub issues:

## [uvular muscle](http://purl.obolibrary.org/obo/UBERON_0010235) SubClassOf [muscle organ](http://purl.obolibrary.org/obo/UBERON_0001630) and ([part of](http://purl.obolibrary.org/obo/BFO_0000050) some [jaw region](http://purl.obolibrary.org/obo/UBERON_0011595)) ##

  - [uvular muscle](http://purl.obolibrary.org/obo/UBERON_0010235) SubClassOf [palatal muscle](http://purl.obolibrary.org/obo/UBERON_0003682)
    - [palatal muscle](http://purl.obolibrary.org/obo/UBERON_0003682) EquivalentTo [skeletal muscle organ](http://purl.obolibrary.org/obo/UBERON_0014892) and ([part of](http://purl.obolibrary.org/obo/BFO_0000050) some [soft palate](http://purl.obolibrary.org/obo/UBERON_0001733))
      - [soft palate](http://purl.obolibrary.org/obo/UBERON_0001733) SubClassOf [part of](http://purl.obolibrary.org/obo/BFO_0000050) some [secondary palate](http://purl.obolibrary.org/obo/UBERON_0001716)
        - [secondary palate](http://purl.obolibrary.org/obo/UBERON_0001716) SubClassOf [part of](http://purl.obolibrary.org/obo/BFO_0000050) some [upper jaw region](http://purl.obolibrary.org/obo/UBERON_0001709)
          - [upper jaw region](http://purl.obolibrary.org/obo/UBERON_0001709) SubClassOf [part of](http://purl.obolibrary.org/obo/BFO_0000050) some [jaw region](http://purl.obolibrary.org/obo/UBERON_0011595)
      -  Transitive: [part of](http://purl.obolibrary.org/obo/BFO_0000050)
      - [skeletal muscle organ](http://purl.obolibrary.org/obo/UBERON_0014892) EquivalentTo [muscle organ](http://purl.obolibrary.org/obo/UBERON_0001630) and ([develops_from](http://purl.obolibrary.org/obo/RO_0002202) some [myotome](http://purl.obolibrary.org/obo/UBERON_0003082)) and ([surrounded_by](http://purl.obolibrary.org/obo/RO_0002219) some [epimysium](http://purl.obolibrary.org/obo/UBERON_0011899)) and ([composed primarily of](http://purl.obolibrary.org/obo/RO_0002473) some [skeletal muscle tissue](http://purl.obolibrary.org/obo/UBERON_0001134)) and ([synapsed by](http://purl.obolibrary.org/obo/uberon/core#synapsed_by) some [motor neuron](http://purl.obolibrary.org/obo/CL_0000100))

In addition to outputting the explanation axioms in Markdown using `--explanation` (or `-e`), an ontology containing just
those axioms can be output with `-o` or passed to any subsequent chained commands:

    robot explain --input uvula.ofn --reasoner ELK \
      --axiom "'uvular muscle' SubClassOf 'muscle organ' and 'part of' some 'jaw region'" \
      annotate --annotation rdfs:label "Uvular Muscle Explanation" \
      --ontology-iri "https://github.com/ontodev/robot/examples/uvular_muscle.ofn" \
      --output results/uvular_muscle.ofn

Finally, since there can be more than one way to derive an inference from an ontology,
`explain` includes one more option, `--max`, which allows you to specify the maximum
number of explanations to output (the default is `1`).

The `robot explain` command can be used in three different modes (`--mode/-M`):

`entailment` (default). In this mode, you can check the explanations for a particular axiom as described above.


`inconsistency`. In this mode, you can generate an explanation for an inconsistent ontology.


`unsatisfiability`. In this mode, you can generate explanations for many unsatisfiable classes at once.

To generate an explanation for an inconsistent ontology, you can use:

    robot explain --input uvula_inconsistent.ofn --reasoner ELK \
      -M inconsistency --explanation results/uvula_inconsistent_explanation.md \
        annotate --ontology-iri "https://github.com/ontodev/robot/examples/uvula_inconsistent_explanation.ofn" \
        --output results/uvula_inconsistent_explanation.ofn

You have three options to generate explanations for many unsatisfiable classes at once:


`all`: generate explanations for all unsatisfiable classes in the ontology.


    robot explain --input uvula_multiple_unsat.ofn --reasoner ELK \
      -M unsatisfiability --unsatisfiable all --explanation results/uvula_multiple_unsat_all_explanation.md \
        annotate --ontology-iri "https://github.com/ontodev/robot/examples/uvula_multiple_unsat_all_explanation.ofn" \
        --output results/uvula_multiple_unsat_all_explanation.ofn


`root`: generate explanations for all _root unsatisfiable classes_. In OWL, a root unsatisfiable
class, roughly, is a class whose unsatisfiability cannot be explained by the unsatisfiability of another class.
A comprehensive explanation of the concept can be found [here](https://www.sciencedirect.com/science/article/pii/S1570826805000260).


`most_general`: this is a very naive, experimental variant of the proper `root` method. It determines
explanations for those unsatisfiable classes that, according to the _asserted class hierarchy_, have no parents that are also
unsatisfiable. Note that this approach only works if the class hierarchy does not contain cycles.


`random:n`: Sometimes, you may want to generate explanations for unsatisfiable classes en masse,
but because of the large number in your source ontology, restrict your investigation to a random subset of `n` random classes.
`n` must be a positive natural number.


    robot explain --input uvula_multiple_unsat.ofn --reasoner ELK \
      -M unsatisfiability --unsatisfiable random:2 --explanation results/uvula_multiple_unsat_2.md \
        annotate --ontology-iri "https://github.com/ontodev/robot/examples/uvula_multiple_unsat_2.ofn" \
        --output results/uvula_multiple_unsat_2.ofn


Additional fourth option `list`: Sometimes you just want to get a list of all unsatisfiable classes.
Note: this option does not actually generate explanations.


    robot explain --input uvula_multiple_unsat.ofn --reasoner ELK \
      -M unsatisfiability -u list --explanation results/uvula_multiple_list.txt \
        annotate --ontology-iri "https://github.com/ontodev/robot/examples/uvula_multiple_list.ofn" \
        --output results/uvula_multiple_list.ofn


`robot explain` can be very useful for debugging unsatisfiable classes. A particular unsatisfiable class can be explained with the following command:

    robot explain --input uvula_unsat.ofn --reasoner ELK \
      --axiom "'uvular muscle' EquivalentTo owl:Nothing" \
      annotate --annotation rdfs:label "Explanation for unsatisfiability of Uvular Muscle" \
      --ontology-iri "https://github.com/ontodev/robot/examples/uvular_muscle_unsat.ofn" \
      --output results/uvular_unsat_explanation.ofn


This is particularly useful when dealing with ontologies that are too large for an ordinary desktop machine with Protege.

