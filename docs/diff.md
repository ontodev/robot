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

### Label Languages

When an entity has labels in more than one language, you can control which one is shown in the `pretty` format with `--label-langs-priority`, a comma-separated list of [language tags](https://www.rfc-editor.org/info/bcp47) in priority order:

    robot diff --left lang-left.owl \
      --right lang-right.owl \
      --format pretty \
      --label-langs-priority en-GB,en \
      --output results/lang-diff.txt

Here the `dog` class carries `de`, `en`, and `en-GB` labels; with `en-GB,en` its British label `hound` is chosen. Selection rules:

- A more general tag matches a more specific one — for example, `en` matches a label tagged `en-GB`. Each label is bound to the *most specific* tag in your list that it matches, and the label bound to the *earliest* listed tag wins.
- Listing a specific tag after a general one deprioritizes it. With `en,en-GB`, a label tagged `en-GB` binds to `en-GB` (position 2), while a label tagged `en-US` binds to `en` (position 1, by cascade) — so the `en-US` label is shown. To prefer British labels instead, list `en-GB` first.
- Use the token `none` to prefer labels that have no language tag (for example, `--label-langs-priority en,none`).
- Use `*` as a catch-all that matches any label (for example, `--label-langs-priority en,*` prefers English but falls back to any available label). `*` is the least specific match, so any listed language is preferred over it.
- When more than one label is bound to the winning tag (for example, two labels tagged `en`), the alphabetically first is chosen.
- If an entity has labels but none in a preferred language, its alphabetically first label is used, so a labelled entity is never shown as its IRI. Entities with no label at all are unaffected.

Tag matching follows [RFC 4647](https://www.rfc-editor.org/rfc/rfc4647.html) Basic Filtering (a tag like `en` matches any label tagged `en` or beginning `en-`, and `*` matches anything); ROBOT then uses your list's priority order and the tie-breaks above to choose a single label.

This option currently applies only to the `pretty` format; the `markdown` and `html` formats are not yet affected.

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
