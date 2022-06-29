# Expand

## Contents

1. [Overview](#overview)
2. [Specifying expansions](#specifying-expansions)
3. [Annotating expansion results](#annotating-expansion-results)
4. [Expansions do not interact](#expansions-do-not-interact)

## Overview

Some logical patterns in OWL are cumbersome to maintain by hand; shortcut annotation properties ("macros") can be used as an editor's representation which are automatically expanded to more complex axioms.

For example, an ontology may include the notion of "part disjointness", i.e., nothing with a 'part of' relationship to an instance of concept A has a 'part of' relationship to any instance of concept B. This can be represented using a GCI like so:

```
('part of' some A) DisjointWith ('part of' some B)
```

Alternatively, one could define an annotation property `'part disjoint with'`:

```
A 'part disjoint with' B
```

This property definition can be annotated with another property, [defined by construct](http://purl.obolibrary.org/obo/OMO_0002000), pointing to a SPARQL CONSTRUCT query, which is used to generate the more complex OWL axioms when running `robot expand`:

```
'part disjoint with' 'defined by construct' """
    PREFIX owl: <http://www.w3.org/2002/07/owl#>
    PREFIX : <http://example.org/
    CONSTRUCT {
      [
        a owl:Restriction ;
        owl:onProperty :part_of ;
        owl:someValuesFrom ?a ;
        owl:disjointWith [
          a owl:Restriction ;
          owl:onProperty :part_of ;
          owl:someValuesFrom ?b
        ]
      ]
    }
    WHERE {
      ?a :part_disjoint_with ?b .
    }
    """
```

Here is an example:

    robot expand \
        --input unexpanded.ttl \
        --output results/expanded.ttl

## Specifying expansions

By default, all SPARQL CONSTRUCT queries found as values for the [defined by construct](http://purl.obolibrary.org/obo/OMO_0002000) property will be executed against the input ontology, and the results merged in.

Alternatively, specific macro terms can be included or excluded from the expansion process with `--expand-term` and `--no-expand-term`:

    robot expand \
        --input unexpanded.ttl \
        --expand-term 'http://example.org/never_part_of' \
        --output results/expanded-terms.ttl

You can also use `--expand-term-file` and `--no-expand-term-file` to import lists of properties from files: one CURIE or IRI per line, similar to [`parts.txt`](/examples/parts.txt). Term in the "no-expand" list (both via arguments and files) will be subtracted from the "expand" list.

## Annotating expansion results

Axioms generated via the `expand` command can be annotated with a [dct:source](http://purl.org/dc/terms/source) relation linking them to the property specifying the expansion:

    robot expand \
        --input unexpanded.ttl \
        --annotate-expansion-axioms true \
        --output results/expanded-annotated.ttl

## Expansions do not interact

Expansion queries are applied to the input ontology, not to the output of any other expansions. So, expansions do not interact with one another, and their order of application does not matter. If you wish to implement interactions between expansions the command could be chained, e.g.:

```
robot expand -i myont.owl --expand-term A expand --expand-term B -o my-expanded.owl
```

It is possible that other expansion approaches that support interaction could be added to this command in the future, such as an RDF rule engine.
