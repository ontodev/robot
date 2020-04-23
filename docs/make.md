# Makefile

## Contents

1. [Overview](#overview)
2. [Releases](#releases)
3. [Modular Development](#modular-development)

## Overview

On Unix platforms (including macOS and Linux) you can use the venerable [GNU Make](https://www.gnu.org/software/make/) tool to string together multiple `robot` commands. Make can also handle dependencies between build tasks.

When working with Makefiles, be careful with whitespace! Make expects tabs in some places and spaces in others, and mixing them up will lead to unexpected results. Configure your text editor to indicate when tabs or spaces are being used.

## Fetching ROBOT

You can use Make to fetch the latest ROBOT like so:

```
build:
    mkdir -p build

build/robot.jar: | build
   curl -L -o build/robot.jar "https://github.com/ontodev/robot/releases/latest/download/robot.jar"

ROBOT := java -jar build/robot.jar
```

The rule to create `build/robot.jar` depends on the `build` rule, which creates a `build` directory. The JAR is downloaded into this directory, and then the `ROBOT` variable is set to the command to run that JAR.

## Releases

The [`Makefile` for COB](https://github.com/OBOFoundry/COB/blob/master/Makefile) is a good example of a simple workflow.

Most release workflows include the following steps:

1. [Report](/report) on the `-edit` file
2. [Merge](/merge) modules and imports
3. [Reason](/reason) to create the main OWL release
4. [Convert](/convert) to create an OBO format release
5. [Filter](/filer) to generate subsets

The [Ontology Development Kit](https://github.com/INCATools/ontology-development-kit) uses ROBOT for the `Makefile` included in the template, and serves as a thorough example of using ROBOT with make.

## Modular Development

Many ontologies are moving towards development using modules created by ROBOT templates. The workflow is as follows:

1. Create a [template](/template) and generate the module
2. [Extract](/extract) required imports (optional, only if using external entities)
3. Import the module into the main ontology (see below)
4. [Merge](/merge) modules when releasing

The [Ontology for Biomedical Investigations](https://github.com/obi-ontology/obi/tree/master/src/ontology) uses various modules to, for example, add new assays. While this process can be done manually, it can be streamlined by adding in Make rules for the target modules:

```
MODULES = new_terms logical_axioms

modules: $(MODULES)

$(MODULES):
   robot template --input ont-edit.owl \
   --template templates/$@.csv \
   annotate \
   --ontology-iri "http://purl.obolibrary.org/obo/ont/modules/$@.owl" \
   --output modules/$@.owl
```

The `modules` rule will generate all modules specified by the `MODULES` variable (in this example, the modules are `new_terms` and `logical_axioms`). It expects the templates as CSV files in the `templates/` directory. Adding the `--input` option allows entities to be found by label from the given ontology, otherwise the entities should be specified by CURIE. The new modules are then generated in the `modules/` directory, annotated with an ontology IRI. Import statements should be added into the -edit ontology, and their paths specified in `catalog-v001.xml`:

```
<uri name="http://purl.obolibrary.org/obo/ont/modules/new_terms.owl" uri="modules/new_terms.owl"/>
<uri name="http://purl.obolibrary.org/obo/ont/modules/logical_axioms.owl" uri="modules/logical_axioms.owl"/>
```
