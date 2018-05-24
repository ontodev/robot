# Makefile

On Unix platforms (including Mac OS X and Linux) you can use the venerable [Make](https://www.gnu.org/software/make/) tool to string together multiple `robot` commands. Make can also handle dependencies between build tasks.

When working with Makefiles, be careful with whitespace! Make expects tabs in some places and spaces in others, and mixing them up will lead to unexpected results. Configure your text editor to indicate when tabs or spaces are being used.

## Releases

The basic release workflow is as follows:
1. [Report](/report) on the `-edit` file
2. [Merge](/merge) modules and imports
3. [Reason](/reason) to create the main OWL release
4. [Convert](/convert) to create an OBO format release
5. [Filter](/filer) to generate subsets

The [Ontology Starter Kit](https://github.com/INCATools/ontology-starter-kit) uses ROBOT for the [Makefile](https://github.com/INCATools/ontology-starter-kit/blob/master/template/src/ontology/Makefile) included in the template. This serves as a good example of ROBOT used for the release workflow. Standard constants are defined at the top of the file and reused throughout (e.g. `ONT=foobar` should be replaced with your ontology name). That example expects ROBOT to be installed on your system, but you can also set the `ROBOT` variable to the latest build:

```
mk:
    mkdir -p build

build/robot.jar: | mk
   curl -L -o build/robot.jar \
   https://build.berkeleybop.org/job/robot/lastSuccessfulBuild/artifact/bin/robot.jar

ROBOT := java -jar build/robot.jar
```

The rule to create `build/robot.jar` depends on the `mk` rule, which creates a `build` directory. The JAR is downloaded into this directory, and then the `ROBOT` variable is set to the command to run that JAR.

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