package examples;

// Standard library imports
import java.util.*;

// Application specific imports
import org.apache.log4j.Logger;
import simkit.*;
import simkit.random.*;

public class ServerWithReneges extends SimEntityBase {

    static Logger log = Logger.getLogger(ServerWithReneges.class);

    /* Simulation Parameters */
    private int numberServers;
    private simkit.random.RandomVariate serviceTime;
    private simkit.random.RandomVariate renegeTime;

    /* Simulation State Variables */
    protected int numberAvailableServers;
    protected java.util.LinkedList<Integer> queue = new java.util.LinkedList<Integer>();
    protected int numberServed;
    protected int numberReneges;

    @viskit.ParameterMap (
        names = {"numberServers",
            "serviceTime",
            "renegeTime"},
        types = {"int",
            "simkit.random.RandomVariate",
            "simkit.random.RandomVariate"}
    )

    /** Creates a new instance of ServerWithReneges */
    public ServerWithReneges(int numberServers,
            simkit.random.RandomVariate serviceTime,
            simkit.random.RandomVariate renegeTime) {
        setNumberServers(numberServers);
        setServiceTime(serviceTime);
        setRenegeTime(renegeTime);
    }

    /** Set initial values of all state variables */
    @Override
    public void reset() {
        super.reset();

        /* StateTransitions for the Run Event */
        numberAvailableServers = getNumberServers();
        queue.clear();
        numberServed = 0;
        numberReneges = 0;
    }

    public void doRun() {
        firePropertyChange("numberAvailableServers", getNumberAvailableServers());
        firePropertyChange("queue", getQueue());
        firePropertyChange("numberServed", getNumberServed());
        firePropertyChange("numberReneges", getNumberReneges());
        
    }

    public void doArrival(java.lang.Integer customer) {
        /* StateTransition for queue */
        java.util.LinkedList<Integer> _old_Queue = getQueue();
        queue.add(customer);
        firePropertyChange("queue", _old_Queue, getQueue());

        waitDelay("Renege",renegeTime.generate(),Priority.DEFAULT,(Object)(customer));
        if (getNumberAvailableServers() > 0) {
            waitDelay("StartService",0.0,Priority.DEFAULT);
        }
    }

    public void doStartService() {
        java.lang.Integer customer = (java.lang.Integer) queue.getFirst();

        /* StateTransition for queue */
        java.util.LinkedList<Integer> _old_Queue = getQueue();
        queue.removeFirst();
        firePropertyChange("queue", _old_Queue, getQueue());

        /* StateTransition for numberAvailableServers */
        int _old_NumberAvailableServers = getNumberAvailableServers();
        numberAvailableServers = numberAvailableServers - 1;
        firePropertyChange("numberAvailableServers", _old_NumberAvailableServers, getNumberAvailableServers());

        interrupt("Renege",new Object[]{customer});
        waitDelay("EndService",serviceTime.generate(),Priority.DEFAULT,(Object)(customer));
    }

    public void doEndService(java.lang.Integer customer1) {
        /* StateTransition for numberAvailableServers */
        int _old_NumberAvailableServers = getNumberAvailableServers();
        numberAvailableServers = numberAvailableServers+1;
        firePropertyChange("numberAvailableServers", _old_NumberAvailableServers, getNumberAvailableServers());

        /* StateTransition for numberServed */
        int _old_NumberServed = getNumberServed();
        numberServed = numberServed+1;
        firePropertyChange("numberServed", _old_NumberServed, getNumberServed());

        if (queue.size() > 0) {
            waitDelay("StartService",0.0,Priority.HIGH);
        }
    }

    public void doRenege(java.lang.Integer customer2) {
        /* StateTransition for queue */
        java.util.LinkedList<Integer> _old_Queue = getQueue();
        queue.remove(customer2);
        firePropertyChange("queue", _old_Queue, getQueue());

        /* StateTransition for numberReneges */
        int _old_NumberReneges = getNumberReneges();
        numberReneges = numberReneges+1;
        firePropertyChange("numberReneges", _old_NumberReneges, getNumberReneges());

    }

    public void setNumberServers(int numberServers) {
        this.numberServers = numberServers;
    }

    public int getNumberServers() {
        return numberServers;
    }

    public void setServiceTime(simkit.random.RandomVariate serviceTime) {
        this.serviceTime = serviceTime;
    }

    public simkit.random.RandomVariate getServiceTime() {
        return serviceTime;
    }

    public void setRenegeTime(simkit.random.RandomVariate renegeTime) {
        this.renegeTime = renegeTime;
    }

    public simkit.random.RandomVariate getRenegeTime() {
        return renegeTime;
    }

    public int getNumberAvailableServers() {
        return  numberAvailableServers;
    }

    @SuppressWarnings("unchecked")
    public java.util.LinkedList<Integer> getQueue() {
        return (java.util.LinkedList<Integer>) queue.clone();
    }

    public int getNumberServed() {
        return  numberServed;
    }

    public int getNumberReneges() {
        return  numberReneges;
    }

}

