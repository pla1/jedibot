package social.pla.jedibot;

public class Pair implements Comparable<Pair> {
    private String name;
    private String value;

    public Pair(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String toString() {
       return String.format("%s %s\n", name, value);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int compareTo(Pair pair) {
        return name.compareTo(pair.getName());
    }
}
