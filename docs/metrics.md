# Metrics

## Contents

1. [Overview](#overview)
2. [Types of metrics](#types-of-metrics)
3. [Output formats](#output-formats)

## Overview
Robot can compute a number of metrics about your ontology, such as entity and
axiom counts, qualitative information such as OWL 2 profiles
and more complex metrics aimed at informing ontology developers such as logical expressivity and axiom shape.
A number of essential metrics can be generated very simply as follows:

    robot metrics --input edit.owl --output results/metrics.tsv

This will generate a table with metrics such as:
- Entity metrics (number of classes, object properties etc.)
- Datatypes used
- Number of axioms (logical and otherwise)
- TBox, RBox, ABox size (size as number of axioms)
- OWL2 profile information

## Types of metrics
There are three general modes for computing metrics:

1. Essential metrics (`--metrics essential`, default)
2. Extended metrics (`--metrics extended`)
3. All metrics (`--metrics all`)

_Essential metrics_ include basic ontology entity counts (number of classes, individuals and properties), axiom counts
and a few qualitiative metrics such as OWL profiles, see the initial example above.

    robot metrics --input edit.owl --metrics essential --output results/metrics.tsv

_Extended metrics_ contain all the essential metrics, and additional details on axiom types, logical expressivity and datatypes.

    robot metrics --input edit.owl --metrics extended --output results/metrics.tsv

_All metrics_ include all the essential and extended metrics, as well as a range of more complex metrics targeted at
OWL and reasoning specialists, such as information about GCIs, shape of the class hierarchy and potential cyclicity.

    robot metrics --input edit.owl --metrics all --output results/metrics.tsv

A full breakdown of all metrics can be found in the following:

|metric|metric_value|metric_type| explanation |
|:---|:---|:---|:---|
|abox_axiom_count|0|single_value| Number of axioms in the ABox |
|annotation_property_count|24|single_value| Number of annotation properties |
|assert_n_subclass_avg|1.0|single_value| Average number of (asserted) subclasses per class |
|assert_n_superclass_avg|1.0416666666666667|single_value| Average number of (asserted) superclasses per class |
|ax_complexrhs_count|15|single_value| Number of axioms with a complex right hand side |
|axiom_count|703|single_value| Number of axioms |
|axiom_length_max|4|single_value| Longest axiom in terms of number of entities used (including duplicate uses). |
|axiom_type_count|SubClassOf 37|map_value| Number of axioms of a specific type, such as SubClassOf |
|axiom_types|AnnotationAssertion|list_value| Indicates the presence of an axiom type in the ontology. |
|certain_cycle|false|single_value| If true, there is a definite cycle in the ontology. If false, cyclicity is unknown (experimental). |
|class_count|24|single_value| Number of classes in the ontology |
|class_sgl_subcl_count|10|single_value| Number of super-classes which have more than one subclass. |
|constructs|AL|list_value| Logical constructs used |
|dataproperty_count|0|single_value| Number of distinct data properties. |
|datatypes_builtin|XSD_BOOLEAN|list_value| Datatypes used from the built-in datatype map (like XSD schema datatypes). |
|datatypes_count|2|single_value| Total number of distinct datatypes. |
|dt_builtin_count|2|single_value| Total number of distinct built-in datatypes. |
|dt_notbuiltin_count|0|single_value| Total number of distinct custom (not built-in) datatypes. |
|expressivity|ALEH+|single_value| Logical expressivity. |
|gci_count|0|single_value| Total count of GCI axioms.|
|gci_hidden_count|2|single_value| Total count of hidden GCIs. |
|individual_count|0|single_value| Number of individuals. |
|instance_per_class_avg|0.0|single_value| Average number of individuals per class. |
|namespace_axiom_count|http://purl.obolibrary.org/obo/BFO_ 17|map_value| Number of axioms using at least one term in a namespace. |
|namespace_entity_count|http://purl.obolibrary.org/obo/BFO_ 17|map_value| Number of distinct entities used from a namespace. |
|logical_axiom_count|41|single_value| Number of logical axioms in ontology |
|most_freq_concept|UBERON:0000475|single_value| Most frequently used class. |
|multiple_inheritance_count|2|single_value| Number of classes with multiple inheritance. |
|named_supercl_count_max|1|single_value| Maximum number of named superclasses. |
|obj_property_count|2|single_value| Number of object properties. |
|ontology_anno_count|0|single_value| Number of ontology annotations. |
|ontology_id|https://github.com/ontodev/robot/examples/edit.owl|single_value| The ontology id. |
|owl2_dl|false|single_value| Does the ontology correspond to the OWL2 DL profile? |
|owl2_el|false|single_value| Does the ontology correspond to the OWL2 EL profile? |
|owl2_ql|false|single_value| Does the ontology correspond to the OWL2 QL profile? |
|owl2_rl|false|single_value| Does the ontology correspond to the OWL2 RL profile? |
|owl2|true|single_value| Does the ontology correspond to the OWL2 Full profile?  |
|owl2dl_profile_violation|UseOfUndeclaredAnnotationProperty 531 |map_value| Counts of OWL2 DL profile violations. |
|owlapi_version|robot.jar|single_value| Which OWLAPI version of robot was used to compute metrics. |
|rbox_axiom_count|2|single_value| How many axioms in the RBOX? |
|rdfs|false|single_value| Does the ontology correspond to the RDFS profile? |
|rule_count|0|single_value| Number of SWRL rules used. |
|signature_entity_count|52|single_value| Total number of entities in signature, including classes and individuals. |
|syntax|RDF/XML Syntax|single_value| What serialisation is used for the ontology? |
|tautology_count|0|single_value| Number of tautological axioms. |
|tbox_axiom_count|39|single_value| Number of TBOX axioms (without RBOX). |
|tbox_nominals|false|single_value| Number of TBOX axioms with nominals. |
|tboxrbox_axiom_count|41|single_value| Number of TBOX axioms (with RBOX).|
|undecl_entity_count|17|single_value| Number of undeclared entities. |

## Output formats

Currently, the following serialisation formats are supported:
- tsv
- html
- json
- yaml

To serialise the metrics as json, for example, we run:

    robot metrics --input edit.owl --format json --output results/metrics.json

or

    robot metrics --input edit.owl -f json --output results/metrics.json

Note that the file extension of the output file does not matter to `robot metrics` - it can be anything
and will not be used to infer the intended serialisation format.

## Error Messages

### Metrics format error

Only the following `--format/-f` options are currently supported: `tsv`, `json`, `yaml`, `html`.

### Metrics type error

Only the following `--metrics/-m` options are currently supported: `essential`, `extended`, `all`.
