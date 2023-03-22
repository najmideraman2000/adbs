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

    /**
     * This is the main method of the program, which takes 3 arguments from the command line.
     * If the number of arguments is not equal to 3, it will print an error message and terminate the program.
     * If the number of arguments is equal to 3, it will assign each argument to its respective variable and call evaluateCQ method.
     *
     * @param args An array of strings that contains the command line arguments.
     */
    public static void main(String[] args) {

        // check if the number of command line arguments is equal to 3
        if (args.length != 3) {
            System.err.println("Usage: Minibase database_dir input_file output_file");
            return;
        }

        // assign each argument to its respective variable
        String databaseDir = args[0];
        String inputFile = args[1];
        String outputFile = args[2];

        // call evaluateCQ method with the assigned arguments
        evaluateCQ(databaseDir, inputFile, outputFile);
    }

    /**
     * This method evaluates a given query using the Minibase database system.
     * It initializes the database catalog with the given directory, parses the input file,
     * builds a query plan, and writes the output to the specified output file.
     * If any exceptions occur during the parsing or query execution, it will print an error message and stack trace.
     *
     * @param databaseDir A string representing the path to the directory containing the database files.
     * @param inputFile A string representing the path to the input file containing the query.
     * @param outputFile A string representing the path to the output file where the query result will be written.
     */
    public static void evaluateCQ(String databaseDir, String inputFile, String outputFile) {
        // TODO: add your implementation
        try {
            // initialize the database catalog with the given directory
            DatabaseCatalog dbc = DatabaseCatalog.getInstance();
            dbc.init(databaseDir);
            // parse the input file and build the query plan
            Query query = QueryParser.parse(Paths.get(inputFile));
            Operator rootOperator = buildQueryPlan(query);
            // write the output to the specified output file
            rootOperator.dump(outputFile);
        } catch (Exception e) {
            // print an error message and stack trace if any exceptions occur
            System.err.println("Exception occurred during parsing");
            e.printStackTrace();
        }
    }

    /**
     * Builds the query plan for a given query.
     *
     * @param query query the query to build the plan for
     * @return the root operator of the query plan
     */
    private static Operator buildQueryPlan(Query query) {
        List<RelationalAtom> relationalBody = new ArrayList<>();
        List<ComparisonAtom> comparisonBody = new ArrayList<>();
        List<Term> headTerms = new ArrayList<>(query.getHead().getVariables());
        // Add sum aggregate to head terms if it exists
        if (query.getHead().getSumAggregate() != null) {
            headTerms.add(query.getHead().getSumAggregate());
        }
        RelationalAtom headRel = new RelationalAtom(query.getHead().getName(), headTerms);

        // Get all variable names used in the body of the query
        List<String> allVar = new ArrayList<>();
        for (Atom atom : query.getBody()) {
            if (!(atom instanceof RelationalAtom)) continue;
            for (Term term : ((RelationalAtom) atom).getTerms()) {
                if (term instanceof Variable && !allVar.contains(((Variable) term).getName())) {
                    allVar.add(((Variable) term).getName());
                }
            }
        }

        // Iterate through the body of the query to extract relational and comparison atoms
        for (Atom atom : query.getBody()) {
            if (atom instanceof RelationalAtom) {
                List<Term> terms = ((RelationalAtom) atom).getTerms();
                String relationName = ((RelationalAtom) atom).getName();
                // Replace constants with new variables and add corresponding comparison atoms
                for (int i = 0; i < terms.size(); i++) {
                    Term term = terms.get(i);
                    if (!(term instanceof Constant)) continue;
                    String newVarName = generateNewVariable(allVar);
                    terms.set(i, new Variable(newVarName));
                    comparisonBody.add(new ComparisonAtom(
                            new Variable(newVarName),
                            term,
                            ComparisonOperator.fromString("=")
                    ));
                }
                relationalBody.add(new RelationalAtom(relationName, terms));
            } else {
                comparisonBody.add((ComparisonAtom)atom);
            }
        }

        Operator rootOperator = null;
        List<String> prevMergedVars = new ArrayList<>();
        // Iterate through the relational atoms to construct the query plan
        for (RelationalAtom relAtom : relationalBody) {
            List<String> subtreeVars = new ArrayList<>();
            // Get all variable names used in the current relational atom
            for (Term term : relAtom.getTerms()) {
                if (!(term instanceof Variable)) continue;
                subtreeVars.add(((Variable) term).getName());
            }

            // Scan operation
            Operator subtree = new ScanOperator(relAtom);

            // Select operation
            List<ComparisonAtom> selectComparisonsInvolved = new ArrayList<>();
            for (ComparisonAtom compAtom : comparisonBody)
                if (checkComparisonInvolved(compAtom, subtreeVars))
                    selectComparisonsInvolved.add(compAtom);
            subtree = new SelectOperator(subtree, selectComparisonsInvolved);

            // Join operation
            List<String> mergedTreeVars = new ArrayList<>();
            mergedTreeVars.addAll(prevMergedVars);
            mergedTreeVars.addAll(subtreeVars);
            if (rootOperator == null) {
                rootOperator = subtree;
            } else {
                List<ComparisonAtom> joinComparisonsInvolved = new ArrayList<>();
                for (ComparisonAtom compAtom : comparisonBody) {
                    if (!checkComparisonInvolved(compAtom, prevMergedVars)
                            && !checkComparisonInvolved(compAtom, subtreeVars)
                            && checkComparisonInvolved(compAtom, mergedTreeVars))
                    {
                        joinComparisonsInvolved.add(compAtom);
                    }
                }
                rootOperator = new JoinOperator(rootOperator, subtree, joinComparisonsInvolved);
            }
            prevMergedVars = mergedTreeVars;
        }
        // Project operation
        assert rootOperator != null;
        if (query.getHead().getSumAggregate() != null) {
            rootOperator = new SumOperator(rootOperator, headRel);
        }
        else {
            rootOperator = new ProjectOperator(rootOperator, headRel);
        }

        return rootOperator;
    }

    /**
     * Generates a new variable name that is not in the list of unavailable variables.
     *
     * @param unavailableVars a list of variables that are not available to use.
     * @return a new variable name that is not in the list of unavailable variables.
     */
    private static String generateNewVariable(List<String> unavailableVars) {
        int count = 0;
        String newVar = "var" + count;
        // keep generating new variable names until one is found that is not in the list of unavailable
        while (unavailableVars.contains(newVar)) {
            count++;
            newVar = "var" + count;
        }
        // add the new variable to the list of unavailable variables
        unavailableVars.add(newVar);
        return newVar;
    }

    /**
     * Checks whether a comparison atom involves at least one variable that is currently in the list of current variables.
     *
     * @param comparisonAtom a comparison atom to check.
     * @param currentVariables a list of current variables.
     * @return true if the comparison atom involves at least one variable that is currently in the list of current variables, false otherwise.
     */
    private static boolean checkComparisonInvolved(ComparisonAtom comparisonAtom, List<String> currentVariables) {
        if (comparisonAtom.getTerm1() instanceof Variable)
            if (!currentVariables.contains(((Variable) comparisonAtom.getTerm1()).getName()))
                return false;
        if (comparisonAtom.getTerm2() instanceof Variable)
            return currentVariables.contains(((Variable) comparisonAtom.getTerm2()).getName());
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
