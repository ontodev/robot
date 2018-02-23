# Unmerge

A [merge](/merge) can be 'undone' using the unmerge command. This can be useful if we want to perform some operation (for example, reasoning) on a merged file, but later subtract the results.

This will merge in foo, and then subtract out foo from the merged ontology.

    robot merge --input edit.owl \
      --input foo.owl \
      unmerge --input foo.owl \
      --output results/unmerged.owl