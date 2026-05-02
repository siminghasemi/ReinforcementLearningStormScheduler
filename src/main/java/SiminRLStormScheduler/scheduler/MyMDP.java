package SiminRLStormScheduler.scheduler;

import SiminRLStormScheduler.DSPModels.Graph.GraphDSP;
import SiminRLStormScheduler.NetworkModels.Graph.GraphNetwork;
import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.mdp.MDP;
import org.deeplearning4j.rl4j.observation.Observation;
import org.deeplearning4j.rl4j.space.ArrayObservationSpace;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import org.deeplearning4j.rl4j.space.ObservationSpace;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.List;

public class MyMDP implements MDP<MyObservation, Integer, DiscreteSpace> {

    List<GraphDSP> DSPSet;
    GraphNetwork networkGraph;


    private DiscreteSpace actionSpace;// = new DiscreteSpace(numNodes);
    private ObservationSpace<MyObservation> observationSpace;// = new ArrayObservationSpace<>(new int[]{ observationSize});
    private MyObservation currentObservation;

    int observationSize = 100;

    public MyMDP(List<GraphDSP> dSPSet, GraphNetwork networkGraph) {
        // Initialize action and observation spaces
        this.DSPSet = dSPSet;
        this.networkGraph = networkGraph;
        actionSpace = new DiscreteSpace(networkGraph.getVertexes().size());//10
        observationSpace = new ArrayObservationSpace<>(new int[]{ observationSize});
    }

    @Override
    public MyObservation reset() {
        // Reset the environment to an initial state
        currentObservation = new MyObservation();//params to initialize obzervation
        return currentObservation;
    }


    // Apply the action, update the state, and return the result
    @Override
    public StepReply<MyObservation> step(Integer action) {
        double reward = 0;
        boolean done = false;

        // 1. Decode the action: assign the current task to the selected node
        /*int selectedNode = action;
        Task currentTask = taskQueue.get(currentTaskIndex);
        Node targetNode = nodes.get(selectedNode);

        // 2. Check resource availability
        boolean resourcesAvailable = targetNode.canAllocate(currentTask);



        if (resourcesAvailable) {
            // 3. Allocate resources and update the node's state
            targetNode.allocate(currentTask);

            // 4. Compute response time and network usage
            double responseTime = computeResponseTime(currentTask, targetNode);
            double networkUsage = computeNetworkUsage(currentTask, targetNode);

            // 5. Calculate reward (negative response time and network usage)
            reward = - (alpha * responseTime + beta * networkUsage);

            // 6. Move to the next task
            currentTaskIndex++;
            if (currentTaskIndex >= taskQueue.size()) {
                done = true;
            }
        } else {
            // Penalize for invalid action (resource constraint violation)
            reward = -penalty;
        }

        */
        // 7. Update the observation
        MyObservation nextObservation = buildObservation();

        return new StepReply<>(nextObservation, reward, done, null);
    }


    @Override
    public boolean isDone() {
        // Determine if the episode has ended
        return false;
    }

    @Override
    public MDP<MyObservation, Integer, DiscreteSpace> newInstance() {
        return null;
    }
    //    @Override
//    public MDP<MyObservation, Integer, DiscreteSpace> newInstance() {
//        return new MyMDP();
//    }

    @Override
    public ObservationSpace<MyObservation> getObservationSpace() {
        return observationSpace;
    }

    @Override
    public DiscreteSpace getActionSpace() {
        return actionSpace;
    }




    @Override
    public void close() {
    }

    private MyObservation buildObservation() {
        List<Double> observationList = new ArrayList<>();

        // 1. Encode Task Features
        /*for (Task task : tasks) {
            observationList.add(normalize(task.getCpuRequirement(), maxCpu));
            observationList.add(normalize(task.getMemoryRequirement(), maxMemory));
            observationList.add(normalize(task.getPriority(), maxPriority));

            // Encode dependencies as binary indicators
            for (Task dependency : tasks) {
                observationList.add(task.getDependencies().contains(dependency) ? 1.0 : 0.0);
            }
        }

        // 2. Encode Node Features
        for (Node node : nodes) {
            observationList.add(normalize(node.getAvailableCpu(), maxCpu));
            observationList.add(normalize(node.getAvailableMemory(), maxMemory));
            observationList.add(normalize(node.getCurrentLoad(), maxLoad));
        }

        // 3. Encode Network Features
        for (Node source : nodes) {
            for (Node target : nodes) {
                if (!source.equals(target)) {
                    observationList.add(normalize(network.getLatency(source, target), maxLatency));
                    observationList.add(normalize(network.getBandwidth(source, target), maxBandwidth));
                }
            }
        }

        // Convert the list to an INDArray
        double[] observationArray = observationList.stream().mapToDouble(Double::doubleValue).toArray();
        INDArray observationINDArray = Nd4j.create(observationArray);

        return new Observation(observationINDArray);*/
        return null;
    }

    // Helper method for normalization
    private double normalize(double value, double maxValue) {
        return maxValue == 0 ? 0 : value / maxValue;
    }

}
