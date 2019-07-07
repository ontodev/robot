# Analyze

Example:

```
robot analyze  --input logical_axioms.owl --output results/analysis_report.tsv
```

This performs an analysis of logical axioms in the ontology. Currently
one type of analysis is performed, a power analysis of each axiom.

Each axiom is tested for its inferential power - i.e. how many
additional axioms does it entail, and how many existing asserted
axioms can only be entailed when it is used.

This is useful to know, as a low power rating means that you may be
under-utilizing axioms and reasoning, or you may be *overstating*
logical definitions.

## Algorithm

### Step 1: Removed Redundant Axioms

Any asserted axiom that can still be entailed is removed from the
ontology

For each subClassOf axiom `A = C SubClassOf D`, we remove `A` from
`O`. We then use the reasoner to test if `D` is an inferred superclass
(direct or indirect of C).

If this is the case, the `A` is redundant and is removed from the
ontology.

### Step 2: Perform relaxation step

Additional SubClassOf axioms are added, according to the
[relax](relax) operation.

```
C EquivalentTo X1 and X2 and ... and Xn
==>
C SubClassOf X1
C SubClassOf X2
...
C SubClassOf Xn
```

The purpose of this step is to avoid treating the relaxed axioms as
"new" inferences.

### Step 3: Test individual axioms

For each logical axiom `A` we calculate:

```Power = | E(O) - E(O-A) |```

Where `E` returns all entailed direct axioms (currently just
subClassOf) from an ontology.

### Step 4: Aggregate statistics and report

The average power is calculated, as well as all axioms that share the maximum power.

## Workflow

By design, this command works on the root ontology in an import
chain. Use [merge](merge) first if you need to merge an import chain
into one ontology.

For example, if you wish to evaluate how much CL, CHEBI, and Uberon,
etc contribute to classification in GO, then you can take either the
go-edit file, or the release go-plus (both of which include full
axioms, plus import modules), merge the results, then run `analyze`.

Currently aggregate stats will not be performed per-source, but you
can easily do this from the output TSV using standard tools.






