package SiminRLStormScheduler.DSPModels.Graph;

/*
 // The class below is from the following reference code
 //  https://www.vogella.com/tutorials/JavaAlgorithmsDijkstra/article.html
 //  under the Eclipse Public License 2.0. ( https://www.eclipse.org/legal/epl-2.0/ ).
 //
 */

//vertex of DSP is an operator (data sources or sinks), each operator (o_i^b) is determined as a tuple o_i^b= {CPU_i^b,Mem_i^b} ∈ V_DSP^b
public class VertexDSP{
    final private int id;//x,y,mapId
    private String name;
    private String type;//source (data node) or sink or transform

    private double CPU_req;//the CPU requirement in MIPS that are needed to execute this operator
    private double Mem_req;//the memory requirement in Bytes that are needed to execute this operator
    private double selectivity;//the memory requirement in Bytes that are needed to execute this operator

    private double miuTime;//the execution time of operator j on reference node, μ^b (j,ref)


    //boolean connectedNode
    public VertexDSP(int id, double muiTime, double CPU, double mem, double selectivity, String type) {
        this.id = id;
        this.name = type + " " + id;
        this.miuTime = muiTime;
        this.CPU_req = CPU;
        this.Mem_req = mem;
        this.selectivity = selectivity;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /*@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }*/

    @Override
    public boolean equals(Object obj) {
        return true;
        /*if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VertexDSP other = (VertexDSP) obj;
        if (id < 0) {
            if (other.id >= 0)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;*/
    }

    @Override
    public String toString() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getMiuTime() {
        return miuTime;
    }

    public void setMiuTime(double miuTime) {
        this.miuTime = miuTime;
    }

    public double getCPU_req() {
        return CPU_req;
    }

    public void setCPU_req(double CPU_req) {
        this.CPU_req = CPU_req;
    }

    public double getMem_req() {
        return Mem_req;
    }

    public void setMem_req(double mem_req) {
        Mem_req = mem_req;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getSelectivity() {
        return selectivity;
    }

    public void setSelectivity(double selectivity) {
        this.selectivity = selectivity;
    }
}