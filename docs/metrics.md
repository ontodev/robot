# Metrics

## Contents

1. [Overview](#overview)
2. [Types of metrics](#types-of-metrics)

## Overview
Robot can compute a number of metrics about your ontology, such as entity and
axiom counts, qualitative information about your ontology such as OWL 2 profiles
and more complex metrics aimed at informing ontology developers about possible performance bottlenecks.

    robot metrics --input edit.owl --output results/metrics.tsv

## Types of metrics
There are three general modes for computing metrics:

1. Essential metrics (`--metrics essential`, default)
2. Extended metrics (`--metrics extended`)
3. All metrics (`--metrics all`)

_Essential metrics_ include basic ontology entity counts (number of classes, individuals and properties), axiom counts
and a few qualitiative metrics such as OWL profiles.

_Extended metrics_ contain all the essential metrics, and additional details on axiom types, logical expressivity and datatypes.

_All metrics_ include all the essential and extended metrics, as well as a range of more complex metrics targeted at
OWL and reasoning specialists, such as information about GCIs, shape of the class hierarchy and potential cyclicity.

## Exceptions

fill in like others