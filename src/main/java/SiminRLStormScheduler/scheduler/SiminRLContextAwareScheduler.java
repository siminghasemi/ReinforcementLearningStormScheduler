package SiminRLStormScheduler.scheduler;

import SiminRLStormScheduler.DSPModels.DSPRequirementModel;
import SiminRLStormScheduler.DSPModels.Graph.EdgeDSP;
import SiminRLStormScheduler.DSPModels.Graph.GraphDSP;
import SiminRLStormScheduler.DSPModels.Graph.VertexDSP;
import SiminRLStormScheduler.DSPModels.PreferencePlacementModel;
import SiminRLStormScheduler.NetworkModels.Graph.EdgeNetwork;
import SiminRLStormScheduler.NetworkModels.Graph.GraphNetwork;
import SiminRLStormScheduler.NetworkModels.Graph.VertexNetwork;
import SiminRLStormScheduler.NetworkModels.NetworkTopologyModel;
import SiminRLStormScheduler.scheduler.schedulerUtility.ISchedulerUtility;
import SiminRLStormScheduler.scheduler.schedulerUtility.SchedulerUtility;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.metric.StormMetricsRegistry;
import org.apache.storm.scheduler.*;
import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense;
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense;
import org.deeplearning4j.rl4j.network.dqn.IDQN;
import org.deeplearning4j.rl4j.observation.Observation;
import org.deeplearning4j.rl4j.policy.Policy;
import org.mortbay.util.MultiMap;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

import java.io.File;
import java.util.*;

import static org.apache.storm.scheduler.EvenScheduler.getAliveAssignedWorkerSlotExecutors;

@SuppressWarnings("Duplicates")
public class SiminRLContextAwareScheduler implements IScheduler {

    //alpha * responseTime - beta * networkUsage) / (teta * constrainSum)
    double alpha=0.3;// * responseTime ;
    double beta=0.000003;//* networkUsage
    double teta=0.000003;

    double gamma= 0.99;   // Gamma in QLearning.QLConfiguration
    double epsilon = 70;//50
    int capacity = 50000;// Exp replay size in QLearning.QLConfiguration
    int batchSize = 3;//32,     // Batch size but Simin take 3 because it is number of node [batchsize][actionNum]

    int fixedInputSize;//for inputSize of DQN layer which should be fixed
    ReplayBuffer replayBuffer;

    protected IDQN dqn;

    ISchedulerUtility schedulserUtility;

    // application and network profile file names
    Map storm_config;
    final String CONF_ApplicationDSPRequirementKey = "geoAwareSiminScheduler.in-DSPRequirementSample";//DSPRequirementSample1.json
    final String CONF_ApplicationDSPPreferenceKey = "geoAwareSiminScheduler.in-PreferencePlacementSample";//PreferencePlacementSample1.json
    final String CONF_NetworkTopologyKey = "geoAwareSiminScheduler.in-NetworkTopologySample";//NetworkTopologySample.json

    Random rand = new Random(System.currentTimeMillis());
    private static final Log LOG = LogFactory.getLog(SiminRLContextAwareScheduler.class);

    public void prepareDQN(List<GraphDSP> DSPSet, GraphNetwork networkGraph, int TaskMaxCount) {

        System.out.println("*** prepareDQN  ***");
        MyObservation obs = null;
        if (DSPSet != null) {
            obs = buildObservationFromCluster(DSPSet, networkGraph, TaskMaxCount);
        }

        // Step 1: Define config
        DQNFactoryStdDense.Configuration netConfig = DQNFactoryStdDense.Configuration.builder()
                .l2(0.001)
                .updater(new Adam(0.0005))
                .numHiddenNodes(64)
                .numLayer(2)
                .build();

        // Step 2: Create factory
        DQNFactoryStdDense factory = new DQNFactoryStdDense(netConfig.toNetworkConfiguration());

        // Step 3: Build DQN, Initialize and Train the DQN
        fixedInputSize = 21795;//100
        if(obs != null)
            fixedInputSize = flattenObservation(obs).length;

        int[] inputShape = new int[]{fixedInputSize}; // Your flattened observation size
        int numActions = 3;//node count //mdp.getActionSpace().getSize();

        dqn = factory.buildDQN(inputShape, numActions);

        replayBuffer = new ReplayBuffer(this.capacity);
    }

    public void trainBatch(List<Experience> batch, IDQN dqn, double gamma) {
        for (Experience exp : batch) {
            INDArray input = toINDArray(exp.state);
            INDArray nextInput = toINDArray(exp.nextState);

            INDArray qValues = dqn.output(input);
            INDArray nextQValues = dqn.output(nextInput);

            double maxQNext = Nd4j.max(nextQValues).getDouble(0);
            double updatedQ = exp.reward + (exp.done ? 0 : gamma * maxQNext);

            qValues.putScalar(exp.action, updatedQ);
            dqn.fit(input, qValues);
        }
    }

    public INDArray toINDArray(MyObservation obs) {
        double[] flat = flattenObservation(obs); // Write this method based on your obs fields

        //DQN requires fixed input dimensionality
        //input size for DQN network layer should be fixed
        double[] padded = new double[fixedInputSize];
        int len = Math.min(fixedInputSize, flat.length);
        System.arraycopy(flat, 0, padded, 0, len);

        double[][] data = new double[batchSize][fixedInputSize];//flat.length
        data[0] = padded; //flat;
        return Nd4j.create(data);
    }

    public double[] flattenObservation(MyObservation obs) {
        List<Double> flat = new ArrayList<>();

        // Flatten task graph
        for (double [][] rows : obs.getTaskGraph()) {
            for(double [] row :rows)
                for (double val : row) flat.add(val);
        }

        // Flatten network matrix
        for (double[] row : obs.getNetworkMatrix()) {
            for (double val : row) flat.add(val);
        }

        // Flatten node resources
        for (double[] row : obs.getResources()) {
            for (double val : row) flat.add(val);
        }

        // Optionally: include task info (e.g., task ID)
        //flat.add((double) obs.getCurrentTask().getStartTask());

        return flat.stream().mapToDouble(d -> d).toArray();
    }


    private WorkerSlot selectSlotByAction(int action, Set<WorkerSlot> availableSlots) {
        List<WorkerSlot> slots = new ArrayList<>(availableSlots);
        if (action < slots.size()) {
            return slots.get(action);
        }
        return null;
    }

    public MyObservation buildObservationFromCluster(List<GraphDSP> DSPSet, GraphNetwork networkGraph, int TaskMaxCount) {
        double[][][] taskGraph = extractTaskGraph(DSPSet, TaskMaxCount);
        double[][] networkMatrix = extractNetworkBandwidthMatrix(networkGraph);
        double[][] resources = extractNodeResources(networkGraph);

        return new MyObservation(taskGraph, networkMatrix, resources);
    }

    public MyObservation buildObservationFromClusterAfterAssingment(List<GraphDSP> DSPSet, GraphNetwork networkGraph, int[] action, int TaskMaxCount, boolean[][][] AllocationTaskMatrix) {
        double[][][] taskGraph = extractTaskGraph(DSPSet, TaskMaxCount);
        double[][] networkMatrix = extractNetworkBandwidthMatrixAfterAssingment(networkGraph, DSPSet, AllocationTaskMatrix);
        double[][] resources = extractNodeResourcesAfterAssingment(networkGraph, DSPSet, action, TaskMaxCount);

        return new MyObservation(taskGraph, networkMatrix, resources);
    }

    private double[][][] extractTaskGraph(List<GraphDSP> DSPSet, int TaskMaxCount) {

        double[][][] taskGraphAdj = new double[DSPSet.size()][TaskMaxCount][TaskMaxCount];

        for (int i = 0; i < DSPSet.size(); i++) {

            GraphDSP dsp = DSPSet.get(i); // Assuming 1 DSP at a time
            List<VertexDSP> dspTasks = dsp.getVertexes();
            int numTasks = dspTasks.size();

            for (EdgeDSP edge : dsp.getEdges()) {
                int from = edge.getSource().getId();
                int to = edge.getDestination().getId();
                if (from < numTasks && to < numTasks)
                    taskGraphAdj[i][from][to] = edge.getLambda();
            }

        }
        return taskGraphAdj;
    }

    private double[][] extractNetworkBandwidthMatrix(GraphNetwork networkGraph) {
        List<VertexNetwork> nodes = networkGraph.getVertexes();
        int numNodes = nodes.size();
        double[][] netAdj = new double[numNodes][numNodes];

        for (EdgeNetwork edge : networkGraph.getEdges()) {
            int from = edge.getSource().getId();
            int to = edge.getDestination().getId();
            if (from < numNodes && to < numNodes)
                netAdj[from][to] = edge.getBandwidth();
        }
        return netAdj;
    }

    private double[][] extractNetworkBandwidthMatrixAfterAssingment(GraphNetwork networkGraph, List<GraphDSP> DSPSet, boolean[][][] x) {

        int numNodes = networkGraph.getVertexes().size();
        double[][] netAdj = extractNetworkBandwidthMatrix(networkGraph);

        MyProblemObjectiveFunctions problem = new MyProblemObjectiveFunctions(x, DSPSet, networkGraph);
        int appCount = DSPSet.size();

        if (DSPSet != null && DSPSet.size() > 0) {

            for (int b = 0; b < appCount; b++) {
                for (EdgeDSP dspEdge : DSPSet.get(b).getEdges()) {
                    int i = dspEdge.getSource().getId();
                    int j = dspEdge.getDestination().getId();
                    for (EdgeNetwork networkEdge : networkGraph.getEdges()) {
                        int u = networkEdge.getSource().getId();
                        int v = networkEdge.getDestination().getId();
                        if (u < numNodes && v < numNodes) {
                            boolean y = problem.FindYbasedOnX(b, i, j, u, v);
                            int yInt = y ? 1 : 0;
                            netAdj[u][v] -= dspEdge.getEventSize() * dspEdge.getLambda() * yInt;//  byte/event * event/second
                        }
                    }
                }
            }
        }
        return netAdj;
    }

    private double[][] extractNodeResources(GraphNetwork networkGraph) {

        List<VertexNetwork> nodes = networkGraph.getVertexes();
        int numNodes = nodes.size();
        double[][] nodeResources = new double[numNodes][2]; // CPU, MEM

        for (int i = 0; i < numNodes; i++) {
            VertexNetwork node = nodes.get(i);
            nodeResources[i][0] = node.getCPU();
            nodeResources[i][1] = node.getMem();
        }

        return nodeResources;
    }

    private double[][] extractNodeResourcesAfterAssingment(GraphNetwork networkGraph, List<GraphDSP> DSPSet, int[] action, int TaskMaxCount) {

        double[][] nodeResources = extractNodeResources(networkGraph);

        //Reduced used Mem and CPU by allocated task to each node
        if (DSPSet != null && DSPSet.size() > 0) {

            int app, task, node;
            double taskCPUReq, taskMemReq;
            for (int i = 0; i < action.length; i++) {
                app = i / TaskMaxCount;
                task = i % DSPSet.get(app).getVertexes().size();
                node = action[i];
                if (DSPSet != null && DSPSet.get(app) != null && DSPSet.get(app).getVertexes() != null && DSPSet.get(app).getVertexes().get(task) != null) {

                    taskCPUReq = DSPSet.get(app).getVertexes().get(task).getCPU_req();
                    nodeResources[node][0] -= taskCPUReq;

                    taskMemReq = DSPSet.get(app).getVertexes().get(task).getMem_req();
                    nodeResources[node][1] -= taskMemReq;
                }
            }
        }

        return nodeResources;
    }


    public int[] selectAction(IDQN dqn, MyObservation obs, double epsilon, List<GraphDSP> DSPSet, int nodeCount, int TaskMaxCount) {
        int appCount = DSPSet.size();

        int[] actionVector = new int[TaskMaxCount * appCount];

        for (int i = 0; i < TaskMaxCount * appCount; i++) {

            if (Math.random() < epsilon) {
                actionVector[i] = new Random().nextInt(dqn.output(toINDArray(obs)).columns());

            } else {
                INDArray qValues = dqn.output(toINDArray(obs));
                actionVector[i] = Nd4j.argMax(qValues, 1).getInt(0);//action space is 0 to n-1, set batchSize = 3 to set action space to node count
            }

            if (actionVector[i] < 0 || actionVector[i] >= nodeCount) {
                //System.out.println("*** actionVector[i] = " + actionVector[i]);
                if(nodeCount > 0 ) // if cluster has no slave node yet
                    actionVector[i] = new Random().nextInt(nodeCount);
//                else
//                    actionVector[i] = 0;
            }
        }

        return actionVector;
    }

    public double computeReward(boolean x[][][], List<GraphDSP> DSPSet, GraphNetwork networkGraph) {
        MyProblemObjectiveFunctions problem = new MyProblemObjectiveFunctions(x, DSPSet, networkGraph);
        //2- calculate  objective function for time and network
        problem.calculateResponseTimeApps();
        double responseTime = 0;
        if (problem.ResponseTime != null && problem.ResponseTime.length > 0)
            responseTime = Collections.max(Arrays.asList(problem.ResponseTime));//MaxResponseTime();
        //solution.objectives()[0] = responseTime;

        problem.calculateNetworkUsageforAllApps();
        double networkUsage = 0;
        if (problem.NetworkUsage != null && problem.NetworkUsage.length > 0)
            networkUsage = Collections.max(Arrays.asList(problem.NetworkUsage));//MaxNetworkUsage(NetworkUsage);
        //solution.objectives()[1] = networkUsage;

        //3- calculate constraint distance for constraint 1-7
        double constraints[] = new double[7];
        // First and the most important constraint is to ensure that the response time of each application does not exceed its latency threshold as follows:
        constraints[0] = problem.DistanceConstraintlatency_threshold(x);

        //The bound of computational resource (CPU and memory) requirements of operators which are placed on nodes are guaranteed by equations 12 and 13:
        constraints[1] = problem.DistanceConstraintCPUbound(x);

        constraints[2] = problem.DistanceConstraintMemorybound(x);

        //In the same way, total rate of data streams assigned on every network link should not exceed its bandwidth. This constraint is guaranteed by
        constraints[3] = problem.DistanceConstraintbandwidthbound(x);

        //To satisfy the DSP application owner’s preference in operator placements, the following constraint guarantees that total deviation of the placements does not exceed the maximum acceptable deviation for DSP application
        constraints[4] = problem.DistanceConstraintPreference_threshold(x);

        //Equations 18 and 19 guarantee that an operator is only placed on one resource and a data stream is placed on one network path respectively:
        constraints[5] = problem.DistanceConstraint6(x);
        constraints[6] = problem.DistanceConstraint7(x);

        double constrainSum = 0;
        for (int i = 0; i < 7; i++)
            constrainSum += constraints[i];

        System.out.println( "\nresponseTime: "+ responseTime + ", networkUsage: " + networkUsage +  ", constrainSum: " + constrainSum);
        double reward = (-alpha * responseTime - beta * networkUsage) / (teta * constrainSum);
//        double reward = (-alpha * responseTime - beta * networkUsage) - (teta * constrainSum);
        return reward;
    }


    public void prepare(Map conf) {
        //the conf is the storm.yaml file as a hashmap and includes all the configuration as key:value
        storm_config = conf;


        schedulserUtility = new SchedulerUtility();
        // 1. create network and DSP graphs from profile json files addressed in storm.yaml
        System.out.println("Load DSPApplicationsFromJson and PreferencePlacement for DSP profiling from DSPRequirementSample1.json and DSPRequirementSample1.json");
        String fileNameDSPRequirement = this.storm_config.get(CONF_ApplicationDSPRequirementKey) != null ? this.storm_config.get(CONF_ApplicationDSPRequirementKey).toString() : "DSPRequirementSample";
        String fileNamePreferencePlacement = this.storm_config.get(CONF_ApplicationDSPPreferenceKey) != null ? this.storm_config.get(CONF_ApplicationDSPPreferenceKey).toString() : "PreferencePlacementSample";
        String fileNameNetworkTopology = this.storm_config.get(CONF_NetworkTopologyKey) != null ? this.storm_config.get(CONF_NetworkTopologyKey).toString() : "NetworkTopologySample.json";

        int appCount = 5;
        NetworkTopologyModel networkTopology = schedulserUtility.LoadNetworkTopologyProfiles(fileNameNetworkTopology);
        List<GraphDSP> DSPSet = schedulserUtility.LoadDSPApplicationsProfile(appCount, fileNameDSPRequirement, fileNamePreferencePlacement);

        //3. Create DSP and Network Graph from their models
        GraphNetwork networkGraph = schedulserUtility.CreateNetworkGraph(networkTopology);
        int TaskMaxCount = calculateTaskMaxCount(DSPSet);


        prepareDQN(DSPSet, networkGraph, TaskMaxCount);
    }

    @Override
    public void prepare(Map<String, Object> map, StormMetricsRegistry smr) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        //IScheduler.super.prepare(map, smr);
        //Retrieve data from storm.yaml config file
        storm_config = map;

        schedulserUtility = new SchedulerUtility();
        // 1. create network and DSP graphs from profile json files addressed in storm.yaml
        System.out.println("Load DSPApplicationsFromJson and PreferencePlacement for DSP profiling from DSPRequirementSample1.json and DSPRequirementSample1.json");
        String fileNameDSPRequirement = this.storm_config.get(CONF_ApplicationDSPRequirementKey) != null ? this.storm_config.get(CONF_ApplicationDSPRequirementKey).toString() : "DSPRequirementSample";
        String fileNamePreferencePlacement = this.storm_config.get(CONF_ApplicationDSPPreferenceKey) != null ? this.storm_config.get(CONF_ApplicationDSPPreferenceKey).toString() : "PreferencePlacementSample";
        String fileNameNetworkTopology = this.storm_config.get(CONF_NetworkTopologyKey) != null ? this.storm_config.get(CONF_NetworkTopologyKey).toString() : "NetworkTopologySample.json";

        int appCount = 5;
        NetworkTopologyModel networkTopology = schedulserUtility.LoadNetworkTopologyProfiles(fileNameNetworkTopology);
        List<GraphDSP> DSPSet = schedulserUtility.LoadDSPApplicationsProfile(appCount, fileNameDSPRequirement, fileNamePreferencePlacement);

        //3. Create DSP and Network Graph from their models
        GraphNetwork networkGraph = schedulserUtility.CreateNetworkGraph(networkTopology);
        int TaskMaxCount = calculateTaskMaxCount(DSPSet);


        prepareDQN(DSPSet, networkGraph, TaskMaxCount);
        LOG.info("*** GeoAwareGroupScheduler: prepare");
    }

    @Override
    public Map config() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        //This function returns the scheduler’s configuration.
        LOG.info("GeoAwareGroupScheduler: config");
        return storm_config;
    }


    @Override
    public void schedule(Topologies topologies, Cluster cluster) {

        System.out.println("*** SiminContextAwareScheduler: begin scheduling");

        schedulserUtility = new SchedulerUtility();
        // 1. create network and DSP graphs from profile json files addressed in storm.yaml
        System.out.println("Load DSPApplicationsFromJson and PreferencePlacement for DSP profiling from DSPRequirementSample1.json and DSPRequirementSample1.json");
        String fileNameDSPRequirement = this.storm_config.get(CONF_ApplicationDSPRequirementKey) != null ? this.storm_config.get(CONF_ApplicationDSPRequirementKey).toString() : "DSPRequirementSample";
        String fileNamePreferencePlacement = this.storm_config.get(CONF_ApplicationDSPPreferenceKey) != null ? this.storm_config.get(CONF_ApplicationDSPPreferenceKey).toString() : "PreferencePlacementSample";
        String fileNameNetworkTopology = this.storm_config.get(CONF_NetworkTopologyKey) != null ? this.storm_config.get(CONF_NetworkTopologyKey).toString() : "NetworkTopologySample.json";

        //storm.yaml
        //geoAwareScheduler.in-DSPRequirementSample: "/home/simin/Desktop/SpanEdge/StormOnEdge-master/data/DSPRequirementSample1.json"
        //geoAwareScheduler.in-PreferencePlacementSample: "/home/simin/Desktop/SpanEdge/StormOnEdge-master/data/PreferencePlacementSample1.json"
        //geoAwareScheduler.in-NetworkTopologySample: "/home/simin/Desktop/SpanEdge/StormOnEdge-master/data/NetworkTopologySample.json"

        int appCount = 5;
        NetworkTopologyModel networkTopology = schedulserUtility.LoadNetworkTopologyProfiles(fileNameNetworkTopology);
        List<GraphDSP> DSPSet = schedulserUtility.LoadDSPApplicationsProfile(appCount, fileNameDSPRequirement, fileNamePreferencePlacement);
        appCount = DSPSet.size();

        //2. Update DSP and network models From TopologySet and cluster
        schedulserUtility.updateDPSFromTopologySet(DSPSet, topologies, cluster);
        networkTopology = schedulserUtility.updateNetwrorkFromCluster(networkTopology, cluster);

        //3. Create DSP and Network Graph from their models
        GraphNetwork networkGraph = schedulserUtility.CreateNetworkGraph(networkTopology);
        int NodeCount = networkGraph.getVertexes().size();
        int TaskMaxCount = calculateTaskMaxCount(DSPSet);

        //4.
        MyObservation obs = buildObservationFromCluster(DSPSet, networkGraph, TaskMaxCount);

        //5.
        int[] action = selectAction(dqn, obs, epsilon, DSPSet, NodeCount, TaskMaxCount);

        //6. convert allocation 3-D matrix to StormAssignment

        boolean[][][] AllocationTaskMatrix = schedulserUtility.convertActionVectorTo3DMatrix(action, DSPSet, appCount, NodeCount, TaskMaxCount);

        //7. storm executor to worker_slot assignment
        assignTasksToWorkerSlots(cluster, AllocationTaskMatrix, DSPSet, networkGraph, NodeCount, TaskMaxCount);

        //8.
        double reward = computeReward(AllocationTaskMatrix, DSPSet, networkGraph);

        //9.
        MyObservation nextObs = buildObservationFromClusterAfterAssingment(DSPSet, networkGraph, action, TaskMaxCount, AllocationTaskMatrix);

        //10.
        Experience exp = new Experience(obs, action, reward, nextObs, false);
        replayBuffer.add(exp);

        //11.
        if (replayBuffer.size() > batchSize) {
            List<Experience> batch = replayBuffer.sample(batchSize);
            trainBatch(batch, dqn, gamma);
        }

        // let system's even StormOnEdge.scheduler handle the rest scheduling work
        // you can also use your own other StormOnEdge.scheduler here, this is what
        // makes storm's StormOnEdge.scheduler composable.
        new EvenScheduler().schedule(topologies, cluster);
    }

    private void deployExecutorToWorkers(List<WorkerSlot> cloudWorkers, List<ExecutorDetails> executors, MultiMap executorWorkerMap) {
        Iterator<WorkerSlot> workerIterator = cloudWorkers.iterator();
        Iterator<ExecutorDetails> executorIterator = executors.iterator();

        //if executors >= workers, do simple round robin
        //for all executors A to all supervisors B
        if (executors.size() >= cloudWorkers.size()) {
            while (executorIterator.hasNext() && workerIterator.hasNext()) {
                WorkerSlot w = workerIterator.next();
                ExecutorDetails ed = executorIterator.next();
                executorWorkerMap.add(w, ed);

                //Reset to first worker again
                if (!workerIterator.hasNext()) {
                    workerIterator = cloudWorkers.iterator();
                }
            }
        } //if workers > executors, choose randomly
        //for all executors A to all supervisors B
        else {
            while (executorIterator.hasNext() && !cloudWorkers.isEmpty()) {
                WorkerSlot w = cloudWorkers.get(rand.nextInt(cloudWorkers.size()));
                ExecutorDetails ed = executorIterator.next();
                executorWorkerMap.add(w, ed);
            }
        }
    }

    private void assignTasksToWorkerSlots(Cluster cluster, boolean[][][] AllocationTaskMatrix, List<GraphDSP> DSPSet, GraphNetwork networkGraph, int NodeCount, int TaskMaxCount) {
        System.out.println("9. custom executor to worker_slot assignment");
        List<SupervisorDetails> supervisors = new ArrayList<SupervisorDetails>(cluster.getSupervisors().values());
        int appCount = DSPSet.size();
        for (int appIndex = 0; appIndex < appCount; appIndex++) {//for every appIndex

            TopologyDetails topology = schedulserUtility.findTopologyIdByAppName(cluster.needsSchedulingTopologies(), DSPSet.get(appIndex));
            if (topology != null) {
                for (int nodeIndex = 0; nodeIndex < NodeCount; nodeIndex++) {

                    SupervisorDetails supervisor = schedulserUtility.findSupervisorByNodeIndex(supervisors, networkGraph.getNetworkTopology(), nodeIndex);
                    if (supervisor != null) {
                        MultiMap executorWorkerMap = new MultiMap();
                        List<WorkerSlot> workerSlotsofNodeIndex = cluster.getAvailableSlots(supervisor);

                        List<ExecutorDetails> executorListAssignedToNodeIndex = schedulserUtility.findExecutorsAssignedToNodeIndex(topology, AllocationTaskMatrix, appIndex, nodeIndex, TaskMaxCount);

                        //determine assingment of executors of this node to its worker_slots (port number ex. 6700, 6701, 6702)
                        deployExecutorToWorkers(workerSlotsofNodeIndex, executorListAssignedToNodeIndex, executorWorkerMap);

                        //for each worker slot of this node, assing its executors in storm cluster
                        for (Object ws : executorWorkerMap.keySet()) {
                            List<ExecutorDetails> executordetailList = (List<ExecutorDetails>) executorWorkerMap.getValues(ws);
                            WorkerSlot workerSlot = (WorkerSlot) ws;
                            cluster.assign(workerSlot, topology.getId(), executordetailList);
                            System.out.println("*** We assigned executors:" + executorWorkerMap.getValues(ws) + " to slot: [" + workerSlot.getNodeId() + ", " + workerSlot.getPort() + "], for supervisor: " + supervisor.toString());
                        }

                    }//if (supervisor != null)
                }//for ( nodeIndex < NodeCount; )
            }//if (!topologyId.isEmpty())
        }//for ( appIndex < AppCount; )


    }

    public int calculateTaskMaxCount(List<GraphDSP> DSPSet) {
        //calculate TaskMaxCount over all applications
        int appCount = DSPSet.size();
        Integer tasksize[] = new Integer[appCount];
        if (tasksize != null && tasksize.length > 0)
            for (int b = 0; b < appCount; b++) {
                if (DSPSet != null && DSPSet.get(b) != null && DSPSet.get(b).getVertexes() != null)
                    tasksize[b] = DSPSet.get(b).getVertexes().size();
            }
        int TaskMaxCount = 0;
        if (tasksize != null && tasksize.length > 0)
            TaskMaxCount = Collections.max(Arrays.asList(tasksize));

        return TaskMaxCount;
    }


    @Override
    public void cleanup() {
        IScheduler.super.cleanup(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void nodeAssignmentSent(String node, boolean successful) {
        IScheduler.super.nodeAssignmentSent(node, successful); //To change body of generated methods, choose Tools | Templates.
    }

}
