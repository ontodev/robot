# Rename

## Contents

1. [Overview](#overview)
2. [Mapping Files (`--mappings`)](#mapping-files)

## Overview

The `rename` command allows you to rename entity IRIs in an ontology in two ways:

#### Full

Renames full IRIs (e.g. `obo:BFO_0000050` to `http://foo.bar/BFO_1234567`):

    robot rename --input test.owl \
      --add-prefix "fb: http://foo.bar/" \
      --mapping obo:BFO_0000051 fb:BFO_1234567 \
      --output results/rename.owl

If you have multiple terms you'd like to rename, you can do so by providing a mappings file (see [Mappings Files]($mappings-files)):

    robot rename --input test.owl \
      --add-prefix "fb: http://foo.bar/" \
      --mappings full-rename.tsv \
      --output results/full-rename.owl

This process fails if where there are entities in the mapping file that are not in the ontology. To avoid failure you can
add the option `--allow-missing-entities true`.

    robot rename --input test.owl \
      --mappings missing-rename.tsv \
      --add-prefix "fb: http://foo.bar/" \
      --allow-missing-entities true \
      --output results/missing-rename.owl

If two or more old IRIs are mapped to the same new IRI, these two entities will be merged. By default, ROBOT will throw an error if this happens. This behavior can be overridden by including `--allow-duplicates true`.

#### Prefixes

Renames the base IRIs of all matching entities (e.g. change the prefix `http://purl.obolibrary.org/obo/` to `http://foo.bar/`), based on mappings in a file specified by `--prefix-mappings`:

    robot rename --input test.owl \
      --prefix-mappings partial-rename.tsv \
      --add-prefix "fb: http://foo.bar/" \
      --output results/partial-rename.owl

More information on the `--add-prefix` option can be found in [Global Options](/global#prefixes).

## Mappings Files

The mappings for renaming should be specified with the `--mappings` (for full renames) or `--prefix-mappings` (for renaming prefixes) option. These should be either comma- or tab-separated tables. Each row should have exactly two columns: on the left, the IRI to replace, and on the right, the IRI to replace it with.

For a full rename (you can use prefixes as long as they are defined by the defaults, `--prefix`, or `--add-prefix`):

```tsv
Old IRI New IRI
obo:BFO_0000051 fb:BFO_1234567
```

If you also want to update the `rdfs:label` of the term you replaced, you can add a third column with the new label value (note that this removes *all* old label annotations):

```tsv
Old IRI	New IRI	New Label
obo:BFO_0000051	fb:BFO_1234567	foo bar
```

For a prefix rename:

```tsv
Old Base  New Base
http://purl.obolibrary.org/obo/ http://foo.bar/
```

The `rename` command expects the first line to contain headers.

---

## Error Messages

### Column Count Error

Each row of the mapping file must have two columns: first, the old IRI, second, the new IRI. These must be separated by either a comma or a tab, depending on the file format.

### Duplicate Mapping Error

This error occurs when two rows have the same 'old IRI' value. This will cause two rename operations to occur for the same IRI, resulting in unexpected values. Make sure each 'old IRI' is only entered in the mappings once.

### Duplicate Rename Error

This error occurs when two rows have the same 'new IRI' value. This will cause a merge of the two old IRIs into the new IRI. If this is the intended behavior, use `--allow-duplicates true`.

### File Format Error

The mappings file must be comma-separated (ending in `.csv`) or tab-separated (ending in `.tsv` or `.txt`).

### Missing Entity Error

For a 'full' IRI replacement, the 'old IRI' must exist in the ontology. If not, nothing can be replaced and this error will be thrown.

### Missing File Error

This error occurs when the file provided for the `--mappings` or `--prefix-mappings` option does not exist. Check the path and try again.

### Missing Mappings Error

This error occurs when a `--mappings` or `--prefix-mappings` file is not provided.

### New IRI Error

ROBOT could not generate the requested new IRI. This is often caused by a missing prefix declaration.
