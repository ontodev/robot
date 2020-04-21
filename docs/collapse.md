# Collapse

Sometimes, a class hierarchy can contain more intermediate classes than necessary, especially when extracting modules. ROBOT includes `collapse` to remove intermediate classes based on a minimal number of subclasses, using the `--threshold` option. If `--threshold` is not provided, it will default to `2`. The threshold must be a positive integer greater than or equal to 2.

```
robot collapse \
 --input module.owl \
 --threshold 3 \
 --output minimized_module.owl
```

Collapse will not remove root classes or leaf classes (i.e. top-level classes, without a named superclass, and bottom-level classes, without any subclasses). For each intermediate class, if it has *fewer* subclasses than the threshold, then it will be removed.

For example, given `--threshold 2`:

```
- class:A
    - class:B
    - class:C
        - class:D
           - class:E
    - class:F
        - class:G
        - class:H
```

Becomes:

```
- class:A
    - class:B
    - class:E
    - class:F
            - class:G
            - class:H
```

`class:C` and `class:D` are removed because they each only have one subclass. `class:F` is kept because it has two subclasses, which is the threshold.

If there are any classes that you don't want removed, you can keep them regardless of the number of subclasses using `--precious <IRI/CURIE>` (for a set of terms in a file, use `--precious-terms <term-file>`). 

    robot collapse \
     --input uberon_module.owl \
     --threshold 3 \
     --precious UBERON:0000483 \
     --output results/uberon_minimized.owl

For example, given `--threshold 2` and `--precious class:D`, that same example from above would become:

```
- class:A
    - class:B
    - class:D
        - class:E
    - class:F
            - class:G
            - class:H
```
 
 ---
 
 ## Error Messages
 
 ### Threshold Error
 
 The `--threshold` input must be an integer.
 
 ### Threshold Value Error
 
 The `--threshold` input must be an integer greater than 1.

