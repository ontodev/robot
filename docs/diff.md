# Diff (Compare)

To compare two text files and check for differences, you can use the Unix `diff` command on Linux or Mac OS X:

    diff file1.txt file2.txt

or on Windows the `FC` command:

    FC file1.txt file2.txt

Any number of graphical diff tools are also available, for example FileMerge is part of Apple's free XCode tools.

Although OWL ontology files are text, it's possible to have two identical ontologies saved to two files with very different structure. ROBOT provides a `diff` command that compares two ontologies while ignoring these differences:

    robot diff --left edit.owl --right release.owl

If `--output` is provided then a report will be written with any differences between the two ontologies:

    robot diff --left edit.owl \
      --right release.owl \
      --output results/release-diff.txt

If your left and right ontologies have different catalog files (perhaps pointing to different import files), you can load them with their correct catalog files using `--left-catalog` and `--right-catalog`:

    robot diff --left imports.owl \
      --left-catalog catalog.xml \
      --right imports-right.owl \
      --right-catalog catalog-right.xml \
      --output results/catalog-diff.txt

See [release-diff.txt](/examples/release-diff.txt) for an example.

The default "plain" output is in OWL Functional syntax with IRIs. You can include entity labels with `--labels true`. In addition, Markdown and HTML diff formats (based on Manchester syntax) are available. You can select the desired format using the `--format` (or `-f`) option, with possible values `plain`, `pretty` (text with labels and CURIEs), `html`, or `markdown`.

You can also compare ontologies by IRI with `--left-iri` and `--right-iri`. You may want to compare a local file to a release, in which case:
<!-- DO NOT TEST -->
```
robot diff --left edit.owl \
  --right-iri http://purl.obolibrary.org/obo/release.owl
```

---

## Error Messages

### Double Input Error

You may specify the input with either `--left`/`--right` or `--left-iri`/`--right-iri`, but you may not use both option methods for one side.

### Missing Input Error

Both `--left` and `--right` input ontologies are required.
