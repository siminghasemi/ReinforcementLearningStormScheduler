package SiminRLStormScheduler.DSPModels;

import SiminRLStormScheduler.DSPModels.Graph.GraphDSP;
import SiminRLStormScheduler.scheduler.schedulerUtility.ISchedulerUtility;
import SiminRLStormScheduler.scheduler.schedulerUtility.SchedulerUtility;

import java.util.List;

public class TestGraph_FirstDepthSearch {

    public static void main(String[] args) {

        int AppCount = 2;// topologies.getAllIds().size(); //= 3;//topologyset.size();

        ISchedulerUtility schedulserUtility = new SchedulerUtility();

        //1. Load DSP and network models from profiles json files

        // **** create network and DSP graphs from profile json files addressed in storm.yaml
        System.out.println("*** Load DSPApplicationsFromJson and PreferencePlacement for DSP profiling from DSPRequirementSample1.json and DSPRequirementSample1.json");

        String fileNameDSPRequirement ="F:/DSP_network_profiles/";// /*this.storm_config.get(CONF_ApplicationDSPRequirementKey) !=null ? this.storm_config.get(CONF_ApplicationDSPRequirementKey).toString() :*/ "DSPRequirementSample";
        String fileNamePreferencePlacement = "F:/DSP_network_profiles/";///*this.storm_config.get(CONF_ApplicationDSPPreferenceKey) != null ? this.storm_config.get(CONF_ApplicationDSPPreferenceKey).toString() : */ "PreferencePlacementSample";
        String fileNameNetworkTopology = "F:/DSP_network_profiles/";///* this.storm_config.get(CONF_NetworkTopologyKey)!=null ? this.storm_config.get(CONF_NetworkTopologyKey).toString() :*/ "NetworkTopologySample.json";

        List<DSPRequirementModel> DSPRequirements = schedulserUtility.LoadDSPRequirementProfiles(AppCount, fileNameDSPRequirement);
        List<PreferencePlacementModel> PreferencePlacements = schedulserUtility.LoadPreferencePlacementProfiles(AppCount, fileNamePreferencePlacement);
        List<GraphDSP> DSPSet = schedulserUtility.LoadDSPApplicationsProfile(AppCount, fileNameDSPRequirement, fileNamePreferencePlacement);

        for (int i = 0; i < DSPSet.size(); i++) {
            System.out.println("*** DSPSet " + i);
            System.out.println("vertex 0: " + DSPSet.get(i).getVertexes().get(0).toString());
            System.out.println("vertex 1: " + DSPSet.get(i).getVertexes().get(1).toString());
            System.out.println("edge (" + i + "," + 0 + "): " + DSPSet.get(i).getEdges().get(0).toString());
            System.out.println("edge (" + i + "," + 1 + "): " + DSPSet.get(i).getEdges().get(1).toString());
            System.out.println("edge (" + i + "," + 2 + "): " + DSPSet.get(i).getEdges().get(2).toString());
        }


        //GraphDSP graph = new GraphDSP();
        /*
        // Adding vertices
        graph.addVertex("A");
        graph.addVertex("B");
        graph.addVertex("C");
        graph.addVertex("D");
        graph.addVertex("E");

        // Adding edges
        graph.addEdge("A", "B");
        graph.addEdge("A", "D");
        graph.addEdge("B", "C");
        graph.addEdge("D", "E");

        // Perform DFS starting from vertex A
        System.out.println("Depth-First Search starting from vertex A:");
        graph.dfs("A");
        */

    }
}
