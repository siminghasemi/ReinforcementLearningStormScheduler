package SiminRLStormScheduler.DSPModels.Graph;

/*
 // The class below is from the following reference code
 //  https://www.vogella.com/tutorials/JavaAlgorithmsDijkstra/article.html
 //  under the Eclipse Public License 2.0. ( https://www.eclipse.org/legal/epl-2.0/ ).
 //
 */


import SiminRLStormScheduler.DSPModels.DSPRequirementModel;
import SiminRLStormScheduler.DSPModels.PreferencePlacementModel;

import java.util.*;

//The execution graph of bth DSP application is represented as a DAG, G_DSP^b= (V_DSP^b , E_DSP^b) where
// V_DSP^b is the set of operators (as well as data sources or sinks) and
// E_DSP^b is the set of data streams between operators.
public class GraphDSP {
    private List<VertexDSP> Vertexes;
    private List<EdgeDSP> edges;
    private PreferencePlacementModel preferencePlacement;
    private DSPRequirementModel DSPRequirement;
    private List<Set<Integer>> pathes;//private List<List<EdgeDSP>> pathes;
    private Map<Integer, List<Integer>> adjacencyList;

    public GraphDSP() {
        this.adjacencyList = new HashMap<>();
    }

    public void addVertex(Integer vertex) {
        adjacencyList.put(vertex, new ArrayList<>());
    }

    public void addEdge(Integer source, Integer destination) {
        adjacencyList.get(source).add(destination);
        // If your graph is undirected, you may want to add the reverse edge as well
        // adjacencyList.get(destination).add(source);
    }

    public List<Integer> getNeighbors(Integer vertex) {
        return adjacencyList.get(vertex);
    }
    /*
    public GraphDSP(List<VertexDSP> Vertexes, List<EdgeDSP> edges,
                    List<Set<Integer>> pathes,
                    PreferencePlacementModel preferencePlacement,
                    DSPRequirementModel DSPRequirement) {
        this.Vertexes = Vertexes;
        this.edges = edges;
        this.pathes = pathes;
        this.preferencePlacement = preferencePlacement;
        this.DSPRequirement = DSPRequirement;
    }*/

    public List<VertexDSP> getVertexes() {
        return Vertexes;
    }

    public void setVertexes(List<VertexDSP> vertexes) {
        Vertexes = vertexes;
    }

    public List<EdgeDSP> getEdges() {
        return edges;
    }

    public void setEdges(List<EdgeDSP> edges) {
        this.edges = edges;
    }

    public List<Set<Integer>> getPathes() {
        return pathes;
    }

    public void setPathes(List<Set<Integer>> pathes) {
        this.pathes = pathes;
    }

    public PreferencePlacementModel getPreferencePlacement() {
        return preferencePlacement;
    }

    public void setPreferencePlacement(PreferencePlacementModel preferencePlacement) {
        this.preferencePlacement = preferencePlacement;
    }

    public DSPRequirementModel getDSPRequirement() {
        return DSPRequirement;
    }

    public void setDSPRequirement(DSPRequirementModel DSPRequirement) {
        this.DSPRequirement = DSPRequirement;
    }

    public Map<Integer, List<Integer>> getAdjacencyList() {
        return adjacencyList;
    }

    public void setAdjacencyList(Map<Integer, List<Integer>> adjacencyList) {
        this.adjacencyList = adjacencyList;
    }
}