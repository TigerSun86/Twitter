package decisiontreelearning.DecisionTree;

/**
 * FileName: Example.java
 * @Description: One example, n-1 item is train value, the last item is target
 *               classification.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Feb 25, 2014
 */
import java.util.ArrayList;
import java.util.Collection;

public class Example {
    private final ArrayList<String> example;

    public Example() {
        this.example = new ArrayList<String>();
    }

    public final ArrayList<String> getExample () {
        return example;
    }

    public final String get (final int index) {
        return example.get(index);
    }

    public final void add (final String value) {
        example.add(value);
    }

    public final void addAll (final Collection<String> values) {
        example.addAll(values);
    }

    public final int size () {
        return example.size();
    }

    public final boolean isEmpty () {
        return example.isEmpty();
    }

    public final boolean checkValue (final int index, final String value) {
        return value.equals(example.get(index));
    }

    @Override
    public final boolean equals (final Object otherExample) {
        if (!(otherExample instanceof Example)) {
            return false;
        }
        final Example ex2 = (Example) otherExample;
        if (this.size() != ex2.size()) {
            return false;
        }
        for (int i = 0; i < this.size(); i++) {
            final String valueInEx2 = ex2.get(i);
            if (!this.get(i).equals(valueInEx2)) {
                return false;
            }
        }
        return true;
    }

    private static final int PRIME = 7;
    private static final int INIT = 3;

    @Override
    public final int hashCode () {
        int hash = INIT;
        for (String value : example) {
            hash = PRIME * hash + value.hashCode();
        }
        return hash;
    }

    @Override
    public final String toString () {
        final StringBuffer sBuffer = new StringBuffer();

        for (String value : example) {
            sBuffer.append(value);
            sBuffer.append(" ");
        }
        if (sBuffer.length() != 0) {
            // Delete the redundant " ".
            sBuffer.deleteCharAt(sBuffer.length() - 1);
        }
        return sBuffer.toString();
    }
}
