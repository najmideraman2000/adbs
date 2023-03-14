package ed.inf.adbs.minibase.operator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class DatabaseCatalog {
    public static DatabaseCatalog instance;
    private String databaseDirectory;
    Map<String, List<String>> schema = new HashMap<>();

    public static DatabaseCatalog getInstance(){
        if (instance == null)
            instance = new DatabaseCatalog();
        return instance;
    }

    public void init(String databaseDirectory) {
        this.databaseDirectory = databaseDirectory;
        String schema_path = this.databaseDirectory + File.separator + "schema.txt";
        try {
            File f = new File(schema_path);
            Scanner scanner = new Scanner(f);
            while (scanner.hasNextLine()) {
                ArrayList<String> fileLine = new ArrayList<>(Arrays.asList(scanner.nextLine().split("\\s+")));
                this.schema.put(fileLine.get(0), fileLine.subList(1, fileLine.size()));
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("Schema file not found at : " + schema_path);
            e.printStackTrace();
        }
    }

    public String getRelationPath(String relationName) {
        return (this.databaseDirectory + File.separator + "files" + File.separator + relationName + ".csv");
    }

    public List<String> getSchema(String relationName) {
        return schema.get(relationName);
    }
}
