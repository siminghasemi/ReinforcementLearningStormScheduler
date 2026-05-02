package SiminRLStormScheduler.scheduler;

import SiminRLStormScheduler.DSPModels.Graph.EdgeDSP;
import SiminRLStormScheduler.DSPModels.Graph.GraphDSP;
import SiminRLStormScheduler.DSPModels.Graph.VertexDSP;
import SiminRLStormScheduler.NetworkModels.Graph.EdgeNetwork;
import SiminRLStormScheduler.NetworkModels.Graph.GraphNetwork;
import SiminRLStormScheduler.NetworkModels.Graph.VertexNetwork;
import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

import java.util.*;
import java.util.stream.IntStream;

public class MyProblemObjectiveFunctions extends AbstractIntegerProblem {
    int AppCount;//= 3;
    int NodeCount;// = 4;
    int TaskMaxCount;// = 5;
    int numberOfVariables;
    List<GraphDSP> dspList;
    GraphNetwork networkGraph;
    boolean x[][][];// allocation matrix
    Double ResponseTime[];
    Double NetworkUsage[];
    double MiuHistory[][];
    double processingSpeed[];


    public MyProblemObjectiveFunctions(boolean x[][][], List<GraphDSP> DSPSet, GraphNetwork networkGraph) {
        this.x = x;

        this.dspList = DSPSet;
        this.networkGraph = networkGraph;
        if (DSPSet != null)
            this.AppCount = DSPSet.size();
        if (networkGraph != null)
            this.NodeCount = networkGraph.getVertexes().size();

        //calculate TaskMaxCount over all applications
        Integer tasksize[] = new Integer[AppCount];
        if (dspList != null) {
            for (int b = 0; b < AppCount; b++) {
                tasksize[b] = dspList.get(b).getVertexes().size();
            }
        }
        if (tasksize != null && tasksize.length > 0)
            this.TaskMaxCount = Collections.max(Arrays.asList(tasksize));//TODO: max(dbset.app.vertex.size)


        this.numberOfVariables = this.AppCount * TaskMaxCount;// chromosom

        numberOfObjectives(2);// Time and NetworkUsage
        numberOfConstraints(7);// Constraint 1-7

        List<Integer> lowerLimit = new ArrayList<>(numberOfVariables);
        List<Integer> upperLimit = new ArrayList<>(numberOfVariables);

        //set lowerBound and upperBound to 1 and NodeNumber //numberOfVariables(numberOfVariables);
        IntStream.range(0, numberOfVariables).forEach(i -> {
            lowerLimit.add(0);
            upperLimit.add(NodeCount - 1);//index of node
        });
        variableBounds(lowerLimit, upperLimit);

        //read execution time of operator i of application b on reference node into MiuHistory[app][operator] from a text file
        //read ratio of processing speed of node v (S_v) into processingSpeed[Nodes] from a text file
        MiuHistory = new double[AppCount][TaskMaxCount];
        if (dspList != null) {
            for (int b = 0; b < dspList.size(); b++) {
                for (int j = 0; j < dspList.get(b).getVertexes().size(); j++) {
                    MiuHistory[b][j] = dspList.get(b).getVertexes().get(j).getMiuTime();
                    //MiuHistory [b][j] = dspList.get(b).getDSPRequirement().miuTimes[j];
                }
            }
        }
        processingSpeed = new double[NodeCount];
        processingSpeed = networkGraph.getNetworkTopology().Speeds;
    }

    @Override
    public IntegerSolution evaluate(IntegerSolution solution) {
        //1- convert chromozom to 3D binary matrix
        convertChromozomTo3DMatrix(solution);

        //2- calculate  objective function for time and network
        calculateResponseTimeApps();
        double responseTime = 0;
        if (ResponseTime != null && ResponseTime.length > 0)
            responseTime = Collections.max(Arrays.asList(ResponseTime));//MaxResponseTime();
//        System.out.println("*** responseTime = " + responseTime );

        calculateNetworkUsageforAllApps();
        double networkUsage = 0;
        if (ResponseTime != null && ResponseTime.length > 0)
            networkUsage = Collections.max(Arrays.asList(NetworkUsage));//MaxNetworkUsage(NetworkUsage);
//        System.out.println("*** networkUsage = " + networkUsage );

        solution.objectives()[0] = responseTime;
        solution.objectives()[1] = networkUsage;

        //3- calculate constraint distance for constraint 1-7
        // First and the most important constraint is to ensure that the response time of each application does not exceed its latency threshold as follows:
        solution.constraints()[0] = DistanceConstraintlatency_threshold(x);
//        System.out.println("*** DistanceConstraintlatency_threshold = " + solution.constraints()[0] );

        //The bound of computational resource (CPU and memory) requirements of operators which are placed on nodes are guaranteed by equations 12 and 13:
        solution.constraints()[1] = DistanceConstraintCPUbound(x);
//        System.out.println("*** DistanceConstraintCPUbound = " + solution.constraints()[1] );

        solution.constraints()[2] = DistanceConstraintMemorybound(x);
//        System.out.println("*** DistanceConstraintMemorybound = " + solution.constraints()[2] );

        //In the same way, total rate of data streams assigned on every network link should not exceed its bandwidth. This constraint is guaranteed by
        solution.constraints()[3] = DistanceConstraintbandwidthbound(x);
//        System.out.println("*** DistanceConstraintbandwidthbound = " + solution.constraints()[3] );

        //To satisfy the DSP application owner’s preference in operator placements, the following constraint guarantees that total deviation of the placements does not exceed the maximum acceptable deviation for DSP application
        solution.constraints()[4] = DistanceConstraintPreference_threshold(x);
//        System.out.println("*** DistanceConstraintPreference_threshold = " + solution.constraints()[4] );

        //Equations 18 and 19 guarantee that an operator is only placed on one resource and a data stream is placed on one network path respectively:
        solution.constraints()[5] = DistanceConstraint6(x);
//        System.out.println("*** DistanceConstraint6 = " + solution.constraints()[5] );
        solution.constraints()[6] = DistanceConstraint7(x);
//        System.out.println("*** DistanceConstraint7 = " + solution.constraints()[6] );


        //System.out.println("*** evaluate solution: " + solution);
//        System.out.println("*** responseTime = " + solution.objectives()[0] );
//        System.out.println("*** networkUsage = " + solution.objectives()[1] );
//        System.out.println("*** DistanceConstraintlatency_threshold = " + solution.constraints()[0] );
//        System.out.println("*** DistanceConstraintCPUbound = " + solution.constraints()[1] );
//        System.out.println("*** DistanceConstraintMemorybound = " + solution.constraints()[2] );
//        System.out.println("*** DistanceConstraintbandwidthbound = " + solution.constraints()[3] );
//        System.out.println("*** DistanceConstraintPreference_threshold = " + solution.constraints()[4] );
//        System.out.println("*** DistanceConstraint6 = " + solution.constraints()[5] );
//        System.out.println("*** DistanceConstraint7 = " + solution.constraints()[6] );
        return solution;
    }


    public void calculateResponseTimeApps() {
        // ResponseTime(b) = (max)(π_k^b  ∈ Π^b ) PRT(π_k, b )
        ResponseTime = new Double[AppCount];
        for (int b = 0; b < AppCount; b++) {
            ResponseTime[b] = MaxRPT(b);
        }
    }

    private double MaxRPT(int app_b) {
        // for pathes in dsp(b) calculate: ResponseTime(b) = (max)(π_k^b  ∈ Π^b ) PRT(π_k, b )
        double max = 0;
        if (dspList != null) {
            //System.out.println("\ngetPathes().size()" + dspList.get(app_b).getPathes().size() + ", ");
            for (int k_path_index = 0; k_path_index < dspList.get(app_b).getPathes().size(); k_path_index++) {//Pathes_dataFlowPathSetOfApp
                double RPT_b_k = RPT(app_b, k_path_index);
                //System.out.print( RPT_b_k + ", ");
                if (RPT_b_k > max)
                    max = RPT_b_k;
            }
        }
        return max;
    }

    private double RPT(int app_b, int k_path_index) {
        //for k_path_index in path set of application b, calculate: PRT(π_k^b )= ∑(l(u,v)∈ E_res  &@e^b (i,j)∈〖 π〗_k^b )   [(x^b (j,v)*〖serviceTime〗^b (j,v)@+@  y^b (i,j)(u,v)*〖 transferTime〗^b  (i,j)(u,v) )]
        double RPT = 0, sum = 0;
        int b = app_b;
        if (dspList != null) {
            List<Set<Integer>> pathes = dspList.get(app_b).getPathes();
            Set<Integer> path = pathes.get(k_path_index);
            // Convert Set<Integer> to int[]
            int[] operatorsInPath = path.stream().mapToInt(Integer::intValue).toArray();
            int i, j, u, v;
            for (int opt_index = 0; opt_index < path.size() - 1; opt_index++) {// for e(i, j) in Path(b,k)
                //get operator i and its successor, operator j from path
                i = operatorsInPath[opt_index];
                j = operatorsInPath[opt_index + 1];
                //if(path.size() < 2){// DAG with one operator, we do not have dag with one operator, at least source-sink
                for (VertexNetwork networkVertex1 : networkGraph.getVertexes()) { // for node(u, v) in NetworkVertexes
                    u = networkVertex1.getId();
                    for (VertexNetwork networkVertex2 : networkGraph.getVertexes()) {
                        v = networkVertex2.getId();
                        boolean y = FindYbasedOnX(b, i, j, u, v);
                        int xInt = x[b][j][v] ? 1 : 0;
                        int yInt = y ? 1 : 0;
                        //sum += xInt * serviceTime(b, j, v) + yInt * transferTime(b, i, j, u, v);
                        double serviceTime = serviceTime(b, j, v);
                        double transferTime = transferTime(b, i, j, u, v);
                        //System.out.println("serviceTime = " + serviceTime + ", transferTime = " + transferTime);
                        sum += xInt * serviceTime + yInt * transferTime;
                    }
                }
            }
            RPT = sum;
        }
        return RPT;
    }

    //with possiblity of one operator dag and consider for sigma network links instead of network nodes
    private double RPT1(int app_b, int k_path_index) {
        //for k_path_index in path set of application b, calculate: PRT(π_k^b )= ∑(l(u,v)∈ E_res  &@e^b (i,j)∈〖 π〗_k^b )   [(x^b (j,v)*〖serviceTime〗^b (j,v)@+@  y^b (i,j)(u,v)*〖 transferTime〗^b  (i,j)(u,v) )]
        double RPT = 0, sum = 0;
        int b = app_b;
        List<Set<Integer>> pathes = dspList.get(app_b).getPathes();
        Set<Integer> path = pathes.get(k_path_index);
        // Convert Set<Integer> to int[]
        int[] operatorsInPath = path.stream().mapToInt(Integer::intValue).toArray();
        int i, j, u, v;
        for (int opt_index = 0; opt_index < path.size() - 1; opt_index++) {// for e(i, j) in Path(b,k)
            //get opertaor i and its successor, operator j from path
            i = operatorsInPath[opt_index];
            if (path.size() < 2) {// DAG with one operator, we do not have dag with one operator, at least source-sink
                j = i;
            } else {
                //operator j is successor of i in path
                j = operatorsInPath[opt_index + 1];
            }
            //for(EdgeNetwork networkEdge : networkGraph.getEdges()){ // for link(u, v) in E_resource/NetworkEdges, no because both operator i and j may placed on same network node
            for (VertexNetwork networkVertex1 : networkGraph.getVertexes()) { // for node(u, v) in NetworkVertexes
                u = networkVertex1.getId();
                if (i == j) {// DAG with one operator
                    v = u;
                    boolean y = FindYbasedOnX(b, i, j, u, v);
                    int xInt = x[b][j][v] ? 1 : 0;
                    int yInt = y ? 1 : 0;
                    sum += xInt * serviceTime(b, j, v) + yInt * transferTime(b, i, j, u, v);
                } else {
                    for (VertexNetwork networkVertex2 : networkGraph.getVertexes()) {
                        v = networkVertex2.getId();
                        boolean y = FindYbasedOnX(b, i, j, u, v);
                        int xInt = x[b][j][v] ? 1 : 0;
                        int yInt = y ? 1 : 0;
                        sum += xInt * serviceTime(b, j, v) + yInt * transferTime(b, i, j, u, v);
                    }
                }

            }
        }
        RPT = sum;
        return RPT;
    }

    public boolean FindYbasedOnX(int b, int i, int j, int u, int v) {

        if (x[b][i][u] && x[b][j][v]
                && isNextOperator(b, i, j)
                && isConnectedNetwork(u, v))
            return true;
        else
            return false;
    }

    private boolean isNextOperator(int b, int i, int j) {
        if (dspList != null) {
            for (EdgeDSP edge : dspList.get(b).getEdges()) {
                if (edge.getSource().getId() == i && edge.getDestination().getId() == j)
                    return true;
            }
        }
        return false;
    }

    private boolean isConnectedNetwork(int u, int v) {
        return true;
    }


    private double serviceTime(int b, int j, int v) {
        //serviceTime^b (j,v)=  1/(μ^b (j,v)  - λ^b (j))
        return 1 / (Miu(b, j, v) - Lamda(b, j));
    }

    private double Miu(int b, int j, int v) {
        //μ^b (j,v)=(μ^b (j,ref))/S_v
        //return (dspList.get(b).getVertexes().get(j).getMiuTime() / networkGraph.getVertexes().get(v).getSpeed()) ;
        //Or
        return (MiuHistory[b][j] / processingSpeed[v]);
    }

    private double Lamda(int b, int j) {
        //λ^b (j)=  ∑_(e^b (i,j)  ∈〖 E〗_dsp^b)  〖λ^b (i,j)〗
        double sum_lamda = 0;
        /*if (dspList.get(b).getVertexes().get(j).getType().equalsIgnoreCase("source"))
            for (EdgeDSP edgeDSP : dspList.get(b).getEdges()) {
                if (edgeDSP.getDestination().getId() == j) {
                    sum_lamda += edgeDSP.getLambda();
                }
            }*/

        // or calculate using lambda matrix in DSPRequirement of dsp graph b
        if (dspList != null) {
            for (int i = 0; i < dspList.get(b).getVertexes().size(); i++) {// numberOfOperator
                sum_lamda += dspList.get(b).getDSPRequirement().lambdas[i][j];
            }
        }

        return sum_lamda;
    }


    private double transferTime(int b, int i, int j, int u, int v) {
        //If operator i,j are in the same node 〖transferTime〗^b  (i,j)(u,v)=0.
        if (u == v)
            return 0;
        int transferTimesum = 0;
        transferTimesum += 1 / (Miunetwork(b, i, j, networkGraph.getEdges().get(u)) - Lamda(b, j));
        //TODO: getedgesinpath
        /*for (EdgeNetwork networkEdge : networkGraph.getedgesinpath(u, v)) {
//            int r = networkEdge.getSource().getId();
//            int q = networkEdge.getDestination().getId();
            transferTimesum += 1 / (Miunetwork(b, i, j, networkEdge) - Lamda(b, j));
        }*/
        return transferTimesum;
    }

    private double Miunetwork(int b, int i, int j, EdgeNetwork networkEdge) {
        //μ_net^b(i,j)(r,q)=(bdw(r,q))/(〖evtSize〗^b (i,j))
        //in getDSPRequirement, double[][] eventSizes;   //for all dataflows between operator i and j, evtSize^b (i,j) is the average size of data/events in Bytes, i.e., the size of operator i output data and consequently the size of operator j input data.
        return (networkEdge.getBandwidth() / dspList.get(b).getDSPRequirement().eventSizes[i][j]);
    }

    public void calculateNetworkUsageforAllApps() {
        NetworkUsage = new Double[AppCount];
        for (int b = 0; b < AppCount; b++) {
            double sum = 0;
            for (EdgeDSP dspEdge : dspList.get(b).getEdges()) {
                int i = dspEdge.getSource().getId();
                int j = dspEdge.getDestination().getId();
                for (EdgeNetwork networkEdge : networkGraph.getEdges()) {
                    int u = networkEdge.getSource().getId();
                    int v = networkEdge.getDestination().getId();
                    boolean y = FindYbasedOnX(b, i, j, u, v);
                    int yInt = y ? 1 : 0;
                    sum += dspEdge.getEventSize() * dspEdge.getLambda() * networkdelay(b, i, j, u, v) * yInt;
                }
            }
            NetworkUsage[b] = sum;
        }
    }


    private double networkdelay(int b, int i, int j, int u, int v) {
        //If operator i,j are in the same node 〖d〗^b  (i,j)(u,v)=0.
        if (u == v)
            return 0;

        int d = 0;
        d += (dspList.get(b).getDSPRequirement().eventSizes[i][j] / networkGraph.getEdges().get(u).getBandwidth());
        //TODO: getedgesinpath
        /*for (EdgeNetwork networkEdge : networkGraph.getedgesinpath(u, v)) {
//            int r = networkEdge.getSource().getId();
//            int q = networkEdge.getDestination().getId();
            d += (dspList.get(b).getDSPRequirement().eventSizes[i][j] / networkEdge.getBandwidth());
        }*/
        return d;
    }

    public double DistanceConstraintlatency_threshold(boolean x[][][]) {
        //First and the most important constraint is to ensure that the response time of each application does not exceed its latency threshold as follows
        //〖ResponseTime〗^b≤〖Latency 〗^b      ∀ b ∈ AppSet
        double diversion = 0;
        for (int b = 0; b < AppCount; b++) {
            diversion += Math.abs(dspList.get(b).getDSPRequirement().latencyThreshold - ResponseTime[b]);
        }
        return diversion;
    }

    public double DistanceConstraintCPUbound(boolean x[][][]) {
        //The bound of computational resource (CPU and memory) requirements of operators which are placed on nodes are guaranteed by equations 12
        //∑_(b ∈ AppSet )〖∑_(i ∈V_DSP^b  )〖x^b (i,u)*〖CPU〗_i^b  〗  〗  ≤〖CPU〗_u        ∀ u∈V_res
        double diversion = 0;
        for (VertexNetwork node : networkGraph.getVertexes()) {
            int u = node.getId();
            double CPUUsage = 0;
            for (int b = 0; b < AppCount; b++) {
                for (VertexDSP operator : dspList.get(b).getVertexes()) {
                    int i = operator.getId();
                    int xInt = x[b][i][u] ? 1 : 0;
                    CPUUsage += xInt * operator.getCPU_req();
                }
            }
            diversion += Math.abs(node.getCPU() - CPUUsage);
        }
        return diversion;
    }

    public double DistanceConstraintMemorybound(boolean x[][][]) {
        //∑_(b ∈ AppSet )▒〖∑_(i ∈V_DSP^b  )▒〖x^b (i,u)* 〖Mem〗_i^b  〗  〗  ≤〖Mem〗_u        ∀ u∈V_res
        double diversion = 0;
        for (VertexNetwork node : networkGraph.getVertexes()) {
            int u = node.getId();
            double MemUsage = 0;
            for (int b = 0; b < AppCount; b++) {
                for (VertexDSP operator : dspList.get(b).getVertexes()) {
                    int i = operator.getId();
                    int xInt = x[b][i][u] ? 1 : 0;
                    MemUsage += xInt * operator.getMem_req();
                }
            }
            diversion += Math.abs(node.getMem() - MemUsage);
        }
        return diversion;
    }

    public double DistanceConstraintbandwidthbound(boolean x[][][]) {
        //In the same way, total rate of data streams assigned on every network link should not exceed its bandwidth. This constraint is guaranteed by:
        double diversion = 0;
        for (EdgeNetwork edge : networkGraph.getEdges()) {
            double bandwidthUsage = 0;
            int u = edge.getSource().getId();
            int v = edge.getDestination().getId();
            for (int b = 0; b < AppCount; b++) {
                for (EdgeDSP dataflow : dspList.get(b).getEdges()) {
                    int i = dataflow.getSource().getId();
                    int j = dataflow.getDestination().getId();
                    boolean y = FindYbasedOnX(b, i, j, u, v);
                    int yInt = y ? 1 : 0;
                    bandwidthUsage += yInt * dataflow.getEventSize() * dataflow.getLambda();
                }
            }
            diversion += Math.abs(edge.getBandwidth() - bandwidthUsage);
        }
        return diversion;
    }

    public double DistanceConstraintPreference_threshold(boolean x[][][]) {
        //ToDO DistanceConstraint1
        double diversion = 0;
        for (int b = 0; b < AppCount; b++) {
            double preferenceexceed = 0;
            for (VertexNetwork node : networkGraph.getVertexes()) {
                int u = node.getId();
                for (VertexDSP operator : dspList.get(b).getVertexes()) {
                    int i = operator.getId();
                    int xInt = x[b][i][u] ? 1 : 0;
//                    System.out.println("b: " + b);
//                    System.out.println("i: " + i);
//                    System.out.println("u: " + u);
                    if (dspList.get(b).getPreferencePlacement().allocationPreference != null
                            && i < dspList.get(b).getPreferencePlacement().allocationPreference.length
                            && u < dspList.get(b).getPreferencePlacement().allocationPreference.length) {
                        int pref = dspList.get(b).getPreferencePlacement().allocationPreference[i][u];// ? 1 : 0;
                        preferenceexceed += 1 - (xInt * pref);
                    }
                }
            }
            diversion += Math.abs(dspList.get(b).getPreferencePlacement().preference_threshold - preferenceexceed);
        }
        return diversion;
    }

    public double DistanceConstraint6(boolean x[][][]) {
        //Equations 18 and 19 guarantee that an operator is only placed on one resource and a data stream is placed on one network path respectively:
        double diversion = 0;
        for (int b = 0; b < AppCount; b++) {
            for (VertexDSP operator : dspList.get(b).getVertexes()) {
                int i = operator.getId();
                double xsum = 0;
                for (VertexNetwork node : networkGraph.getVertexes()) {
                    int u = node.getId();
                    int xInt = x[b][i][u] ? 1 : 0;
                    xsum += xInt;
                }
                diversion += Math.abs(1 - xsum);
            }
        }
        return diversion;

    }

    public double DistanceConstraint7(boolean x[][][]) {
        double diversion = 0;
        for (int b = 0; b < AppCount; b++) {
            for (EdgeDSP dataflow : dspList.get(b).getEdges()) {
                int i = dataflow.getSource().getId();
                int j = dataflow.getDestination().getId();
                double ysum = 0;
                for (EdgeNetwork edge : networkGraph.getEdges()) {
                    int u = edge.getSource().getId();
                    int v = edge.getDestination().getId();
                    boolean y = FindYbasedOnX(b, i, j, u, v);
                    int yInt = y ? 1 : 0;
                    ysum += yInt;
                }
                diversion += Math.abs(1 - ysum);
            }
        }
        return diversion;
    }

    private void convertChromozomTo3DMatrix(IntegerSolution solution) {
        x = new boolean[AppCount][TaskMaxCount][NodeCount];
        int app, task, node;
        for (int index = 0; index < numberOfVariables; index++) {
            app = index / TaskMaxCount;
            task = index % TaskMaxCount;
            node = solution.variables().get(index);
            if (node >= 0 && node < NodeCount)
                x[app][task][node] = true;
        }
    }
}
