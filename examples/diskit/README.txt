To build DISkit, first obtain dis.jar from the disjava project on sourceforge 
and simkit.jar and place them in the diskit/lib directory. If you got this from
Viskit's cvs, you should already have simkit.jar elsewhere and dis.jar has been
provided. Using ant with build.xml, target dist deposits a diskit.jar to the 
diskit/dist/lib directory, move this to your Viskit/lib installation when 
complete. 

To run the examples, bring up Viskit->Assembly, click + on the SimEntities
panel, filedialog select the Viskit/examples/diskit directory. It should
parse all the xml it finds, including build.xml but don't worry. Once complete
there should be SimPinger, SimSender3D, Destinator3D, SimMover3DLite now
loaded. If you click + and add individually, there is some order involved.

Now load DISMultiMover3DAssembly. Inspect the SimSender3D node to verify
multicast IP address and port are to taste. This simulation will randomly
move 4 SimMover3DLites about a volume of probability, not in a uniform 
search. Select the Run Assembly menu item, but do not start the simulation
just yet.

From File->Open, load the DISServer3DTestAssembly, it will replace the
node diagram. Inspect the SimServer3DTest entity by double clicking and
verify multicast IP address and port are to taste. This simulation doesn't
really run in the sim-sense; it is the "server" side of the network for the mover 
clients to report to. It can be run on the same machine without much problem
for this test, OS permitting but java should handle multicasting. Once loaded,
Run it, another run window will appear on a different JVM. At this point
the "clock" has been set to 0. Clock time to sim-time may be adjusted as per
IP and port however it is currently set at 1.0 to 1000 milliseconds on each
end, the idea here to see if there is any clock skew between client and
server sides. Clicking the rewind button will reset the clock to 0, which
is useful just after launching the client side ( hitting Play arrow in the
DISMultiMover3DAssembly Run panel ) to synchronize the two clocks. The socket
is now listened to and will respond to Pdu's. 

Start the movers by clicking Play in the first Run window. Note, as of this
writing, each Run must have at least 1 verbose SimEntity, which creates at
least one Output tag; that is the workaround. The examples should already have
at least one Output tag.

You should see numbers and vectors indicating positions of the movers in 3-space
and the corresponding numbers and vectors appearing on the server side. Multiple
mover sources can also be run, the server side should not need to be restarted
once launched.