# Global Options

## Java Options

Java options can be used to configure the JVM that ROBOT runs on. A full list of the Java command line options can be found by entering `java` on the command line. Non-standard options can be found by entering `java -X`.

Sometimes when working with very large ontologies, the JVM will run out of memory (`OutOfMemoryError`). You can increase the max heap size using the Java option `-Xmx4G`, which increases the heap to 4G. Any size can be specified here, e.g. `-Xmx8G`.

#### Linux & MacOS

The Java options for ROBOT can be set by running `export ROBOT_JAVA_ARGS=<options>` prior to running a ROBOT command. This will only save the Java options in the current process space. 

To set the Java options for ROBOT permanently, you will need to edit your `.bash_profile` and add the line `export ROBOT_JAVA_ARGS=<options>`. This is the same file you edited to add ROBOT to your system PATH, usually located in your root directory. You can verify that the variable is set by running `echo $ROBOT_JAVA_ARGS`.

#### Windows

The Java options for ROBOT can be set by running `set ROBOT_JAVA_ARGS=<options>` prior to running a ROBOT command. This will only save the Java options in the current process space. To set the Java options for ROBOT permanently, run `setx ROBOT_JAVA_ARGS <options>`.

#### Platform Independent

Alternatively, if you are running ROBOT directly from the jar, these can be specified in the command itself:
```
java <java options> -jar robot.jar <command> <robot options>
```

## Prefixes

Terms in OBO and OWL are identified using [IRIs](https://en.wikipedia.org/wiki/Internationalized_resource_identifier) (Internationalized Resource Identifiers), which generalize the familiar addresses for web pages. IRIs have many advantages, but one of their disadvantages is that they can be pretty long. So we have standard ways to abbreviate IRIs in a particular context by specifying **prefixes**. For example, Turtle files start with `@prefix` statements, SPARQL queries start with `PREFIX` statements, and JSON-LD data includes a `@context` with prefixes.

For robot we use the JSON-LD format. See [`obo_context.jsonld`](https://github.com/ontodev/robot/blob/master/robot-core/src/main/resources/obo_context.jsonld) for the JSON-LD context that is used by default. It includes common, general linked-data prefixes, and prefixes for all the OBO library projects.

If you do not want to use the defaults, you can use the `--noprefixes` option. If you want to replace the defaults, use the `--prefixes` option and specify your JSON-LD file. Whatever your choice, you can add more prefixes using the `--prefix` option, as many times as you like. Finally, you can print or save the current prefixes using the `export-prefixes` command. Here are some examples:

    robot --noprefixes --prefix "foo: http://foo#" \
      export-prefixes --output results/foo.json

    robot --prefixes foo.json -p "bar: http://bar#" -p "baz: http://baz#" \
      export-prefixes

The various prefix options can be used with any command. When chaining commands, you usually want to specify all the prefix options first, so that they are used "globally" by all commands. But you can also use prefix options for single commands. Here's a silly example with a global prefix "foo" and a local prefix "bar". The first export includes both the global and local prefixes, while the second export includes only the global prefix.

    robot --noprefixes --prefix "foo: http://foo#" \
      export-prefixes --prefix "bar: http://bar#" \
      export-prefixes

## XML Catalogs

OWLAPI, Protégé, and ROBOT use XML catalogs to specify where import files are located when loading an ontology. By default, this catalog is called `catalog-v001.xml`. ROBOT assumes that a `catalog-v001.xml` file exists in the working directory and attempts to resolve imports based on that. Because Protègè also predicts that catalog, we recommend sticking to this standard. For more details, see [Importing Ontologies in Protègè and OWL 2](https://protegewiki.stanford.edu/wiki/Importing_Ontologies_in_P41).

That said, occasionally, you may want to use different catalog files for different purposes. This is especially useful for automated releases with [Makefiles](/makefile). ROBOT provides an option to specify the catalog location with `--catalog`. 

For example, you may want to [`merge`](/merge) a set of edited import ontologies to create a module. You may have one set of imports for one module, and another set of imports for another module. You can also chain this command with [`annotate`](/annotate) to specify the output ontology's IRI.

    robot merge --catalog catalog.xml\
     --input imports.owl \
    annotate --ontology-iri https://github.com/ontodev/robot/examples/merged.owl\
     --output results/merged.owl

If a catalog file is specified and cannot be located, the ontology will be loaded without a catalog file. Similarly, if you do not provide a `--catalog` and the `catalog-v001.xml` file does not exist in your working directory, the ontology will be loaded without a catalog file. Finally, if the catalog specifies an import file that does not exist, the command will fail.

## Verbose

ROBOT logs a variety of messages that are typically hidden from the user. When something goes wrong, a detailed exception message is thrown. If the exception message does not provide enough details, you can run the command again with the `-vvv` (very-very-verbose) flag to see the stack trace.

There are three levels of verbosity:
1. `-v`, `--verbose`: WARN-level logging
2. `-vv`, `--very-verbose`: INFO-level logging
3. `-vvv`, `--very-very-verbose`: DEBUG-level logging, including stack traces

## XML Entities

If the `--xml-entities` option is included, entities will be used for namespace abbreviations. For example, in a typical RDF/XML file (OBI, for example), the base prefix may be defined as:
```
xml:base="http://purl.obolibrary.org/obo/obi.owl"
```

If namespace abbreviations are used, the RDF/XML file will include a header with prefix abbreviations (prior to the `rdf:RDF` tag). The `obo` prefix, for example, is:
```
<!ENTITY obo "http://purl.obolibrary.org/obo/" >
```

The `obo` abbreviation would be substituted for any instance of `http://purl.obolibrary.org/obo/` in the rest of the RDF/XML file, as demonstrated by the base prefix:
```
xml:base="&obo;obi.owl"
```

---

## Error Messages

### JSON-LD Error

ROBOT encountered a problem while writing the given prefixes to JSON-LD.
