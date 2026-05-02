package SiminRLStormScheduler.NetworkModels.Graph;

/*
 // The class below is from the following reference code
 //  https://www.vogella.com/tutorials/JavaAlgorithmsDijkstra/article.html
 //  under the Eclipse Public License 2.0. ( https://www.eclipse.org/legal/epl-2.0/ ).
 //
 */
public class VertexNetwork {
    final private int id;//x,y,mapId
    private String name;
    private String nodeType;
    private double CPU;//processing capability in Millions of Instructions per Second (MIPS)
    private double Mem;//the available memory in Bytes
    private double speed;//is the processing speed of node u compared to a reference processor

    public VertexNetwork(int id, double CPU, double mem, double speed) {
        this.id = id;
        this.name = "Node " + id;
        this.CPU = CPU;
        this.Mem = mem;
        this.speed = speed;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNodeType() {
        return nodeType;
    }

    public double getCPU() {
        return CPU;
    }

    public double getMem() {
        return Mem;
    }

    public double getSpeed() {
        return speed;
    }

    /*
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VertexNetwork other = (VertexNetwork) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }*/

    @Override
    public String toString() {
        return name;
    }


}