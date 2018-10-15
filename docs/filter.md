# Filter

The `filter` command allows you to create a new ontology from a source ontology by copying only the selected axioms. The `remove` command is the opposite of `filter`, allowing you to remove selected axioms. `filter` accepts the same options as `remove` and processes them in the same order. See [`remove`](/remove) for details on configuring the options.

By default, `filter` will include all axioms from the input ontology that contain *one or more* entities from the specified set. To be more strict and only include axioms in which *all* entities in the axiom are in the specified set, use `--trim false`.

This operation maintains structural integrity; lineage is maintained, and gaps will be filled where classes have been excluded. If you wish to *not* preserve the hierarchy, include `--preserve-structure false`.

## Annotations

The `filter` command also includes a special `--select "annotations"` option. If this is included, all annotations on filtered terms will be included, regardless of if those properties are in the set of terms or not.

The following command will only return `OBI:0000070` with just a label annotation:
```
robot filter --input obi.owl --term OBI:0000070 --term rdfs:label
```

Alternatively, if you want `OBI:0000070` with all annotations:
```
robot filter --input obi.owl --term OBI:0000070 --select annotations
```

## Examples

Copy a class ('organ') and all its descendants, with all annotations:

    robot filter --input uberon_module.owl\
     --term UBERON:0000062\
     --select "annotations self descendants"\
     --output results/filter_class.owl

Copy all of OBI except descendants of 'assay' (`remove` is preferred):

    robot filter --input uberon_module.owl\
     --term UBERON:0000062\
     --select annotations\
     --select descendants\
     --select complement\
     --output results/remove_class.owl

Copy a subset of classes based on an annotation property (maintains hierarchy):

    robot filter --input uberon_module.owl\
     --prefix "core: http://purl.obolibrary.org/obo/uberon/core#"\
     --select "oboInOwl:inSubset=core:uberon_slim"\
     --select annotations\
     --output results/uberon_slim.owl
