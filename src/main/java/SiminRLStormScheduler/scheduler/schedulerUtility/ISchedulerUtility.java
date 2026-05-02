package SiminRLStormScheduler.scheduler.schedulerUtility;

import SiminRLStormScheduler.DSPModels.DSPRequirementModel;
import SiminRLStormScheduler.DSPModels.Graph.GraphDSP;
import SiminRLStormScheduler.DSPModels.PreferencePlacementModel;
import SiminRLStormScheduler.NetworkModels.Graph.GraphNetwork;
import SiminRLStormScheduler.NetworkModels.NetworkTopologyModel;
import SiminRLStormScheduler.scheduler.MyMDP;
import org.apache.storm.scheduler.*;
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense;
import org.deeplearning4j.rl4j.observation.Observation;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAII;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

import java.util.List;

public interface ISchedulerUtility {

    // **** create network and DSP graphs from profile json files addressed in storm.yaml

    // Creat DSP Graphes
    List<DSPRequirementModel> LoadDSPRequirementProfiles(int AppCount, String fileNameDSPRequirement);
    //List<GraphDSP> LoadDSPApplicationsProfile(int AppCount, String fileNameDSPRequirement, String fileNamePreferencePlacement);

    List<PreferencePlacementModel> LoadPreferencePlacementProfiles(int AppCount, String fileNamePreferencePlacement);
    List<GraphDSP> LoadDSPApplicationsProfile(int AppCount, String fileNameDSPRequirement, String fileNamePreferencePlacement);

    // Creat eNetworkGraph
    NetworkTopologyModel LoadNetworkTopologyProfiles(String fileNamePreferencePlacement);
    GraphNetwork CreateNetworkGraph(NetworkTopologyModel networkTopology);



    // **** create network and DSP graphs from storm scheduler models
    List<GraphDSP> updateDPSFromTopologySet(List<GraphDSP> dspSet, Topologies topologies, Cluster cluster);

    NetworkTopologyModel updateNetwrorkFromCluster(NetworkTopologyModel networkTopologyModel, Cluster cluster);

    NSGAII<IntegerSolution> setupNSGAAlgorithm(List<GraphDSP> DSPSet, GraphNetwork networkGraph);


    // TOPSIS
    double[][] paretolistToDecisionMatrix(List<IntegerSolution> paretoList, int n, int numObjectives);// DM [n][numObjectives]
    double[][] entropy(double[][] DM, int n, int numObj);
    int TOPSIS(double[][] v, int n, int numObj);

    boolean[][][] convertActionVectorTo3DMatrix(int[] action, List<GraphDSP> DSPSet, int AppCount, int NodeCount, int TaskMaxCount);
    boolean[][][] convertChromozomTo3DMatrix(IntegerSolution solution, int AppCount, int NodeCount, int TaskMaxCount);

    TopologyDetails findTopologyIdByAppName(List<TopologyDetails> topologies, GraphDSP DSPGraph);
    SupervisorDetails findSupervisorByNodeIndex(List<SupervisorDetails> supervisors, NetworkTopologyModel networkTopologyModel, int nodeIndex);
    List<ExecutorDetails> findExecutorsAssignedToNodeIndex(TopologyDetails topology, boolean[][][] AllocationTaskMatrix, int appIndex, int nodeIndex, int TaskMaxCount);

    //RL-DQN
    MyMDP InitMyMDP(List<GraphDSP> DSPSet, GraphNetwork networkGraph);
    QLearningDiscreteDense<Observation> setupDQNAlgorithm(MyMDP mdp);
}
