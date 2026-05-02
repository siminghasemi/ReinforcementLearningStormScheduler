package SiminRLStormScheduler.scheduler;

public class Experience {
    public MyObservation state;
    public int[] action;
    public double reward;
    public MyObservation nextState;
    public boolean done;

    public Experience(MyObservation state, int[] action, double reward, MyObservation nextState, boolean done) {
        this.state = state;
        this.action = action;
        this.reward = reward;
        this.nextState = nextState;
        this.done = done;
    }
}
