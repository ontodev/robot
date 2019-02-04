# Materialize

Robot can materialize inferred superclasses and superclass expressions using the expression materializing reasoner, which wraps an existing OWL Reasoner

    robot materialize --reasoner ELK \
      --input emr_example.obo \
      --term BFO:0000050  \
      --output results/emr_output.obo

This operation is similar to the reason command, but will also assert parents of the form `P some D`, for all P (properties) in the set passed in via `--term` or `--term-file`.

This can be [chained](/chaining) with [remove](/remove) and [reduce](/reduce) to create a complete ontology subset. First, `materialize` asserts the inferred superclass expressions. Then, `remove` takes out any object properties (and the axioms that they are used in) that we do not need in the subset. Finally, `reduce` removes any duplicated axioms created by `materialize`.

    robot materialize --reasoner ELK \
      --input emr_example.obo \
      remove --term BFO:0000050 --trim true \
      --select complement --select object-properties \
      reduce --output results/emr_reduced.obo

See [reason](/reason) for details on supported reasoners (EMR is not supported in `materialize`, as it is used to wrap another reasoner here).
