#!/bin/bash
# Build a classpath by looking for jars in the right places. Doesn't pack the  
# most minimal set needed for runtime, but is quick.


export BASEDIR=${HOME}/CVS/Viskit
export LIBDIR=${BASEDIR}/lib
export LIBJWSDPDIR=${LIBDIR}/jwsdp-1.4
export LIBEXTDIR=${LIBDIR}/ext
export LIBBUILDDIR=${BASEDIR}/build/lib
export CLASSPATH=


for i in $LIBJWSDPDIR/*.jar $LIBDIR/*.jar $LIBEXTDIR/*.jar $LIBBUILDDIR/*.jar
do
	CLASSPATH=$i:$CLASSPATH
done


java -cp $CLASSPATH viskit.Splash2

