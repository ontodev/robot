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

### Subproperties and property chains

For backwards compatibility reasons, by default subObjectPropertyOf axioms are ignored when evaluating the redundancy of subClassOf axioms. This means that, given the following example:

```
C SubClassOf P some D
C SubClassOf Q some D
P SubObjectPropertyOf Q
```

the first subClassOf axiom would by default _not_ be considered redundant and therefore _not_ be removed.

To force the `reduce` command to factor in subproperties (including property chains) when evaluating redundancy, use the `--include-subproperties` option:

    robot reduce --reasoner ELK \
      --input redundant-over-subproperties.ofn \
      --include-subproperties true \
      --output results/reduced-redundant-over-subproperties.ofn