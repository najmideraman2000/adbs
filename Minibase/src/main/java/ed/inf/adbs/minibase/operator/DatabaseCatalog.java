package ed.inf.adbs.minibase.operator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class DatabaseCatalog {
    public static DatabaseCatalog instance;
    private String databaseDir;
    Map<String, List<String>> relationSchemaMap = new HashMap<>();

    public static DatabaseCatalog getInstance(){
        if (instance == null)
            instance = new DatabaseCatalog();
        return instance;
    }

    public void init(String databaseDir) {
        this.databaseDir = databaseDir;
        String schema_path = this.databaseDir + File.separator + "schema.txt";
        try {
            File f = new File(schema_path);
            Scanner scanner = new Scanner(f);
            while (scanner.hasNextLine()) {
                ArrayList<String> line = new ArrayList<>(Arrays.asList(scanner.nextLine().split("\\s+")));
                this.relationSchemaMap.put(line.get(0), line.subList(1, line.size()));
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("Schema file not found at : " + schema_path);
            e.printStackTrace();
        }
    }

    public String getRelationPath(String relationName) {
        return (this.databaseDir + File.separator + "files" + File.separator + relationName + ".csv");
    }

    public List<String> getSchema(String relationName) {
        return relationSchemaMap.get(relationName);
    }
}
