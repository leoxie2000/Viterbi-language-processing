import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class Viterbi {
    Map<String,Integer> stateFrequency = new HashMap<>();
    Map<String,Map<String,Integer>> transitionMap = new HashMap<>(); //<State,<next state,frequency>>
    Map<String,Map<String,Integer>> observationProbabilityMap = new HashMap<>();//<state,<observation,frequency>>
    List<Map<String,String>> backpointer = new ArrayList<>(); //[map<nextstate,currentstate>]
    Map<String,Map<String,Double>> normalizedTransitionMap = new HashMap<>(); //transition map after taking log
    Map<String,Map<String,Double>> normalizedObservationMap = new HashMap<>();//observation map after taking log
    String start = "#";
    int unobservedScore = -100;

    //total counts
    int correctTags = 0;
    int falseTags = 0;


    public Viterbi(){}


    private void buildHMM(String sentenceFile, String tagFile) throws Exception {
        BufferedReader setenceIN = new BufferedReader(new FileReader(sentenceFile));
        BufferedReader tagIN = new BufferedReader(new FileReader(tagFile));

        try {
            List<String> tagSequence = new ArrayList<>();
            List<String> sentenceSequence = new ArrayList<>();

            String sentence = setenceIN.readLine();

            String tag = tagIN.readLine();
            stateFrequency.put("#", 1);

            while (sentence != null && tag != null) {
                String[] sentenceComponents = sentence.split(" ");
                String[] tagComponents = tag.split(" ");
                tagSequence.add(start);
                sentenceSequence.add(start);

                //adding tags to frequencyMap to count how many times each state appear
                for (String singletag : tagComponents) {
                    if (stateFrequency.containsKey(singletag)) {
                        int number = stateFrequency.get(singletag);
                        stateFrequency.put(singletag, number + 1);
                    }
                    if (!stateFrequency.containsKey(singletag)) {
                        stateFrequency.put(singletag, 1);
                    }

                    //add it to a list to keep track of order
                    tagSequence.add(singletag);


                }
                for (String singlesentence : sentenceComponents) {
                    sentenceSequence.add(singlesentence);
                }
                sentence = setenceIN.readLine();
                tag = tagIN.readLine();


                if (sentence != null && tag != null) stateFrequency.put("#", stateFrequency.get("#") + 1);
            }

            //outside while loop

            for (int i = 0; i < tagSequence.size(); i++) {
                //add to transition map
                String currentTag = tagSequence.get(i);
                String currentSentence = sentenceSequence.get(i);

                // if there is current state, change it's next state's frequency
                if (transitionMap.containsKey(currentTag)) {

                    //if not the last element
                    if (i != tagSequence.size() - 1) {
                        Map<String, Integer> innerMap = transitionMap.get(currentTag);
                        String nextTag = tagSequence.get(i + 1);

                        //if next state has been counted before
                        if (innerMap.containsKey(nextTag)) {
                            innerMap.put(nextTag, innerMap.get(nextTag) + 1);
                        }

                        //
                        else if (!innerMap.containsKey(nextTag)) {
                            innerMap.put(nextTag, 1);

                        }

                        transitionMap.put(currentTag, innerMap);

                    }

                } else if (!transitionMap.containsKey(currentTag)) {
                    if (i != tagSequence.size() - 1) {
                        Map<String, Integer> innerMap = new HashMap<>();
                        innerMap.put(tagSequence.get(i + 1), 1);
                        transitionMap.put(currentTag, innerMap);


                    }
                }


                //finished with adding to transition map
                //if current tag not in map, add it with the current map and sentence
                if (!observationProbabilityMap.containsKey(currentTag)) {
                    Map<String, Integer> innerMap = new HashMap<>();
                    innerMap.put(currentSentence, 1);
                    observationProbabilityMap.put(currentTag, innerMap);

                }

                //if the current tag is in the map, change inner map accordingly
                else if (observationProbabilityMap.containsKey(currentTag)) {
                    Map<String, Integer> innerMap = observationProbabilityMap.get(currentTag);

                    if (innerMap.containsKey(currentSentence)) {
                        innerMap.put(currentSentence, innerMap.get(currentSentence) + 1);
                    } else if (!innerMap.containsKey(currentSentence)) {
                        innerMap.put(currentSentence, 1);
                    }
                    observationProbabilityMap.put(currentTag, innerMap);
                }


            }
        } catch (Exception e) {
            System.err.println(e);
        } finally {
            setenceIN.close();
            tagIN.close();
        }
    }

    public void logrithmize() {
            Set<String> outerKeys = transitionMap.keySet();

            //loop through current states
            for (String outerKey : outerKeys) {
                int normalNumber = stateFrequency.get(outerKey);
                Map<String, Integer> innermap = transitionMap.get(outerKey);
                Set<String> innerKeys = innermap.keySet();
                //for each transitioned next states of the current states
                for (String innerKey : innerKeys) {
                    double n = (double) innermap.get(innerKey) / normalNumber;
                    double probability = Math.log(n);

                    //normalizing transition map
                    if (!normalizedTransitionMap.containsKey(outerKey)) {
                        Map<String, Double> insertMap = new HashMap<>();
                        insertMap.put(innerKey, probability);
                        normalizedTransitionMap.put(outerKey, insertMap);
                    } else if (normalizedTransitionMap.containsKey(outerKey)) {
                        Map<String, Double> insertMap = normalizedTransitionMap.get(outerKey);
                        insertMap.put(innerKey, probability);
                        normalizedTransitionMap.put(outerKey, insertMap);

                    }

                }
            }
        }


    public void logrithmize1(){
        Set<String> outerKeys = observationProbabilityMap.keySet();

        //loop through current states
        for(String outerKey: outerKeys){
            int normalNumber = stateFrequency.get(outerKey);
            Map<String,Integer> innermap = observationProbabilityMap.get(outerKey);
            Set<String> innerKeys = innermap.keySet();
            //for each transitioned next states of the current states
            for(String innerKey:innerKeys){
                double n = (double)innermap.get(innerKey)/normalNumber;
                double probability = Math.log(n);

                //normalizing observation map
                if(!normalizedObservationMap.containsKey(outerKey)){
                    Map<String,Double> insertMap = new HashMap<>();
                    insertMap.put(innerKey,probability);
                    normalizedObservationMap.put(outerKey,insertMap);
                }
                else if(normalizedObservationMap.containsKey(outerKey)){
                    Map<String,Double> insertMap = normalizedObservationMap.get(outerKey);
                    insertMap.put(innerKey,probability);
                    normalizedObservationMap.put(outerKey,insertMap);

                }

            }
        }
    }

    //Viterbi algorithm
    public List<String> decode(String[] line) {
        List<String> path = new ArrayList<>();
        backpointer = new ArrayList<>();
        Set<String> currentStates = new HashSet<>();
        currentStates.add("#");
        Map<String, Double> currentScore = new HashMap<>();
        currentScore.put("#", (double) 0);

        //loop till observation-1
        for (int i = 0; i <= line.length - 1; i++) {
            Set<String> nextStates = new HashSet<>();
            Map<String, Double> nextScores = new HashMap<>();


            for (String currState : currentStates) {
                Map<String, Double> innerMap = normalizedTransitionMap.get(currState);
                //add next states

                for (String nextstate : innerMap.keySet()) {
                    if(normalizedTransitionMap.containsKey(nextstate)) nextStates.add(nextstate);

                    double currscore = currentScore.get(currState);
                    double transitionscore = innerMap.get(nextstate);
                    double observationscore = 0;
                    if (normalizedObservationMap.get(nextstate).containsKey(line[i])) {
                        observationscore = normalizedObservationMap.get(nextstate).get(line[i]);
                    } else if (!normalizedObservationMap.get(nextstate).containsKey(line[i])) {
                        observationscore = unobservedScore;
                    }

                    double nextscore = currscore + transitionscore + observationscore;

                    //only update score if higher than current or not existent yet.
                    if ((!nextScores.containsKey(nextstate)) || nextscore > nextScores.get(nextstate)) {

                        nextScores.put(nextstate,  nextscore);


                        //remembering the predecesor
                        if(backpointer.size()>i) {

                            if (backpointer.get(i) == null) {
                                Map<String, String> pred = new HashMap<>();
                                pred.put(nextstate, currState);
                                backpointer.add(i, pred);
                            } else if (backpointer.get(i) != null) {
                                Map<String, String> pred = backpointer.get(i);
                                pred.put(nextstate, currState);
                                backpointer.remove(i);
                                backpointer.add(i, pred);
                            }

                        }
                        else if(backpointer.size() <=i ){
                            Map<String, String> pred = new HashMap<>();
                            pred.put(nextstate, currState);
                            backpointer.add(i, pred);
                        }

                    }
                }


            }
            currentStates = nextStates;
            currentScore = nextScores;
        }

        //finding the last entry with highest possibility
        Set<String> scoreKeys =  currentScore.keySet();
        double maxscore = -99999;
        String maxkey = null;
        for(String key: scoreKeys){
            double score = currentScore.get(key);
            if(score > maxscore) {
                maxscore = score;
                maxkey = key;
            }

        }
        path.add(maxkey);

        //tracing back the backpointer to return path
        for(int i = backpointer.size()-1; i>=0; i--) {
            Map<String, String> tracer = backpointer.get(i);
            maxkey = tracer.get(maxkey);
            path.add(0,maxkey);

        }

        return  path;
    }

    //run viterbi on the file and comparing result to the tag file
    public void fileReading(String testsentences, String testtags) throws Exception{
        BufferedReader in = new BufferedReader(new FileReader(testsentences));
        BufferedReader in2 = new BufferedReader(new FileReader(testtags));

        try {


            String line = in.readLine();
            String check = in2.readLine();

            //loop through each line
            while (line != null && check != null) {


                String[] allwords = line.split(" ");
                String[] reference = check.split(" ");
                List<String> path = decode(allwords);

                //comparing if the tags are same and count correct vs. false
                for (int i = 1; i <= path.size() - 1; i++) {

                    if (path.get(i).toLowerCase(Locale.ROOT).equals((reference[i - 1]).toLowerCase(Locale.ROOT)))
                        correctTags += 1;
                    else if (!path.get(i).toLowerCase(Locale.ROOT).equals((reference[i - 1]).toLowerCase(Locale.ROOT)))
                        falseTags += 1;
                }


                line = in.readLine();
                check = in2.readLine();
            }
        }
        catch (Exception e){
            System.err.println(e);

        }
        finally {
            in.close();
            in2.close();
        }
    }

    //interface for testing
    public void consoleTest(){
        System.out.println("Enter Q to quit, Enter line to decode into tags(make sure you have space between words and period to end)");
        Scanner in = new Scanner(System.in);
        String line = in.nextLine();

            if(line.equals("Q") ){
                return;

            }
            else {
                String[] allwords = line.split(" ");
                List<String> path = decode(allwords);
                List<String> newlist = new ArrayList<>();
                for(int i = 1; i <=path.size()-1;i++) {
                    newlist.add((path.get(i)));
                }
                System.out.println(newlist);
                consoleTest();


            }

        }

    //for adding the Example-hmm.csv HMM data to the maps
    public void parseExample(String file) throws  Exception{
        BufferedReader in = new BufferedReader(new FileReader(file));
        try {
            String line = in.readLine();
            line = in.readLine();
            //observation
            for (int i = 0; i <= 6; i++) {
                String[] allwords = line.split(",");
                int size = allwords.length;
                String key = allwords[0];
                for (int j = 1; j <= size - 1; j += 2) {
                    Map<String, Double> innermap = new HashMap<>();


                    if (!normalizedObservationMap.containsKey(key)) {
                        innermap.put(allwords[j], Double.parseDouble(allwords[j + 1]));
                        normalizedObservationMap.put(key, innermap);
                    } else if (normalizedObservationMap.containsKey(key)) {
                        innermap = normalizedObservationMap.get(key);
                        innermap.put(allwords[j], Double.parseDouble(allwords[j + 1]));
                        normalizedObservationMap.put(key, innermap);
                    }
                }
                line = in.readLine();

            }
            line = in.readLine();
            //transition
            for (int i = 0; i <= 7; i++) {
                String[] allwords = line.split(",");
                int size = allwords.length;
                String key = allwords[0];
                for (int j = 1; j <= size - 1; j += 2) {
                    Map<String, Double> innermap = new HashMap<>();

                    if (!normalizedTransitionMap.containsKey(key)) {
                        innermap.put(allwords[j], Double.parseDouble(allwords[j + 1]));
                        normalizedTransitionMap.put(key, innermap);
                    } else if (normalizedTransitionMap.containsKey(key)) {
                        innermap = normalizedTransitionMap.get(key);
                        innermap.put(allwords[j], Double.parseDouble(allwords[j + 1]));
                        normalizedTransitionMap.put(key, innermap);
                    }
                }
                line = in.readLine();

            }
        }
        catch (Exception e){
            System.err.println(e);
        }
        finally {
            in.close();
        }
    }


    //main method
    public static void main(String[] args) throws Exception{
        Viterbi v = new Viterbi();

        //run these 3 methods to build model
        v.buildHMM("texts/brown-train-sentences.txt","texts/brown-train-tags.txt");
        v.logrithmize();
        v.logrithmize1();

        //run this method to run viterbi and compare results
        v.fileReading("texts/brown-test-sentences.txt","texts/brown-test-tags.txt");
       System.out.println("Correct: "+v.correctTags+" False: "+v.falseTags);

       //run this method for console testing
       v.consoleTest();

       //run following methods to test the example file specifically
        //v.parseExample("ps5/example-hmm.csv");

       // v.fileReading("ps5/example-sentences.txt","ps5/example-tags.txt");
        //System.out.println("Correct: "+v.correctTags+" False: "+v.falseTags);

    }


}
