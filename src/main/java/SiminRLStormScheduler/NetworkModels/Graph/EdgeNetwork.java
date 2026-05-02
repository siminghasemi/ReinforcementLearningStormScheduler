package SiminRLStormScheduler.NetworkModels.Graph;

/*
 // The class below uses the following reference code
 //  https://www.vogella.com/tutorials/JavaAlgorithmsDijkstra/article.html
 //  under the Eclipse Public License 2.0. ( https://www.eclipse.org/legal/epl-2.0/ ).
 //
 */
public class EdgeNetwork{
    private final int id;
    private final VertexNetwork source;
    private final VertexNetwork destination;
    private final double bandwidth;//its bandwidth in Byte per second (Bps).


    public EdgeNetwork(int id, VertexNetwork source, VertexNetwork destination, double bandwidth) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.bandwidth = bandwidth;
    }

    public int getId() {
        return id;
    }

    public VertexNetwork getDestination() {
        return destination;
    }

    public VertexNetwork getSource() {
        return source;
    }

    public double getBandwidth() {
        return bandwidth;
    }

    @Override
    public String toString() {
        return "Edge " + source + " " + destination;
    }


}