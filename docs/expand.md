# Expand

## Contents

## Overview

Some logical patterns in OWL are cumbersome to maintain by hand; shortuct annotation properties ("macros") can be used as an
editor's representation which are automatically expanded to more complex axioms.

For example, an ontology may include the notion of "part disjointness", i.e., nothing with a 'part of' relationship
to an instance of concept A has a 'part of' relationship to any instance of concept B. This can be represented using
a GCI like so:

```
('part of' some A) DisjointWith ('part of' some B)
```

Alternatively, one could define an annotation property `'part disjoint with'`:

```
A 'part disjoint with' B
```

This property definition can be annotated with another property, [defined by construct](http://purl.obolibrary.org/obo/OMO_defined_by_construct),
pointing to a SPARQL CONSTRUCT query, which is used to generate the more complex OWL axioms when running `robot expand`:

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

By default, all SPARQL CONSTRUCT queries found as values for the [defined by construct](http://purl.obolibrary.org/obo/OMO_defined_by_construct) property
will be executed against the input ontology, and the results merged in.

Alternatively, specific macro terms can be included or excluded from the expansion process:

    robot expand \
        --input unexpanded.ttl \
        --expand-term 'http://example.org/never_part_of' \
        --no-expand-term 'http://example.org/part_disjoint_with' \
        --output results/expanded-terms.ttl

You can also use `--expand-term-file` and `--no-expand-term-file` to import lists of properties from files.
