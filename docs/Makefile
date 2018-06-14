## ROBOT Documentation Makefile
#
# This file helps us keep our documentation up-to-date
# by generating Markdown files form various source files.
# The goal is DRY: Don't Repeat Yourself!


### Configuration

# Collect the list of query files for report.
REPORT_QUERIES := $(wildcard ../robot-core/src/main/resources/queries/*.rq)
REPORT_DOCS := $(foreach x, $(REPORT_QUERIES), report/$(notdir $(basename $(x))).md)


### Main Tasks

all: $(REPORT_DOCS) report/index.md


### Report Tasks

report:
	mkdir -p $@

# Convert each SPARQL file to a Markdown file.
report/%.md: ../robot-core/src/main/resources/queries/%.rq | report
	awk '/^PREFIX/ && !x {print "```sparql"; x=1} 1' $< \
	| sed 's/^#//' \
	| sed 's/^ //' \
	> $@
	echo '```' >> $@

# Use the master profile to generate a page of links.
report/index.md: ../robot-core/src/main/resources/report_profile.txt | report
	awk -F'	' '{print "- " $$1 " [`" $$2 "`](" $$2 ".html)"}' $< \
	> $@
