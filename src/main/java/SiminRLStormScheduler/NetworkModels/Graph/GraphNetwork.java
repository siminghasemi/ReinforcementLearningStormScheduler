package SiminRLStormScheduler.NetworkModels.Graph;

/*
 // The class below is from the following reference code
 //  https://www.vogella.com/tutorials/JavaAlgorithmsDijkstra/article.html
 //  under the Eclipse Public License 2.0. ( https://www.eclipse.org/legal/epl-2.0/ ).
 //
 */
import SiminRLStormScheduler.NetworkModels.NetworkTopologyModel;

import java.util.List;

public class GraphNetwork {
    private final List<VertexNetwork> vertexes;
    private final List<EdgeNetwork> edges;
    private NetworkTopologyModel networkTopology;
    public GraphNetwork(List<VertexNetwork> vertexes, List<EdgeNetwork> edges, NetworkTopologyModel networkTopology) {
        this.vertexes = vertexes;
        this.edges = edges;
        this.networkTopology = networkTopology;
    }

    public List<VertexNetwork> getVertexes() {
        return vertexes;
    }

    public List<EdgeNetwork> getEdges() {
        return edges;
    }

    public NetworkTopologyModel getNetworkTopology() {
        return networkTopology;
    }
}