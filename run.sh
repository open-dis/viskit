#!/bin/bash
# todo: Generate from build.xml.
# $Id:$
SCRIPTDIR=`dirname $0`
cd $SCRIPTDIR
ant build.run.no.runtime.extensions
