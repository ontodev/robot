# Validate Profile

OWL 2 has a number of <a href="https://www.w3.org/TR/owl2-profiles/" target="_blank">profiles</a> that strike different balances between expressive power and reasoning efficiency. ROBOT can validate an input ontology against a profile (EL, DL, RL, QL, and Full) and generate a report. For example:

    robot validate-profile --profile EL \
      --input merged.owl \
      --output results/merged-validation.txt

## Profiles

* <a href="https://www.w3.org/2007/OWL/wiki/Primer#OWL_2_EL" target="_blank">EL</a>
* <a href="https://www.w3.org/2007/OWL/wiki/Primer#OWL_2_RL" target="_blank">RL</a>
* <a href="https://www.w3.org/2007/OWL/wiki/Primer#OWL_2_QL">QL</a>
* <a href="https://www.w3.org/2007/OWL/wiki/Primer#OWL_2_DL_and_OWL_2_Full" target="_blank">DL</a>
* <a href="https://www.w3.org/2007/OWL/wiki/Primer#OWL_2_DL_and_OWL_2_Full" target="_blank">Full</a>

---

## Error Messages

### Missing Profile Error

Occurs when a `--profile` option is not provided.

### Invalid Profile Error

Occurs when the argument to `--profile` is not one of the following: EL, DL, RL, QL, or Full. See the above documentation for more details.

### Profile Violation Error

Occurs when the `--input` ontology does not conform to the `--profile`. See the profile descriptions for more details.
