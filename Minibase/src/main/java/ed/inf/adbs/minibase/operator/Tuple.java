/**
 * The Tuple class represents a row in a relation, which consists of a relation name and a list of terms.
 */

package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.Utils;
import ed.inf.adbs.minibase.base.Term;

import java.util.List;

public class Tuple {
    private final String relationName;
    private final List<Term> terms;

    /**
     * Creates a Tuple object with the specified relation name and list of terms.
     *
     * @param relationName the name of the relation
     * @param terms a list of terms representing the values in the tuple
     */
    public Tuple(String relationName, List<Term> terms) {
        this.relationName = relationName;
        this.terms = terms;
    }

    /**
     * Returns the name of the relation this tuple belongs to.
     *
     * @return the name of the relation
     */
    public String getName() {
        return relationName;
    }

    /**
     * Returns the list of terms representing the values in this tuple.
     *
     * @return a list of terms
     */
    public List<Term> getTerms() {
        return terms;
    }

    /**
     * Returns a string representation of this tuple, where each term is separated by a comma.
     *
     * @return a string representation of this tuple
     */
    @Override
    public String toString() {
        return Utils.join(terms, ", ");
    }
}