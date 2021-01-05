# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.7.2] - 2020-11-18

### Changed
- Update to whelk 1.0.4

### Fixed
- Fix blank node subjects in [`report`] in [#767]
- Fixed IRI handling for [`template`] in [#783]

## [1.7.1] - 2020-10-22

### Added
- Add Whelk OWL reasoner in [#730]
- Add [`validate`] command [#691]
- Add `--errors <path>` option to [`template`] in [#713]
- Add new formats to [`report`]: HTML, JSON, and XLSX [#699]
- Add `--fail-on-violation <bool>` option to [`verify`] in [#738]

### Fixed
- Handle empty [`template`] property charactersitics in [#719]
- Fix referencing properties by CURIE in [`export`] in [#722]
- Fix [`validate`] `--write-all true` in [#726]
- Fix reported row number in [`validate`] error tables in [#727]
- Fix equivalent class rendering for [`template`] in [#728]
- Fix ontology IRI rendering for [`report`] in [#739]

## [1.7.0] - 2020-07-31

### Added
- Add [`export`] command [#481]
- Add JSON format to [`export`] in [#645]
- Add Excel format to [`export`] in [#646]
- Add `--create-tdb <true/false>` option to [`query`] in [#685]

### Changed
- Updated `obographs` from 0.0.8 to 0.2.1 [#657]

### Fixed
- Fix filtering axioms with multiple axiom selectors [#644]
- Fix comparator method for sorting empty strings with [`export`] in [#654]
- Fix releasing dataset after exception when running [`report`] with `--tdb true` [#659]
- Reduced time spent loading datasets for [`query`] in [#666]
- Fix writing JSON format to use `OutputStream` with ['convert'] in [#671]
- Fix IRI resolution for `template` in [#689]
- Fix MIREOT [`extract`] on overlapping class/individual entity for [#709] in [#710]
- Fix issue with `--add-prefixes` option in [#715]

## [1.6.0] - 2020-03-04

### Added
- Add [`collapse`] command [#578]
- Add support for anonymous class assertions in [`template`] in [#630]
- Add maven plugin to update OBO context [#608]
- Add more efficient mode for [`reduce`] when only considering named classes [#619]

### Changed
- Switch whitespace queries to regex [#606]
- Update nucleus import for tests [#597]

### Fixed
- Fix warning when term is in imports (extract command) [#625]
- Fix index out of bounds error in extract [#617]
- Use URIs to fix resource path issues [#603]

## [1.5.0] - 2019-11-28

### Added

- Add new [`python`] command, allowing ROBOT to be controlled from Python using [Py4j](https://www.py4j.org/).
- Add `internal`/`external` selectors for [`remove`]/[`filter`] in [#570]
- Add language selectors for [`remove`]/[`filter`] in [#574]
- Add `tautologies` and `structural-tautologies` selectors for [`remove`]/[`filter`] in [#579]
- Add catalog options for right-side of [`diff`] in [#584]

## [1.4.3] - 2019-09-12

### Fixed

- Fix excessive logging, [#567] and [#374]

## [1.4.2] - 2019-09-11

### Added

- Update [`repair`] to migrate annotations [#510]
- Allow use of Jena TDB for [`report`] in [#558]
- Add `--exclude-tautologies` option for [`reason`] in [#560]

### Changed

- Follow redirects for gzipped input IRIs [#537]

### Fixed

- Fix bug with imported axioms [#523]
- Fix index out of bounds for [`report`] `--print` in [#546]
- Fix stack overflow in [`remove`]/[`filter`] in [#547]
- Fix [`query`] when chaining with `--input-iri` [#555]
- Fix [`template`] bug with equivalent classes [#559]
- Fix [`template`] bug with nested annotation [#564]

## [1.4.1] - 2019-06-27

### Added

- Add `--tdb true` option to [`query`] for Jena TDB on-disk storage [#475]
- Add IRI pattern matching to [`remove`]/[`filter`] in [#448]
- Add `--signature` option to [`remove`]/[`filter`], improve docs [#484]
- Add more output options to [`diff`] in [#461]
- Allow specified prefixes for output [#488]

### Changed

- Made major update to [`template`] command [#403]

### Fixed

- Fix invalid reference errors for OWL built-ins [#455]
- Fix import handling for SPARQL UPDATE [#471]
- Fix [`remove`]/[`filter`] for terms not in ontology [#507]

## [1.4.0] - 2019-03-14

### Added

- Add [`rename`] command [#419], allowing you to replace lists of old IRIs with new IRIs
- Add SPARQL Update support [#352], but note the warning in the 'Update' section of the documentation at <http://robot.obolibrary.org/query>
- Add `--annotate-with-source` option for [`extract`] in [#392]
- Add `--intermediates` option for [`extract`] [#441]
- Add `--individuals` option for [`extract`] [#385]
- Add new selectors for [`remove`] and [`filter`]: `domain` and `range` [#427], `ontology` [#452]

### Changed

- Made improvements to the built-in reports [#438]

## [1.3.0] - 2019-01-18

### Added

- Add [`explain`] command, contributed by [Jim Balhoff](https://github/balhoff)

## [1.2.0] - 2018-12-06

### Added

- Add [`remove`] command for removing axioms from an ontology
- Add [`filter`] command for copying selected axioms to a new ontology
- Add `--use-graphs true` option for [`query`] allows queries over imports as named graphs [#158]
- Add `--labels` option to [`diff`] command [#363]
- Add support for gzipped files [#371]

### Changed

- **Breaking Change**: We have upgraded from Apache Jena 2.13.0 to 3.8.0 [#314], which involves the renaming of several packages and changes to the return types of `QueryOperation`. One other change we've noted is that the new Jena adds fewer `xsd:string` datatypes than the previous version.
- Upgrade to OWLAPI 4.5.6 sometimes changes the ordering of elements in RDFXML format, causing spurious differences in line order when comparing output from previous versions of ROBOT. But note that Protege5.5 uses the same version of the OWLAPI so orderings should be consistent between the two.

## [1.1.0] - 2018-08-04

### Added

- Add [`report`] command
- Add `--collapse-import-closure` option for [`merge`]: When `true` (the default) all imports will be merged and all `owl:import` statements will be removed. **Possible breaking change**: In previous versions of ROBOT, `owl:import` statements were not removed. [#275]
- Add global `--catalog FILE` option [#274]
- Add `--check` option for [`convert`] allows conversion of more OBO-format files
- Add `--include-annotations` option for [`merge`] allows better control of ontology annotations [#277]
- Add `--copy-ontology-annotations` option for [`extract`] in [#319]
- Add `--dump-unsatisfiable` option for [`reason`] [#174]

### Fixed

- improved error messages, linking to ROBOT website [#246]

## [1.0.0] - 2018-02-08

First official release of ROBOT!

[Unreleased]: https://github.com/ontodev/robot/compare/v1.7.2...HEAD
[1.7.2]: https://github.com/ontodev/robot/compare/v1.7.1...v1.7.2
[1.7.1]: https://github.com/ontodev/robot/compare/v1.7.0...v1.7.1
[1.7.0]: https://github.com/ontodev/robot/compare/v1.6.0...v1.7.0
[1.6.0]: https://github.com/ontodev/robot/compare/v1.5.0...v1.6.0
[1.5.0]: https://github.com/ontodev/robot/compare/v1.4.3...v1.5.0
[1.4.3]: https://github.com/ontodev/robot/compare/v1.4.2...v1.4.3
[1.4.2]: https://github.com/ontodev/robot/compare/v1.4.1...v1.4.2
[1.4.1]: https://github.com/ontodev/robot/compare/v1.4.0...v1.4.1
[1.4.0]: https://github.com/ontodev/robot/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/ontodev/robot/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/ontodev/robot/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/ontodev/robot/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/ontodev/robot/releases/tag/v1.0.0

[Jim Balhoff]: https://github/balhoff

[`collapse`]: http://robot.obolibrary.org/collapse
[`convert`]: http://robot.obolibrary.org/convert
[`diff`]: http://robot.obolibrary.org/diff
[`explain`]: http://robot.obolibrary.org/explain
[`export`]: http://robot.obolibrary.org/export
[`extract`]: http://robot.obolibrary.org/extract
[`filter`]: http://robot.obolibrary.org/filter
[`merge`]: http://robot.obolibrary.org/merge
[`python`]: http://robot.obolibrary.org/python
[`query`]: http://robot.obolibrary.org/query
[`reason`]: http://robot.obolibrary.org/reason
[`reduce`]: http://robot.obolibrary.org/reduce
[`remove`]: http://robot.obolibrary.org/remove
[`rename`]: http://robot.obolibrary.org/rename
[`repair`]: http://robot.obolibrary.org/repair
[`report`]: http://robot.obolibrary.org/report
[`template`]: http://robot.obolibrary.org/template
[`validate`]: http://robot.obolibrary.org/validate

[#783]: https://github.com/ontodev/robot/pull/783
[#767]: https://github.com/ontodev/robot/pull/767
[#758]: https://github.com/ontodev/robot/pull/758
[#739]: https://github.com/ontodev/robot/pull/739
[#738]: https://github.com/ontodev/robot/pull/738
[#728]: https://github.com/ontodev/robot/pull/728
[#727]: https://github.com/ontodev/robot/pull/727
[#726]: https://github.com/ontodev/robot/pull/726
[#722]: https://github.com/ontodev/robot/pull/722
[#719]: https://github.com/ontodev/robot/pull/716
[#715]: https://github.com/ontodev/robot/pull/715
[#713]: https://github.com/ontodev/robot/pull/713
[#710]: https://github.com/ontodev/robot/pull/710
[#709]: https://github.com/ontodev/robot/issues/709
[#699]: https://github.com/ontodev/robot/pull/699
[#691]: https://github.com/ontodev/robot/pull/691
[#689]: https://github.com/ontodev/robot/pull/689
[#685]: https://github.com/ontodev/robot/pull/685
[#671]: https://github.com/ontodev/robot/pull/671
[#666]: https://github.com/ontodev/robot/pull/666
[#659]: https://github.com/ontodev/robot/issues/659
[#657]: https://github.com/ontodev/robot/pull/657
[#654]: https://github.com/ontodev/robot/issues/654
[#646]: https://github.com/ontodev/robot/issues/646
[#645]: https://github.com/ontodev/robot/issues/645
[#644]: https://github.com/ontodev/robot/issues/644
[#630]: https://github.com/ontodev/robot/issues/630
[#625]: https://github.com/ontodev/robot/issues/625
[#619]: https://github.com/ontodev/robot/issues/619
[#617]: https://github.com/ontodev/robot/issues/617
[#608]: https://github.com/ontodev/robot/issues/608
[#606]: https://github.com/ontodev/robot/issues/606
[#603]: https://github.com/ontodev/robot/issues/603
[#597]: https://github.com/ontodev/robot/issues/597
[#584]: https://github.com/ontodev/robot/issues/584
[#579]: https://github.com/ontodev/robot/issues/579
[#578]: https://github.com/ontodev/robot/issues/579
[#574]: https://github.com/ontodev/robot/issues/574
[#570]: https://github.com/ontodev/robot/issues/570
[#567]: https://github.com/ontodev/robot/issues/567
[#564]: https://github.com/ontodev/robot/issues/564
[#560]: https://github.com/ontodev/robot/issues/560
[#559]: https://github.com/ontodev/robot/issues/559
[#558]: https://github.com/ontodev/robot/issues/558
[#555]: https://github.com/ontodev/robot/issues/555
[#547]: https://github.com/ontodev/robot/issues/547
[#546]: https://github.com/ontodev/robot/issues/546
[#537]: https://github.com/ontodev/robot/issues/537
[#523]: https://github.com/ontodev/robot/issues/523
[#510]: https://github.com/ontodev/robot/issues/510
[#507]: https://github.com/ontodev/robot/issues/507
[#488]: https://github.com/ontodev/robot/issues/488
[#484]: https://github.com/ontodev/robot/issues/484
[#481]: https://github.com/ontodev/robot/issues/481
[#475]: https://github.com/ontodev/robot/issues/475
[#471]: https://github.com/ontodev/robot/issues/471
[#461]: https://github.com/ontodev/robot/issues/461
[#455]: https://github.com/ontodev/robot/issues/455
[#452]: https://github.com/ontodev/robot/issues/452
[#448]: https://github.com/ontodev/robot/issues/448
[#441]: https://github.com/ontodev/robot/issues/441
[#438]: https://github.com/ontodev/robot/issues/438
[#427]: https://github.com/ontodev/robot/issues/427
[#419]: https://github.com/ontodev/robot/issues/419
[#403]: https://github.com/ontodev/robot/issues/403
[#392]: https://github.com/ontodev/robot/issues/392
[#385]: https://github.com/ontodev/robot/issues/385
[#374]: https://github.com/ontodev/robot/issues/374
[#371]: https://github.com/ontodev/robot/issues/371
[#363]: https://github.com/ontodev/robot/issues/363
[#352]: https://github.com/ontodev/robot/issues/352
[#319]: https://github.com/ontodev/robot/issues/319
[#314]: https://github.com/ontodev/robot/issues/314
[#277]: https://github.com/ontodev/robot/issues/277
[#275]: https://github.com/ontodev/robot/issues/275
[#274]: https://github.com/ontodev/robot/issues/274
[#246]: https://github.com/ontodev/robot/issues/246
[#174]: https://github.com/ontodev/robot/issues/174
[#158]: https://github.com/ontodev/robot/issues/158
