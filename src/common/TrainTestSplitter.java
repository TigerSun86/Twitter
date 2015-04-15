package common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import util.MyMath;

/**
 * FileName: TrainTestSplitter.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Apr 20, 2014 11:45:34 PM
 */
public class TrainTestSplitter {
    public static final double DEFAULT_RATIO = 2.0 / 3;
    public static long randomSeed = 1;

    /* private static final String FILE =
     * "http://archive.ics.uci.edu/ml/machine-learning-databases/breast-cancer-wisconsin/wdbc.data"
     * ; */
    @SuppressWarnings("unused")
    private static final String FILE =
            "http://my.fit.edu/~sunx2013/MachineLearning/car.txt";
    @SuppressWarnings("unused")
    private static final String ATTR =
            "http://my.fit.edu/~sunx2013/MachineLearning/car-attr.txt";

    public static void main (String[] args) {
        // final String fName = deleteCommaAndPutClassBack(FILE);
        // generateTrainTest(FILE, ATTR);
        // final DataSet set = new Iris();
        // generateKFoldTrainTest(set.getDataFileUrl(), set.getAttrFileUrl(),
        // 10);
    }

    @SuppressWarnings("unused")
    private static String
            deleteIDAndCommaAndPutClassBack (final String fileName) {
        final RawExampleList exs = new RawExampleList();
        final DataReader in = new DataReader(fileName);
        int numOfAttr = -1;
        while (true) {
            final String line = in.nextLine();
            if (line == null) {
                break;
            }
            if (line.length() <= 1) {
                continue; // Skip empty line.
            }
            final String[] examStr = line.split(",");
            final RawExample ex = new RawExample();
            // 0 is id, 1 is class.
            for (int i = 2; i <= examStr.length - 1; i++) {
                ex.xList.add(examStr[i]);
            }
            ex.t = examStr[1];

            if (numOfAttr == -1) { // Initialize.
                numOfAttr = ex.xList.size();
                exs.add(ex);
            } else if (numOfAttr == ex.xList.size()) {
                exs.add(ex);
            } else {
                System.err.println("Inconsistent number of attributes in line "
                        + (in.getLineNumber() + 1));
            }
        } // End of while (true) {
        in.close();

        final String name =
                fileName.substring(fileName.lastIndexOf('/') + 1,
                        fileName.lastIndexOf('.'));
        final String resourcePath =
                Thread.currentThread().getContextClassLoader().getResource("")
                        .toString();
        final String newName = resourcePath + name + ".txt";
        System.out.println(newName);
        writeExamples(exs, newName);
        return newName;
    }

    @SuppressWarnings("unused")
    private static String deleteCommaAndPutClassBack (final String fileName) {
        final RawExampleList exs = new RawExampleList();
        final DataReader in = new DataReader(fileName);
        int numOfAttr = -1;
        while (true) {
            final String line = in.nextLine();
            if (line == null) {
                break;
            }
            if (line.length() <= 1) {
                continue; // Skip empty line.
            }
            final String[] examStr = line.split(",");
            final RawExample ex = new RawExample();
            // 0 is class.
            for (int i = 1; i <= examStr.length - 1; i++) {
                ex.xList.add(examStr[i]);
            }
            ex.t = examStr[0];

            if (numOfAttr == -1) { // Initialize.
                numOfAttr = ex.xList.size();
                exs.add(ex);
            } else if (numOfAttr == ex.xList.size()) {
                exs.add(ex);
            } else {
                System.err.println("Inconsistent number of attributes in line "
                        + (in.getLineNumber() + 1));
            }
        } // End of while (true) {
        in.close();

        final String name =
                fileName.substring(fileName.lastIndexOf('/') + 1,
                        fileName.lastIndexOf('.'));
        final String resourcePath =
                Thread.currentThread().getContextClassLoader().getResource("")
                        .toString();
        final String newName = resourcePath + name + ".txt";
        System.out.println(newName);
        writeExamples(exs, newName);
        return newName;
    }

    @SuppressWarnings("unused")
    private static void generateTrainTest (final String fileName,
            final String attrFile) {
        final RawAttrList rawAttr = new RawAttrList(attrFile);
        final RawExampleList exs = new RawExampleList(fileName);
        Collections.shuffle(exs); // Shuffle examples.
        final RawExampleList[] exs2 = split(exs, rawAttr, DEFAULT_RATIO);
        final RawExampleList train = exs2[0];
        final RawExampleList test = exs2[1];

        final String name =
                fileName.substring(fileName.lastIndexOf('/') + 1,
                        fileName.lastIndexOf('.'));

        final String resourcePath =
                Thread.currentThread().getContextClassLoader().getResource("")
                        .toString();
        final String trainName = resourcePath + name + "-train.txt";
        final String testName = resourcePath + name + "-test.txt";
        writeExamples(train, trainName);
        System.out.println(trainName);
        writeExamples(test, testName);
        System.out.println(testName);
    }

    @SuppressWarnings("unused")
    private static void generateKFoldTrainTest (final String fileName,
            final String attrFile, final int k) {
        final RawAttrList rawAttr = new RawAttrList(attrFile);
        final RawExampleList exs = new RawExampleList(fileName);
        Collections.shuffle(exs); // Shuffle examples.

        final RawExampleList[] folds =
                TrainTestSplitter.splitSetIntoKFold(exs, rawAttr, k);

        final String name =
                fileName.substring(fileName.lastIndexOf('/') + 1,
                        fileName.lastIndexOf('.'));

        final String resourcePath =
                Thread.currentThread().getContextClassLoader().getResource("")
                        .toString();
        for (int test = 0; test < k; test++) {
            final RawExampleList testSet = folds[test];
            final RawExampleList trainSet = new RawExampleList();
            for (int other = 0; other < k; other++) {
                if (other != test) { // All other set is train set.
                    trainSet.addAll(folds[other]);
                }
            }

            final String trainName =
                    resourcePath + name + "-train-" + test + ".txt";
            final String testName =
                    resourcePath + name + "-test-" + test + ".txt";
            writeExamples(trainSet, trainName);
            System.out.println(trainName + " " + trainSet.size());
            writeExamples(testSet, testName);
            System.out.println(testName + " " + testSet.size());
        }
    }

    private static void writeExamples (final RawExampleList exs,
            final String fileName) {
        DataWriter out = new DataWriter(fileName);
        if (out.hasNoException()) {
            out.write(exs.toString());
            out.close();
        }
    }

    public static BitSet keptByRandom (RawExampleList exs, RawAttrList attrs,
            double ratioKeeping) {
        final ArrayList<String> classList = attrs.t.valueList;

        // Count number of instances for each class.
        final int[] numForEachClass = new int[classList.size()];
        for (RawExample e : exs) {
            final int classI = classList.indexOf(e.t);
            numForEachClass[classI]++;
        }

        // Get selected indexes for each class.
        final int[][] selectedOfEachClass = new int[classList.size()][];
        for (int i = 0; i < classList.size(); i++) {
            selectedOfEachClass[i] =
                    MyMath.mOutofN(
                            (int) Math.round(numForEachClass[i] * ratioKeeping),
                            numForEachClass[i], randomSeed);
            // Sort it ascendingly to make the picking later easier.
            Arrays.sort(selectedOfEachClass[i]);
        }

        // Get the final kept indexes by selected of class.
        final BitSet kept = new BitSet(exs.size());
        final int[] counterForEachClass = new int[classList.size()];
        final int[] counterForSelected = new int[classList.size()];
        for (int i = 0; i < exs.size(); i++) {
            final RawExample e = exs.get(i);
            final int classI = classList.indexOf(e.t);
            // This e is in selectedOfEachClass[classI].
            if ((selectedOfEachClass[classI].length != 0)
                    && (selectedOfEachClass[classI].length != counterForSelected[classI])
                    && (selectedOfEachClass[classI][counterForSelected[classI]] == counterForEachClass[classI])) {
                kept.set(i);
                counterForSelected[classI]++;
            }
            counterForEachClass[classI]++;
        }

        return kept;
    }

    /**
     * public static RawExampleList[] splitSetIntoKFold (RawExampleList exs,
     * RawAttrList attrs, int k) {
     * 
     * Splits given RawExampleList into k lists randomly.
     * 
     * This method guarantees returning k lists with same class ratio, and
     * won't change the original order of examples.
     * 
     * If the class is continuous value, just split into k lists, while still
     * won't change the original order of examples.
     * 
     * @return: An array with k ExampleSets.
     */
    public static RawExampleList[] splitSetIntoKFold (RawExampleList exs,
            RawAttrList attrs, int k) {
        if (attrs.t.isContinuous) { // For continuous class.
            List<Integer> idxes = new ArrayList<Integer>();
            for (int idx = 0; idx < exs.size(); idx++) {
                idxes.add(idx);
            }
            List<HashSet<Integer>> splitted =
                    MyMath.splitToKSets(idxes, k, randomSeed);
            final RawExampleList[] exArray = new RawExampleList[k];
            for (int ik = 0; ik < k; ik++) {
                exArray[ik] = new RawExampleList();
                HashSet<Integer> idxesInThisSet = splitted.get(ik);
                for (int idx = 0; idx < exs.size(); idx++) {
                    if (idxesInThisSet.contains(idx)) {
                        exArray[ik].add(exs.get(idx));
                    }
                }
            } // for (int ik = 0; ik < k; ik++)
            return exArray;
        }

        // Else discrete class.
        final ArrayList<String> classList = attrs.t.valueList;

        // Count number of instances for each class.
        final int[] numForEachClass = new int[classList.size()];
        for (RawExample e : exs) {
            final int classI = classList.indexOf(e.t);
            numForEachClass[classI]++;
        }

        // Calculate each fold should keep how many number of examples for each
        // class.
        final int[][] numOfExNeeded = new int[classList.size()][k];
        for (int cla = 0; cla < classList.size(); cla++) {
            numOfExNeeded[cla] = new int[k];
            int exRemain = numForEachClass[cla];
            for (int ki = 0; ki < k; ki++) {
                // Try to make each fold have same number of examples in each
                // class, If don't use exRemain / (k - ki), last fold might have
                // much more/less examples than all other folds.
                numOfExNeeded[cla][ki] =
                        (int) Math.round(((double) exRemain) / (k - ki));
                exRemain -= numOfExNeeded[cla][ki];
            }
            assert exRemain == 0;
        }
        // Result for return.
        final RawExampleList[] exArray = new RawExampleList[k];
        for (int i = 0; i < k; i++) {
            exArray[i] = new RawExampleList();
        }

        // Assign each example randomly to each fold.
        final Random ran = new Random(randomSeed);
        for (RawExample e : exs) {
            final int classIndexOfe = classList.indexOf(e.t);
            boolean isRunning = true;
            while (isRunning) {
                final int kBelong = ran.nextInt(k);
                if (numOfExNeeded[classIndexOfe][kBelong] > 0) {
                    // If kBelong still need example for class classIndexOfe.
                    exArray[kBelong].add(e);
                    numOfExNeeded[classIndexOfe][kBelong]--;
                    isRunning = false;
                } // else kBelong is full, try another kBelong.
            }
        }
        return exArray;
    }

    public static RawExampleList[] splitSetInto3Fold (final RawExampleList exs,
            final RawAttrList attrs) {
        final RawExampleList[] set1 = split(exs, attrs, 1.0 / 3);
        final RawExampleList[] set2 = split(set1[1], attrs, 0.5);
        final RawExampleList[] exArray = new RawExampleList[3];
        exArray[0] = set1[0];
        exArray[1] = set2[0];
        exArray[2] = set2[1];
        return exArray;
    }

    /**
     * public static RawExampleList[] split
     * (RawExampleList exs, RawAttrList attrs, double ratio)
     * 
     * Splits given RawExampleList into 2 lists by given ratio in percentage.
     * The first output RawExampleList has the number of examples of the ratio,
     * the second one has remaining examples. Splits examples randomly.
     * 
     * This method guarantees returning 2 lists with same class ratio.
     * 
     * @return: An array with 2 ExampleSets.
     */
    public static RawExampleList[] split (final RawExampleList exs,
            final RawAttrList attrs, final double ratio) {
        final RawExampleList[] exArray = new RawExampleList[2];
        if (Double.compare(ratio, 1) >= 0) { // Special case.
            exArray[0] = exs;
            exArray[1] = new RawExampleList();
            return exArray;
        } else if (Double.compare(ratio, 0) <= 0) { // Special case.
            exArray[0] = new RawExampleList();
            exArray[1] = exs;
            return exArray;
        }
        exArray[0] = new RawExampleList();
        exArray[1] = new RawExampleList();

        // Guarantee each class getting the same ratioKeeping.
        final BitSet kept = keptByRandom(exs, attrs, ratio);

        for (int i = 0; i < exs.size(); i++) {
            if (kept.get(i)) {
                exArray[0].add(exs.get(i));
            } else {
                exArray[1].add(exs.get(i));
            }
        }
        return exArray;
    }

    public static RawExampleList[] splitSetbyClass (final RawExampleList s,
            final RawAttrList attrs) {
        final ArrayList<String> classes = attrs.t.valueList;
        final RawExampleList[] subS = new RawExampleList[classes.size()];
        for (int i = 0; i < subS.length; i++) {
            subS[i] = new RawExampleList();
        }
        for (RawExample e : s) {
            final int index = classes.indexOf(e.t);
            subS[index].add(e);
        }
        return subS;
    }
}
