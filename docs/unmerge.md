# Unmerge

`unmerge` removes all axioms contained in one ontology from another. The first `--input` is the ontology to remove axioms from, subsequent `--input` ontologies contain the axioms to be removed:

<!-- DO NOT TEST -->
```
robot unmerge --input my-ontology.owl \
  --input remove-axioms.owl \
  --input remove-axioms-2.owl \
  --output my-ontology-fixed.owl
```

If any axioms exist in the subsequent ontologies that do not exist in the first ontology, they will be silently ignored.

### Unmerging a Merge

A [merge](/merge) can be 'undone' using the unmerge command. This can be useful if we want to perform some operation (for example, reasoning) on a merged file, but later subtract the results.

This will merge in foo, and then subtract out foo from the merged ontology.

    robot merge --input edit.owl \
      --input foo.owl \
      unmerge --input foo.owl \
      --output results/unmerged.owl