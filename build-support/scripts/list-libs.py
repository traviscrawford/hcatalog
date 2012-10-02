#!/usr/bin/env python

import os
import sys

from optparse import OptionParser

"""Generate a sorted list of libs to compare expected with actual libs.

Our explicit and transitive dependencies change on a regular basis, often
causing unexpected libraries to sneak into our build. To ensure no surprises
we fail the build if unexpected libraries are found.

This script is needed because sort on developer laptops and production
machines sort differently.
"""

parser = OptionParser()
parser.add_option('--input_dir',
  dest='input_dir',
  help='Directory to walk for file names.')
parser.add_option('--output_file',
  dest='output_file',
  help='File to write the sorted list of files found under input_dir.')
(options, args) = parser.parse_args()

if options.input_dir == None or options.output_file == None:
  parser.print_help()
  sys.exit(1)

libs = []
for root, dirs, files in os.walk(options.input_dir):
  [libs.append('%s/%s' % (root, f)) for f in files if f.endswith('.jar')]

out = open(options.output_file, 'w')
for lib in sorted(libs):
  out.write('%s\n' % lib)
out.close()

