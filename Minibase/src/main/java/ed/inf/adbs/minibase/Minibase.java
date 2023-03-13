package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.operator.*;
import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.parser.QueryParser;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory database system
 *
 */
public class Minibase {

    public static void main(String[] args) {

        if (args.length != 3) {
            System.err.println("Usage: Minibase database_dir input_file output_file");
            return;
        }

        String databaseDir = args[0];
        String inputFile = args[1];
        String outputFile = args[2];

        evaluateCQ(databaseDir, inputFile, outputFile);
    }

    public static void evaluateCQ(String databaseDir, String inputFile, String outputFile) {
        // TODO: add your implementation
        try {
            DatabaseCatalog dbc = DatabaseCatalog.getInstance();
            dbc.init(databaseDir);
            Query query = QueryParser.parse(Paths.get(inputFile));
            Operator rootOperator = buildQueryPlan(query);
            rootOperator.dump(outputFile);
        } catch (Exception e) {
            System.err.println("Exception occurred during parsing");
            e.printStackTrace();
        }
    }

    private static Operator buildQueryPlan(Query query) {
        List<RelationalAtom> relationalBody = new ArrayList<>();
        List<ComparisonAtom> comparisonBody = new ArrayList<>();
        List<Term> headVars = new ArrayList<>(query.getHead().getVariables());
        RelationalAtom headReal = new RelationalAtom(query.getHead().getName(), headVars);

        List<String> allVar = new ArrayList<>();
        for (Atom atom : query.getBody()) {
            if (atom instanceof RelationalAtom) {
                for (Term term : ((RelationalAtom) atom).getTerms()) {
                    if (term instanceof Variable && !allVar.contains(((Variable) term).getName())) {
                        allVar.add(((Variable) term).getName());
                    }
                }
            }
        }

        for (Atom atom : query.getBody()) {
            if (atom instanceof RelationalAtom) {
                List<Term> termList = ((RelationalAtom) atom).getTerms();
                String relationName = ((RelationalAtom) atom).getName();
                for (int i = 0; i < termList.size(); i++) {
                    Term originalTerm = termList.get(i);
                    if (originalTerm instanceof Constant) {
                        String newVarName = generateNewVariableName(allVar);
                        termList.set(i, new Variable(newVarName));
                        comparisonBody.add(new ComparisonAtom(
                                new Variable(newVarName),
                                originalTerm,
                                ComparisonOperator.fromString("=")
                        ));
                    }
                }
                relationalBody.add(new RelationalAtom(relationName, termList));
            } else {
                comparisonBody.add((ComparisonAtom)atom);
            }
        }

        Operator rootOperator = null;
        List<String> previousVariables = new ArrayList<>();
        for (RelationalAtom atom : relationalBody) {
            List<String> subtreeVariables = new ArrayList<>();
            for (Term term : atom.getTerms()) {
                if (term instanceof Variable) subtreeVariables.add(((Variable) term).getName());
            }

            // Scan operation
            Operator subtree = new ScanOperator(atom);

            // Select operation
            List<ComparisonAtom> selectCompAtomList = new ArrayList<>();
            for (ComparisonAtom cAtom : comparisonBody)
                if (variableAllAppeared(cAtom, subtreeVariables))
                    selectCompAtomList.add(cAtom);
            subtree = new SelectOperator(subtree, selectCompAtomList);

            // Join operation
            List<String> mergedVariables = new ArrayList<>();
            mergedVariables.addAll(previousVariables);
            mergedVariables.addAll(subtreeVariables);
            if (rootOperator == null) {
                rootOperator = subtree;
            } else {
                List<ComparisonAtom> joinCompAtomList = new ArrayList<>();
                for (ComparisonAtom cAtom : comparisonBody) {
                    if (!variableAllAppeared(cAtom, previousVariables) &&
                            !variableAllAppeared(cAtom, subtreeVariables) &&
                            variableAllAppeared(cAtom, mergedVariables))
                        joinCompAtomList.add(cAtom);
                }
                rootOperator = new JoinOperator(rootOperator, subtree, joinCompAtomList);
            }
            previousVariables = mergedVariables;
        }
        // Project operation
        rootOperator = new ProjectOperator(rootOperator, headReal);

        return rootOperator;
    }

    private static String generateNewVariableName(List<String> usedNames) {
        int count = 0;
        String newVar = "var" + String.valueOf(count);
        while (usedNames.contains(newVar)) {
            count++;
            newVar = "var" + String.valueOf(count);
        }
        usedNames.add(newVar);
        return newVar;
    }

    private static boolean variableAllAppeared(ComparisonAtom comparisonAtom, List<String> currentVariables) {
        if (comparisonAtom.getTerm1() instanceof Variable)
            if (!currentVariables.contains(((Variable) comparisonAtom.getTerm1()).getName()))
                return false;
        if (comparisonAtom.getTerm2() instanceof Variable)
            if (!currentVariables.contains(((Variable) comparisonAtom.getTerm2()).getName()))
                return false;
        return true;
    }

    /**
     * Example method for getting started with the parser.
     * Reads CQ from a file and prints it to screen, then extracts Head and Body
     * from the query and prints them to screen.
     */

    public static void parsingExample(String filename) {
        try {
            Query query = QueryParser.parse(Paths.get(filename));
            // Query query = QueryParser.parse("Q(x, y) :- R(x, z), S(y, z, w), z < w");
            // Query query = QueryParser.parse("Q(SUM(x * 2 * x)) :- R(x, 'z'), S(4, z, w), 4 < 'test string' ");

            System.out.println("Entire query: " + query);
            Head head = query.getHead();
            System.out.println("Head: " + head);
            List<Atom> body = query.getBody();
            System.out.println("Body: " + body);
        }
        catch (Exception e)
        {
            System.err.println("Exception occurred during parsing");
            e.printStackTrace();
        }
    }
}
