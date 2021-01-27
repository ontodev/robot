# Export Prefixes

You can print or save the current [prefixes](/global#prefixes) using the `export-prefixes` command:

    robot --prefixes foo.json \
      --prefix "bar: http://bar#" \
      --prefix "baz: http://baz#" \
      export-prefixes

This prints the [default prefixes](https://github.com/ontodev/robot/blob/master/robot-core/src/main/resources/obo_context.jsonld) *plus* the user-added prefixes in [JSON-LD format](https://json-ld.org/) The prefixes are contained within a `@context` that can be used to resolve [JSON-LD compact IRIs](https://www.w3.org/TR/json-ld/#compact-iris). For the above, command, the printed output would look something like:

```
{
  "@context": {
    "obo": "http://purl.obolibrary.org/obo/",
    "oboInOwl": "http://www.geneontology.org/formats/oboInOwl#",
    ...
    "foo": "http://foo#",
    "bar": "http://bar#",
    "baz": "http://baz#"
  }
}
```

You can also include an output to write the prefixes to a file:

    robot --noprefixes --prefix "foo: http://foo#" \
      export-prefixes --output results/foo.json

The `--noprefixes` flag is used in this example, meaning the default prefixes are not included. For more details on adding prefixes, see [prefixes](/global#prefixes).
