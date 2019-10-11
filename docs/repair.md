# Repair

ROBOT can repair certain problems encountered in ontologies. So far, this is limited to updating axioms pointing to deprecated classes with their replacement class (indicated using [term replaced by](http://purl.obolibrary.org/obo/IAO_0100001)).

    robot repair \
      --input need-of-repair.owl \
      --output results/repaired.owl
      

By default, annotation axioms are not migrated to replacement classes. However, this can be enabled for a list of annotation properties passed either as arguments to `--annotation-property` or in a term file `--annotation-properties-file`:

    robot repair \
      --input xref-need-of-repair.obo \
      --annotation-property oboInOwl:hasDbXref \
      --output results/xref-repaired.obo
