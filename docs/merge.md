# Merge

OWL ontologies are often divided into several `.owl` files, with `owl:imports` statements to bring them together. Sometimes you want to take all those imports and merge them into a single ontology with a single `.owl` file. This can be chained with [`annotate`](/annotate) to specify an IRI for the output ontology.

    robot merge --input edit.owl \
    annotate --ontology-iri https://github.com/ontodev/robot/examples/merged.owl\
     --output results/merged.owl

You don't need `owl:import` statements: you can merge any number of ontologies by using multiple `--input` arguments. All the ontologies and their imports are merged into the first ontology.

    robot merge --input edit.owl --input edit2.owl --output results/merged2.owl

You can also specify merging of multiple files that match a pattern with `--inputs`. The argument to `--inputs` must be a quoted wildcard pattern. This option supports `?` to match any single character, or `*` to match any number of characters.

This command will merge `edit.owl` and `edit2.owl`:

    robot merge --inputs "edit*.owl" --output results/merged2.owl

A merge can be 'undone' with [unmerge](/unmerge).

### Import Closure

The `--collapse-import-closure` option is, by default, `true`. As shown in the first example above, running `merge` on an ontology with `owl:imports` statements will merge these into a single file. The `owl:imports` statements are also removed from the ontology.

By adding `--collapse-import-closure false`, the imports will not be merged in and the `owl:imports` statements will remain.

### Ontology Annotations

The `--include-annotations` option is, by default, `false`. This refers to the ontology annotations, and when merging, these are typically ignored and the output will only include the ontology annotations from the *first* input. 

If you would like to merge the ontology annotations from several inputs, add `--include-annotations true`.

### Provenance Annotations

It’s also possible to annotate the imported or merged ontology axioms with the source ontology or version IRI.

  * `--annotate-derived-from true`: annotates all axioms with the source's version IRI if it exists, else with the ontology IRI, using `prov:wasDerivedFrom`. If the axiom already has an annotation using this property (`prov:wasDerivedFrom`), the existing annotation will be kept and no new annotation will be added.

    robot merge --catalog catalog.xml \
      --input imports-nucleus.owl \
      --annotate-derived-from true \
      --output results/merged_derived_from.owl
      
  * `--annotate-defined-by true`: annotates all entities (class, data, annotation, object property and named individual declaration axioms) with the source's IRI using `rdfs:isDefinedBy`. If the term already has an annotation using this property (`rdfs:isDefinedBy`), the existing annotation will be kept and no new annotation will be added.

    robot merge --input example2.owl --input merge.owl \
      --annotate-defined-by true \
      --output results/merged_defined_by.owl
