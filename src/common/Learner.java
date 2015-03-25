package common;

/**
 * FileName:     Learner.java
 * @Description: 
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Nov 27, 2014 4:50:32 PM 
 */
public interface Learner {
    public ProbPredictor learn (RawExampleList dataSet, RawAttrList attrs) ;
}
