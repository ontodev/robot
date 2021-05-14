#!/usr/bin/env python3

import os
import subprocess
import sys

from collections import defaultdict

res = subprocess.check_output(['grep "    --output" docs/*.md'], shell=True)
test_files = defaultdict(set)
for line in res.decode("utf-8").split("\n"):
	if not line.split():
		continue
	fname = line.split()[0][5:-1]
	output = line.split()[2][8:]
	if output:
		if output not in test_files:
			test_files[output] = set()
		test_files[output].add(fname)

duplicates = {k: v for k, v in test_files.items() if len(v) > 1}
if duplicates:
	print(f"ERROR: {len(duplicates)} test output(s) are used in more than one doc")
	for output, fnames in duplicates.items():
		print(f"- {output}: " + ", ".join(fnames))
	sys.exit(1)