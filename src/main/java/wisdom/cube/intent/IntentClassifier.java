package wisdom.cube.intent;

/**
 * Maps transcript text to {@link IntentClassification} (F4.T2).
 */
public interface IntentClassifier {

    IntentClassification classify(String transcript);
}
