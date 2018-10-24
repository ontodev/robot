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

See <a href="/examples/release-diff.txt" target="_blank">`release-diff.txt`</a> for an example. In the output, 'Ontology 1' corresponds to your `--left` input and 'Ontology 2' corresponds to your `--right` input.

The output is in OWL Manchester syntax, but you can include entity labels with `--labels true`.

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
