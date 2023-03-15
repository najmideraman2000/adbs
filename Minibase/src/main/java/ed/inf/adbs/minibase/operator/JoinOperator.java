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

    public JoinOperator(Operator leftChildOperator, Operator rightChildOperator, List<ComparisonAtom> comparisonAtoms) {
        this.leftChildOperator = leftChildOperator;
        leftVarsName = leftChildOperator.getVarsName();
        this.rightChildOperator = rightChildOperator;
        rightVarsName = rightChildOperator.getVarsName();
        for (String leftVar : leftVarsName) {
            this.varsName.add(leftVar);
            if (rightVarsName.contains(leftVar)) {
                this.joinConditionIndices.put(leftVarsName.indexOf(leftVar), rightVarsName.indexOf(leftVar));
                this.duplicateColumns.add(rightVarsName.indexOf(leftVar));
            }
        }
        for (String rightVar : rightVarsName) {
            if (rightVar == null) {
                this.varsName.add(null);
            } else {
                if (!this.varsName.contains(rightVar)) this.varsName.add(rightVar);
            }
        }
        this.comparisonAtomList = comparisonAtoms;
    }

    @Override
    public void reset() {
        this.leftChildOperator.reset();
        this.rightChildOperator.reset();
        this.leftTuple = null;
    }

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
                if (valid) {
                    if (!valid(this.leftTuple, rightTuple)) {
                        valid = false;
                    }
                }
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

    public boolean valid(Tuple leftTuple, Tuple rightTuple) {
        for (ComparisonAtom compAtom : this.comparisonAtomList) {
            String op = compAtom.getOp().toString();
            int operand1Index;
            int operand2Index;
            boolean reverse = false;
            if ( leftVarsName.contains(((Variable) compAtom.getTerm1()).getName()) ) {
                operand1Index = leftVarsName.indexOf(((Variable) compAtom.getTerm1()).getName());
                operand2Index = rightVarsName.indexOf(((Variable) compAtom.getTerm2()).getName());
            } else {
                reverse = true;
                operand1Index = rightVarsName.indexOf(((Variable) compAtom.getTerm1()).getName());
                operand2Index = leftVarsName.indexOf(((Variable) compAtom.getTerm2()).getName());
            }

            Term operand1;
            Term operand2;
            if (!reverse) {
                operand1 = leftTuple.getTerms().get(operand1Index);
                operand2 = rightTuple.getTerms().get(operand2Index);
            } else {
                operand1 = rightTuple.getTerms().get(operand1Index);
                operand2 = leftTuple.getTerms().get(operand2Index);
            }

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
            if (!valid) return false;
        }
        return true;
    }
}