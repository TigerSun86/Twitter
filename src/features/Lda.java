package features;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.regex.Pattern;

import twitter4j.Status;
import util.Dbg;
import util.SysUtil;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import features.WordStatisDoc.WordStatisDocSetting;

/**
 * FileName: Lda.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Aug 9, 2015 8:39:12 PM
 */
public class Lda {
    public static class LdaSetting {
        public WordStatisDocSetting docPara = new WordStatisDocSetting();
        public int numOfCl = 10;
        public int numOfIter = 2000;
    }

    public LdaSetting para = new LdaSetting();

    public static class InferencerAndPipe {
        TopicInferencer inferencer;
        Pipe pipe;

        public InferencerAndPipe(TopicInferencer inferencer, Pipe pipe) {
            super();
            this.inferencer = inferencer;
            this.pipe = pipe;
        }
    }

    public InferencerAndPipe cluster (List<Status> tweets) {
        long time = SysUtil.getCpuTime();
        WordStatisDoc doc = new WordStatisDoc(this.para.docPara);
        doc.init(tweets);

        if (Dbg.dbg) {
            System.out.println("**** LDA");
            System.out.printf("Author: %s, numOfCl: %d, numOfIter: %d%n",
                    tweets.get(0).getUser().getScreenName(), para.numOfCl,
                    para.numOfIter);
            System.out.println(para.docPara.toString());
            System.out.println("Size of vocabulary: " + doc.wordList.size());
        }

        // Begin by importing documents from text to feature sequences
        ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

        // It's already in lower case and no stop words.
        pipeList.add(new CharSequence2TokenSequence(Pattern
                .compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
        pipeList.add(new TokenSequence2FeatureSequence());

        InstanceList instances = new InstanceList(new SerialPipes(pipeList));

        StringBuilder sb = new StringBuilder();
        for (List<String> d : doc.wordsOfDocs) {
            assert d.size() >= 1;
            // Clear and reuse StringBuilder
            sb.setLength(0);
            for (String s : d) {
                sb.append(s + " ");
            }
            sb.deleteCharAt(sb.length() - 1); // Remove the last space.
            instances.addThruPipe(new Instance(sb.toString(), null,
                    "Lda instances", null));
        }

        // Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
        // Note that the first parameter is passed as the sum over topics, while
        // the second is the parameter for a single dimension of the Dirichlet
        // prior.
        ParallelTopicModel.logger.setLevel(Level.OFF);
        int numTopics = para.numOfCl;
        ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);
        model.setRandomSeed(1); // Set RandomSeed before add instances.
        model.addInstances(instances);

        // Same data set, in 2 and 4 threads need 31 seconds, in 1 thread only
        // need 1 second, so just 1 thread.
        model.setNumThreads(1);

        // Run the model for 50 iterations and stop (this is for testing only,
        // for real applications, use 1000 to 2000 iterations)
        model.setNumIterations(para.numOfIter);
        try {
            model.estimate();
        } catch (IOException e) {
            e.printStackTrace();
        }
        TopicInferencer inferencer = model.getInferencer();
        inferencer.setRandomSeed(1);

        if (Dbg.dbg) {
            // The data alphabet maps word IDs to strings
            Alphabet dataAlphabet = instances.getDataAlphabet();

            // Get an array of sorted sets of word ID/count pairs
            ArrayList<TreeSet<IDSorter>> topicSortedWords =
                    model.getSortedWords();
            System.out.println("**** top 20 words in topics:");
            // Show top 20 words in topics
            for (int topic = 0; topic < numTopics; topic++) {
                Iterator<IDSorter> iterator =
                        topicSortedWords.get(topic).iterator();

                Formatter out = new Formatter(new StringBuilder(), Locale.US);
                out.format("%d\t", topic);
                int rank = 0;
                while (iterator.hasNext() && rank < 20) {
                    IDSorter idCountPair = iterator.next();
                    out.format("%s ",
                            dataAlphabet.lookupObject(idCountPair.getID()));
                    out.format("(%.0f) ", idCountPair.getWeight());

                    rank++;
                }
                System.out.println(out);
            }
            System.out.println();

            // Create a new instance with high probability of topic 0
            StringBuilder topicZeroText = new StringBuilder();
            Iterator<IDSorter> iterator = topicSortedWords.get(0).iterator();

            int rank = 0;
            while (iterator.hasNext() && rank < 20) {
                IDSorter idCountPair = iterator.next();
                topicZeroText.append(dataAlphabet.lookupObject(idCountPair
                        .getID()) + " ");
                rank++;
            }
            topicZeroText.setLength(0);
            // Create a new instance named "test instance" with empty target and
            // source fields.
            InstanceList testing = new InstanceList(instances.getPipe());
            testing.addThruPipe(new Instance(topicZeroText.toString(), null,
                    "test instance", null));
            System.out.println("Fake instance from topic 0: "
                    + topicZeroText.toString());

            // Don't need to care about the words appearing only in test set but
            // not in training set, because Mallet ignores the words like that,
            // see TopicInferencer.java line 101.
            // Parameters 100,10,10 are in the tutorial slide:
            // http://mallet.cs.umass.edu/mallet-tutorial.pdf page 105.
            double[] testProbabilities =
                    inferencer.getSampledDistribution(testing.get(0), 100, 10,
                            10);
            System.out.println("0\t" + Arrays.toString(testProbabilities));

            System.out.println("Time used: " + (SysUtil.getCpuTime() - time));
        }
        return new InferencerAndPipe(inferencer, instances.getPipe());
    }
}
