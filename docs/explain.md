# Explain

When reviewing inferred axioms, such as after applying the [`reason`](/reason) command,
you may like more information about why a particular axiom was inferred:

    robot explain -i uvula.ofn -r ELK --axiom "'uvular muscle' SubClassOf 'muscle organ' and 'part of' some 'jaw region'" -o uvular_muscle.md

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

Here's the rendered example:

## [uvular muscle](http://purl.obolibrary.org/obo/UBERON_0010235) SubClassOf [muscle organ](http://purl.obolibrary.org/obo/UBERON_0001630) and ([part of](http://purl.obolibrary.org/obo/BFO_0000050) some [jaw region](http://purl.obolibrary.org/obo/UBERON_0011595)) ##

  - [uvular muscle](http://purl.obolibrary.org/obo/UBERON_0010235) SubClassOf [palatal muscle](http://purl.obolibrary.org/obo/UBERON_0003682)
    - [palatal muscle](http://purl.obolibrary.org/obo/UBERON_0003682) EquivalentTo [skeletal muscle organ](http://purl.obolibrary.org/obo/UBERON_0014892) and ([part of](http://purl.obolibrary.org/obo/BFO_0000050) some [soft palate](http://purl.obolibrary.org/obo/UBERON_0001733))
      - [soft palate](http://purl.obolibrary.org/obo/UBERON_0001733) SubClassOf [part of](http://purl.obolibrary.org/obo/BFO_0000050) some [secondary palate](http://purl.obolibrary.org/obo/UBERON_0001716)
        - [secondary palate](http://purl.obolibrary.org/obo/UBERON_0001716) SubClassOf [part of](http://purl.obolibrary.org/obo/BFO_0000050) some [upper jaw region](http://purl.obolibrary.org/obo/UBERON_0001709)
          - [upper jaw region](http://purl.obolibrary.org/obo/UBERON_0001709) SubClassOf [part of](http://purl.obolibrary.org/obo/BFO_0000050) some [jaw region](http://purl.obolibrary.org/obo/UBERON_0011595)
      -  Transitive: [part of](http://purl.obolibrary.org/obo/BFO_0000050)
      - [skeletal muscle organ](http://purl.obolibrary.org/obo/UBERON_0014892) EquivalentTo [muscle organ](http://purl.obolibrary.org/obo/UBERON_0001630) and ([develops_from](http://purl.obolibrary.org/obo/RO_0002202) some [myotome](http://purl.obolibrary.org/obo/UBERON_0003082)) and ([surrounded_by](http://purl.obolibrary.org/obo/RO_0002219) some [epimysium](http://purl.obolibrary.org/obo/UBERON_0011899)) and ([composed primarily of](http://purl.obolibrary.org/obo/RO_0002473) some [skeletal muscle tissue](http://purl.obolibrary.org/obo/UBERON_0001134)) and ([synapsed by](http://purl.obolibrary.org/obo/uberon/core#synapsed_by) some [motor neuron](http://purl.obolibrary.org/obo/CL_0000100))

In addition to outputting the explanation axioms in Markdown, an ontology containing just 
those axioms is provided to any subsequent chained commands:

    robot explain -i uvula.ofn -r ELK --axiom "'uvular muscle' SubClassOf 'muscle organ' and 'part of' some 'jaw region'" convert -o uvular_muscle.ofn

You can still output a Markdown file, if desired, before the chained command.

Finally, since there can be more than one way to derive an inference from an ontology, 
`explain` includes one more option, `--max`, which allows you to specify the maximum  
number of explanations to output (the default is `1`).