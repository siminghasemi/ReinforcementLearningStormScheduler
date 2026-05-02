package SiminRLStormScheduler.NetworkModels;

import SiminRLStormScheduler.NetworkModels.Graph.GraphNetwork;
import SiminRLStormScheduler.scheduler.schedulerUtility.ISchedulerUtility;
import SiminRLStormScheduler.scheduler.schedulerUtility.SchedulerUtility;

public class  TestReadNetworkFromJson {

    public static void main(String[] args) {

        //2. Read network graph data from file and fill in the NetworkGraph
//        String fileNameNetworkTopology = "NetworkTopologySample.json"; //DSPRequirementSample1.json
//        NetworkReader networkReader = new NetworkReader();
//        NetworkTopologyModel networkTopology = networkReader.loadNetworkTopologyFromJson(fileNameNetworkTopology);
//        GraphNetwork networkGraph = networkReader.CreateNetworkGraph(networkTopology);


        ISchedulerUtility schedulserUtility = new SchedulerUtility();
//        String fileNameNetworkTopology = /* this.storm_config.get(CONF_NetworkTopologyKey)!=null ? this.storm_config.get(CONF_NetworkTopologyKey).toString() :*/ "NetworkTopologySample.json";
        String fileNameNetworkTopology = "F:/DSP_network_profiles/";
        NetworkTopologyModel networkTopology = schedulserUtility.LoadNetworkTopologyProfiles(fileNameNetworkTopology);
        GraphNetwork networkGraph = schedulserUtility.CreateNetworkGraph(networkTopology);

        //show some test
        System.out.println("vertex 0: " + networkGraph.getVertexes().get(0).toString());
        System.out.println("vertex 1: " + networkGraph.getVertexes().get(1).toString());
        System.out.println("edge (0,0): " + networkGraph.getEdges().get(0).toString());
        System.out.println("edge (0,1): " + networkGraph.getEdges().get(1).toString());
        System.out.println("edge (1,0): " + networkGraph.getEdges().get(5).toString());

    }
}
