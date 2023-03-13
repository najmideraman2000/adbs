package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.base.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Filter the tuples according to a series of conditions provided by {@link ComparisonAtom}.
 * This operator will filter the output of its child operator,
 * only the tuples which satisfy all the select conditions can pass this operator.
 * Notice: (1) The check of conditions are implemented in a separate class: {@link SelectCondition},
 *         which provides a {@link SelectCondition#check(Tuple)} method to check whether a tuple satisfy a condition.
 *         (2) The input {@code ComparisonAtom} list will be converted into a {@code SelectCondition} list,
 *         then the select conditions are checked by travelling through this list and calling the {@code check()} method.
 * @see SelectCondition
 */
public class SelectOperator extends Operator {

    private Operator child;
    private List<SelectCondition> conditions = new ArrayList<>();

    /**
     * Initialisation. Copy the variable mask from child operator, since it will not be changed in select operation.
     * @param child The child operator.
     * @param compAtomList a list of SELECT conditions, as a list of {@link ComparisonAtom} instances.
     */
    public SelectOperator(Operator child, List<ComparisonAtom> compAtomList) {
        this.child = child;
        this.variableMask = this.child.getVariableMask();

        for (ComparisonAtom comparisonAtom : compAtomList) {
            this.conditions.add(new SelectCondition(comparisonAtom, this.variableMask));
        }
    }

    /**
     * Reset the operator status.
     * No state in this operator needs to be reset, but its child operator needs to be reset.
     */
    @Override
    public void reset() {
        this.child.reset();
    }

    /**
     * Get and return the next tuple that satisfies the SELECT conditions.
     * This method iteratively fetch next tuple from its child operator
     * until a fetched tuple satisfies all the SELECT conditions (and this tuple will be returned).
     * In this function, the check of SELECT conditions are encapsulated in the class {@link SelectCondition}.
     * @return the next {@link Tuple}, or {@code null} if the child operator reaches the end
     */
    @Override
    public Tuple getNextTuple() {
        Tuple nextTuple = this.child.getNextTuple();
        while (nextTuple != null) {
            boolean pass = true;
            for (SelectCondition condition : this.conditions) {
                if (!condition.check(nextTuple)) {
                    pass = false;
                    break;
                }
            }
            if (pass) {
                return nextTuple;
            }
            else
                nextTuple = this.child.getNextTuple();
        }
        return null;
    }
}