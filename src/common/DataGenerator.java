package common;

import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

import common.Region.Circle;
import common.Region.NotRegion;
import common.Region.Parallelogram;
import common.Region.Ribbon;

/**
 * FileName: DataGenerator.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Jun 17, 2014 11:31:32 AM
 */
public class DataGenerator {
    public static final String[] CLASS = { "Y", "N" };

    private static final double R1 = (1 - (Math.sqrt(2)) / 2);
    private static final Point2D.Double[] C1 = {
            new Point2D.Double(1 - R1, R1), new Point2D.Double(R1, 1 - R1) };
    private static final int NUM = 80;

    private static final double RATIO = 0.5;

    private static final RawAttrList ATTRS = new RawAttrList();
    static {
        ATTRS.xList.add(new RawAttr("X1", true));
        ATTRS.xList.add(new RawAttr("X2", true));
        ATTRS.t = new RawAttr("Class");
        ATTRS.t.valueList.add(CLASS[0]);
        ATTRS.t.valueList.add(CLASS[1]);
    }

    public static void main (String[] args) {
        geneDenseXor();
    }

    private static void geneCircle () {
        final Region yC = new Circle(C1[0], R1);
        final Region nC = new Circle(C1[1], R1);
        final HashSet<Point2D.Double> ySet = gene(yC, NUM);
        final HashSet<Point2D.Double> nSet = gene(nC, NUM);

        final RawExampleList s = new RawExampleList();
        for (Point2D.Double p : ySet) {
            final RawExample e = new RawExample();
            e.xList.add(String.valueOf(p.x));
            e.xList.add(String.valueOf(p.y));
            e.t = CLASS[0];
            s.add(e);
        }
        for (Point2D.Double p : nSet) {
            final RawExample e = new RawExample();
            e.xList.add(String.valueOf(p.x));
            e.xList.add(String.valueOf(p.y));
            e.t = CLASS[1];
            s.add(e);
        }
        final RawExampleList[] sub =
                TrainTestSplitter.split(s, ATTRS,
                        RATIO);
        System.out.println("Sub 1 size: " + sub[0].size());
        System.out.println(sub[0]);
        System.out.println("Sub 2 size: " + sub[1].size());
        System.out.println(sub[1]);
    }

    private static final Ribbon[] C1RIB = { new Ribbon(1, 0, -0.75),
            new Ribbon(-1, 0.75, 1.25) };
    private static final Ribbon[] C2RIB = { new Ribbon(1, 0, 0.75),
            new Ribbon(-1, 0.75, 1.25) };
    private static final Parallelogram C1RECT = new Parallelogram(C1RIB[0],
            C1RIB[1]);
    private static final Parallelogram C2RECT = new Parallelogram(C2RIB[0],
            C2RIB[1]);

    private static void geneRectangle () {
        final HashSet<Point2D.Double> ySet = gene(C1RECT, NUM);
        final HashSet<Point2D.Double> nSet = gene(C2RECT, NUM);

        final RawExampleList s = new RawExampleList();
        for (Point2D.Double p : ySet) {
            final RawExample e = new RawExample();
            e.xList.add(String.valueOf(p.x));
            e.xList.add(String.valueOf(p.y));
            e.t = CLASS[0];
            s.add(e);
        }
        for (Point2D.Double p : nSet) {
            final RawExample e = new RawExample();
            e.xList.add(String.valueOf(p.x));
            e.xList.add(String.valueOf(p.y));
            e.t = CLASS[1];
            s.add(e);
        }
        final RawExampleList[] sub =
                TrainTestSplitter.split(s, ATTRS,
                        RATIO);
        System.out.println("Sub 1 size: " + sub[0].size());
        System.out.println(sub[0]);
        System.out.println("Sub 2 size: " + sub[1].size());
        System.out.println(sub[1]);
    }

    /* Y(a)N(b)
     * N(c)Y(d) */
    private static final Ribbon[] XOR_RIB_HOR = { new Ribbon(0, 0, 0.5),
            new Ribbon(0, 0.5, 1) };
    private static final Ribbon[] XOR_RIB_VER = { new Ribbon(0, 0.5),
            new Ribbon(0.5, 1) };
    private static final Parallelogram[] XOR_REG_C1 = {
            new Parallelogram(XOR_RIB_HOR[1], XOR_RIB_VER[0]),
            new Parallelogram(XOR_RIB_HOR[0], XOR_RIB_VER[1]) };
    private static final Parallelogram[] XOR_REG_C2 = {
            new Parallelogram(XOR_RIB_HOR[0], XOR_RIB_VER[0]),
            new Parallelogram(XOR_RIB_HOR[1], XOR_RIB_VER[1]) };

    private static void geneXor () {
        final RawExampleList train = new RawExampleList();
        final RawExampleList test = new RawExampleList();

        // Generate and split instances for each region separately,
        // To guarantee each region will have equal number in train and test.
        // a region.
        RawExampleList[] sub =
                geneTrainAndTest(XOR_REG_C1[0], NUM, RATIO, CLASS[0]);
        train.addAll(sub[0]);
        test.addAll(sub[1]);

        // d region.
        sub = geneTrainAndTest(XOR_REG_C1[1], NUM, RATIO, CLASS[0]);
        train.addAll(sub[0]);
        test.addAll(sub[1]);

        // b region.
        sub = geneTrainAndTest(XOR_REG_C2[0], NUM, RATIO, CLASS[1]);
        train.addAll(sub[0]);
        test.addAll(sub[1]);

        // c region.
        sub = geneTrainAndTest(XOR_REG_C2[1], NUM, RATIO, CLASS[1]);
        train.addAll(sub[0]);
        test.addAll(sub[1]);

        Collections.shuffle(train);
        Collections.shuffle(test);

        System.out.println("train size: " + train.size());
        System.out.println(train);
        System.out.println("test size: " + test.size());
        System.out.println(test);
    }

    private static final Ribbon[] XOR_D_RIB_HOR = { new Ribbon(0, 0.2, 0.3),
            new Ribbon(0, 0.7, 0.8) };
    private static final Ribbon[] XOR_D_RIB_VER = { new Ribbon(0.2, 0.3),
            new Ribbon(0.7, 0.8) };
    private static final Parallelogram[] XOR_D_REG_C1 = {
            new Parallelogram(XOR_D_RIB_HOR[1], XOR_D_RIB_VER[0]),
            new Parallelogram(XOR_D_RIB_HOR[0], XOR_D_RIB_VER[1]) };
    private static final Parallelogram[] XOR_D_REG_C2 = {
            new Parallelogram(XOR_D_RIB_HOR[0], XOR_D_RIB_VER[0]),
            new Parallelogram(XOR_D_RIB_HOR[1], XOR_D_RIB_VER[1]) };
    private static final int D_NUM = 20;

    private static final void geneDense (final Region reg,
            final String className, final RawExampleList train,
            final RawExampleList test) {
        int remain = D_NUM;
        while (remain != 0) {
            final RawExampleList setD =
                    geneAdditionalSet(reg, remain, className, train, test);
            remain -= setD.size();
            train.addAll(setD);
        }
        remain = D_NUM;
        while (remain != 0) {
            final RawExampleList setD =
                    geneAdditionalSet(reg, remain, className, train, test);
            remain -= setD.size();
            test.addAll(setD);
        }
    }

    private static void geneDenseXor () {
        final RawExampleList train = new RawExampleList();
        final RawExampleList test = new RawExampleList();

        final RawExampleList cleanTrain = new RawExampleList();
        final RawExampleList cleanTest = new RawExampleList();
        // Generate and split instances for each region separately,
        // To guarantee each region will have equal number in train and test.
        // a region.
        RawExampleList[] sub =
                geneTrainAndTest(XOR_REG_C1[0], NUM, RATIO, CLASS[0]);
        train.addAll(sub[0]);
        test.addAll(sub[1]);
        cleanTrain.addAll(sub[0]);
        cleanTest.addAll(sub[1]);

        geneDense(XOR_D_REG_C1[0], CLASS[0], train, test);

        // d region.
        sub = geneTrainAndTest(XOR_REG_C1[1], NUM, RATIO, CLASS[0]);
        train.addAll(sub[0]);
        test.addAll(sub[1]);
        cleanTrain.addAll(sub[0]);
        cleanTest.addAll(sub[1]);

        geneDense(XOR_D_REG_C1[1], CLASS[0], train, test);

        // b region.
        sub = geneTrainAndTest(XOR_REG_C2[0], NUM, RATIO, CLASS[1]);
        train.addAll(sub[0]);
        test.addAll(sub[1]);
        cleanTrain.addAll(sub[0]);
        cleanTest.addAll(sub[1]);

        geneDense(XOR_D_REG_C2[0], CLASS[1], train, test);

        // c region.
        sub = geneTrainAndTest(XOR_REG_C2[1], NUM, RATIO, CLASS[1]);
        train.addAll(sub[0]);
        test.addAll(sub[1]);
        cleanTrain.addAll(sub[0]);
        cleanTest.addAll(sub[1]);

        geneDense(XOR_D_REG_C2[1], CLASS[1], train, test);

        Collections.shuffle(train);
        Collections.shuffle(test);
        Collections.shuffle(cleanTrain);
        Collections.shuffle(cleanTest);

        System.out.println("train size: " + train.size());
        System.out.println(train);
        System.out.println("test size: " + test.size());
        System.out.println(test);
        System.out.println("clean train size: " + cleanTrain.size());
        System.out.println(cleanTrain);
        System.out.println("clean test size: " + cleanTest.size());
        System.out.println(cleanTest);
    }

    /* NNN
     * NYN
     * NNN */
    private static final Parallelogram SQ_REG1 = new Parallelogram(new Ribbon(
            0, (2 - Math.sqrt(2)) / 4, (2 + Math.sqrt(2)) / 4), new Ribbon(
            (2 - Math.sqrt(2)) / 4, (2 + Math.sqrt(2)) / 4));
    private static final NotRegion SQ_REG2 = new NotRegion(SQ_REG1);

    private static void geneSquare () {
        final RawExampleList train = new RawExampleList();
        final RawExampleList test = new RawExampleList();

        RawExampleList[] sub = geneTrainAndTest(SQ_REG1, NUM, RATIO, CLASS[0]);
        train.addAll(sub[0]);
        test.addAll(sub[1]);
        sub = geneTrainAndTest(SQ_REG2, NUM, RATIO, CLASS[1]);
        train.addAll(sub[0]);
        test.addAll(sub[1]);

        Collections.shuffle(train);
        Collections.shuffle(test);

        System.out.println("train size: " + train.size());
        System.out.println(train);
        System.out.println("test size: " + test.size());
        System.out.println(test);
    }

    private static RawExampleList[] geneTrainAndTest (final Region reg,
            final int num, final double ratio, final String className) {
        HashSet<Point2D.Double> set = gene(reg, num);
        RawExampleList s = new RawExampleList();
        for (Point2D.Double p : set) {
            final RawExample e = new RawExample();
            e.xList.add(String.valueOf(p.x));
            e.xList.add(String.valueOf(p.y));
            e.t = className;
            s.add(e);
        }

        RawExampleList[] sub = TrainTestSplitter.split(s, ATTRS, ratio);
        return sub;
    }

    private static RawExampleList geneAdditionalSet (final Region reg,
            final int num, final String className, final RawExampleList old1,
            final RawExampleList old2) {
        HashSet<Point2D.Double> set = gene(reg, num);
        RawExampleList s = new RawExampleList();
        for (Point2D.Double p : set) {
            final RawExample e = new RawExample();
            e.xList.add(String.valueOf(p.x));
            e.xList.add(String.valueOf(p.y));
            e.t = className;
            if (!old1.contains(e) && !old2.contains(e)) {
                s.add(e);
            }
        }
        return s;
    }

    private static HashSet<Point2D.Double>
            gene (final Region reg, final int num) {
        final Random ran = new Random();
        final HashSet<Point2D.Double> set = new HashSet<Point2D.Double>();
        while (set.size() < num) {
            final Point2D.Double p =
                    new Point2D.Double(ran.nextDouble(), ran.nextDouble());
            if (reg.isInside(p)) {
                set.add(p);
            }
        }
        return set;
    }
}
