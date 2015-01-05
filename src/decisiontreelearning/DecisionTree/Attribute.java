package decisiontreelearning.DecisionTree;

/**
 * FileName: Attribute.java
 * @Description: One attribute for learning, contains its name and all possible
 *               values.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Feb 25, 2014
 */
import java.util.ArrayList;

public class Attribute {
    private final String name;
    private final ArrayList<String> valueList;

    public Attribute(final String nameIn) {
        this.name = nameIn;
        this.valueList = new ArrayList<String>();
    }

    public final void add (final String value) {
        this.valueList.add(value);
    }

    public final void addAll (final ArrayList<String> valueListIn) {
        this.valueList.addAll(valueListIn);
    }

    public final String getName () {
        return name;
    }

    public final ArrayList<String> valueList () {
        return valueList;
    }

    @Override
    public final boolean equals (final Object attr) {
        if (!(attr instanceof Attribute)) {
            return false;
        }
        return name.equals(((Attribute) attr).getName());
    }

    @Override
    public final int hashCode () {
        return name.hashCode();
    }

    @Override
    public final String toString () {
        return name.toString() + valueList;
    }
}
