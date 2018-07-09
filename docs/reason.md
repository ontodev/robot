# Reason

One of the main benefits of working with OWL is the availability of powerful automated reasoners. There are several reasoners available, and each has different capabilities and characteristics. For this example we'll be using <a href="https://code.google.com/p/elk-reasoner/" target="_blank">ELK</a>, a very fast reasoner that supports the EL subset of OWL 2.

    robot reason --reasoner ELK \
      --input ribosome.owl \
      --output results/reasoned.owl

It's also possible to place the new inferences in their own ontology with `--create-new-ontology true`:

    robot reason --reasoner ELK \
      --create-new-ontology true \
      --input ribosome.owl \
      --output results/new_axioms.owl

Using `--create-new-ontology-with-annotations true` will place the inferences in their own ontology, along with their annotations (e.g. labels, definitions, etc.).

Sometimes it is important to know which axioms were inferred (designated by an axiom annotation "is_inferred true"):

```
robot reason --reasoner ELK \
  --annotate-inferred-axioms true \
  --input ribosome.owl \
  --output results/reasoned.owl
```

Finally, `reason` includes two more options to help clean the reasoned output (by default, these are `false`):
* `--exclude-duplicate-axioms`: if set to true, the axioms will not be added to the output if they exist in an import.
* `--remove-redundant-subclass-axioms`: if set to true, any redundant axioms (those that have been asserted and were also inferred) will be removed from the output.

If no `--reasoner` is provided, ROBOT will default to ELK. The following other reasoner options are supported:
  
  * `hermit` - [HermiT](http://www.hermit-reasoner.com/)
  * `jfact` - [JFact](http://jfact.sourceforge.net/)
  * `emr` - [Expression Materializing Reasoner](http://static.javadoc.io/org.geneontology/expression-materializing-reasoner/0.1.3/org/geneontology/reasoner/ExpressionMaterializingReasoner.html)
  * `structural` - [Structural Reasoner](http://owlcs.github.io/owlapi/apidocs_4/org/semanticweb/owlapi/reasoner/structural/StructuralReasoner.html)

---

## Error Messages

### Create Ontology Error

You must select between `--create-new-ontology-with-annotations` (`-m`) and `--create-new-ontology` (`-n`). Both cannot be passed in as `true` to one reason command, as they have opposite results.
