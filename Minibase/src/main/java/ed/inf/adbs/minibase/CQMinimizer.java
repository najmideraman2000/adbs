package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.parser.QueryParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 *
 * Minimization of conjunctive queries
 *
 */
public class CQMinimizer {

    public static void main(String[] args) {

        if (args.length != 2) {
            System.err.println("Usage: CQMinimizer input_file output_file");
            return;
        }

        String inputFile = args[0];
        String outputFile = args[1];

        minimizeCQ(inputFile, outputFile);

//        parsingExample(inputFile);
    }

    /**
     * CQ minimization procedure
     *
     * Assume the body of the query from inputFile has no comparison atoms
     * but could potentially have constants in its relational atoms.
     *
     */
    public static void minimizeCQ(String inputFile, String outputFile) {
        // TODO: add your implementation

        try {
            Query query = QueryParser.parse(Paths.get(inputFile));
            Head head = query.getHead();
            List<Atom> body = query.getBody();
            List<RelationalAtom> bodyRel = new ArrayList<>();
            for (Atom atom : body) {
                bodyRel.add((RelationalAtom) atom);
            }

            List<Variable> headVars = head.getVariables();
            List<String> headTerms = new ArrayList<>();
            for (Variable var : headVars) {
                headTerms.add(var.getName());
            }

            int bodySize = bodyRel.size();
            int index = 0;

            for (int i = 0; i < bodySize; i++) {
                if (has_homomorphism(index, bodyRel, headTerms)) {
                    bodyRel.remove(index);
                }
                else {
                    index++;
                }
            }

            File file = new File(outputFile);
            //creat output file
//            if(!file.getParentFile().exists()) {
//                file.getParentFile().mkdirs();
//            }
            FileWriter fileWriter;
            try {
                fileWriter = new FileWriter(file);
                fileWriter.write(head + " :- " + bodyRel.toString().substring(1, bodyRel.toString().length() - 1));
                fileWriter.close();
                System.out.println("Succesful Minimization");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.out.println("write fail");
            }

        } catch (Exception e) {
            System.err.println("Exception occurred during CQ minimization");
            e.printStackTrace();
        }
    }

    /**
     * Determines if there exists a homomorphism between the atom at removeIndex and any other atom in body.
     *
     * @param removeIndex index of atom to remove from body
     * @param body list of relational atoms to search for homomorphism
     * @param headTerms list of strings representing the terms in the head of the rule
     * @return true if there exists a homomorphism, false otherwise
     */
    public static boolean has_homomorphism(int removeIndex, List<RelationalAtom> body, List<String> headTerms) {
        // Get atom to remove
        RelationalAtom atomToRemove = body.get(removeIndex);
        // Search for homomorphism between atomToRemove and other atoms in body
        for (int i = 0; i < body.size(); i++) {
            RelationalAtom atomTarget = body.get(i);

            if (i == removeIndex) continue;
            // Skip if comparing to same atom or if atom names don't match
            if (!Objects.equals(atomTarget.getName(), atomToRemove.getName())) continue;

            boolean canBeMapped = true;

            // Check if terms can be mapped
            for (int j = 0; j < atomToRemove.getTerms().size(); j++) {
                Term curTerm = atomToRemove.getTerms().get(j);
                Term targetTerm = atomTarget.getTerms().get(j);
                if (((curTerm instanceof Constant) || headTerms.contains(curTerm.toString()))
                        && !curTerm.toString().equals(targetTerm.toString())) {
                    canBeMapped = false;
                    break;
                }
            }
            if (canBeMapped) {
                // Create mappings for terms
                HashMap<Term, Term> mappings = new HashMap<>();
                for (int j = 0; j < atomToRemove.getTerms().size(); j++) {
                    Term cur_term = atomToRemove.getTerms().get(j);
                    Term mapped_term = atomTarget.getTerms().get(j);
                    if (!(cur_term instanceof Constant) && !(headTerms.contains(cur_term.toString()))) {
                        mappings.put(cur_term,mapped_term);
                    }
                }
                // Apply mappings to body and check if they are valid
                List<RelationalAtom> mappedBody = mapping(body, mappings);
                if (checkMappings(body, mappedBody, headTerms)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Creates a new list of relational atoms where the terms in the original atoms are replaced
     * with their corresponding mappings from the given HashMap.
     *
     * @param body list of relational atoms to apply mappings to
     * @param mappings HashMap of Term mappings to apply to the atoms
     * @return a new list of relational atoms with the mapped terms
     */
    public static List<RelationalAtom> mapping(List<RelationalAtom> body, HashMap<Term, Term> mappings) {
        List<RelationalAtom> mappedBody = new ArrayList<>();
        // Apply mappings to each atom in the body
        for (RelationalAtom relationalAtom : body) {
            List<Term> mappedTerms = new ArrayList<>();
            for (int j = 0; j < relationalAtom.getTerms().size(); j++) {
                Term cur_term = relationalAtom.getTerms().get(j);
                if (mappings.get(cur_term) != null) {
                    mappedTerms.add(mappings.get(cur_term));
                } else {
                    mappedTerms.add(cur_term);
                }
            }
            String relationName = relationalAtom.getName();
            mappedBody.add(new RelationalAtom(relationName, mappedTerms));
        }
        return mappedBody;
    }

    /**
     * Checks if the given mappings are a valid homomorphism by verifying that all atoms in mappedBody can be
     * mapped to atoms in the original body while preserving the relationship between their terms.
     *
     * @param body original list of relational atoms
     * @param mappedBody list of relational atoms with terms that have been mapped according to the given mappings
     * @param headTerms list of terms in the head of the rule
     * @return true if the given mappings are a valid homomorphism, false otherwise
     */
    public static boolean checkMappings(List<RelationalAtom> body, List<RelationalAtom> mappedBody, List<String> headTerms) {
        int atomTotal = 0;
        // Check each mapped atom to see if it can be mapped to an atom in the original body
        for (RelationalAtom mappedAtom : mappedBody) {
            for (RelationalAtom bodyAtom : body) {
                if (Objects.equals(mappedAtom.getName(), bodyAtom.getName()) && mappedAtom.getTerms().size() == bodyAtom.getTerms().size()) {
                    boolean homo = true;
                    for (int k = 0; k < mappedAtom.getTerms().size(); k++) {
                        Term cur_mappedTerm = mappedAtom.getTerms().get(k);
                        Term cur_bodyTerm = bodyAtom.getTerms().get(k);
                        if (((cur_mappedTerm instanceof Constant) || headTerms.contains(cur_mappedTerm.toString()))
                                && !cur_mappedTerm.toString().equals(cur_bodyTerm.toString())) {
                            homo = false;
                            break;
                        }
                    }
                    if (homo) {
                        atomTotal++;
                        break;
                    }
                }
            }
        }
        return atomTotal == body.size();
    }

    /**
     * Example method for getting started with the parser.
     * Reads CQ from a file and prints it to screen, then extracts Head and Body
     * from the query and prints them to screen.
     */

    public static void parsingExample(String filename) {

        try {
            Query query = QueryParser.parse(Paths.get(filename));
            // Query query = QueryParser.parse("Q(x, y) :- R(x, z), S(y, z, w)");
            // Query query = QueryParser.parse("Q(x) :- R(x, 'z'), S(4, z, w)");

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
