# Tables

## Building the Table

### Tables

The ROBOT API provides classes to build tables which can be exported to TSV, CSV, HTML, JSON, or Excel.

First, instantiate a [`Table`](https://github.com/ontodev/robot/blob/master/robot-core/src/main/java/org/obolibrary/robot/export/Table.java) object with your format:
```java
Table table = new Table("html");
```

Supported formats are: `tsv`, `csv`, `html`, `xlsx`, `json`, and `yaml`.

### Columns

Next, you need to add your columns. For each column in your output, create a [`Column`](https://github.com/ontodev/robot/blob/master/robot-core/src/main/java/org/obolibrary/robot/export/Column.java) object and add it to the table:
```java
String name = "foo";
Column col = new Column(name);
table.addColumn(col);
```

The above code creates a simple column with the name as the header. If you are rendering OWL Objects and wish to pass these objects into the cells instead of string values, you should include a [`ShortFormProvider`](https://owlcs.github.io/owlapi/apidocs_4/org/semanticweb/owlapi/util/ShortFormProvider.html). If all your cell values are strings, you do not need a `ShortFormProvider`.
```java
String name = "foo";
Column col = new Column(name, shortFormProvider);
table.addColumn(c);
```

### Rows and Cells

Once your columns are added, you can add your [`Row`](https://github.com/ontodev/robot/blob/master/robot-core/src/main/java/org/obolibrary/robot/export/Row.java) and [`Cell`](https://github.com/ontodev/robot/blob/master/robot-core/src/main/java/org/obolibrary/robot/export/Cell.java) objects for each column. You can iterate through your added columns using the `table.getColumns()` method:
```java
Row row = new Row();
for (Column col : table.getColumns()) {
	String value = "bar";
	Cell c = new Cell(col, value);
	row.add(c);
}
table.addRow(row);
```

If you have more than one value per cell, you can pass a `List<String>` of values instead of the single string value. When you write the output, these will be separated within the cell by a provided `split` character (described below).

You can also choose to pass both a `value` and a `displayValue`. If you want to sort your rows by a value that is *different* than the display value, you can use these two. For example:
```java
// value to sort on
String termID = "foo:123";
// value to display
String termLabel = "bar";
Cell c = new Cell(col, termID, termLabel);
```

### Sorting Rows

When you create a column, you can choose to have it used as a column to sort on. You can provide multiple columns to sort on by passing an increasing integer to `setSort`. If `setSort` is never called on a column object, the column will not be used for sorting.
```java
// This column will be sorted first
Column col = new Column("foo");
col.setSort(0);

// This column will be sorted second
Column col2 = new Column("foo 2");
col2.setSort(1);
```

You can also do a reverse sort:
```java
col.setSort(0, true);
```

Then, add these to the table and set the sort order:
```java
table.addColumn(col);
table.addColumn(col2);
table.setSortColumns();
```

After you've added all rows, sort them (based on the sort columns) before writing the output:
```java
table.sortRows();
```

## Writing to Output

For most outputs (excluding `JSON` and `YAML`), you must provide a `String split` character. This can be an empty string if you do not have any cells with more than one value. Otherwise, we recommend using something like a comma or a pipe character.

In general, you can write your table to your desired output with the `write` method. This uses the provided `format` from when you instantiated the table.
```java
String path = "mytable.tsv";
String split = "";
boolean success = table.write(path, split);
```

See below for more specific ways to write your output.

### TSV and CSV

You can write either TSV or CSV based on the file name or a separator character:

```java
String split = "";
List<String> rows = table.toList(split);

// Write the table to a path
String path = "mytable.csv";
// IOHelper gets the format from the file extension (.csv)
IOHelper.writeTable(rows, path);

// Or you can pass a file object and separator
File file = new File(path);
char separator = ',';
IOHelper.writeTable(rows, file, separator);
```

### Excel Workbook

The `asWorkbook` method returns a [POI `Workbook`](https://poi.apache.org/apidocs/dev/org/apache/poi/ss/usermodel/Workbook.html) object:

```java
String split = "";
String path = "mytable.xlsx";
try (Workbook wb = table.asWorkbook(split);
     FileOutputStream fos = new FileOutputStream(path)) {
  wb.write(fos);
}
```

### HTML

All `toHTML(...)` methods return an HTML string:
```java
String split = "";
String path = "mytable.html";
try (PrintWriter out = new PrintWriter(path)) {
	String html = table.toHTML(split);
	out.print(html);
}
```

This string includes Bootstrap CSS. You can choose to make a "non-standalone" version of the HTML which does not include the HTML headers or the Bootstrap CSS as well. This can be useful for plugging the HTML output into existing HTML pages:
```java
String split = "";
boolean standalone = false;
String html = table.toHTML(split, standalone);
...
```

### JSON and YAML

The methods for creating JSON and YAML from the `Table` are similar:

```java
String json = table.toJSON();
String path = "mytable.json";
try (PrintWriter out = new PrintWriter(path)) {
	out.print(json);
}
```

```java
String yaml = table.toYAML();
String path = "mytable.yml";
try (PrintWriter out = new PrintWriter(path)) {
	out.print(yaml);
}
```
