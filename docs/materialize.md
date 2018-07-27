# Materialize

Robot can materialize all parent superclass and superclass expressions using the expression materializing reasoner, which wraps an existing OWL Reasoner

    robot materialize --reasoner ELK \
      --input emr_example.obo \
      --term BFO:0000050  \
      --output results/emr_output.obo

This operation is similar to the reason command, but will also assert parents of the form `P some D`, for all P in the set passed in via `--term`

This can be [chained](/chaining) with [remove](/remove) (by specifying a set of properties to *keep*, then selecing the complement properties of that set) and [reduce](/reduce) to create a complete ontology subset:

    robot materialize --reasoner ELK \
      --input emr_example.obo \
      remove --term BFO:0000050 \
      --select complement --select object-properties \
      reduce --output results/emr_reduced.obo

See [reason](/reason) for details on supported reasoners (EMR is not supported in `materialize`).
