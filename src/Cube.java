import java.util.Arrays;

public class Cube {
    public char[] data;

    public Cube(String string) {
        this.data = string.toCharArray();
    }

    public Cube(Cube other) {
        char[] temp = new char[24];
        for (int i = 0; i < 24; i++) {
            temp[i] = other.data[i];
        }
        this.data = temp;
    }

    public Cube(char[] other) {
        char[] temp = new char[24];
        for (int i = 0; i < 24; i++) {
            temp[i] = other[i];
        }
        this.data = temp;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public boolean equals(Object other) {
        return hashCode() == other.hashCode();
    }

    @Override
    public String toString() {
        return String.valueOf(data);
    }
}
