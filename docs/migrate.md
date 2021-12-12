# Migrate

Sometimes we want to migrate terms from one ontology to another,
for example when we want to obsolete a branch in one ontology
and move its terms to another.

    robot migrate --input edit.owl --source source.owl \
    --source-id robot --term UBERON:0000062 --mappings mappings_edit.tsv \
    annotate --ontology-iri https://github.com/ontodev/robot/examples/merged.owl\
     --output results/merged.owl


