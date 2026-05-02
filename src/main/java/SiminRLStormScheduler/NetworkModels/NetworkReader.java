package SiminRLStormScheduler.NetworkModels;

import SiminRLStormScheduler.NetworkModels.Graph.EdgeNetwork;
import SiminRLStormScheduler.NetworkModels.Graph.GraphNetwork;
import SiminRLStormScheduler.NetworkModels.Graph.VertexNetwork;
import com.google.gson.Gson;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class NetworkReader {

    public NetworkTopologyModel loadNetworkTopologyFromJson(String basePath, String fileName) {
        //String basePath = "F:\\PhD Main Project\\Implementation\\NSGA\\OptimizationProblem\\src\\main\\java\\DSPModels\\";
//        String basePath = "F:/PhD Main Project/Implementation/NSGA/OptimizationProblem/src/main/java/SiminStormScheduler/scheduler/schedulerUtility/";
//        String basePath = "F:/DSP_network_profiles/";
        System.out.println(" *** loadNetworkTopologyFromJson");

        Gson gson = new Gson();
        NetworkTopologyModel model = null;
        try {
            //DSPRequirement = gson.fromJson(Files.newBufferedReader(Paths.get(fileName)), DSPRequirementModel.class);
            model = gson.fromJson(new FileReader(basePath + fileName), NetworkTopologyModel.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return model;
    }

    public GraphNetwork CreateNetworkGraph(NetworkTopologyModel network) {

        //Create NetworkGraph From NetworkTopologyModel

        List<VertexNetwork> vertexes = new ArrayList<>();
        for (int i = 0; i < network.NumberOfNodes; i++) {
            VertexNetwork vertex = new VertexNetwork(i,
                    network.CPUResources[i],
                    network.MemResources[i],
                    network.Speeds[i]);
            vertexes.add(vertex);
        }

        List<EdgeNetwork> edges = new ArrayList<>();
        for (int i = 0; i < network.NumberOfNodes; i++) {
            VertexNetwork source = vertexes.get(i);
            for (int j = 0; j < network.NumberOfNodes; j++) {
                VertexNetwork destination = vertexes.get(j);
                int edgeId = (i + 1) * (j + 1);
                EdgeNetwork edge = new EdgeNetwork(edgeId, source, destination, network.Bandwidths[i][j]);
                edges.add(edge);
            }
        }

        GraphNetwork networkGraph = new GraphNetwork(vertexes, edges, network);

        //calculate network the shortest paths between every node u,v based on floyd algorithm based on bandwidth matrix for graph

        //todo: FloydWarshall_shortestPaths(network.Bandwidths);

        /*
        for (int u = 0; u < network.NumberOfNodes; u++) {
            for (int v = 0; v < network.NumberOfNodes; v++) {
                if( u == v)
                    continue;
                set networkpath u,v =
            }
        }*/

        return networkGraph;
    }
/*
    public void FloydWarshall_shortestPaths(double[][] graph) {
        int n = graph.length;
        double[][] dist = new double[n][n];

        // Initialize the distance matrix
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                dist[i][j] = graph[i][j];
            }
        }

        // Find the shortest paths
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (dist[i][k] != INF && dist[k][j] != INF && dist[i][k] + dist[k][j] < dist[i][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j];
                    }
                }
            }
        }

        // Print the shortest paths
        printShortestPaths(dist);

    }
    private void printShortestPaths(int[][] dist) {
        int n = dist.length;

        System.out.println("Shortest Paths:");
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (dist[i][j] == INF) {
                    System.out.print("INF\t");
                } else {
                    System.out.print(dist[i][j] + "\t");
                }
            }
            System.out.println();
        }
    }*/
}
