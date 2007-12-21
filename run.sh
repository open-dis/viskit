#!/bin/bash
# todo: Generate from build.xml.
SCRIPTDIR=`dirname $0`
cd $SCRIPTDIR
java -verbose -classpath ./lib/tools.jar:./build/classes:./lib/simkit.jar:./lib/bsh-2.0b4.jar:./lib/jgraph.jar:./lib/actions.jar:./lib/jhall.jar:./lib/jdom.jar:./lib/jfreechart-1.0.1.jar:./lib/jcommon-1.0.0.jar:./lib/junit.jar:./lib/commons-beanutils-1.7.0.jar:./lib/commons-codec-1.3.jar:./lib/commons-collections-3.1.jar:./lib/commons-configuration-1.1.jar:./lib/commons-digester-1.6.jar:./lib/commons-lang-2.0.jar:./lib/commons-logging-1.0.4.jar:./lib/log4j-1.2.14.jar:./lib/looks-1.3.1.jar:./lib/xerces.jar:./lib/xmlrpc-2.0.jar:./lib/jwsdp-1.5/jaxb-impl.jar:./lib/jwsdp-1.5/jaxb-libs.jar:./lib/jwsdp-1.5/jaxb-xjc.jar:./lib/jwsdp-1.5/xercesImpl.jar:./lib/jwsdp-1.5/xsdlib.jar:./lib/jwsdp-1.5/jaxb-api.jar:./lib/jwsdp-1.5/jax-qname.jar:./lib/jwsdp-1.5/relaxngDatatype.jar:./lib/jwsdp-1.5/namespace.jar:./lib/ext/dis.jar:./lib/ext/diskit.jar viskit.EventGraphAssemblyComboMain

