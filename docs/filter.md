# Filter

The `filter` command allows you to create a new ontology from a source ontology by copying only the selected axioms. The `remove` command is the opposite of `filter`, allowing you to remove selected axioms. `filter` accepts the same options as `remove` and processes them in the same order, with just a few differences. See [`remove`](/remove) for details on configuring the options.

Including and excluding terms is the opposite of `remove`:
* If you wish to *exclude* a term or set of terms that would be filtered for otherwise, you can do so with `--exclude-term <term>` or `--exclude-terms <term-file>`. These terms will **never** be in the output.
* If you wish to *include* a term or set of terms that would not be filtered for otherwise, you can do so with `--include-term <term>` or `--include-terms <term-file>`. These terms will **always** be in the output.

The key difference between `remove` and `filter` comes in the fourth processing step:

4. The final step is to take each axiom of the specified types, and compare it to the target set. The `--signature` option works the same as `remove`, but `--trim` differs. When using `filter --trim true` (the default), if *all* objects for the axiom are in the target set then that axiom is copied to the new ontology. When using `filter --trim false`, if *any* object for the axiom are in the target set, then that axiom is copied.

Consider an example with this small ontology:

```
ax1: A subClassOf B
ax2: A subClassOf R some C
ax3: D subClassOf E
```

Then `filter --term A --term R --term C --select "self parents" --axioms all --signature true --trim true` works as follows:

1. The initial target set includes all the specified `--term`s: `{A, R, C}`
2. For `self` we get just the initial target set `{A, R, C}`; for `parents` we get `{B, R some C}`; so the new target set is the union `{A, B, R, C, R some C}`
3. `--axioms all` means that we consider all axioms
4. For each axiom we compare the objects for the axiom to the target set. With `--signature true` we only consider the named objects, i.e. objects that have IRIs, not anonymous objects such as `R some C`. With `--trim true` we are checking that *all* objects for the axiom is in the target set.
    - the objects for `ax1` are `{A, B}`, and all of these are in the target set, so `ax1` is matched and copied
    - the objects for `ax2` are `{A, R, C}` (with `R some C` excluded), and all of these are in the target set, so `ax2` is matched and copied
    - the objects for `ax3` are `{D, E}`, and none of these are in the target set, so `ax3` is not matched and is not copied

The `remove` and `filter` operations maintains structural integrity by default: lineage is maintained, and gaps will be filled where classes have been removed. If you wish to *not* preserve the hierarchy, include `--preserve-structure false`.

## Annotations

The `filter` command also includes a special `--select "annotations"` selector. If this selector is used, all annotations on filtered terms will be copied, regardless of if those properties are in the set of terms or not.

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

    robot filter --input uberon_module.owl \
      --term UBERON:0000062 \
      --select "annotations self descendants" \
      --signature true \
      --output results/filter_class.owl

Copy all of OBI except descendants of 'assay' (`remove` is preferred):

    robot filter --input uberon_module.owl \
      --term UBERON:0000062 \
      --select annotations \
      --select descendants \
      --select complement \
      --signature true \
      --output results/remove_class.owl

Copy a subset of classes based on an annotation property (maintains hierarchy):

    robot filter --input uberon_module.owl \
      --prefix "core: http://purl.obolibrary.org/obo/uberon/core#" \
      --select "oboInOwl:inSubset=core:uberon_slim" \
      --select annotations \
      --signature true \
      --output results/uberon_slim.owl

Copy a class, all axioms that a class appears in and annotations on all the classes (only `UBERON:0000062` here) in the filter set:

    robot filter --input uberon_module.owl \
      --term UBERON:0000062 \
      --select annotations \
      --trim false \
      --signature true \
      --output results/uberon_annotated.owl

Create a "base" subset that only includes internal axioms (alternatively, use `remove --axioms external`):

    robot filter --input template.owl \
      --base-iri http://example.com/ \
      --select "annotations" \
      --axioms internal \
      --include-term IAO:0000117 \
      --include-term IAO:0000119 \
      --preserve-structure false \
      --output results/template-base-filter.owl

