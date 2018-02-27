# Repair

ROBOT can repair certain problems encountered in ontologies. So far, this is limited to updating axioms pointing to deprecated classes with their replacement class.

    robot repair \
      --input need-of-repair.owl \
      --output results/repaired.owl