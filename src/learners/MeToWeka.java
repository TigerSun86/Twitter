package learners;

import java.util.ArrayList;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import common.RawAttr;
import common.RawAttrList;
import common.RawExample;
import common.RawExampleList;

/**
 * FileName: MeToWeka.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Mar 30, 2015 8:35:39 PM
 */
public class MeToWeka {
    public FastVector attributes = null;
    public Instances dataForConvert = null;

    public MeToWeka(RawAttrList attrs) {
        attributes = convertAttributes(attrs);
        dataForConvert = new Instances("Test-dataset", attributes, 0);
        dataForConvert.setClassIndex(dataForConvert.numAttributes() - 1);
    }

    private static FastVector convertAttributes (RawAttrList attrs) {
        FastVector newAttrs = new FastVector();
        for (RawAttr attr : attrs.xList) {
            Attribute newAttr = convertAttribute(attr);
            newAttrs.addElement(newAttr);
        }
        // Add class attribute.
        Attribute newAttr = convertAttribute(attrs.t);
        newAttrs.addElement(newAttr);
        return newAttrs;
    }

    private static Attribute convertAttribute (RawAttr attr) {
        Attribute newAttr;
        if (attr.isContinuous) {
            newAttr = new Attribute(attr.name);
        } else { // Discrete.
            FastVector values = new FastVector();
            for (String v : attr.valueList) {
                values.addElement(v);
            }
            newAttr = new Attribute(attr.name, values);
        }
        return newAttr;
    }

    public Instances convertInstances (RawExampleList exs) {
        Instances dataset =
                new Instances("Test-dataset", attributes, exs.size());
        dataset.setClassIndex(dataset.numAttributes() - 1);
        for (RawExample e : exs) {
            Instance inst = convertInstance(1.0, e);
            dataset.add(inst);
        }
        return dataset;
    }

    public Instance convertInstance (double weight, RawExample e) {
        double[] values = new double[dataForConvert.numAttributes()];
        for (int i = 0; i < e.xList.size(); i++) {
            double value = convertValue(e.xList.get(i), dataForConvert, i);
            values[i] = value;
        }
        double value =
                convertValue(e.t, dataForConvert,
                        dataForConvert.numAttributes() - 1);
        values[dataForConvert.numAttributes() - 1] = value;
        Instance inst = new Instance(weight, values);
        return inst;
    }

    public Instance getInstForPredict (ArrayList<String> e) {
        double[] values = new double[dataForConvert.numAttributes()];
        for (int i = 0; i < e.size(); i++) {
            double value = convertValue(e.get(i), dataForConvert, i);
            values[i] = value;
        }
        values[dataForConvert.numAttributes() - 1] = 0;
        Instance inst = new Instance(1.0, values);
        inst.setDataset(dataForConvert);
        return inst;
    }

    private static double convertValue (String oldValue, Instances dataset,
            int attrIdx) {
        double value;
        if (dataset.attribute(attrIdx).isNumeric()) { // Continuous value.
            value = Double.parseDouble(oldValue);
        } else { // if (dataset.attribute(i).isNominal()) discrete value.
            value = dataset.attribute(attrIdx).indexOfValue(oldValue);
        }
        return value;
    }

}
