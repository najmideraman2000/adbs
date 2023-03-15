package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.base.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class SumOperator extends Operator {
    private boolean done = false;
    protected Operator child;
    protected String projectionName;
    protected int aggIndex;
    protected String aggVariable;
    protected List<Integer> projectIndices = new ArrayList<>();
    protected List<Integer> outputBuffer = new ArrayList<>();
    protected HashMap<String, Integer> tuple2BufferIndex = new HashMap<>();
    public String aggVar;
    public List<List<Term>> termList = new ArrayList<>();

    public SumOperator(Operator childOperator, RelationalAtom queryHead) {
        this.child = childOperator;
        List<String> childVariableMask = childOperator.getVarsName(); // the variableMask before projection
        this.projectionName = queryHead.getName();
        for (int i = 0; i < queryHead.getTerms().size() - 1; i++) {
            String varName = ((Variable) queryHead.getTerms().get(i)).getName();
            int idx = childVariableMask.indexOf(varName);
            this.projectIndices.add(idx);
            this.varsName.add(varName); // this.variableMask will record the variable positions after projection
        }
        // process the last aggregation term:
        this.aggIndex = queryHead.getTerms().size()-1;
        SumAggregate avgTerm = ((SumAggregate) queryHead.getTerms().get(this.aggIndex));
        this.aggVar = avgTerm.getProductTerms().get(0).toString();
        System.out.println(childVariableMask);
        int idx = childVariableMask.indexOf(this.aggVar);
        this.projectIndices.add(idx);
        this.varsName.add(avgTerm.toString());
    }

    @Override
    public void reset() {
        this.child.reset();
        this.tuple2BufferIndex = new HashMap<>();
        this.outputBuffer = new ArrayList<>();
    }

    @Override
    public Tuple getNextTuple() {
        if (Objects.equals(this.aggVar, "1")) {
            if (done) return null;
            done = true;
            return aggregateColumn();
        } else {
            aggregate();

            if (this.outputBuffer.size() > 0) {
                int cons = this.outputBuffer.remove(0);
                List<Term> termList = new ArrayList<>(this.termList.remove(0));
                termList.add(this.aggIndex, new IntegerConstant(cons));
                return new Tuple("SUM("+this.aggVariable+")", termList);
//                return this.outputBuffer.remove(0).getSumTuple();
            } else {
                return null;
            }
        }
    }

    protected void aggregate() {
        Tuple childOutput = this.child.getNextTuple();
        while (childOutput != null) {
            // extract the term list and remove the aggregation term
            List<Term> termList = new ArrayList<>();
            System.out.println(this.projectIndices.size());
            for (int pi : this.projectIndices) {
                System.out.println(pi);
                termList.add(childOutput.getTerms().get(pi));
            }
            Tuple newTuple = new Tuple(this.projectionName, termList);
            IntegerConstant aggTerm = (IntegerConstant) newTuple.getTerms().remove(this.aggIndex);

            // convert the term list (without aggregation term) into string, acting as a key for hashmap
            String bufferKey = newTuple.getTerms().toString();
            if (this.tuple2BufferIndex.containsKey(bufferKey)) {
                // GROUP operation, accumulate the aggregation term
                int bufferIndex = this.tuple2BufferIndex.get(bufferKey);
                int k = this.outputBuffer.get(bufferIndex);
                k += aggTerm.getValue();
                this.outputBuffer.set(bufferIndex, k);
            } else {
                this.outputBuffer.add(aggTerm.getValue());
                this.termList.add(newTuple.getTerms());
                this.tuple2BufferIndex.put(bufferKey, this.outputBuffer.size()-1);
            }
            childOutput = this.child.getNextTuple();
        }
    }

    public Tuple aggregateColumn() {
        Tuple childOutput = this.child.getNextTuple();
        int k = 0;
        while (childOutput != null) {
            k++;
            childOutput = this.child.getNextTuple();
        }
        List<Term> termList = new ArrayList<>();
        termList.add(this.aggIndex, new IntegerConstant(k));
        return new Tuple("HHH", termList);
    }
}