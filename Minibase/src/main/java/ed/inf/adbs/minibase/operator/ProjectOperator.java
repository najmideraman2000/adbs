package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.base.*;

import java.util.ArrayList;
import java.util.List;

public class ProjectOperator extends Operator {
    private Operator child;
    private String projectionName;
    private List<Integer> projectIndices = new ArrayList<>();
    private List<String> reportBuffer = new ArrayList<>();

    public ProjectOperator(Operator childOperator, RelationalAtom queryHead) {
        this.child = childOperator;
        List<String> childVariableMask = childOperator.getVariableMask(); // the variableMask before projection
        this.projectionName = queryHead.getName();
        for (int i = 0; i < queryHead.getTerms().size(); i++) {
            String varName = ((Variable) queryHead.getTerms().get(i)).getName();
            int idx = childVariableMask.indexOf(varName);
            this.projectIndices.add(idx);
            this.variableMask.add(varName);
        }
    }

    @Override
    public void reset() {
        this.child.reset();
        this.reportBuffer = new ArrayList<>();
    }

    @Override
    public Tuple getNextTuple() {
        Tuple childOutput = this.child.getNextTuple();
        while (childOutput != null) {
            List<Term> termList = new ArrayList<>();
            for (int pi : this.projectIndices) {
                termList.add(childOutput.getTerms().get(pi));
            }
            Tuple newTuple = new Tuple(this.projectionName, termList);
            if (!this.reportBuffer.contains(newTuple.toString())) {
                this.reportBuffer.add(newTuple.toString());
                return newTuple;
            }
            childOutput = this.child.getNextTuple();
        }
        return null;
    }
}