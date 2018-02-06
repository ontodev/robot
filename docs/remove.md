# Remove

While [filter](/filter) specifies what to keep, `remove` allows removal of given entities (classes, individuals, and properties). For example, to remove the `has_part` (BFO:0000051) object property and all axioms that use it:
```
robot remove --input edit.owl --entity BFO:0000051 --output removed.owl
```

More specifically, you can specify removal with non-generic types:
  * `--class <arg>`
  * `--individual <arg>`
  * `--object-property <arg>`
  * `--annotation-property <arg>`
  * `--datatype-property <arg>`

Multiple entities can be removed by providing the path to a text file containing each entity's CURIE on a separate line (see <a href="examples/ids.txt" target="_blank">`ids.txt`</a> for an example):
```
robot remove --input edit.owl --entities ids.txt --output removed.owl
```

Other options:
  * Remove all individuals with `--all-individuals`
  * Remove all descendant classes of a class with `--descendant-classes <arg>`
  * Remove all anonymous superclasses of a class with `--anonymous-superclasses <arg>`