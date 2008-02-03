#!/bin/bash
# todo: Generate from build.xml.
SCRIPTDIR=`dirname $0`
cd $SCRIPTDIR
ant build-run-default-extended
