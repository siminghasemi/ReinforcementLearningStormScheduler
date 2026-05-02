package SiminRLStormScheduler.DSPModels;

public class DSPRequirementModel {
    public String name;
    public int numberOfOperator;
    public double latencyThreshold;// constraint, latency threshold in second
    //public String[] types;          //for all operators of application b, source/node/sink
    //must the operator 0 be first source and last operator be the last sink

    public double[] CPURequirements;//for all operators of application b, the CPU requirement to be executed in MIPS
    public double[] MemRequirements;//for all operators of application b, the memory requirement in Bytes that are needed to be executed in Bytes
    //public double[] selectivities;  //for all operators of application b, the ratio of number of input events to output events (i.e., selectivity)
    public double[] miuTimes;       //for all operators of application b, μ^b (j,ref) is the execution time of operator j on reference node
    public double[][] lambdas;      //for all dataflows between operator i and j, λ^b (i,j) is the arrival data stream rate in tuples/second
    public double[][] eventSizes;   //for all dataflows between operator i and j, evtSize^b (i,j) is the average size of data/events in Bytes, i.e., the size of operator i output data and consequently the size of operator j input data.
}
