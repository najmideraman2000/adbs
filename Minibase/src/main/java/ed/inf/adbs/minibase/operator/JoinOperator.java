/**
 * The JoinOperator class represents the join operator that merges tuples from two child operators.
 * It inherits the Operator class.
 */

package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.base.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JoinOperator extends Operator {
    private final Operator leftChildOperator;
    private final Operator rightChildOperator;
    private final HashMap<Integer, Integer> joinConditionIndices = new HashMap<>();
    private final List<Integer> duplicateColumns = new ArrayList<>();
    private Tuple leftTuple = null;
    private final List<ComparisonAtom> comparisonAtomList;
    private final List<String> leftVarsName;
    private final List<String> rightVarsName;

    /**
     * Constructs a JoinOperator object that performs a join on the tuples returned by the left and right child operators.
     *
     * @param leftChildOperator the left child operator
     * @param rightChildOperator the right child operator
     * @param comparisonAtoms the list of comparison atoms for the join condition
     */
    public JoinOperator(Operator leftChildOperator, Operator rightChildOperator, List<ComparisonAtom> comparisonAtoms) {
        this.leftChildOperator = leftChildOperator;
        leftVarsName = leftChildOperator.getVarsName();
        this.rightChildOperator = rightChildOperator;
        rightVarsName = rightChildOperator.getVarsName();
        // Add variables from left child operator to varsName
        for (String leftVar : leftVarsName) {
            this.varsName.add(leftVar);
            // Add join condition indices and duplicate columns
            if (rightVarsName.contains(leftVar)) {
                this.joinConditionIndices.put(leftVarsName.indexOf(leftVar), rightVarsName.indexOf(leftVar));
                this.duplicateColumns.add(rightVarsName.indexOf(leftVar));
            }
        }
        // Add variables from right child operator to varsName
        for (String rightVar : rightVarsName) {
            if (rightVar == null) {
                this.varsName.add(null);
            } else {
                if (!this.varsName.contains(rightVar)) this.varsName.add(rightVar);
            }
        }
        this.comparisonAtomList = comparisonAtoms;
    }

    /**
     * Resets the state of the JoinOperator by resetting the left and right child operators and setting leftTuple to null.
     */
    @Override
    public void reset() {
        this.leftChildOperator.reset();
        this.rightChildOperator.reset();
        this.leftTuple = null;
    }

    /**
     * Returns the next tuple produced by the JoinOperator by joining tuples from the left and right child operators.
     *
     * @return the next tuple produced by the JoinOperator
     */
    @Override
    public Tuple getNextTuple() {
        if (this.leftTuple == null)
            this.leftTuple = this.leftChildOperator.getNextTuple();

        while (this.leftTuple != null) {
            Tuple rightTuple = this.rightChildOperator.getNextTuple();

            while (rightTuple != null) {
                boolean valid = true;
                for (Integer leftIndex : this.joinConditionIndices.keySet()) {
                    int rightIndex = this.joinConditionIndices.get(leftIndex);
                    if (!this.leftTuple.getTerms().get(leftIndex).toString().equals(rightTuple.getTerms().get(rightIndex).toString())) {
                        valid = false;
                        break;
                    }
                }
                // Check join condition
                if (valid) {
                    if (!valid(this.leftTuple, rightTuple)) {
                        valid = false;
                    }
                }
                // If tuple is valid, create a new tuple with joined terms
                if (valid) {
                    List<Term> joinTerms = new ArrayList<>(this.leftTuple.getTerms());
                    for (int i = 0; i < rightTuple.getTerms().size(); i++) {
                        if (!this.duplicateColumns.contains(i)) {
                            joinTerms.add(rightTuple.getTerms().get(i));
                        }
                    }
                    return new Tuple("Join", joinTerms);
                }
                rightTuple = this.rightChildOperator.getNextTuple();
            }
            this.rightChildOperator.reset();
            this.leftTuple = this.leftChildOperator.getNextTuple();
        }
        return null;
    }

    /**
     * Check if two tuples satisfy the comparison atoms in the list.
     *
     @param leftTuple the first tuple to be compared
     @param rightTuple the second tuple to be compared
     @return true if both tuples satisfy all comparison atoms, false otherwise
     */
    public boolean valid(Tuple leftTuple, Tuple rightTuple) {
        // Loop through all comparison atoms in the list
        for (ComparisonAtom compAtom : this.comparisonAtomList) {
            // Get the operator from the comparison atom
            String op = compAtom.getOp().toString();
            int operand1Index;
            int operand2Index;
            boolean reverse = false;
            // Determine which operand is from the left tuple and which is from the right tuple
            if ( leftVarsName.contains(((Variable) compAtom.getTerm1()).getName()) ) {
                operand1Index = leftVarsName.indexOf(((Variable) compAtom.getTerm1()).getName());
                operand2Index = rightVarsName.indexOf(((Variable) compAtom.getTerm2()).getName());
            } else {
                reverse = true;
                operand1Index = rightVarsName.indexOf(((Variable) compAtom.getTerm1()).getName());
                operand2Index = leftVarsName.indexOf(((Variable) compAtom.getTerm2()).getName());
            }

            // Get the operands for the current comparison atom based on which tuple they come from
            Term operand1;
            Term operand2;
            if (!reverse) {
                operand1 = leftTuple.getTerms().get(operand1Index);
                operand2 = rightTuple.getTerms().get(operand2Index);
            } else {
                operand1 = rightTuple.getTerms().get(operand1Index);
                operand2 = leftTuple.getTerms().get(operand2Index);
            }

            // Determine if the comparison is valid based on the operator
            boolean valid;
            boolean equals = operand1.toString().equals(operand2.toString());
            switch (op) {
                case "=":
                    valid = equals;
                    break;
                case "!=":
                    valid = !equals;
                    break;
                case ">":
                    if (operand1 instanceof IntegerConstant) valid = ((IntegerConstant) operand1).getValue() > ((IntegerConstant) operand2).getValue();
                    else valid = ((StringConstant) operand1).getValue().compareTo(((StringConstant) operand2).getValue()) > 0;
                    break;
                case ">=":
                    if (operand1 instanceof IntegerConstant) valid = ((IntegerConstant) operand1).getValue() >= ((IntegerConstant) operand2).getValue();
                    else valid = ((StringConstant) operand1).getValue().compareTo(((StringConstant) operand2).getValue()) >= 0;
                    break;
                case "<":
                    if (operand1 instanceof IntegerConstant) valid = ((IntegerConstant) operand1).getValue() < ((IntegerConstant) operand2).getValue();
                    else valid = ((StringConstant) operand1).getValue().compareTo(((StringConstant) operand2).getValue()) < 0;
                    break;
                case "<=":
                    if (operand1 instanceof IntegerConstant) valid = ((IntegerConstant) operand1).getValue() <= ((IntegerConstant) operand2).getValue();
                    else valid = ((StringConstant) operand1).getValue().compareTo(((StringConstant) operand2).getValue()) <= 0;
                    break;
                default:
                    valid = false;
                    break;
            }
            // If the comparison is not valid, return false immediately
            if (!valid) return false;
        }
        return true;
    }
}