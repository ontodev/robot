# Filter

The `filter` command allows you to create a new ontology from a source ontology by copying only the selected axioms. The `remove` command is the opposite of `filter`, allowing you to remove selected axioms. `filter` accepts the same options as `remove` and processes them in the same order. See [`remove`](/remove) for details on configuring the options.

By default, `filter` will remove dangling entities copied as the result of the filtering. To include dangling entities, run with `--trim false`.

Examples:

1. Copy 'assay' and all its descendants from OBI:

```
robot filter --input obi.owl --entity OBI:0000070 --select "self descendants"
```

2. Copy the 'assay' branch from OBI and all the annotations on the ontology itself:

```
robot filter --input obi.owl --entity OBI:0000070 --select "self descendants ontology"
```

3. Copy all of OBI except descendants of 'assay' (`remove` is preferred):

```
robot filter --input obi.owl --entity OBI:0000070 --select descendants --select complement
```

4. Copy a subset of classes based on an annotation property:

```
robot filter --input foo.owl --select classes --select "oboInOwl:inSubset='bar'"
```

