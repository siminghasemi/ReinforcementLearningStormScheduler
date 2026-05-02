package SiminRLStormScheduler.DSPModels;

import SiminRLStormScheduler.DSPModels.Graph.EdgeDSP;
import SiminRLStormScheduler.DSPModels.Graph.GraphDSP;
import SiminRLStormScheduler.DSPModels.Graph.VertexDSP;
import com.google.gson.Gson;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class DSPReader {

    public DSPRequirementModel loadDSPApplicationsFromJson(String filePath, String fileName) throws FileNotFoundException {
        //load DSPApplication From Json
//        String basePath = "F:/DSP_network_profiles/";
        System.out.println(" *** loadDSPApplicationsFromJson");

        Gson gson = new Gson();
        DSPRequirementModel model = null;
        try {
            //DSPRequirement = gson.fromJson(Files.newBufferedReader(Paths.get(fileName)), DSPRequirementModel.class);
            model = gson.fromJson(new FileReader(filePath + fileName), DSPRequirementModel.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return model;
    }

    public PreferencePlacementModel loadPreferencePlacementFromJson(String basePath , String fileName) throws FileNotFoundException {
        // load DSPApplication From Json
//        String fileName = "F:/DSP_network_profiles/";
        System.out.println(" *** loadPreferencePlacementFromJson");

        Gson gson = new Gson();
        PreferencePlacementModel model = null;
        try {
            //DSPRequirement = gson.fromJson(Files.newBufferedReader(Paths.get(fileName)), DSPRequirementModel.class);
            model = gson.fromJson(new FileReader(basePath + fileName), PreferencePlacementModel.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return model;
    }

    public GraphDSP CreateDSPGraphFromDSPRequirementModel(DSPRequirementModel dspReq, PreferencePlacementModel preference) {
        // Create DSPGraph From DSPRequirementModel
        System.out.println("*** CreateDSPGraphFromDSPRequirementModel");

        GraphDSP graphDSP = new GraphDSP();

        //Add vertexes
        List<VertexDSP> vertexes = new ArrayList<>();
        for (int i = 0; i < dspReq.numberOfOperator; i++) {
            //VertexDSP vertex = new VertexDSP(i, dspReq.miuTimes[i], dspReq.CPURequirements[i], dspReq.MemRequirements[i], 0.0, "");
            VertexDSP vertex = new VertexDSP(i, dspReq.miuTimes[0], dspReq.CPURequirements[0], dspReq.MemRequirements[0], 0.0, "");
            vertexes.add(vertex);
            graphDSP.addVertex(i);
        }

        //Add Edges
        List<EdgeDSP> edges = new ArrayList<>();
        for (int i = 0; i < dspReq.numberOfOperator; i++) {
            VertexDSP source = vertexes.get(i);
            for (int j = 0; j < dspReq.numberOfOperator; j++) {
                VertexDSP destination = vertexes.get(j);
                if (dspReq.lambdas[i][j] != 0) {//directed graph, only directed links between operator that has dataflow
                    int edgeId = (i + 1) * (j + 1);
                    //EdgeDSP edge = new EdgeDSP(edgeId, source, destination, dspReq.lambdas[i][j], dspReq.eventSizes[i][j]);
                    EdgeDSP edge = new EdgeDSP(edgeId, source, destination, dspReq.lambdas[0][0], dspReq.eventSizes[0][0]);
                    edges.add(edge);
                    graphDSP.addEdge(i, j);
                }
            }
        }

        //Creat path set of this graph based on node type (source and sink) and there is link between operators
        //must the operator 0 be first source and last operator be the last sink
        List<Set<Integer>> paths = find_paths(graphDSP, 0, dspReq.numberOfOperator - 1);

        //GraphDSP DSPGraph = new GraphDSP(vertexes, edges, pathes, preference, dspReq);
        graphDSP.setVertexes(vertexes);
        graphDSP.setEdges(edges);
        graphDSP.setPathes(paths);
        graphDSP.setPreferencePlacement(preference);
        graphDSP.setDSPRequirement(dspReq);

        return graphDSP;
    }

    public List<Set<Integer>> find_paths(GraphDSP graph, Integer start, Integer end) {
        Set<Integer> visited = new HashSet<>();
        Set<Integer> path = new HashSet<>();
        return dfs(graph, start, end, path, visited);
    }

    private List<Set<Integer>> dfs(GraphDSP graph, Integer start, Integer end, Set<Integer> path, Set<Integer> visited) {
        path.add(start);
        if (visited.contains(start))
            return new ArrayList<>();

        System.out.print(start + " ");
        visited.add(start);

        if (Objects.equals(start, end)) {
            List<Set<Integer>> paths = new ArrayList<>();
            paths.add(path);
            return paths;
        }

        List<Set<Integer>> paths = new ArrayList<>();
        List<Integer> neighbors = graph.getAdjacencyList().get(start);
        if (neighbors != null) {
            for (Integer neighbor : neighbors) {
                if (!path.contains(neighbor)) {
                    List<Set<Integer>> new_paths = dfs(graph, neighbor, end, path, visited);
                    for (Set<Integer> p : new_paths) {
                        paths.add(p);
                    }
                }
            }
        }
        return paths;
    }

}
