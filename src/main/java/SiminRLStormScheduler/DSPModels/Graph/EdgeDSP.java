package SiminRLStormScheduler.DSPModels.Graph;

/*
 // The class below uses the following reference code
 //  https://www.vogella.com/tutorials/JavaAlgorithmsDijkstra/article.html
 //  under the Eclipse Public License 2.0. ( https://www.eclipse.org/legal/epl-2.0/ ).
 //
 */

//each data stream, e^b (i,j) ∈ E_DSP^b, between operator i and operator j, is specified by e^b (i,j)={λ^b (i,j),〖evtSize〗^b (i,j)},
public class EdgeDSP{
    private final int id;
    private final VertexDSP source;
    private final VertexDSP destination;
    private final double lambda;//where λ^b (i,j) is the  event arrival rate of operator j (data steam rate) in tuples/second
    private final double eventSize;//evtSize^b (i,j) is the average size of data/events in Bytes, i.e., the size of operator i output data and consequently the size of operator j input data.

    public EdgeDSP(int id, VertexDSP source, VertexDSP destination, double lambda, double eventSize) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.lambda = lambda;
        this.eventSize = eventSize;
    }

    public int getId() {
        return id;
    }

    public VertexDSP getDestination() {
        return destination;
    }

    public VertexDSP getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "DataFlow " + source + " to " + destination;
    }

    public double getLambda() {
        return lambda;
    }

    public double getEventSize() {
        return eventSize;
    }
}