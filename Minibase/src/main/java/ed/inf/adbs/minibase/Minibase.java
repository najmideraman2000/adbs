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
        List<Term> headTerms = new ArrayList<>(query.getHead().getVariables());
        RelationalAtom headRel = new RelationalAtom(query.getHead().getName(), headTerms);

        List<String> allVar = new ArrayList<>();
        for (Atom atom : query.getBody()) {
            if (!(atom instanceof RelationalAtom)) continue;
            for (Term term : ((RelationalAtom) atom).getTerms()) {
                if (term instanceof Variable && !allVar.contains(((Variable) term).getName())) {
                    allVar.add(((Variable) term).getName());
                }
            }
        }

        for (Atom atom : query.getBody()) {
            if (atom instanceof RelationalAtom) {
                List<Term> terms = ((RelationalAtom) atom).getTerms();
                String relationName = ((RelationalAtom) atom).getName();
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
        for (RelationalAtom relAtom : relationalBody) {
            List<String> subtreeVars = new ArrayList<>();
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
        rootOperator = new ProjectOperator(rootOperator, headRel);

        return rootOperator;
    }

    private static String generateNewVariable(List<String> unavailableVars) {
        int count = 0;
        String newVar = "var" + count;
        while (unavailableVars.contains(newVar)) {
            count++;
            newVar = "var" + count;
        }
        unavailableVars.add(newVar);
        return newVar;
    }

    private static boolean checkComparisonInvolved(ComparisonAtom comparisonAtom, List<String> currentVariables) {
        if (comparisonAtom.getTerm1() instanceof Variable)
            if (!currentVariables.contains(((Variable) comparisonAtom.getTerm1()).getName()))
                return false;
        if (comparisonAtom.getTerm2() instanceof Variable)
            return currentVariables.contains(((Variable) comparisonAtom.getTerm2()).getName());
        return true;
    }

//    /**
//     * Example method for getting started with the parser.
//     * Reads CQ from a file and prints it to screen, then extracts Head and Body
//     * from the query and prints them to screen.
//     */
//
//    public static void parsingExample(String filename) {
//        try {
//            Query query = QueryParser.parse(Paths.get(filename));
//            // Query query = QueryParser.parse("Q(x, y) :- R(x, z), S(y, z, w), z < w");
//            // Query query = QueryParser.parse("Q(SUM(x * 2 * x)) :- R(x, 'z'), S(4, z, w), 4 < 'test string' ");
//
//            System.out.println("Entire query: " + query);
//            Head head = query.getHead();
//            System.out.println("Head: " + head);
//            List<Atom> body = query.getBody();
//            System.out.println("Body: " + body);
//        }
//        catch (Exception e)
//        {
//            System.err.println("Exception occurred during parsing");
//            e.printStackTrace();
//        }
//    }
}
