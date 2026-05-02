package SiminRLStormScheduler.scheduler.schedulerUtility;

import SiminRLStormScheduler.DSPModels.DSPReader;
import SiminRLStormScheduler.DSPModels.DSPRequirementModel;
import SiminRLStormScheduler.DSPModels.Graph.GraphDSP;
import SiminRLStormScheduler.DSPModels.PreferencePlacementModel;
import SiminRLStormScheduler.NSGA_Classes.CustomCrossOver;
import SiminRLStormScheduler.NSGA_Classes.MyProblem_ObjectiveFunctions;
import SiminRLStormScheduler.NetworkModels.Graph.GraphNetwork;
import SiminRLStormScheduler.NetworkModels.NetworkReader;
import SiminRLStormScheduler.NetworkModels.NetworkTopologyModel;
import SiminRLStormScheduler.scheduler.MyMDP;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.storm.generated.Bolt;
import org.apache.storm.generated.SpoutSpec;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.scheduler.*;
import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning;
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense;
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense;
import org.deeplearning4j.rl4j.observation.Observation;
import org.nd4j.linalg.learning.config.Adam;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAII;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.IntegerPolynomialMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;

import java.util.*;

public class SchedulerUtility implements ISchedulerUtility {
    private static final Log LOG = LogFactory.getLog(SchedulerUtility.class);
    DSPReader dspReader = new DSPReader();
    NetworkReader networkReader = new NetworkReader();

    @Override
    public List<DSPRequirementModel> LoadDSPRequirementProfiles(int AppCount, String filePath) {

        //1. Read DSP apps data from file
        LOG.info("Load DSPRequirementModel from DSPRequirementSample1.json for each DSP app");
        //storm.yaml
        //geoAwareScheduler.in-DSPRequirementSample: "/home/simin/Desktop/SpanEdge/StormOnEdge-master/data/DSPRequirementSample1.json"
        List<DSPRequirementModel> DSPRequirements = new ArrayList<>();
        String filenameDSPRequirement = "DSPRequirementSample";

        for (int i = 0; i < AppCount; i++) {
            try {
                System.out.println("Load DSPRequirementModel from DSPRequirementSample" + i + ".json for each DSP app");

                String fileNameDSPRequirement = filenameDSPRequirement + (i + 1) + ".json";
                DSPRequirementModel DSPReq = dspReader.loadDSPApplicationsFromJson(filePath, fileNameDSPRequirement);
                DSPRequirements.add(DSPReq);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println("Exception when loading the DSP Requirement and Preference (" + i + "). Scheduler stopped");
                return null;
            }
        }
        return DSPRequirements;
    }

    @Override
    public List<PreferencePlacementModel> LoadPreferencePlacementProfiles(int AppCount, String filePath) {

        //1. Read DSP apps data from file
        LOG.info("Load PreferencePlacementModel from PreferencePlacementSample1.json for each DSP app");
        //storm.yaml
        //geoAwareScheduler.in-PreferencePlacementSample: "/home/simin/Desktop/SpanEdge/StormOnEdge-master/data/PreferencePlacementSample1.json"

        String filename = "PreferencePlacementSample";
        List<PreferencePlacementModel> PreferencePlacements = new ArrayList<>();

        for (int i = 0; i < AppCount; i++) {
            try {
                String filePathPreferencePlacement = filename + (i + 1) + ".json";
                PreferencePlacementModel preference = dspReader.loadPreferencePlacementFromJson(filePath, filePathPreferencePlacement);
                PreferencePlacements.add(preference);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println("Exception when loading the DSP Requirement and Preference (" + i + "). Scheduler stopped");
                return null;
            }

        }
        return PreferencePlacements;
    }

    public List<GraphDSP> LoadDSPApplicationsProfile(int AppCount, String filepathDSPRequirement, String filepathPreferencePlacement) {

        //1. Read DSP apps data from file and fill the list<DSPGraph>
        //b_th DSP application is represented as a DAG, G_DSP^b= (V_DSP^b , E_DSP^b) where V_DSP^b is the set of operators (as well as data sources or sinks) and E_DSP^b is the set of data streams between operators
        LOG.info("* LoadDSPApplicationsProfile: Load DSPApplicationsFromJson and PreferencePlacement for DSP profiling from DSPRequirementSample1.json and DSPRequirementSample1.json");
        List<GraphDSP> DSPSet = new ArrayList<>();

        //spanedge
//        geoAwareScheduler.in-SourceInfo: "/home/simin/Desktop/SpanEdge/StormOnEdge-master/data/Scheduler-SpoutCloudsPair.txt"
//        geoAwareScheduler.in-CloudInfo: "/home/simin/Desktop/SpanEdge/StormOnEdge-master/data/Scheduler-LatencyMatrix.txt"
//        geoAwareScheduler.out-ZGConnector: "/home/simin/Desktop/SpanEdge/StormOnEdge-master/data/PairSupervisorTasks.txt"

        //my scheduler
//        geoAwareScheduler.in-SourceInfo: "/home/simin/Desktop/SpanEdge/StormOnEdge-master/data/DSPRequirementSample"+"1.json"

        String filenameDSPRequirement = "DSPRequirementSample";
        String filenamePreferencePlacement = "PreferencePlacementSample";

        for (int i = 0; i < AppCount; i++) {
            try {
                String fileNameDSPRequirement = filenameDSPRequirement + (i + 1) + ".json";
                DSPRequirementModel DSPReq = dspReader.loadDSPApplicationsFromJson(filepathDSPRequirement, fileNameDSPRequirement);

                String fileNamePreferencePlacement = filenamePreferencePlacement + (i + 1) + ".json";
                PreferencePlacementModel preference = dspReader.loadPreferencePlacementFromJson(filepathPreferencePlacement, fileNamePreferencePlacement);

                GraphDSP dspGraph = dspReader.CreateDSPGraphFromDSPRequirementModel(DSPReq, preference);
                DSPSet.add(dspGraph);

            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println("Exception when loading the DSP Requirement and Preference (" + i + "). Scheduler stopped");
            }

        }
        return DSPSet;
    }


    @Override
    public NetworkTopologyModel LoadNetworkTopologyProfiles(String filePathNetworkTopology) {
        //3. Read network graph data from file and fill in the NetworkGraph
        System.out.println("*** Load NetworkTopology for cloud from NetworkTopologySample.json");
        String filename = "NetworkTopologySample.json";
        NetworkTopologyModel networkTopology = networkReader.loadNetworkTopologyFromJson(filePathNetworkTopology, filename);
        return networkTopology;
    }

    public GraphNetwork CreateNetworkGraph(NetworkTopologyModel networkTopology) {
        //3. Read network graph data from file and fill in the NetworkGraph
        System.out.println("CreateNetworkGraph");
        GraphNetwork networkGraph = networkReader.CreateNetworkGraph(networkTopology);
        return networkGraph;
    }

    @Override
    public List<GraphDSP> updateDPSFromTopologySet(List<GraphDSP> dspSet, Topologies topologies, Cluster cluster) {

//        for (TopologyDetails topology : cluster.needsSchedulingTopologies()) {
//            for (DSPRequirementModel dspRequirement: DSPRequirements) {
//                if(dspRequirement.name.equalsIgnoreCase(topology.getName())){
//                    dspRequirement.numberOfOperator = topology.getExecutors().size();
//                }
//            }
//        }

        for (TopologyDetails topology : topologies.getTopologies()) {
            System.out.println("Topology name : " + topology.getName());

            System.out.println("*** cluster.getAvailableSlots.size : " + cluster.getAvailableSlots().size());
            System.out.println(cluster.getAvailableSlots());
            if (!cluster.needsScheduling(topology) || cluster.getNeedsSchedulingComponentToExecutors(topology).isEmpty() || cluster.getAvailableSlots().isEmpty()) {
                System.out.println("This topology doesn't need scheduling.");
                //LOG.info("This topology doesn't need scheduling.");
                continue;
            }

            DSPRequirementModel dspRequirementModel = null;
            boolean DSPExistInTopologies = false;
            if (dspSet != null && dspSet.size() > 0) {
                for (GraphDSP graphDSP : dspSet) {
                    if (graphDSP.getDSPRequirement().name.equalsIgnoreCase(topology.getName())) {
                        dspRequirementModel = graphDSP.getDSPRequirement();
                        DSPExistInTopologies = true;
                        break;
                    }
                }
            }
            if (!DSPExistInTopologies) {
                continue;
            }

            StormTopology st = topology.getTopology();
            Map<String, Bolt> bolts = st.get_bolts();
            Map<String, SpoutSpec> spouts = st.get_spouts();

            dspRequirementModel.numberOfOperator = bolts.size() + spouts.size();

        }

        return dspSet;
    }

    @Override
    public NetworkTopologyModel updateNetwrorkFromCluster(NetworkTopologyModel networkTopologyModel, Cluster cluster) {

        networkTopologyModel.NumberOfNodes = cluster.getSupervisors().size();

        //map the supervisors and workers based on cloud names
        List<SupervisorDetails> supervisors = new ArrayList<SupervisorDetails>(cluster.getSupervisors().values());
        for (SupervisorDetails supervisor : supervisors) {
            for (int j = 0; j < networkTopologyModel.NumberOfNodes; j++) {
                if (networkTopologyModel.CPUResources[j] == supervisor.getTotalCpu()
                        && networkTopologyModel.MemResources[j] == supervisor.getTotalMemory()) {
                    networkTopologyModel.Hosts[j] = supervisor.getHost();
                    networkTopologyModel.Ids[j] = supervisor.getId();
                    networkTopologyModel.AllPorts[j] = supervisor.getAllPorts().toString();

                    Map<String, Object> metadata = (Map<String, Object>) supervisor.getSchedulerMeta();
                    //simin explains: in storm.yaml of each supervisor, it add this metadata:
                    //## On supervisor node: Every nodes that run Supervisor instances need to put information about their cloud location
                    //supervisor.scheduler.meta:
                    //name: "SUPERVISOR_1"
                    //cloud-name: "CLOUD_A"
                    //speed: "1.5"
                    if (metadata.get("speed") != null) {//cloud-name
                        networkTopologyModel.Speeds[j] = Integer.parseInt(metadata.get("speed").toString());
                    }

                    break;
                }
            }
        }

        return networkTopologyModel;
    }

    public SupervisorDetails findSupervisorByNodeIndex(List<SupervisorDetails> supervisors, NetworkTopologyModel networkTopologyModel, int nodeIndex) {

        for (SupervisorDetails supervisor : supervisors) {
            if (networkTopologyModel.CPUResources[nodeIndex] == supervisor.getTotalCpu()
                    && networkTopologyModel.MemResources[nodeIndex] == supervisor.getTotalMemory()) {
                return supervisor;
            }
        }

        return null;
    }

    public TopologyDetails findTopologyIdByAppName(List<TopologyDetails> topologies, GraphDSP DSPGraph) {
        DSPRequirementModel dspRequirement = DSPGraph.getDSPRequirement();
        for (TopologyDetails topology : topologies) {
            if (dspRequirement.name.equalsIgnoreCase(topology.getName())) {
                return topology;
            }
        }
        return null;
    }

    public List<ExecutorDetails> findExecutorsAssignedToNodeIndex(TopologyDetails topology, boolean[][][] AllocationTaskMatrix, int appIndex, int nodeIndex, int TaskMaxCount) {
        Set<ExecutorDetails> allExecutors = topology.getExecutors();
        Iterator<ExecutorDetails> executorIterator = allExecutors.iterator();
        List<ExecutorDetails> executorDetailsList = new ArrayList<>();

        for (int taskIndex = 0; taskIndex < TaskMaxCount; taskIndex++) {//TaskMaxCount: 28

            if (AllocationTaskMatrix[appIndex][nodeIndex][taskIndex] == true) {

                while (executorIterator.hasNext()) {
                    ExecutorDetails executor = executorIterator.next();
                    //System.out.println("*** executor: " + executor.toString());
                    if (executor.startTask == (taskIndex + 1)) {
                        executorDetailsList.add(executor);
                        break;
                    }
                }
            }
        }

        return executorDetailsList;
    }

    public NSGAII<IntegerSolution> setupNSGAAlgorithm(List<GraphDSP> DSPSet, GraphNetwork networkGraph) {
        //3. Create MyOptimization Problem and pass DSP graph set and Network graph to calculate objective functions for each allocaion matrix (chromozom)

        //load problem parameters from file
        String problemName = "NSGA_Classes.MyProblem_ObjectiveFunctions";
        //Problem<IntegerSolution>
        Problem<IntegerSolution> problem = new MyProblem_ObjectiveFunctions(problemName, DSPSet, networkGraph);


        //Then, the crossover, mutation and selection operators are created and configured
        double crossoverProbability = 0.9;
        double crossoverDistributionIndex = 20.0;
        //var
//        CrossoverOperator<DoubleSolution> crossover = new SBXCrossover(crossoverProbability,
//                crossoverDistributionIndex);
        CrossoverOperator<IntegerSolution> crossover = new CustomCrossOver(crossoverProbability,
                crossoverDistributionIndex);


        double mutationProbability = 1.0 / problem.numberOfVariables();
        double mutationDistributionIndex = 20.0;
        MutationOperator<IntegerSolution> mutation = new IntegerPolynomialMutation(mutationProbability,
                mutationDistributionIndex);

        SelectionOperator<List<IntegerSolution>, IntegerSolution> selection = new BinaryTournamentSelection<>(
                new RankingAndCrowdingDistanceComparator<>());

        //After indicating the popultion size, the algorithm can be instantiated by using the NSGAIIBuilder class:
        int populationSize = 100;
        int offspringPopulationSize = populationSize;

        //NSGAII<IntegerSolution> algorithm =
        NSGAII<IntegerSolution> nsgaii = new NSGAIIBuilder<>(
                problem,
                crossover,
                mutation,
                populationSize)
                .setSelectionOperator(selection)
                .setMaxEvaluations(25000)
                .build();

        nsgaii.run();
        //The last step is to run NSGA-II, get the result and store the found solutions
        List<IntegerSolution> population = nsgaii.result();
//        if (population != null && population.size() > 0)
//            printFinalSolutionSet(population);
        return nsgaii;
    }


    //****      Topsis      ******

    public double[][] paretolistToDecisionMatrix(List<IntegerSolution> paretoList, int paretoSize, int numObjectives) { // DM [n][numObjectives]
        if (paretoList.size() == 0) {
            return null;
        }
        double[][] DecisionMatrix = new double[paretoSize][numObjectives];
        for (int i = 0; i < paretoSize; i++) {
            DecisionMatrix[i][0] = paretoList.get(i).objectives()[0];
            DecisionMatrix[i][1] = paretoList.get(i).objectives()[1];
        }
        return DecisionMatrix;
    }

    public double[][] entropy(double[][] DM, int n, int numObj) {
        double v[][] = new double[n][numObj];
        double sum[] = new double[numObj];
        // p[][]
        for (int j = 0; j < numObj; j++) {
            sum[j] = 0;
            for (int i = 0; i < n; i++)
                sum[j] += DM[i][j];
        }
        double p[][] = new double[n][numObj];
        for (int j = 0; j < numObj; j++)
            for (int i = 0; i < n; i++)
                p[i][j] = DM[i][j] / sum[j];

        //entropy
        //ej , dj
        double e[] = new double[numObj];
        double d[] = new double[numObj];
        double sumd = 0;
        for (int j = 0; j < numObj; j++) {
            double s = 0;
            for (int i = 0; i < n; i++) {
                s += (p[i][j]) * (Math.log(p[i][j]));
            }
            e[j] = (-1) * (1 / Math.log(n)) * s;
            d[j] = 1 - e[j];
            sumd += d[j];
        }
        //wj
        double w[] = new double[numObj];
        for (int j = 0; j < numObj; j++) {
            w[j] = d[j] / sumd;
            System.out.println("w[" + j + "] = " + w[j]);
        }


        //vij
        for (int j = 0; j < numObj; j++)
            for (int i = 0; i < n; i++)
                v[i][j] = w[j] * p[i][j];
        return v;
    }

    public int TOPSIS(double[][] v, int n, int numObj) {
        double vP[] = new double[numObj];//A+ min
        double vM[] = new double[numObj];//A- Max
        for (int j = 0; j < numObj; j++) {
            vP[j] = 99999999.9;
            vM[j] = -1;
        }
        for (int j = 0; j < numObj; j++) {
            for (int i = 0; i < n; i++) {
                if (v[i][j] < vP[j])
                    vP[j] = v[i][j];
                if (v[i][j] > vM[j])
                    vM[j] = v[i][j];
            }
        }
        System.out.print("v+[" + 0 + "] = " + vP[0] + "  ,");
        System.out.println("v+[" + 1 + "] = " + vP[1]);
        System.out.print("v-[" + 0 + "] = " + vM[0] + "  ,");
        System.out.println("v-[" + 1 + "] = " + vM[1]);

        //di+ , di-
        double dP[] = new double[n];//distance of ith solution from A+ min
        double dM[] = new double[n];//distance of ith solution from A- Max
        for (int i = 0; i < n; i++) {
            double sumP = 0, sumM = 0;
            for (int j = 0; j < numObj; j++) {
                sumP += Math.pow(v[i][j] - vP[j], 2);
                sumM += Math.pow(v[i][j] - vM[j], 2);
            }
            dP[i] = Math.sqrt(sumP);
            dM[i] = Math.sqrt(sumM);
        }

        //ci
        double c[] = new double[n];//closness distance of ith solution from A+ min
        for (int i = 0; i < n; i++) {
            c[i] = dM[i] / (dP[i] + dM[i]);
        }

        //index of best solution with maximum ci
        int indBest = 0;
        double max = -1;
        for (int i = 0; i < n; i++) {
            if (c[i] > max) {
                max = c[i];
                indBest = i;
            }
        }
        return indBest;
    }

    public boolean[][][] convertChromozomTo3DMatrix(IntegerSolution solution, int AppCount, int NodeCount, int TaskMaxCount) {
        boolean[][][] x = new boolean[AppCount][TaskMaxCount][NodeCount];
        int app, task, node;
        for (int index = 0; index < solution.variables().size(); index++) {
            app = index / TaskMaxCount;
            task = index % TaskMaxCount;
            node = solution.variables().get(index);
            if (node >= 0 && node < NodeCount)
                x[app][task][node] = true;
        }
        return x;
    }


    //RL

    public boolean[][][] convertActionVectorTo3DMatrix(int[] action, List<GraphDSP> DSPSet, int AppCount, int NodeCount, int TaskMaxCount){
        boolean[][][] x = new boolean[AppCount][TaskMaxCount][NodeCount];
        int app, task, node;
        for (int index = 0; index < action.length; index++) {
            app = index / TaskMaxCount;
            task = index % DSPSet.get(app).getVertexes().size();
            node = action[index];
            if (node >= 0 && node < NodeCount)
                x[app][task][node] = true;
        }
        return x;
    }
    public MyMDP InitMyMDP(List<GraphDSP> DSPSet, GraphNetwork networkGraph) {
        //3. Initialize and Train the DQN
        return new MyMDP(DSPSet, networkGraph);
    }
    public QLearningDiscreteDense<Observation> setupDQNAlgorithm(MyMDP mdp) {

        //2. Configure the DQN
        DQNFactoryStdDense.Configuration netConfig = DQNFactoryStdDense.Configuration.builder()
                .l2(0.001)
                .updater(new Adam(0.0005))
                .numHiddenNodes(64)
                .numLayer(3)
                .build();

        QLearning.QLConfiguration rlConfig = new QLearning.QLConfiguration(
                123,    // Random seed
                10000,  // Max step By epoch
                800000, // Max step
                50000,  // Exp replay size
                32,     // Batch size
                500,    // Target update (hard)
                10,     // Num step noop warmup
                0.01,   // Reward scaling
                0.99,   // Gamma
                1.0,    // TD error clipping
                0.1f,   // Min epsilon
                1000,   // Num step for eps greedy anneal
                true    // Double DQN
        );

        //3. Initialize and Train the DQN
        //MyMDP mdp = new MyMDP();

        /*
        QLearningDiscreteDense<Observation> dql = new QLearningDiscreteDense<>(
                mdp,
                netConfig,
                rlConfig
        );

        //dql.train();

        return dql;
        */
        return null;
    }
}
