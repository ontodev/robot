# Mirror

Many ontologies make use of `owl:imports` to bring in other ontologies, or portions of other ontologies. Large import chains involving multiple large ontologies are more prone to run-time failure due to network errors or latency. It can therefore be beneficial to "mirror", or cache an external ontology's import chain locally. This can be thought of as analogous to what happens with standard dependency management tools for software development.

The following command will mirror the imports locally:

    robot mirror --input test.owl \
      --directory results/my-cache \
      --output results/my-catalog.xml

This will generate a directory `results/my-cache/purl.obolibrary.org/obo/ro/` (based on the ontology IRI) with the imported ontologies as files. The file <a href="/examples/my-catalog.xml" target="_blank">`my-catalog.xml`</a> is a generated XML catalog mapping the source URIs to local files.