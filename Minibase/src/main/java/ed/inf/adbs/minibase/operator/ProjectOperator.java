/**
 * ProjectOperator class represents the operator to perform projection on the child operator
 */

package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.base.*;

import java.util.ArrayList;
import java.util.List;

public class ProjectOperator extends Operator {
    private final Operator childOperator;
    private final String projectionName;
    private final List<Integer> projectIndices = new ArrayList<>();
    private List<String> buffer = new ArrayList<>();

    /**
     * Creates a new ProjectOperator instance.
     *
     * @param childOperator the child operator to be projected
     * @param queryHead the relational atom representing the projection
     */
    public ProjectOperator(Operator childOperator, RelationalAtom queryHead) {
        this.childOperator = childOperator;
        List<String> childVariableMask = childOperator.getVarsName(); // the variableMask before projection
        this.projectionName = queryHead.getName();
        for (int i = 0; i < queryHead.getTerms().size(); i++) {
            String varName = ((Variable) queryHead.getTerms().get(i)).getName();
            int idx = childVariableMask.indexOf(varName);
            this.projectIndices.add(idx);
            this.varsName.add(varName);
        }
    }

    /**
     * Resets the operator to its initial state.
     */
    @Override
    public void reset() {
        this.childOperator.reset();
        this.buffer = new ArrayList<>();
    }

    /**
     * Returns the next projected tuple.
     *
     * @return the next projected tuple or null if there is no more tuple to project
     */
    @Override
    public Tuple getNextTuple() {
        Tuple nextTuple = this.childOperator.getNextTuple();
        while (nextTuple != null) {
            List<Term> terms = new ArrayList<>();
            for (int index : this.projectIndices) {
                terms.add(nextTuple.getTerms().get(index));
            }
            Tuple newTuple = new Tuple(this.projectionName, terms);
            if (!this.buffer.contains(newTuple.toString())) {
                this.buffer.add(newTuple.toString());
                return newTuple;
            }
            nextTuple = this.childOperator.getNextTuple();
        }
        return null;
    }
}