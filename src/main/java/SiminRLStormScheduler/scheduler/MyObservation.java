package SiminRLStormScheduler.scheduler;


import org.apache.storm.scheduler.ExecutorDetails;
import org.deeplearning4j.rl4j.space.Encodable;
import org.deeplearning4j.rl4j.space.ObservationSpace;
import org.nd4j.linalg.api.ndarray.INDArray;

class MyObservation implements Encodable {
    private double[][][] taskGraph;
    private double[][] networkMatrix;
    private double[][] resources;
    //private ExecutorDetails currentTask;

    public MyObservation(double[][][] taskGraph, double[][] networkMatrix, double[][] resources) {
        this.taskGraph = taskGraph;
        this.networkMatrix = networkMatrix;
        this.resources = resources;
        //this.currentTask = currentTask;
    }
    public MyObservation(){}

    // Getters and setters

    public double[][][] getTaskGraph() {
        return taskGraph;
    }

    public void setTaskGraph(double[][][] taskGraph) {
        this.taskGraph = taskGraph;
    }

    public double[][] getNetworkMatrix() {
        return networkMatrix;
    }

    public void setNetworkMatrix(double[][] networkMatrix) {
        this.networkMatrix = networkMatrix;
    }

    public double[][] getResources() {
        return resources;
    }

    public void setResources(double[][] resources) {
        this.resources = resources;
    }

    /**
     * @deprecated
     */
    @Override
    public double[] toArray() {
        return new double[0];
    }

    @Override
    public boolean isSkipped() {
        return false;
    }

    @Override
    public INDArray getData() {
        return null;
    }

    @Override
    public Encodable dup() {
        return null;
    }
}



class MyObservationSpace implements ObservationSpace<MyObservation> {
    @Override
    public String getName() {
        return "TaskAllocationObservation";
    }

    @Override
    public int[] getShape() {
        return new int[] { 1 };
    }

    @Override
    public INDArray getLow() {
        return null; // Define based on your observation encoding
    }

    @Override
    public INDArray getHigh() {
        return null; // Define based on your observation encoding
    }
}
