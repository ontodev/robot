# Reduce

ROBOT can be used to remove redundant subClassOf axioms:

    robot reduce --reasoner ELK \
      --input ribosome.owl \
      --output results/reduced.owl

See [reason](/reason) for details on supported reasoners (EMR is not supported in `reduce`).
