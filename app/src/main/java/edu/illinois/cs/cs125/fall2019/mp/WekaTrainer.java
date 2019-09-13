package edu.illinois.cs.cs125.fall2019.mp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.util.Scanner;

import weka.classifiers.trees.J48;
import weka.core.SerializationHelper;

/**
 * Workbench for training the Weka machine learning model that distinguishes random walks from human paths.
 * <p>
 * This is only needed in Checkpoint 5.
 * <p>
 * CAUTION: trainingpaths.json will not exist in the app or during official grading.
 */
@SuppressWarnings({"JavadocReference", "JavaDoc", "RedundantSuppression"})
public class WekaTrainer {

    /**
     * Entry point to allow running this class outside Android.
     * <p>
     * CAUTION: When running outside of Android or Robolectric, it is not possible to use Android
     * classes or any methods that rely on them. When running inside Android, it is not possible
     * to access the development computer's filesystem. This method should only be used to train the
     * model which gets baked into the app; it must not be called from app code.
     * @param args command-line arguments (unused)
     * @throws java.io.IOException if something goes wrong loading the JSON file or saving the model
     * @throws Exception if something goes wrong serializing the classifier
     */
    public static void main(final String[] args) throws Exception {
        // Load the training paths from JSON
        Scanner scanner = new Scanner(new File("app/trainingpaths.json"));
        JsonObject data = new JsonParser().parse(scanner.nextLine()).getAsJsonObject();
        scanner.close();

        // Process the loaded data into an Instances dataset and use it to train the classifier
        J48 classifier = new J48();
        // Your code here...

        // Save the trained model to disk so the app can use it
        SerializationHelper.write("app/src/main/res/raw/pathclassifier.model", classifier);
        // Make sure the new file is added to Git!
    }

    /*
     * While the app can't call the main method, feel free to put static methods here that
     * calculate statistics on a path. You can then use them during both training (from main)
     * and classification (from RandomWalkDetector).
     */

}
