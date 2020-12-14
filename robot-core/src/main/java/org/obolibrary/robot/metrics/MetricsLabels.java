package org.obolibrary.robot.metrics;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class MetricsLabels {

  public static final String OWLAPI_VERSION = "owlapi_version";
  public static final String ABOX_SIZE_INCL = "abox_axiom_count_incl";
  public static final String ABOX_SIZE = "abox_axiom_count";
  public static final String TBOX_SIZE_INCL = "tbox_axiom_count_incl";
  public static final String TBOX_SIZE = "tbox_axiom_count";
  public static final String TBOXRBOX_SIZE_INCL = "tboxrbox_axiom_count_incl";
  public static final String TBOXRBOX_SIZE = "tboxrbox_axiom_count";
  public static final String SIGNATURE_SIZE = "signature_entity_count";
  public static final String SIGNATURE_SIZE_INCL = "signature_entity_count_incl";
  public static final String ANNOTATION_PROP_COUNT = "annotation_property_count";
  public static final String ANNOTATION_PROP_COUNT_INCL = "annotation_property_count_incl";
  public static final String AVG_ASSERT_N_SUBCLASS = "assert_n_subclass_avg";
  public static final String AVG_ASSERT_N_SUBCLASS_INCL = "assert_n_subclass_avg_incl";
  public static final String AVG_ASSERT_N_SUPERCLASS = "assert_n_superclass_avg";
  public static final String AVG_ASSERT_N_SUPERCLASS_INCL = "assert_n_superclass_avg_incl";
  public static final String AVG_INSTANCE_PER_CLASS = "instance_per_class_avg";
  public static final String AVG_INSTANCE_PER_CLASS_INCL = "instance_per_class_avg_incl";
  public static final String AXIOM_COUNT = "axiom_count";
  public static final String AXIOM_COUNT_INCL = "axiom_count_incl";
  public static final String AXIOMTYPE_COUNT = "axiom_type_count";
  public static final String AXIOMTYPE_COUNT_INCL = "axiom_type_count_incl";
  public static final String CLASSEXPRESSION_COUNT = "class_expression_count";
  public static final String CLASSEXPRESSION_COUNT_INCL = "class_expression_count_incl";
  public static final String CLASS_COUNT = "class_count";
  public static final String CLASS_COUNT_INCL = "class_count_incl";
  public static final String CLASS_SGL_SUBCLASS_COUNT = "class_sgl_subcl_count";
  public static final String CLASS_SGL_SUBCLASS_COUNT_INCL = "class_sgl_subcl_count_incl";
  public static final String CONSTRUCTS = "constructs";
  public static final String CONSTRUCTS_INCL = "constructs_incl";
  public static final String DATAPROPERTY_COUNT = "dataproperty_count";
  public static final String DATAPROPERTY_COUNT_INCL = "dataproperty_count_incl";
  public static final String DATATYPE_COUNT = "datatypes_count";
  public static final String DATATYPE_COUNT_INCL = "datatypes_count_incl";
  public static final String EXPRESSIVITY = "expressivity";
  public static final String EXPRESSIVITY_INCL = "expressivity_incl";
  public static final String GCI_COUNT = "gci_count";
  public static final String GCI_COUNT_INCL = "gci_count_incl";
  public static final String GCI_HIDDEN_COUNT = "gci_hidden_count";
  public static final String GCI_HIDDEN_COUNT_INCL = "gci_hidden_count_incl";
  public static final String INDIVIDUAL_COUNT = "individual_count";
  public static final String INDIVIDUAL_COUNT_INCL = "individual_count_incl";
  public static final String LOGICAL_AXIOM_COUNT = "logical_axiom_count";
  public static final String LOGICAL_AXIOM_COUNT_INCL = "logical_axiom_count_incl";
  public static final String AXIOM_COMPLEXRHS_COUNT = "ax_complexrhs_count";
  public static final String AXIOM_COMPLEXRHS_COUNT_INCL = "ax_complexrhs_count_incl";
  public static final String AVG_SIZE_COMPLEXRHS = "complexrhs_expression_count_avg";
  public static final String AVG_SIZE_COMPLEXRHS_INCL = "complexrhs_expression_count_avg_incl";
  public static final String MAX_NUM_NAMED_SUPERCLASS = "named_supercl_count_max";
  public static final String MAX_NUM_NAMED_SUPERCLASS_INCL = "named_supercl_count_max_incl";
  public static final String MULTI_INHERITANCE_COUNT = "multiple_inheritance_count";
  public static final String MULTI_INHERITANCE_COUNT_INCL = "multiple_inheritance_count_incl";
  public static final String ONTOLOGY_ID = "ontology_iri";
  public static final String ONTOLOGY_VERSION_ID = "ontology_version_iri";
  public static final String OBJPROPERTY_COUNT = "obj_property_count";
  public static final String OBJPROPERTY_COUNT_INCL = "obj_property_count_incl";
  public static final String RBOX_SIZE = "rbox_axiom_count";
  public static final String RBOX_SIZE_INCL = "rbox_axiom_count_incl";
  public static final String REF_CLASS_COUNT = "ref_class_count";
  public static final String REF_CLASS_COUNT_INCL = "ref_class_count_incl";
  public static final String REF_DATAPROP_COUNT = "ref_dataprop_count";
  public static final String REF_DATAPROP_COUNT_INCL = "ref_dataprop_count_incl";
  public static final String REF_INDIV_COUNT = "ref_indiv_count";
  public static final String REF_INDIV_COUNT_INCL = "ref_indiv_count_incl";
  public static final String REF_OBJPROP_COUNT = "ref_objprop_count";
  public static final String REF_OBJPROP_COUNT_INCL = "ref_objprop_count_incl";
  public static final String SYNTAX = "syntax";
  public static final String UNDECLARED_ENTITY_COUNT = "undecl_entity_count";
  public static final String UNDECLARED_ENTITY_COUNT_INCL = "undecl_entity_count_incl";
  public static final String VALID_IMPORTS = "valid_imports";
  public static final String BOOL_PROFILE_OWL2 = "owl2";
  public static final String BOOL_PROFILE_OWL2_DL = "owl2_dl";
  public static final String BOOL_PROFILE_OWL2_EL = "owl2_el";
  public static final String BOOL_PROFILE_OWL2_QL = "owl2_ql";
  public static final String BOOL_PROFILE_OWL2_RL = "owl2_rl";
  public static final String VIOLATION_PROFILE_OWL2_DL = "owl2dl_profile_violation";
  public static final String BOOL_PROFILE_RDFS = "rdfs";

  public static final String CONSISTENT = "consistent";
  public static final String UNSATISFIABLECLASSES_COUNT = "unsatisfiable_class_count";
  public static final String UNSATISFIABLECLASSES = "unsatisfiable_classes";
  public static final String INFERRED_SUBSUMPTIONS_COUNT = "inferred_subsumption_count";
  public static final String ERROR_MESSAGE_REASONING = "reasoner_error_message";
  public static final String INFERRED_ONLY_ENTAILMENT_COUNT = "inferred_only_entailment_count";
  public static final String DATATYPES = "datatypes_builtin";
  public static final String DATATYPES_INCL = "datatypes_builtin_incl";
  public static final String MAX_AXIOMLENGTH = "axiom_length_max";
  public static final String MAX_AXIOMLENGTH_INCL = "axiom_length_max_incl";
  public static final String DATATYPES_NOT_BUILT_IN = "datatypes_not_built_in";
  public static final String DATATYPES_NOT_BUILT_IN_INCL = "datatypes_not_built_in_incl";
  public static final String DATATYPE_AXIOMCOUNT = "datatypes_axiom_count";
  public static final String DATATYPE_AXIOMCOUNT_INCL = "datatypes_axiom_count_incl";
  public static final String COHERENT = "coherent";
  public static final String REASONERNAME = "reasoner";
  public static final String REASONERNAME_CLASS = "reasoner_class";
  public static final String REASONER_JAR = "reasoner_jar";
  public static final String REASONERVERSION = "reasoner_version";
  public static final String REASONER_TIMEOUT = "reasoner_timeout";
  public static final String DELEGATE_REASONER_CT = "delreasoner_ct";
  public static final String EL_DELEGATE_REASONER_CT = "el_delreasoner_ct";
  public static final String OWL_DELEGATE_REASONER_CT = "owl_delreasoner_ct";
  public static final String FILENAME = "filename";
  public static final String FILEPATH = "filepath";
  public static final String FILESIZE = "filesize";
  public static final String JAVA_EXCEPTION = "exception";
  public static final String JAVA_EXCEPTION_CAUSE = "exception_cause";
  public static final String EXPERIMENT_ID = "experiment_id";
  public static final String EXPERIMENT_VERSION = "experiment_version";
  public static final String EXPERIMENT_DATE = "experiment_date";
  public static final String JAVA_EXCEPTION_MESSAGE = "exception_message";
  public static final String GRAPH_TRAVERSE_MODE = "graph_traverse";
  public static final String LOGICAL_AXIOM_COUNT_REASONER = "logical_axiom_ct_reasoner";
  public static final String AD_CLTIME_PER_ATOM = "cltime_atoms";
  public static final String COMP_OVERHEAD = "comp_overhead";
  public static final String INFERRED_TAXONOMY_AXIOM_COUNT = "inf_ax_ct";
  public static final String INCREMENTAL_REASONER_NR_CLASSIFY_CALLS = "incr_reas_clfy_ct";
  public static final String SUBSUMPTIONS_ATOMIC_POSSIBLE = "subs_atomic_possible";
  public static final String SUBSUMPTIONS_ATOMIC_AVOIDED = "subs_atomic_avoid_theory";
  public static final String SUBSUMPTIONS_ATOMIC_AVOIDED_REASONER = "subs_atomic_avoid_reasoner";
  public static final String SUBSUMPTIONS_ATOMIC_REDUNDANT = "subs_atomic_redundant";
  public static final String SUBSUMPTIONS_ATOMIC_REASONER = "subs_atomic_reasoner";
  public static final String SIGNATURE_NO_URIS = "signature_w_o_iris";
  public static final String RULE_CT = "rule_count";
  public static final String RULE_CT_INCL = "rule_count_incl";
  public static final String INF_CLASSHIERARCHY_DEPTH = "inf_hier_depth";
  public static final String AXIOM_TYPES = "axiom_types";
  public static final String AXIOM_TYPES_INCL = "axiom_types_incl";
  public static final String NS_USE_AXIOMS = "namespace_axiom_count";
  public static final String NS_USE_AXIOMS_INCL = "namespace_axiom_count_incl";
  public static final String NS_USE_SIGNATURE = "namespace_entity_count";
  public static final String NS_USE_SIGNATURE_INCL = "namespace_entity_count_incl";
  public static final String TAUTOLOGYCOUNT = "tautology_count";
  public static final String TAUTOLOGYCOUNT_INCL = "tautology_count_incl";
  public static final String CYCLE = "certain_cycle";
  public static final String CYCLE_INCL = "certain_cycle_incl";
  public static final String EXPORT_EXCEPTION = "export_exception";
  public static final String FIXED_VIOLATIONS = "fixed_profile_violations";
  public static final String STRIPPED_AXIOMTYPES = "stripped_axiomtypes";

  public static final String STARTPROCESS_TS = "startprocess_ts";
  public static final String OWLAPILOAD_TS = "owlapiload_ts";
  public static final String CREATEREASONER_TS = "createreasoner_ts";
  public static final String CLASSIFICATION_TS = "classification_ts";
  public static final String DISPOSE_TS = "dispose_ts";
  public static final String FINISH_TS = "finishprocess_ts";

  public static final String PREPROCESSING_TS = "preprocessing_ts";
  public static final String CONSISTENCYCHECK_TS = "consistencycheck_ts";
  public static final String PREREASONINGOPTIMISATION_TS = "prereasoningoptimisation_ts";
  public static final String TRAVERSAL_TS = "traversal_ts";
  public static final String POSTPROCESSING_TS = "postprocessing_ts";
  public static final String CLASSIFICATION_CORRECT = "classification_correct";
  public static final String REASONING_PREPROCESSING_TIME = "reason_pp_dur";
  public static final String REASONING_MODULARREASONING_TIME = "reason_mmcl_dur";
  public static final String REASONING_CONSISTENCY_TIME = "reason_cc_dur";
  public static final String REASONING_PREREASONINGOPTIMISATION_TIME = "reason_pro_dur";
  public static final String REASONING_TRAVERSALPHASE_TIME = "reason_trav_dur";
  public static final String REASONING_POSTROCESSING_TIME = "reason_pop_dur";
  public static final String REASONING_DECOMPOSITION_TIME = "reason_dec_dur";
  public static final String REASONER_ID = "reasonerid";
  public static final String REASONER_DISPOSE_TIME = "reasoner_disposetime";
  public static final String RUNID = "experiment_runid";
  public static final String ONTOLOGY_ANNOTATIONS_COUNT = "ontology_anno_count";
  public static final String DATATYPE_BUILTIN_COUNT_INCL = "dt_builtin_count_incl";
  public static final String DATATYPE_BUILTIN_COUNT = "dt_builtin_count";
  public static final String DATATYPE_NOTBUILTIN_COUNT_INCL = "dt_notbuiltin_count_incl";
  public static final String DATATYPE_NOTBUILTIN_COUNT = "dt_notbuiltin_count";
  public static final String TBOX_CONTAINS_NOMINALS = "tbox_nominals";
  public static final String TBOX_CONTAINS_NOMINALS_INCL = "tbox_nominals_incl";
  public static final String ABOX_CONTAINS_NOMINALS = "abox_nominals";
  public static final String ABOX_CONTAINS_NOMINALS_INCL = "abox_nominals_incl";
  public static final String MOST_FRQUENTLY_USED_CONCEPT = "most_freq_concept";
  public static final String MOST_FRQUENTLY_USED_CONCEPT_INCL = "most_freq_concept_incl";
  public static final String VALID_IMPORTS_INCL = "valid_imports_incl";

  public static Map<String, String> getLabels() {
    Map<String, String> data = new HashMap<>();
    for (Field field : MetricsLabels.class.getDeclaredFields()) {

      try {
        String name = field.getName();
        String value = field.get(MetricsLabels.class).toString();
        data.put(name, value);

      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return data;
  }
}
