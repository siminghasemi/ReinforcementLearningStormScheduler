package SiminRLStormScheduler.scheduler;

import SiminRLStormScheduler.DSPModels.DSPReader;
import SiminRLStormScheduler.DSPModels.DSPRequirementModel;
import SiminRLStormScheduler.DSPModels.Graph.GraphDSP;
import SiminRLStormScheduler.DSPModels.PreferencePlacementModel;
import SiminRLStormScheduler.NetworkModels.Graph.GraphNetwork;
import SiminRLStormScheduler.NetworkModels.NetworkTopologyModel;
import SiminRLStormScheduler.scheduler.schedulerUtility.ISchedulerUtility;
import SiminRLStormScheduler.scheduler.schedulerUtility.SchedulerUtility;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAII;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class TestSiminRLContextAwareScheduler {

    public static void main(String[] args) {

        //*****************************   SiminRLContextAwareScheduler   *****************************


        System.out.println("*** Load DSP and network data");

        ISchedulerUtility schedulserUtility = new SchedulerUtility();

        int AppCount = 3;

        // 1. create network and DSP graphs from profile json files addressed in storm.yaml
        System.out.println("*** TestSiminScheduler, Load DSPApplicationsFromJson and PreferencePlacement for DSP profiling from DSPRequirementSample1.json and DSPRequirementSample1.json");

        String fileNameDSPRequirement = "F:/DSP_network_profiles/";// /*this.storm_config.get(CONF_ApplicationDSPRequirementKey) !=null ? this.storm_config.get(CONF_ApplicationDSPRequirementKey).toString() :*/ "DSPRequirementSample";
        String fileNamePreferencePlacement = "F:/DSP_network_profiles/";///*this.storm_config.get(CONF_ApplicationDSPPreferenceKey) != null ? this.storm_config.get(CONF_ApplicationDSPPreferenceKey).toString() :*/ "PreferencePlacementSample";
        String fileNameNetworkTopology = "F:/DSP_network_profiles/";///*this.storm_config.get(CONF_NetworkTopologyKey)!=null ? this.storm_config.get(CONF_NetworkTopologyKey).toString() :*/ "NetworkTopologySample.json";

        //3. Create DSP and Network Graph from their models
        NetworkTopologyModel networkTopology = schedulserUtility.LoadNetworkTopologyProfiles(fileNameNetworkTopology);
        GraphNetwork networkGraph = schedulserUtility.CreateNetworkGraph(networkTopology);

        List<GraphDSP> DSPSet = schedulserUtility.LoadDSPApplicationsProfile(AppCount, fileNameDSPRequirement, fileNamePreferencePlacement);


        SiminRLContextAwareScheduler scheduler = new SiminRLContextAwareScheduler();

        int NodeCount = networkGraph.getVertexes().size();
        int appCount = DSPSet.size();
        int TaskMaxCount = scheduler.calculateTaskMaxCount(DSPSet);

        System.out.println("NodeCount = " + NodeCount + ", appCount = " + appCount + ", TaskMaxCount = " + TaskMaxCount);

        scheduler.prepareDQN(DSPSet, networkGraph, TaskMaxCount);


        //scheduler.schedule(null, null);
        List episodeRewards = new ArrayList();
        List episodeMSEs = new ArrayList();
        int windowSize = 20;

        int MaxStep = 500;  //10000,  Max step By epoch
        for (int step = 0; step < MaxStep; step++) {

            DSPSet = createRandomDSPSet(AppCount, TaskMaxCount);
            //4.
            MyObservation obs = scheduler.buildObservationFromCluster(DSPSet, networkGraph, TaskMaxCount);

            //5.
            int[] action = scheduler.selectAction(scheduler.dqn, obs, scheduler.epsilon, DSPSet, NodeCount, TaskMaxCount);

            //printActionVector(action, step);

            //6. convert allocation 3-D matrix to StormAssignment
            boolean[][][] AllocationTaskMatrix = schedulserUtility.convertActionVectorTo3DMatrix(action, DSPSet, appCount, NodeCount, TaskMaxCount);

            //7. storm executor to worker_slot assignment
            //assignTasksToWorkerSlots(cluster, AllocationTaskMatrix, DSPSet, networkGraph, NodeCount, TaskMaxCount);

            //8.
            double reward = scheduler.computeReward(AllocationTaskMatrix, DSPSet, networkGraph);

            episodeRewards.add(reward);

            System.out.println(reward);

            double mse = computeMSE(episodeRewards, windowSize);
            episodeMSEs.add(mse);

            //9.
            MyObservation nextObs = scheduler.buildObservationFromClusterAfterAssingment(DSPSet, networkGraph, action, TaskMaxCount, AllocationTaskMatrix);

            //10.
            Experience exp = new Experience(obs, action, reward, nextObs, false);
            scheduler.replayBuffer.add(exp);

            //11.
            if (scheduler.replayBuffer.size() > scheduler.batchSize) {
                List<Experience> batch = scheduler.replayBuffer.sample(scheduler.batchSize);
                scheduler.trainBatch(batch, scheduler.dqn, scheduler.gamma);
            }

        }

        exportRewardsToCSV("reward_log.csv", episodeRewards);
        exportRewardsToCSV("MSE_log.csv", episodeMSEs);

    }

    private static void printActionVector(int[] action, int step) {
        System.out.print("step = " + step + ", action[" + action.length + "] = {");
        for (int i = 0; i < action.length; i++) {
            System.out.print(action[i] + ",");
        }
        System.out.println("}");
    }

    private static void exportRewardsToCSV(String filename, List episodeRewards) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Episode,TotalReward");
            for (int i = 0; i < episodeRewards.size(); i++) {
                writer.println(i + "," + episodeRewards.get(i));
            }
            System.out.println("Reward log written to " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double computeMSE_1(List<Double> episodeRewards, int windowSize) {
        if (episodeRewards.size() < windowSize + 1) {
            return Double.NaN; // not enough data yet
        }
        double mse = 0.0;
        for (int i = episodeRewards.size() - windowSize; i < episodeRewards.size(); i++) {
            double diff = episodeRewards.get(i) - episodeRewards.get(i - 1);
            mse += diff * diff;
        }
        return mse / windowSize;
    }

    private static double computeMSE(List<Double> episodeRewards, int windowSize) {
        if (episodeRewards.size() < windowSize + 1) {
            return Double.NaN; // not enough data yet
        }
        double mse = 0.0;
        for (int i = 1; i < episodeRewards.size(); i++) {
            double diff = episodeRewards.get(i) - episodeRewards.get(i - 1);
            mse += diff * diff;
        }
        return mse / episodeRewards.size();
    }

    private static List<GraphDSP> createRandomDSPSet(int AppCount, int TaskMaxCount) {

        List<GraphDSP> DSPSet = new ArrayList<>();

        for (int i = 0; i < AppCount; i++) {

            DSPRequirementModel dSPReq = new DSPRequirementModel();
            dSPReq.numberOfOperator = new Random().nextInt(TaskMaxCount) + 1;
            dSPReq.latencyThreshold = new Random().nextDouble() * 3000;

            dSPReq.CPURequirements = new double[dSPReq.numberOfOperator];
            dSPReq.MemRequirements = new double[dSPReq.numberOfOperator];
            dSPReq.miuTimes = new double[dSPReq.numberOfOperator];
            dSPReq.lambdas = new double[dSPReq.numberOfOperator][dSPReq.numberOfOperator];
            dSPReq.eventSizes = new double[dSPReq.numberOfOperator][dSPReq.numberOfOperator];

            for (int j = 0; j < dSPReq.numberOfOperator; j++) {
                dSPReq.CPURequirements[j] = new Random().nextDouble() * 5 + 1;
                dSPReq.MemRequirements[j] = new Random().nextDouble() * 300 + 1;
                dSPReq.miuTimes[j] = 150 + new Random().nextDouble() * 100;//new Random().nextDouble() * 100 + 1;

                for (int k = 0; k < dSPReq.numberOfOperator; k++) {
                    dSPReq.lambdas[j][k] = new Random().nextDouble() * 5 + 1;//10
                    dSPReq.eventSizes[j][k] = new Random().nextDouble() * 500 + 1;
                }
            }

            PreferencePlacementModel preference = new PreferencePlacementModel();
            preference.numberOfNodes = 3;
            preference.numberOfOperators = dSPReq.numberOfOperator;
            preference.preference_threshold = new Random().nextInt(TaskMaxCount) + 1;

            preference.allocationPreference = new int[dSPReq.numberOfOperator][preference.numberOfNodes];

            for (int j = 0; j < dSPReq.numberOfOperator; j++) {
                for (int k = 0; k < preference.numberOfNodes; k++) {
                    preference.allocationPreference[j][k] = new Random().nextInt(1);
                }
            }

            DSPReader dspReader = new DSPReader();
            GraphDSP dspGraph = dspReader.CreateDSPGraphFromDSPRequirementModel(dSPReq, preference);
            DSPSet.add(dspGraph);

        }
        return DSPSet;
    }

}
