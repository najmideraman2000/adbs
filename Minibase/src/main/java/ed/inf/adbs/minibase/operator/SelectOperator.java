package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.base.*;

import java.util.List;

public class SelectOperator extends Operator {
    private final Operator childOperator;
    private final List<ComparisonAtom> comparisonAtomList;

    /**
     * Constructs a new SelectOperator object with a child operator and a list of comparison atoms.
     *
     * @param childOperator The child operator.
     * @param compAtomList  The list of comparison atoms.
     */
    public SelectOperator(Operator childOperator, List<ComparisonAtom> compAtomList) {
        this.childOperator = childOperator;
        this.varsName = this.childOperator.getVarsName();
        this.comparisonAtomList = compAtomList;
    }

    /**
     * Resets the child operator.
     */
    @Override
    public void reset() {
        this.childOperator.reset();
    }

    /**
     * Returns the next tuple that satisfies the comparison atoms.
     *
     * @return The next tuple that satisfies the comparison atoms, or null if no such tuple exists.
     */
    @Override
    public Tuple getNextTuple() {
        Tuple nextTuple = this.childOperator.getNextTuple();
        while (nextTuple != null) {
            if (valid(nextTuple)) return nextTuple;
            else nextTuple = this.childOperator.getNextTuple();
        }
        return null;
    }

    /**
     * Returns whether a tuple satisfies all the comparison atoms.
     *
     * @param tuple The tuple to be checked.
     * @return True if the tuple satisfies all the comparison atoms, false otherwise.
     */
    public boolean valid(Tuple tuple) {
        for (ComparisonAtom compAtom : this.comparisonAtomList) {
            String operation = compAtom.getOp().toString();
            Term term1 = null;
            Term term2 = null;
            int term1Index = 0;
            int term2Index = 0;
            if (compAtom.getTerm1() instanceof Variable) term1Index = this.varsName.indexOf(((Variable) compAtom.getTerm1()).getName());
            else term1 = compAtom.getTerm1();
            if (compAtom.getTerm2() instanceof Variable) term2Index = this.varsName.indexOf(((Variable) compAtom.getTerm2()).getName());
            else term2 = compAtom.getTerm2();

            Term operand1 = term1 == null ? tuple.getTerms().get(term1Index) : term1;
            Term operand2 = term2 == null ? tuple.getTerms().get(term2Index) : term2;

            boolean valid;
            boolean equals = operand1.toString().equals(operand2.toString());
            switch (operation) {
                case "=":
                    valid = equals;
                    break;
                case "!=":
                    valid = (!equals);
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