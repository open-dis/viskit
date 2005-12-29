cp ../build/lib/viskit.jar .
jar xvf viskit.jar viskit/xsd/cli/Boot.class
jar xvf viskit.jar viskit/xsd/cli/Launcher.class

# following is for cli mode only
#jar cmf manif.txt boot.jar ArrivalProcess.xml ServerAssembly3.xml SimpleServer.xml config.properties jaxb-api.jar jaxb-impl.jar jaxb-libs.jar simkit.jar viskit/ viskit.jar xmlrpc-2.0.jar bsh-2.0b4.jar 



# following includes gui specific 
jar cvmf manif.txt boot.jar ArrivalProcess.xml ServerAssembly3.xml SimpleServer.xml config.properties jaxb-api.jar jaxb-impl.jar jaxb-libs.jar simkit.jar viskit/ xmlrpc-2.0.jar bsh-2.0b4.jar looks-1.3.1.jar commons-configuration-1.1.jar commons-beanutils-1.7.0.jar commons-codec-1.3.jar commons-collections-3.1.jar commons-digester-1.6.jar commons-lang-2.0.jar commons-logging-1.0.4.jar log4j-1.2.8.jar jgraph.jar actions.jar vconfig.xml c_app.xml c_gui.xml c_history_template.xml jhall.jar jdom.jar tools.jar diskit.jar dis.jar viskit.jar
