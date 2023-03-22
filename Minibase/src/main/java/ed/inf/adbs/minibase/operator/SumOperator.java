/**
 * The SumOperator class represents an operator that performs a sum operation on a column of integers from a given child operator.
 */

package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.base.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class SumOperator extends Operator {
    private boolean done = false;
    protected Operator childOperator;
    protected String projectionName;
    protected int sumIndex;
    protected String sumVar;
    protected List<Integer> projectIndices = new ArrayList<>();
    protected List<Integer> output = new ArrayList<>();
    protected HashMap<String, Integer> tupleIndex = new HashMap<>();
    public String var;
    public List<List<Term>> termList = new ArrayList<>();

    /**
     * Constructs a SumOperator object with a child operator and a query head that specifies
     * the name of the projection and the variable name for the sum operation.
     *
     * @param childOperator the child operator to perform the sum operation on
     * @param queryHead the query head that specifies the name of the projection and the
     *                  variable name for the sum operation
     */
    public SumOperator(Operator childOperator, RelationalAtom queryHead) {
        this.childOperator = childOperator;
        List<String> childVarsName = childOperator.getVarsName();
        this.projectionName = queryHead.getName();
        // Create a list of indices of the columns to be projected
        for (int i = 0; i < queryHead.getTerms().size() - 1; i++) {
            String varName = ((Variable) queryHead.getTerms().get(i)).getName();
            int index = childVarsName.indexOf(varName);
            this.projectIndices.add(index);
            this.varsName.add(varName);
        }
        // Get the index of the variable for the sum operation
        this.sumIndex = queryHead.getTerms().size()-1;
        SumAggregate sumTerm = ((SumAggregate) queryHead.getTerms().get(this.sumIndex));
        this.var = sumTerm.getProductTerms().get(0).toString();
        int index = childVarsName.indexOf(this.var);
        this.projectIndices.add(index);
        this.varsName.add(sumTerm.toString());
    }

    /**
     * Resets the operator by resetting its child operator and clearing its output list and tuple index.
     */
    @Override
    public void reset() {
        this.childOperator.reset();
        this.tupleIndex = new HashMap<>();
        this.output = new ArrayList<>();
    }

    /**
     * Retrieves the next tuple resulting from the sum operation. If the sum operation is
     * complete and there are no more tuples to be processed, this method returns null.
     *
     * @return the next tuple resulting from the sum operation, or null if the sum operation
     *         is complete and there are no more tuples to be processed
     */
    @Override
    public Tuple getNextTuple() {
        if (Objects.equals(this.var, "1")) {
            if (done) return null;
            done = true;
            return aggregateColumn();
        } else {
            aggregate();
            if (this.output.size() > 0) {
                int constant = this.output.remove(0);
                List<Term> termList = new ArrayList<>(this.termList.remove(0));
                termList.add(this.sumIndex, new IntegerConstant(constant));
                return new Tuple("SUM("+this.sumVar +")", termList);
            } else {
                return null;
            }
        }
    }

    /**
     * Processes each tuple from the child operator, aggregates the values in the sum variable
     * column, and stores the result in the output list.
     */
    protected void aggregate() {
        Tuple tuple = this.childOperator.getNextTuple();
        while (tuple != null) {
            List<Term> termList = new ArrayList<>();
            for (int index : this.projectIndices) {
                termList.add(tuple.getTerms().get(index));
            }
            Tuple newTuple = new Tuple(this.projectionName, termList);
            IntegerConstant aggTerm = (IntegerConstant) newTuple.getTerms().remove(this.sumIndex);

            String bufferKey = newTuple.getTerms().toString();
            if (this.tupleIndex.containsKey(bufferKey)) {
                int bufferIndex = this.tupleIndex.get(bufferKey);
                int k = this.output.get(bufferIndex);
                k += aggTerm.getValue();
                this.output.set(bufferIndex, k);
            } else {
                this.output.add(aggTerm.getValue());
                this.termList.add(newTuple.getTerms());
                this.tupleIndex.put(bufferKey, this.output.size()-1);
            }
            tuple = this.childOperator.getNextTuple();
        }
    }

    /**
     * Aggregates the number of rows in the child operator by counting the number of tuples returned.
     *
     @return a Tuple with the number of rows as the only term.
     */
    public Tuple aggregateColumn() {
        Tuple tuple = this.childOperator.getNextTuple();
        int k = 0;
        while (tuple != null) {
            k++;
            tuple = this.childOperator.getNextTuple();
        }
        List<Term> termList = new ArrayList<>();
        termList.add(this.sumIndex, new IntegerConstant(k));
        return new Tuple(this.projectionName, termList);
    }
}