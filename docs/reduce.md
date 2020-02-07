# Reduce

ROBOT can be used to remove redundant subClassOf axioms:

    robot reduce --reasoner ELK \
      --input ribosome.owl \
      --output results/reduced.owl

See [reason](/reason) for details on supported reasoners (EMR is not supported in `reduce`).

Available options for `reduce`:
* `--preserve-annotated-axioms`: if set to true, axioms that have axiom annotations will not be removed, even if found to be redundant (default `false`).
* `--named-classes-only`: if set to true, only subclass axioms between named classes will be checked for redundancy. Anonymous class expressions will be ignored (default `false`).

### Warning

Reciprocal subclass axioms (e.g. `A SubClassOf B`, `B SubClassOf A`), entailing equivalence between `A` and `B`, may be removed by `reduce`. In this case it is important to
assert an equivalence axiom (`A EquivalentTo B`) using the `reason` command before running reduce.