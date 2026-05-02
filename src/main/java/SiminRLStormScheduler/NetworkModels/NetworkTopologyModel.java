package SiminRLStormScheduler.NetworkModels;

public class NetworkTopologyModel {
    //supervisor[0]: Host:ubuntu, Id:cac27e64-3a72-43b5-8b38-bbc0f55ca305-127.0.1.1,
    // AllPorts:[6700, 6701, 6702, 6703], TotalMemory:4096.0, TotalCpu:400.0
    public int NumberOfNodes;
    public String[] Hosts;
    public String[] Ids;
    public String[] AllPorts;
    public double[] CPUResources;//processing capability in Millions of Instructions per Second (MIPS)
    public double[] MemResources;//the available memory in Bytes
    public double[] Speeds;//the processing speed of node u compared to a reference processor
    public double[][] Bandwidths;// bandwidth between node (u,v) in Byte per second (Bps).
}
