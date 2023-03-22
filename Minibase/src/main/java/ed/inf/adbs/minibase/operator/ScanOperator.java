/**
 * This class represents a Scan Operator that reads data from a relation and returns the tuples.
 */
package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.base.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ScanOperator extends Operator{
    private final String relationName;
    private Scanner scanner;
    private final List<String> schema;

    /**
     * Constructs a new ScanOperator object for the given RelationalAtom.
     *
     * @param atom the RelationalAtom to scan.
     */
    public ScanOperator(RelationalAtom atom) {
        for (Term term : atom.getTerms()) {
            if (term instanceof Variable) this.varsName.add(((Variable) term).getName());
            else this.varsName.add(null);
        }
        this.relationName = atom.getName();
        DatabaseCatalog dbCat = DatabaseCatalog.getInstance();
        this.schema = dbCat.getSchema(relationName);
        this.reset();
    }

    /**
     * Resets the operator to the beginning of the relation.
     */
    @Override
    public void reset() {
        DatabaseCatalog dbCat = DatabaseCatalog.getInstance();
        try {
            this.scanner = new Scanner(new File(dbCat.getRelationPath(relationName)));
        } catch (FileNotFoundException e) {
            System.out.println("Relation data file not found: " + dbCat.getRelationPath(relationName));
            e.printStackTrace();
        }
    }

    /**
     * Returns the next tuple in the relation, or null if there are no more tuples.
     *
     * @return the next tuple in the relation, or null if there are no more tuples.
     */
    @Override
    public Tuple getNextTuple() {
        if (this.scanner.hasNextLine()) {
            String line = this.scanner.nextLine();
            String[] rawData = line.split("[^a-zA-Z0-9]+");
            ArrayList<Term> terms = new ArrayList<>();
            for (int i = 0; i < rawData.length; i++) {
                if (this.schema.get(i).equals("int")) {
                    terms.add(new IntegerConstant(Integer.parseInt(rawData[i])));
                } else {
                    terms.add(new StringConstant(rawData[i]));
                }
            }
            return new Tuple(this.relationName, terms);
        } else {
            return null;
        }
    }
}
