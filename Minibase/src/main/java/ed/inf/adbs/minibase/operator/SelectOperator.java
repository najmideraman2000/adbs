package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.base.*;

import java.util.ArrayList;
import java.util.List;

public class SelectOperator extends Operator {
    private Operator child;
    private List<ComparisonAtom> comparisonAtomList = new ArrayList<>();

    public SelectOperator(Operator child, List<ComparisonAtom> compAtomList) {
        this.child = child;
        this.variableMask = this.child.getVariableMask();
        this.comparisonAtomList = compAtomList;
    }

    @Override
    public void reset() {
        this.child.reset();
    }

    @Override
    public Tuple getNextTuple() {
        Tuple nextTuple = this.child.getNextTuple();
        while (nextTuple != null) {
            if (check(nextTuple)) {
                return nextTuple;
            }
            else
                nextTuple = this.child.getNextTuple();
        }
        return null;
    }

    public boolean check(Tuple tuple) {
        for (ComparisonAtom cAtom : this.comparisonAtomList) {
            String op = cAtom.getOp().toString();
            Term term1 = null;
            Term term2 = null;
            int term1Idx = 0;
            int term2Idx = 0;
            if (cAtom.getTerm1() instanceof Variable) {
                term1Idx = this.variableMask.indexOf(((Variable) cAtom.getTerm1()).getName());
            } else {
                term1 = cAtom.getTerm1();
            }
            if (cAtom.getTerm2() instanceof Variable) {
                term2Idx = this.variableMask.indexOf(((Variable) cAtom.getTerm2()).getName());
            } else {
                term2 = cAtom.getTerm2();
            }

            Term operand1 = term1 == null ? tuple.getTerms().get(term1Idx) : term1;
            Term operand2 = term2 == null ? tuple.getTerms().get(term2Idx) : term2;

            boolean pass;
            switch (op) {
                case "=":
                    pass = operand1.toString().equals(operand2.toString());
                    break;
                case "!=":
                    pass = (!operand1.toString().equals(operand2.toString()));
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