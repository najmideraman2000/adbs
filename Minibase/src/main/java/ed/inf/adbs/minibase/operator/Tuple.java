package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.Utils;
import ed.inf.adbs.minibase.base.Term;

import java.util.List;

public class Tuple {
    private final String relationName;
    private final List<Term> terms;

    public Tuple(String relationName, List<Term> terms) {
        this.relationName = relationName;
        this.terms = terms;
    }

    public String getName() {
        return relationName;
    }

    public List<Term> getTerms() {
        return terms;
    }

    @Override
    public String toString() {
        return Utils.join(terms, ", ");
    }
}