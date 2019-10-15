# Minimize

Sometimes, a class hierarchy can contain more intermediate classes than necessary, especially when extracting modules. ROBOT includes `minimize` to remove intermediate classes based on a minimal number of subclasses, using the `--threshold` option.

```
robot minimize \
 --input module.owl \
 --threshold 3 \
 --output minimized_module.owl
```

Any intermediate class (has one or more subclasses) that has *less* than the threshold number of subclasses will be removed. Top-level classes (do not have a named superclass) and bottom-level classes (do not have any subclasses) will not be removed. 

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

If there are any classes that you don't want removed, you can keep them regardless of the number of subclasses using `--precious-term <IRI/CURIE>` (for a set of terms in a file, use `--precious-terms <term-file>`). 

    robot minimize \
     --input uberon_module.owl \
     --threshold 3 \
     --precious-term UBERON:0000483 \
     --output results/uberon_minimized.owl

For example, given `--threshold 2` and `--precious-term class:D`, that same example from above would become:

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