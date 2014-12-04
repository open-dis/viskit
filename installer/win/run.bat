@rem Attaching a debugger setup explained here: http://wiki.netbeans.org/FaqDebuggingRemote

java -Xdebug -Xrunjdwp:transport=dt_socket,server=y -Xms256m -Dswing.aatext=true -Djava.endorsed.dirs=lib/jaxb/endorsed -jar lib/viskit-exe.jar