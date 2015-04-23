package features;

import java.util.Random;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.ClassifierSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.AttributeSelectedClassifier;
import weka.core.Instances;

/**
 * FileName: AttrSel.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Apr 16, 2015 6:56:15 PM
 */
public class AttrSel {
    private AttributeSelection attsel;
    private Instances dataBeforeSel;
    private ASSearch search;

    public AttrSel(ASSearch search) {
        this.search = search;
    }

    public AttrSel() {
        this(new weka.attributeSelection.BestFirst());
    }

    public void showSelAttr () {
        try {
            int[] indices = attsel.selectedAttributes();
            System.out.println("selected attribute indices:");
            for (int i = 0; i < indices.length; i++) {
                if (i != 0) {
                    System.out.print(", ");
                }
                System.out.print(dataBeforeSel.attribute(i).name());
            }
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void selectAttr (Instances data, Classifier base) {
        ClassifierSubsetEval eval = new ClassifierSubsetEval();
        eval.setClassifier(base);
        selectAttr(data, eval);
    }

    public void selectAttr (Instances data) {
        CfsSubsetEval eval = new CfsSubsetEval();
        selectAttr(data, eval);
    }

    private void selectAttr (Instances data, ASEvaluation eval) {
        dataBeforeSel = data;
        attsel = new AttributeSelection();
        attsel.setEvaluator(eval);
        attsel.setSearch(search);
        try {
            attsel.SelectAttributes(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Instances reduceInstDi (Instances data) {
        Instances newData = null;
        ;
        try {
            newData = attsel.reduceDimensionality(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newData;
    }
}
