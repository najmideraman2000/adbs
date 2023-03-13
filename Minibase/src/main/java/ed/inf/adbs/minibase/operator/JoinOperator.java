package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.base.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JoinOperator extends Operator {
    private final Operator leftChild;
    private final Operator rightChild;
    private final HashMap<Integer, Integer> joinConditionIndices = new HashMap<>();
    private final List<Integer> rightDuplicateColumns = new ArrayList<>();
    private Tuple leftTuple = null;
    private final List<ComparisonAtom> comparisonAtomList;
    private final List<String> leftVariableMask;
    private final List<String> rightVariableMask;

    public JoinOperator(Operator leftChild, Operator rightChild, List<ComparisonAtom> comparisonAtoms) {
        this.leftChild = leftChild;
        leftVariableMask = leftChild.getVariableMask();
        this.rightChild = rightChild;
        rightVariableMask = rightChild.getVariableMask();
        for (String leftVar : leftVariableMask) {
            this.variableMask.add(leftVar);
            if (rightVariableMask.contains(leftVar)) {
                // construct new join conditions for these identical variable pairs
                this.joinConditionIndices.put(leftVariableMask.indexOf(leftVar), rightVariableMask.indexOf(leftVar));
                this.rightDuplicateColumns.add(rightVariableMask.indexOf(leftVar));
            }
        }
        for (String rightVar : rightVariableMask) {
            if (rightVar == null) {
                this.variableMask.add(null);
            } else {
                if (!this.variableMask.contains(rightVar))
                    this.variableMask.add(rightVar);
            }
        }
        this.comparisonAtomList = comparisonAtoms;
    }

    @Override
    public void reset() {
        this.leftChild.reset();
        this.rightChild.reset();
        this.leftTuple = null;
    }

    @Override
    public Tuple getNextTuple() {
        if (this.leftTuple == null)
            this.leftTuple = this.leftChild.getNextTuple();

        while (this.leftTuple != null) {
            Tuple rightTuple = this.rightChild.getNextTuple();

            while (rightTuple != null) {
                boolean pass = true;
                for (Integer leftIndex : this.joinConditionIndices.keySet()) {
                    int rightIndex = this.joinConditionIndices.get(leftIndex);
                    if (!this.leftTuple.getTerms().get(leftIndex).equals(rightTuple.getTerms().get(rightIndex))) {
                        pass = false;
                        break;
                    }
                }
                if (pass) {
                    if (!check(this.leftTuple, rightTuple)) {
                        pass = false;
                    }
                }
                if (pass) {
                    List<Term> joinTermList = new ArrayList<>();
                    // the join result contains all columns in left tuple, and the non-duplicate columns in right tuple
                    for (Term leftTerm : this.leftTuple.getTerms())
                        joinTermList.add(leftTerm);
                    for (int i = 0; i < rightTuple.getTerms().size(); i++) {
                        if (!this.rightDuplicateColumns.contains(i)) {
                            joinTermList.add(rightTuple.getTerms().get(i));
                        }
                    }
                    return new Tuple("Join", joinTermList);
                }
                rightTuple = this.rightChild.getNextTuple();
            }
            this.rightChild.reset();
            this.leftTuple = this.leftChild.getNextTuple();
        }
        return null;
    }

    public boolean check(Tuple leftTuple, Tuple rightTuple) {
        for (ComparisonAtom cAtom : this.comparisonAtomList) {
            String op = cAtom.getOp().toString();
            int operand1Idx = 0;
            int operand2Idx = 0;
            boolean reverseOrder = false;
            if ( leftVariableMask.contains(((Variable) cAtom.getTerm1()).getName()) ) {
                operand1Idx = leftVariableMask.indexOf(((Variable) cAtom.getTerm1()).getName());
                operand2Idx = rightVariableMask.indexOf(((Variable) cAtom.getTerm2()).getName());
            } else {
                reverseOrder = true;
                operand1Idx = rightVariableMask.indexOf(((Variable) cAtom.getTerm1()).getName());
                operand2Idx = leftVariableMask.indexOf(((Variable) cAtom.getTerm2()).getName());
            }

            Term operand1;
            Term operand2;
            if (!reverseOrder) {
                operand1 = leftTuple.getTerms().get(operand1Idx);
                operand2 = rightTuple.getTerms().get(operand2Idx);
            } else {
                operand1 = rightTuple.getTerms().get(operand1Idx);
                operand2 = leftTuple.getTerms().get(operand2Idx);
            }

            boolean pass;
            switch (op) {
                case "=":
                    pass = operand1.toString().equals(operand2.toString());
                    break;
                case "!=":
                    pass = !operand1.toString().equals(operand2.toString());
                    break;
                case ">":
                    if (operand1 instanceof IntegerConstant) pass = ((IntegerConstant) operand1).getValue() > ((IntegerConstant) operand2).getValue();
                    else pass = ((StringConstant) operand1).getValue().compareTo(((StringConstant) operand2).getValue()) > 0;
                    break;
                case ">=":
                    if (operand1 instanceof IntegerConstant) pass = ((IntegerConstant) operand1).getValue() >= ((IntegerConstant) operand2).getValue();
                    else pass = ((StringConstant) operand1).getValue().compareTo(((StringConstant) operand2).getValue()) >= 0;
                    break;
                case "<":
                    if (operand1 instanceof IntegerConstant) pass = ((IntegerConstant) operand1).getValue() < ((IntegerConstant) operand2).getValue();
                    else pass = ((StringConstant) operand1).getValue().compareTo(((StringConstant) operand2).getValue()) < 0;
                    break;
                case "<=":
                    if (operand1 instanceof IntegerConstant) pass = ((IntegerConstant) operand1).getValue() <= ((IntegerConstant) operand2).getValue();
                    else pass = ((StringConstant) operand1).getValue().compareTo(((StringConstant) operand2).getValue()) <= 0;
                    break;
                default:
                    System.out.println("!!!! None of the if-branches is evoked in the Selection Operator !!!!");
                    pass = false;
                    break;
            }
            if (!pass) return false;
        }
        return true;
    }
}