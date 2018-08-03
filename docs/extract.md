# Extract

The reuse of ontology terms creates links between data, making the ontology and the data more valuable. But often you want to reuse just a subset of terms from a target ontology, not the whole thing. Here we take the filtered ontology from the previous step and extract a "STAR" module for the term 'adrenal cortex' and its supporting terms:

    robot extract --method STAR \
        --input filtered.owl \
        --term-file uberon_module.txt \
        --output results/uberon_module.owl

See <a href="/examples/uberon_module.txt" target="_blank">`uberon_module.txt`</a> for an example of a term file. Terms should be listed line by line, and comments can be included with `#`. Individual terms can be specified with `--term` followed by the CURIE.

The `--method` options fall into two groups: Minimal Information for Reuse of External Ontology Term (MIREOT) and Syntactic Locality Module Extractor (SLME).

- MIREOT: extract a simple hierarchy of terms
- STAR: use the SLME to extract a fixpoint-nested module
- TOP: use the SLME to extract a top module
- BOT: use the SLME to extract a bottom module

For MIREOT, both "upper" (ancestor) and "lower" (descendant) limits can be specified, like this:

    robot extract --method MIREOT \
        --input uberon_fragment.owl \
        --upper-term "obo:UBERON_0000465" \
        --lower-term "obo:UBERON_0001017" \
        --lower-term "obo:UBERON_0002369" \
        --output results/uberon_mireot.owl

To specify upper and lower term files, use `--upper-terms` and `--lower-terms`. The upper terms are the upper boundaries of what will be extracted. If no upper term is specified, all terms up to the root (`owl:Thing`) will be returned. The lower term (or terms) is required; this is the limit to what will be extracted, e.g. no descendants of the lower term will be included in the result.

NOTE: The `extract` command works on the input ontology, not its imports. To extract from imports you should first [merge](/merge).

For more details see:

- [MIREOT](http://dx.doi.org/10.3233/AO-2011-0087)
- [SLME](http://owlcs.github.io/owlapi/apidocs_4/uk/ac/manchester/cs/owlapi/modularity/SyntacticLocalityModuleExtractor.html)
- [ModuleType](http://owlcs.github.io/owlapi/apidocs_4/uk/ac/manchester/cs/owlapi/modularity/ModuleType.html)

You can also include ontology annotations from the input ontology with `--copy-ontology-annotations true`. By default, this is false.

    robot extract --method BOT \
      --input annotated.owl \
      --term UBERON:0000916 \
      --copy-ontology-annotations true \
      --output results/annotated_module.owl

---

## Error Messages

### Missing MIREOT Term(s) Error

MIREOT requires either `--lower-term` or `--branch-from-term` to proceed. `--upper-term` is optional.

### Missing Lower Term(s) Error

If an `--upper-term` is specified for MIREOT, `--lower-term` (or terms) must also be specified.

### Invalid Method Error

The `--method` option only accepts: MIREOT, STAR, TOP, and BOT.

### Invalid Option Error

The following flags *should not* be used with STAR, TOP, or BOT methods:
* `--upper-term` & `--upper-terms`
* `--lower-term` & `--lower-terms`
* `--branch-from-term` & `--branch-from-terms`
