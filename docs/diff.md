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

See <a href="/examples/release-diff.txt" target="_blank">`release-diff.txt`</a> for an example.