package decisiontreelearning.Rule;

/**
 * FileName: RuleCondition.java
 * @Description: RuleCondition structure, to represent either precondition or
 *               postcondition in Rule.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Feb 25, 2014
 */
public class RuleCondition {
    public final String name;
    public final String value;

    public RuleCondition(String name, String value) {
        this.name = name;
        this.value = value;
    }

    // For postcondition, usually don'tList care about the name.
    public RuleCondition(String value) {
        this.name = null;
        this.value = value;
    }

    @Override
    public String toString () {
        return name + "=" + value.toString();
    }
}
