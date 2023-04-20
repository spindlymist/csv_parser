package csv;

import java.io.File;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        CSVParser csv = new CSVParser();
        try {
            List<List<String>> records = csv.ParseFile(new File("data/test_01.csv"));
            for (List<String> record : records) {
                for (String s : record) {
                    System.out.print(s + " | ");
                }
                System.out.println("\n--------------------------------------------------------------------------------");
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
