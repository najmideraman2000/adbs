package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.base.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the project operation: select some or all columns and may re-order the columns from the child relation.
 * Notice: if the query head contains aggregation operations,
 */
public class ProjectOperator extends Operator {

    private Operator child;
    private String projectionName;

    private List<Integer> projectIndices = new ArrayList<>();
    // a map from fields in new relation to child relation, indicates where to find the projection column in child tuple
    // e.g. projectIndices[0] = 2 means the first column after projection is the third column in original relation

    private List<String> reportBuffer = new ArrayList<>();
    // a buffer of all the reported tuples, used for duplication check

    /**
     * Initialise. Extract the target variable mask from query head,
     * build a mapping relation from indices after projection to corresponding indices before projection.
     * @param childOperator the child operator.
     * @param queryHead the query head, whose term list indicates the projection requirements.
     */
    public ProjectOperator(Operator childOperator, RelationalAtom queryHead) {
        this.child = childOperator;
        List<String> childVariableMask = childOperator.getVariableMask(); // the variableMask before projection
        this.projectionName = queryHead.getName();
        // for each variable in the relational atom of query head, find the corresponding position in child relation,
        // and build a mapping relation from the target index (after projection) to original index
        for (int i = 0; i < queryHead.getTerms().size(); i++) {
            String varName = ((Variable) queryHead.getTerms().get(i)).getName();
            int idx = childVariableMask.indexOf(varName);
            this.projectIndices.add(idx);
            this.variableMask.add(varName); // this.variableMask will record the variable positions after projection
        }
//        System.out.println(childVariableMask + "- -> " + this.variableMask + "(" + this.projectIndices + ")");
    }

    /**
     * Reset the child operator, and also clean the report buffer.
     */
    @Override
    public void reset() {
        this.child.reset();
        this.reportBuffer = new ArrayList<>();
    }

    /**
     * Get the next output tuple from child operator,
     * use {@code this.projectIndices} to map the original tuple to the projected tuple,
     * check duplication using {@code this.reportBuffer} before returning.
     * @return the next projected tuple (without duplication).
     */
    @Override
    public Tuple getNextTuple() {
        Tuple childOutput = this.child.getNextTuple();
        while (childOutput != null) {
            // use the map to construct projected tuple from original tuple by aligning indices
            List<Term> termList = new ArrayList<>();
            for (int pi : this.projectIndices) {
                termList.add(childOutput.getTerms().get(pi));
            }
            // construct a new tuple and checks duplication
            Tuple newTuple = new Tuple(this.projectionName, termList);
            if (!this.reportBuffer.contains(newTuple.toString())) {
                this.reportBuffer.add(newTuple.toString());
                return newTuple;
            }
            // if this new tuple duplicates with some previous reported tuple,
            // iterate to the next child output tuple
            childOutput = this.child.getNextTuple();
        }
        return null;
    }
}