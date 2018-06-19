## 1 Set Up

Before beginning this tutorial, make sure you have the most recent version of ROBOT installed. [Installation instructions are available here]([http://robot.obolibrary.org/](http://robot.obolibrary.org/)). You only need the Command Line Tool - don’t worry about the Library.

You will also need the exercise files, [available here](https://github.com/ontodev/robot/blob/master/docs/tutorial/tutorial.zip). Simply download this ZIP and save it in an easily-accessible location. Open your terminal or command prompt and navigate to your newly-downloaded directory.

## 2 Templates

### 2.1 Adding new classes

The `template` command takes a CSV or TSV spreadsheet and transforms each row into one or more OWL axioms about an entity, specified by an `ID`. For detailed information on templates and template strings, [read the documentation](http://robot.obolibrary.org/template).

Open `basic-template.csv` in your favorite spreadsheet editor. You’ll see that the first three rows are already filled out. The first row consists of human-readable labels for the template strings located in the second row. The third row is an example of a class to be added.

Any template string (second row) that begins with a `C` is going to be a logical axiom, whereas any template string that starts with an `A` will be an annotation on the entity (like a label). `AT` is a typed annotation (e.g. integer, boolean), and `AL` is a language annotation. Finally `>A` signifies an annotation on the annotation to the left (e.g. a database_cross_reference on a definition). Logical axioms can be either subclass statements or equivalent statements based on `CLASS_TYPE`, which is shown in exercise 2.2.2.

Finally, if an annotation template string contains `SPLIT=|`, multiple annotations will be created for the contents separated by `|`. `|` can be replaced with any separator of your choice.

For the logical axioms with the `C`, any instance of `%` is going to be replaced by the content of the cell in that column. In the example, you’ll see the `Parent` column (column 4) uses the template string `C %`. That means that the cell value in that column will become the `subClassOf` value. In section 2.2, we will go over adding more complex subclass statements.

Currently, the `Parent` cell for the third row is blank. Whenever ROBOT encounters a blank cell when templating, it will skip adding that axiom for the entity defined in that row.

#### Exercise 2.1.1: create a class from a template

In your terminal or console, navigate to the tutorial directory and enter the following command:

```
robot template --template template-2-1-1.tsv --output template-2-1-1.owl
```

Now, open `template-2-1-1.owl` in Protege, and you’ll see a single class under owl:Thing - "liver".

You may also need to use classes from an external ontology. Any ontology that is specified as the `--input` for a template command will be used to find labels of entities. This allows you to use labels instead of CURIEs or IRIs for cell contents. If a label does not exist in the `--input` ontology, the command will fail with an error message pointing to the cell that caused the error.

#### Exercise 2.1.2: create a class with an external parent

Open `template-2-1-2.csv` and in row 3, column 4 (the "Parent" for `UBERON:0002107`), you will see `endocrine system`. Open your terminal and enter the following:

```
robot template --input edit.owl\
               --template template-2-1-2.tsv\
               --output template-2-1-2.owl
```

When you open `template-2-1-2.owl`, you can see that "liver" is a child of `UBERON_0000949`.

You’ll notice in the previous example that, even though we provided the label in the template, the actual ontology file had an ID instead of the label. This is because that class does not yet exist in the ontology you created, yet you have referenced it as a parent to your new class.

 #### Exercise 2.1.3: create a class with an external parent, part two

We are going to use the same template as in ex 2.1.2 (`template-2-1-2.tsv`), so all you need to do is run this command:

```
robot template --input edit.owl\
               --ancestors true\
               --template template-2-1-2.tsv\
               --output template-2-1-3.owl
```
Open the output ontology in Protege, and you’ll see that the class we created ("liver") now has ancestors ("anatomical entity" to "endocrine system") with labels, instead of just the IDs. This is because the `--ancestors true` option brings in the ancestors of your new term from the external `--input` ontology based on the [MIREOT](http://precedings.nature.com/documents/3574/version/1) principle.

Often, templates are used to add new classes to existing ontologies. In these cases, we don’t want to have to perform additional operations after templating our axioms, so we can actually merge the input ontology during the template process.

 #### Exercise 2.1.4: add a new class to an existing ontology
 
The `--merge-before` option tells the command to merge the new axioms into the `--input` ontology, and the result is all axioms in the `--output` ontology.

Again, using `template-2-1-2.tsv`, we will run the following:
```
robot template --input edit.owl\
               --merge-before\
               --template template-2-1-2.tsv\
               --output template-2-1-4.owl
```
When you open `template-2-1-4.owl`, you’ll see that all the classes from `edit.owl`, along with the new class from the template, exist in this ontology. You can add as many classes as you want with a single template command – try adding more lines to `template-2-1-2.tsv` and running this command again!

### 2.2 Adding logical axioms and annotations

The template command can also be used to add new axioms (logical or annotation) to existing classes. If the class already exists in the ontology, the template axioms will just be added to it. If it does not exist, a new declaration will be created.

#### Exercise 2.2.1: deprecating classes with templates

We’ve decided that the "body proper" (`UBERON:0013702`) and "main body axis" (`UBERON:0013701`) classes are out of scope for our ontology, so we want to deprecate them. Open `template-2-2-1.tsv` and you'll see there are 3 columns:

1. ID
2. Label
2. Deprecation Status

In the template string row (row 2), you can see that column 1 has `ID` and column 3 has `AT owl:deprecated^^xsd:boolean`. The `AT` prefix specifies that the `owl:deprecated` property will have a datatype associated with it - in this case, `xsd:boolean`. The column 2 template string (for label) is left blank because we do not need to re-add the labels; they already exist in the ontology. This is simply for editing purposes so that we can easily see which entities we are working on.
 
Now, run the following:
```
robot template --input edit.owl\
               --merge-before\
               --template template-2-2-1.tsv\
               --output template-2-2-1.owl
```
Now, open Protege and find "main body axis" and "body proper". You’ll see they have been crossed out, but are still children of "organism subdivision". The deprecated `true` value does not automatically remove associated axioms, so that needs to be done manually.

#### Exercise 2.2.2: adding logic with templates

We are going to add the "liver" class again, but this time we are going to use a logical description to define it as "part of the endocrine system", instead of just a subclass of "endocrine system". Open `template-2-2-2.tsv` and you’ll see that there are some new columns.

Column D is titled "Class Type" and the template string is `CLASS_TYPE`. This defines the type of axiom(s) that will be created from the row. There are two options: `subclass` or `equivalent`. The first entry, "abdomen element", will have an equivalent axiom; all cells with a `C` logical template string will be combined to create one equivalent axiom. The second entry, "liver", will have subclass axioms; each cell with a `C` logical template string will be a separate subclass statement.

Column F is titled "Part Of" and the template string is `C ‘part of’ some %`. The `%` symbol will be filled in to create a logical statement representing that the entity in that row is "part of" something else. The property must exist in your `--input` ontology in order to use it in the template string.

Run the following command in the terminal:

```
robot template --input edit.owl\
               --merge-before\
               --template template-2-2-2.tsv\
               --output template-2-2-2.owl
```
When you open `template.owl` in Protégé, find the class "abdomen element". You can see that it has one equivalent axiom in the class description box. "Liver" is the subclass of "abdomen element", and you can see that it has two subclass statements (one named class and one anonymous class) in the class description box.

## 3 Editing Commands

### 3.1 Remove

The `remove` command is used to remove entities and their axioms from an ontology. It is a highly configurable command with a series of options. The concept behind `remove` is that you provide an entity or a set of entities with `--entity` (one CURIE) or `--entities` (a text file with multiple CURIEs separated by line breaks). Then, you specify any related entities you would like to remove (for example, you could specify a class and select to remove all descendants of that class) with the `--select` option. Finally, you can also specify the types of axioms that you would like to remove with the `--axioms` option. For detailed information on these options, [read the documentation]([http://robot.obolibrary.org/remove](http://robot.obolibrary.org/remove)).

By default, if you do not specify an `--entity`, all entities will be added to the remove set. If you do not specify `--select`, any entities in the set will be removed, instead of looking at related entities. Finally, without specifying an `--axioms` option, all axioms are removed.

#### Exercise 3.1.1: remove a class and its descendants

We are going to remove "organism subdivision" and all descendants from the `edit.owl` ontology. To do so, enter the following command:
```
robot remove --input edit.owl\
             --entity UBERON:0000475\
             --select "self descendants"\
             --output remove-3-1-1.owl
```
The `--entity` option specifies (by CURIE) which class we are starting with. The `--select` option tells the command that we want to remove "self" (the class itself) and "descendants". If you don’t specify "self", only the descendants would be removed.

Open `remove-3-1-1.owl` in Protégé and you will see that "organism subdivision" is gone from under "multicellular anatomical structure".

#### Exercise 3.1.2: create a ‘simple’ version

For the "simple" version of our ontology, we are going to remove all logical definitions and only maintain the is_a hierarchy:
```
robot remove --input edit.owl\
             --axioms equivalent\
      remove --select "anonymous parents"\
             --output remove-3-1-2.owl
```
Open `remove-3-1-2.owl` and browse through the classes. You will notice that there are no longer any anonymous parents or equivalence statements. We did this in a two-step process: first we removed all the equivalent axioms, then we sent that output (also called [chaining]([http://robot.obolibrary.org/chaining](http://robot.obolibrary.org/chaining))) to a second remove command that removed any anonymous parents.

#### Exercise 3.1.3: remove annotation properties

Finally, in some subsets, you may only want limited annotation properties on the classes. For this, we are only going to keep the label for each entity. We will do this using the "complement" feature, which selects the opposite of what you provide for in the seed:
```
robot remove --input edit.owl\
             --entity rdfs:label\
             --select "complement annotation-properties"\
             --output remove-3-1-3.owl
```
We needed to add `annotation-properties` to the selection. If we didn’t, it would have removed ALL entities other than `rdfs:label`. This way, we are only removing all other annotation properties.

### 3.2 Filter

The `filter` command uses the same rules as `remove`, except instead of removing the matching entities, it copies them to a new ontology.

#### Exercise 3.2.1: extract a branch

Instead of getting rid of a class and all its descendants, this time we are going to create a subset ontology that consists of just a class and all its descendants. This can be similarly accomplished with the extract method, but sometimes extract pulls in undesired dependencies.
```
robot filter --input edit.owl\
             --entity UBERON:0000475\
             --select "self descendants annotations"\
             --output filter-3-2-1.owl
```

`annotations` must be added to the select statement for filter most of the time. This ensures that the annotations of entities are included, otherwise ONLY the entities in the seed set and selected entities are included. If you include annotation properties in your seed set, then they will be included without the need for the `annotations` selector.

#### Exercise 3.2.2: create a subset based on an annotation

It is also helpful to extract a subset based on the `in_subset` annotation property. The `--select` option provides a way to select entities based on an annotation. The general format is `CURIE=value`. The value can be a literal (in single quotes), a CURIE, or an IRI (in diamond brackets).

Subset values are usually annotation properties that are children of `subset_property`, from OBO format legacy. Because the value is another entity, we will need to reference the IRI of the subset property. For this one, we will be selecting entities in `uberon_slim`, which has the IRI `[http://purl.obolibrary.org/obo/uberon/core#uberon_slim](http://purl.obolibrary.org/obo/uberon/core#uberon_slim)`.

To get the entities in this slim, run the following:
```
robot filter --select\
             "CURIE=<[http://purl.obolibrary.org/obo/uberon/core#uberon_slim]\
              (http://purl.obolibrary.org/obo/uberon/core#uberon_slim)>"\
             --output filter-3-2-2.owl
```

It should be noted that if no `--entity` or `--entities` option is provided, the full set of entities in the ontology will be the seed set. The same is true for the `remove` command.

### 3.3 Import Management

Often, bioontologies need to import terms from external sources. The external ontologies usually contain more entities than we need, and it wouldn't make sense to import all of them. The `extract` command is another alternative to create modules. One may choose to use `extract` over `filter` because, depending on the method of extraction, it ensures that all logical dependencies are included in the module.

Maintaining the logical structure of an external class is important for using that class in logical definitions in the ontology. That way, running the reasoner will return accurate results.

#### Exercise 3.3.1: extracting a module with BOT

To get an entity (or a set of entities) and all of the *logical* ancestors (including anonymous ancestors), the `BOT` method is used. Conversely, if `filter` was used with the `ancestors` selector, only the named ancestors would be included. Any class that is used in an equivalence axiom, or any anonymous parents, will not be included, unless the selector explicitly says so.

We are going to add a logical definition to the "adrenal gland" to state that it secretes epinephrine. First, we need to create an import module with "epinephrine secretion" (`GO:0048242`):
```
robot extract --input-iri http://purl.obolibrary.org/obo/go.owl\
              --term GO:0048242\
              --method BOT\
              --output go_import.owl
```
This command will take a moment, as it has to retrieve GO from online via the `--input-iri` option. If you have a local file, you can use `--input [filename]` instead.

Now, open `edit.owl` in Protégé and add the import statement to the ontology. In the "Active Ontology" tab (default window upon opening), find the "Ontology Imports" section at the bottom. Click the plus sign next to "Direct Imports" and "Import and ontology contained in a specific file". Browse to where `go_import.owl` is located (in your tutorial directory) and then click "Continue". Finally, click "Finish".

When you go to the "Entities" tab, you'll see that "biological_process" is now under `owl:Thing`. These entities are not in bold, meaning that they are not *asserted* in your active ontology. All of the descendant terms are important for the logical structure of "epinephrine secretion".

Now you can add the following SubClass Of statement to "adrenal gland" with the Class Expression Editor:
```
'contains process' some 'epinephrine secretion'
```

#### Exercise 3.3.2: merge imports into the ontology

The merge command can be used to merge separate ontologies, or to merge all import statements into an ontology. Since we have already added the import statement to our `edit.owl` ontology, we can now merge it in to create a product:
```
robot merge --input edit.owl\
            --collapse-import-closure true\
            --output merged.owl
```
The `--collapse-import-closure` option states what to do with the import statements. If you are merging separate ontologies (multiple `--input` options), you may not want their imports to be included. By default, `--colapse-import-closure` is `false` so you do not need to include that option in this case.

When you open `merged.owl`, you'll see that "biological_process" and its descendants are now in bold, showing that they are asserted in your ontology. Also, the import statement is gone from the "Active Ontology" tab.

## 4 Queries

### 4.1 SPARQLing with ROBOT

The [query](http://robot.obolibrary.org/query) command provides an interface to query the ontologies with ROBOT. The command is as follows:
```
robot query --input [ontology] --query [query] [output]
```
The query output is TSV, CSV, or ttl (Turtle) format. For most purposes, the spreadsheet formats (TSV and CSV) should be sufficient. In the following sections, we will go through a series of SPARQL query examples that you can follow along with.

Make sure you have navigated to your tutorial directory in the terminal and have a text editor open to save the queries.

### 4.2 Introduction to SPARQL SELECT statements

SPARQL (pronounced sparkle) is the query language we use to access data in ontologies, much like how SQL is used to access data in relational databases. Often, there are too many entities in our ontologies to simply browse it to answer a question, so we rely on SPARQL to gather the data for us. SPARQL queries are used for much more than just answering questions, but here we will focus on the `SELECT` queries.

The second part of the SELECT statement consists of a series of triples in which one of the entities may be replaced by a question word (a variable), starting with the `?` character. Each statement ends with `.`. This is the pattern that SPARQL will use to retrieve the question words. For example, if you know the ID of a class but wish to retrieve the `rdfs:label` value:
```
foo:bar rdfs:label ?label .
```

#### Exercise 4.2.1: a basic SELECT query

The first part of the statement specifies all the question words, and then points to the pattern we would like to match. Copy the following query to your text editor, and save the file as `query.rq` in your tutorial directory:

```
PREFIX obo: <http://purl.obolibrary.org/obo/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?label WHERE { obo:UBERON_0000062 rdfs:label ?label . }
```

It's important to declare your prefixes at the start of the file, otherwise `query` will not know how to resolve things like `obo` and `rdfs`.

Now, run the following command:
```
robot query --input tutorial.owl --query query.rq query.tsv
```

Open `query.tsv` and you should see one result under `?label`: "organ"

#### Exercise 4.2.2: multi-variable queries

For each of the following queries, replace the contents of `query.rq` and rerun the command to see their output in `query.tsv`.

You can include any number of question words, separated by spaces. For example, you can return the IRIs of all entities and their labels:
```
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?subject ?label WHERE { ?subject rdfs:label ?label . }
```
Alternatively, you can just tell the query to return *all* the question words with the `*` character:
```
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT * WHERE { ?subject rdfs:label ?label . }
```
Keep in mind that the subject (the first word of the triple) will usually return an IRI or a *blank node*. Blank nodes represent an entity that has been defined in the ontology, but has not been given an IRI. In the world of bioontologies, these are often called *anonymous ancestors*, *anonymous parents*, or *anonymous classes*. Instead of returning an IRI, a blank node will be returned as an `_` followed by a series of characters. We will cover blank nodes in the Advanced SPARQL section.

#### Exercise 4.2.3: omitting variables

For the following query, replace the content of `query.tsv` and rerun the command to see the output in `query.tsv`.

It is also important to remember that you do not need to return all the question words used in your query. For example, you may just want the all labels used in your ontology:
```
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?label WHERE { ?subject rdfs:label ?label . }
```
The question words can be anything you’d like. You could rewrite the above to `?panda rdfs:label ?tugboat` and it would still return the same data. We recommend using meaningful words, though, just to keep things organized!

### 4.3 Introduction to basic filtering

Simply asking for all the entities and their labels isn’t always that useful; we want to be able to ask more complex questions. SPARQL queries can have as many sets of triples as necessary to be as expressive as you’d like. In order to return just the relevant data, you will probably need to use `FILTER` statements within your queries.

There are two basic types of `FILTERS`: string matching and value comparisons.

The most common string matching filter is the `regex` statement. This refers to "regular expression" to find strings that match a given pattern. The regex filter is like a function that accepts two required arguments: the question word and the pattern to match.

#### Exercise 4.3.1: regex filtering

For each of the following queries, replace the content of `query.tsv` and rerun the command to see the output in `query.tsv`.

To find all entities that have a label with the word "gland":

```
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT * WHERE {
  ?subject rdfs:label ?label .
  FILTER regex(?label, "gland*")
}
```

By adding the `!` character right before the function, you can return the opposite - all labels that do not have the word "gland":
```
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT * WHERE {
  ?subject rdfs:label ?label .
  FILTER (!regex(?label, "gland*"))
}
```

#### Other filtering examples

Value comparisons use standard operators to compare literal values. These are usually used with numeric literals, but can also be used with string literals. For example, you might suspect there are duplicate labels in your ontology (note that our `edit.owl` file does not contain duplicate labels, but you can always add one to test this out!). You can check with:

```
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT DISTINCT ?sub1 ?sub2 WHERE {
  ?sub1 rdfs:label ?lab1 .
  ?sub2 rdfs:label ?lab2 .
  FILTER (?lab1 = ?lab2)
  FILTER (?sub1 != ?sub2)
}
```

We added the `DISTINCT` keyword here to remove any duplicate values that would pop up. This is because every entity in the ontology will at some point be both `?sub1` and `?sub2`; the query compares *all possible pairs*. We also have to add in the filter that `?sub1` is not equal to `?sub2` for this reason.

The two filters could also be combined as `FILTER (?lab1 = ?lab2 && ?sub1 != ?sub2)`.

Alternatively, to find only unique labels:
```
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?sub1 ?lab1 WHERE {
  ?sub1 rdfs:label ?lab1 .
  ?sub2 rdfs:label ?lab2 .
  FILTER (?lab1 != ?lab2)
}
```
We don’t want to use `SELECT * WHERE` here because that will return every possible combination of `?sub1` and `?sub2`. Depending on the size of your ontology, that could get out of hand pretty quickly. Returning `?sub1` and `?lab1` will still return all distinct labels, since you are enumerating through all entities.
