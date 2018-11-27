# Repair

ROBOT can repair certain problems encountered in ontologies. So far, this is limited to updating axioms pointing to deprecated classes with their replacement class (indicated using [term replaced by](http://purl.obolibrary.org/obo/IAO_0100001)).

    robot repair \
      --input need-of-repair.owl \
      --output results/repaired.owl
      
