#!/bin/bash
# on cygwin using Windows Java, you need to dos-shellify the classpath 
#


export BASEDIR=${HOME}/CVS/Viskit
export LIBDIR=${BASEDIR}/lib
export LIBJWSDPDIR=${LIBDIR}/jwsdp-1.4
export LIBEXTDIR=${LIBDIR}/ext
export LIBBUILDDIR=${BASEDIR}/build/lib
export CLASSPATH=


for i in $LIBJWSDPDIR/*.jar $LIBDIR/*.jar $LIBEXTDIR/*.jar $LIBBUILDDIR/*.jar
do
	CLASSPATH="c+\\cygwin\\"$i:$CLASSPATH
done

echo $CLASSPATH | sed 's/\//\\/g' | sed 's/:/;/g' | sed 's/+/:/g' > classpath
export CLASSPATH=`cat ./classpath`

java -cp $CLASSPATH viskit.Splash2

