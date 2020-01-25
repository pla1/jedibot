package social.pla.jedibot;

public class Dump {
    public static void main(String[] args) {
       new Dump();
    }
    public Dump() {
        String[] tables = {"application", "feed", "subscription"};
        for (String table:tables) {
            System.out.format("\n\nTABLE: %s\n", table);
            Utils.printTable(table);
        }
    }
}
