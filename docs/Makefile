## ROBOT Documentation Makefile
#
# This file helps us keep our documentation up-to-date
# by generating Markdown files form various source files.
# The goal is DRY: Don't Repeat Yourself!


### Jekyll
#
# You can test the documentation locall using Jekyll and various GitHub Pages stuff:
# https://help.github.com/articles/setting-up-your-github-pages-site-locally-with-jekyll/


### Configuration

# Collect the list of query files for report.
REPORT_QUERIES := $(wildcard ../robot-core/src/main/resources/report_queries/*.rq)
REPORT_DOCS := $(foreach x, $(REPORT_QUERIES), report_queries/$(notdir $(basename $(x))).md)


### Main Tasks

all: $(REPORT_DOCS) report_queries/index.md


### Report Tasks

report_queries:
	mkdir -p $@

# Convert each SPARQL file to a Markdown file.
report_queries/%.md: ../robot-core/src/main/resources/report_queries/%.rq | report_queries
	awk '/^PREFIX/ && !x {print "```sparql"; x=1} 1' $< \
	| sed 's/^#//' \
	| sed 's/^ //' \
	> $@
	echo '```' >> $@

# Use the master profile to generate a page of links.
report_queries/index.md: ../robot-core/src/main/resources/report_profile.txt | report_queries
	echo '# Report Queries' > $@
	echo '' >> $@
	echo 'The table below lists all the queries run by the `report` command,' >> $@
	echo 'with their default severity levels.' >> $@
	echo 'See [`report`](../report) for more details.' >> $@
	echo '' >> $@
	echo '| Name  | Default Severity  ' >> $@
	echo '|-------|-------------------' >> $@
	awk -F'	' '{x=$$2; gsub(/_/," ",$$2); print "| [" $$2 "](" x ")  | " $$1}' $< \
	>> $@
