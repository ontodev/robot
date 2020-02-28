# Repair

ROBOT can repair certain problems encountered in ontologies. So far, this is limited to updating axioms pointing to deprecated classes with their replacement class (indicated using [term replaced by](http://purl.obolibrary.org/obo/IAO_0100001)).

This situation can arise in a number of different ways:

 1. When you are editing an ontology in Protege, you obsolete a class, but forget to repair axioms that reference this.
 2. When you rebuild an import module using [extract](extract), and the upstream ontology has obsoleted classes that you are using.
 
For more on obsoletion workflows, see the [obsoletion guide](https://ontology-development-kit.readthedocs.io/en/latest/ObsoleteTerm.html) in the Ontology Development Kit documentation.

To repair an ontology (with the name `need-of-repair.owl`) run the following command:

    robot repair \
      --input need-of-repair.owl \
      --output results/repaired.owl

This will generate a new file `results/repaired.owl`. You can compare this with the original file (either using unix `diff` or [robot diff](diff)). If the changes that were made look good then you can simply replace the source file with the repaired file (`mv results/repaired.owl need-of-repair.owl`).

By default, annotation axioms are not migrated to replacement classes. However, this can be enabled for a list of annotation properties passed either as arguments to `--annotation-property` or in a term file `--annotation-properties-file`:

    robot repair \
      --input xref-need-of-repair.obo \
      --annotation-property oboInOwl:hasDbXref \
      --output results/xref-repaired.obo
