# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]


## [1.6.0] - 2020-03-03

This release adds the [`collapse`](http://robot.obolibrary.org/collapse) command.

## [1.5.0] - 2019-11-28

This release includes the new [`python`](http://robot.obolibrary.org/python) command, allowing ROBOT to be controlled from Python using [Py4j](https://www.py4j.org/).

Other new features include:

- `internal`/`external` selectors for `remove`/`filter` (#570)
- language selectors for `remove`/`filter` (#574)
- `tautologies` and `structural-tautologies` selectors for `remove`/`filter`(#579)
- catalog options for right-side of `diff` (#584)

And there have been some minor bug fixes.

## [1.4.3] - 2019-09-12

This release addresses excessive logging (see #567 and #374).

## [1.4.2] - 2019-09-11

This release includes a few new features and several bug fixes:

- update `repair` to migrate annotations (#510)
- use Jena TDB for `report` (#558)
- add `--exclude-tautologies` option (#560)
- follow redirects for gzipped input IRIs (#537)
- fix bug with imported axioms (#523)
- fix index out of bounds for `report --print` (#546)
- fix stack overflow in `remove`/`filter` (#547)
- fix query when chaining with `--input-iri` (#555)
- fix template bug with equivalent classes (#559)
- fix template bug with nested annotation (#564)

## [1.4.1] - 2019-06-27

This release adds several options and fixes some bugs:

- major update to `template` command (#403)
- add `--tdb true` option to `query` for Jena TDB on-disk storage (#475)
- add IRI pattern matching to `remove`/`filter` (#448)
- add `--signature` option to `remove`/`filter`, improve docs (#484)
- add more output options to `diff` (#461)
- specify prefixes for output (#488)
- fix invalid reference errors for OWL built-ins (#455)
- fix import handling for SPARQL UPDATE (#471)
- fix `remove`/`filter` for terms not in ontology (#507)

## [1.4.0] - 2019-03-14

This release adds the `rename` command (see #419), allowing you to replace lists of old IRIs with new IRIs. It also adds SPARQL Update support (#352), for adding and removing triples from ontologies using SPARQL. This feature builds on Apache Jena, but note the warning in the 'Update' section of the documentation at http://robot.obolibrary.org/query.

Other improvements include:

- new `--annotate-with-source` option for `extract` (#392)
- new `--intermediates` option for `extract` (#441)
- new `--individuals` option for `extract` (#385)
- improvements to the built-in reports (#438)
- new selectors for `remove` and `filter`: domain and range (#427), ontology (#452)

## [1.3.0] - 2019-01-18

This release of ROBOT introduces the `explain` command, contributed by Jim Balhoff, which finds explanations for axioms inferred by a reasoner. See http://robot.obolibrary.org/explain

## [1.2.0] - 2018-12-06

This release of ROBOT introduces the `remove` and `filter` commands, which allow you to remove selected axioms from an ontology. See <http://robot.obolibrary.org/remove>.

**Breaking Change**: We have upgraded from Apache Jena 2.13.0 to 3.8.0 (#314), which involves the renaming of several packages and changes to the return types of `QueryOperation`. One other change we've noted is that the new Jena adds fewer `xsd:string` datatypes than the previous version.

Other changes:

- `--use-graphs true` option for `query` allows queries over imports as named graphs, #158
- upgrade to OWLAPI 4.5.6 sometimes changes the ordering of elements in RDFXML format, causing spurious differences in line order when comparing output from previous versions of ROBOT. But note that Protege5.5 uses the same version of the OWLAPI so orderings should be consistent between the two.
- add `--labels` option to `diff` command, #363
- add support for gzipped files, #371

## [1.1.0] - 2018-08-04

This release of ROBOT includes the new `report` command and a number of other improvements.

### Added

- `--collapse-import-closure` option for `merge`: When `true` (the default) all imports will be merged and all `owl:import` statements will be removed. **Possible breaking change**: In previous versions of ROBOT, `owl:import` statements were not removed. #275
- global `--catalog FILE` option #274
- `--check` option for `convert` allows conversion of more OBO-format files
- `--include-annotations` option for `merge` allows better control of ontology annotations #277
- `--copy-ontology-annotations` option for `extract`, #319
- `--dump-unsatisfiable` option for `reason`, #174

### Fixed
- improved error messages, linking to ROBOT website #246


## [1.0.0] - 2018-02-08

First official release of ROBOT!


[Unreleased]: https://github.com/olivierlacan/keep-a-changelog/compare/v1.6.0...HEAD
[1.6.0]: https://github.com/ontodev/robot/compare/v1.5.0...v1.6.0
[1.5.0]: https://github.com/ontodev/robot/compare/v1.4.3...v1.5.0
[1.4.3]: https://github.com/ontodev/robot/compare/v1.4.2...v1.4.3
[1.4.2]: https://github.com/ontodev/robot/compare/v1.4.1...v1.4.2
[1.4.1]: https://github.com/ontodev/robot/compare/v1.4.0...v1.4.1
[1.4.0]: https://github.com/ontodev/robot/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/ontodev/robot/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/ontodev/robot/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/ontodev/robot/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/ontodev/robo/releases/tag/v1.0.0
