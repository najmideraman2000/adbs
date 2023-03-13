package ed.inf.adbs.minibase.operator;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public abstract class Operator {
    protected List<String> variableMask = new ArrayList<>();

    public void dump(String outputFile) {
        try {
            PrintWriter writer = null;
            if (outputFile != null && !outputFile.equals("")) {
                writer = new PrintWriter(outputFile);
            }

            boolean isFirstLine = true;
            // use this flag to let the print of later lines to begin with a '\n' token
            // (the purpose is to remove the empty line at the end occurred when the print operations are all PrintWriter.println() )

            Tuple nextTuple = this.getNextTuple();
            while (nextTuple != null) {
                if (writer == null) {
                } else {
                    if (isFirstLine) {
                        // if the current tuple is the first line of output, print it without modification
                        writer.print(nextTuple.toString());
                        isFirstLine = false;
                    } else {
                        // when the output file already has some lines, use '\n' to start a new line and then print this tuple
                        writer.print("\n" + nextTuple.toString());
                    }
                }
                nextTuple = this.getNextTuple();
            }

            if (writer!=null)
                writer.close();

        } catch (Exception e) {
            System.err.println("Exception occurred during dump operation");
            e.printStackTrace();
        }
    }

    public abstract void reset();

    public abstract Tuple getNextTuple();

    public List<String> getVariableMask() {
        return this.variableMask;
    }
}
