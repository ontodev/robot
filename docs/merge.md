# Merge

OWL ontologies are often divided into several `.owl` files, with `owl:imports` statements to bring them together. Sometimes you want to take all those imports and merge them into a single ontology with a single `.owl` file.

    robot merge --input edit.owl --output results/merged.owl

You don't need `owl:import` statements: you can merge any number of ontologies by using multiple `--input` arguments. All the ontologies and their imports are merged into the first ontology.

    robot merge --input edit.owl --input edit2.owl --output results/merged2.owl

You can also specify merging of multiple files that match a pattern with `--inputs`. The argument to `--inputs` must be a quoted wildcard pattern. This option supports `?` to match any single character, or `*` to match any number of characters.

This command will merge `edit.owl` and `edit2.owl`:

    robot merge --inputs "edit*.owl" --output results/merged2.owl

A merge can be 'undone' with [unmerge](/unmerge).