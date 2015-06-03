/**
 * Multi-Thread App
 *
 * Note: I chose to include the AsyncTask classes in the main activity instead of putting them in
 * their own files. I had them in their own files originally, but it became a mess trying to deal
 * with Contexts and manipulating UI elements. Someone on StackOverflow said that AsyncTasks should
 * almost always be inside an activity, so I moved them into MainActivity and I was much more
 * satisfied with the solution.
 * Also, I added a few extra features that I felt the project needed. The progress bar only appears
 * when one of the background threads is performing file operations. While a thread is running in
 * the background, the buttons become disabled until it is finished. This prevents the user from
 * trying to write to the file and read from it at the same time, which would be possible since they
 * run on separate threads. And, the app makes use of Toast notifications to let the user know when
 * file operations have been completed, or if there was a problem.
 *
 * @author John Walker
 * I spent roughly 8-10 hours on this assignment.
 */

package com.example.johnathon.multi_threadedapp;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;

/**
 * Contains the following methods:
 * -makeToast
 * -enableButtons
 * -disableButtons
 * -setProgressPercent
 * -resetProgressBar
 * -onCreate (the 'main' method)
 *
 * Contains the following classes:
 * -FileWriterTask
 * -FileReaderTask
 */
public class MainActivity extends ActionBarActivity {

    private ListView listView;
    private ProgressBar progressBar;
    private Button createButton;
    private Button loadButton;
    private Button clearButton;

    /**
     * displays a Toast message to the user
     */
    private void makeToast(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * enables all the buttons. For use by background threads once they are finished
     */
    private void enableButtons() {
        createButton.setEnabled(true);
        loadButton.setEnabled(true);
        clearButton.setEnabled(true);
    }

    // disables all the buttons. For use by running background threads to prevent other threads from
    // being started before completion
    private void disableButtons() {
        createButton.setEnabled(false);
        loadButton.setEnabled(false);
        clearButton.setEnabled(false);
    }

    // Sets the current percentage of progress for the progress bar
    private void setProgressPercent(Integer progress) {
        progressBar.setProgress(progress);
    }

    /**
     * Makes the progress bar invisible and sets its progress to zero.
     */
    private void resetProgressBar() {
        progressBar.setVisibility(View.INVISIBLE);
        progressBar.setProgress(0);
    }

    /**
     * The 'main' method. All of the UI elements are instantiated here. All of the onClickListeners
     * are set-up here. The numbers.txt file is created. On click, background threads are started
     * that perform file operations.
     *
     * @param savedInstanceState OS handles re-creation of activity if it was closed due to system
     *                           restraints and the user navigates back to it.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // instantiate our various UI elements
        createButton = (Button)     findViewById(R.id.createButton);
        loadButton   = (Button)     findViewById(R.id.loadButton);
        clearButton  = (Button)     findViewById(R.id.clearButton);
        listView     = (ListView)   findViewById(R.id.listView);
        progressBar  = (ProgressBar)findViewById(R.id.progressBar);

        // the progress bar will be enabled by the tasks that use it. For now, it will be invisible
        progressBar.setVisibility(View.INVISIBLE);

        // make sure the 'numbers.txt' file is created
        final File file = new File(getFilesDir(), "numbers.txt");

        // When the 'Create' button is pressed, numbers.txt is opened and filled with numbers.
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new FileWriterTask().execute(file);
            }
        });

        // When the 'Load' button is pressed, the numbers.txt file is read into memory and stored in
        // a list.
        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new FileReaderTask().execute(file);
            }
        });

        // When the 'Clear' button is pressed, the list will be cleared.
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listView.setAdapter(null);
            }
        });
    }

    /**
     * This class writes the numbers 1-10 into a file in internal memory. Since it only stores ten
     * numbers, keeping track of its progress and updating the progress bar is very simple. To
     * simulate a more difficult task, the 'Thread.sleep(x)' method is used. To accommodate this, it
     * implements AsyncTask, which allows it to run in the background, thus freeing up the main
     * thread so that the UI can continue to run smoothly while these operations are performing.
     *
     * @author John Walker
     * @see AsyncTask
     */
    private class FileWriterTask extends AsyncTask<File, Integer, Boolean> {

        /**
         * Before the thread starts: make sure the progress bar is visible and disable the buttons
         * so that the user doesn't get any funny ideas... (like trying to read and write at the
         * same time).
         */
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            disableButtons();
        }

        /**
         * Updates the progress bar with the current percentage of the progress of the file
         * operation
         *
         * @param progress how many lines have been written to the file. A total of 10 lines total
         *                 will be written
         */
        @Override
        protected void onProgressUpdate(Integer[] progress) {
            setProgressPercent(progress[0] * 10);
        }

        /**
         * Writes the file to internal memory. It also keeps the progress bar updated.
         *
         * @param file the file to write to
         * @return     were we successful in writing to the file?
         */
        @Override
        protected Boolean doInBackground(File[] file) {
            Boolean successful = true; // will be flagged as false if an exception is thrown
            FileOutputStream fOut = null;

            // I would have loved to use the 'try-with-resources' here, but it isn't available
            // in this version of Android... (2.2 Froyo - broadest audience in Google Play Store)
            try {
                // open a new file that can only be read by this app
                fOut = openFileOutput((file[0]).getName(), Context.MODE_PRIVATE);

                // write the numbers 1 through 10 in numbers.txt
                for (int i = 1; i <= 10; i++) {
                    fOut.write(Integer.toString(i).getBytes());
                    fOut.write("\n".getBytes());

                    // keep track of our progress
                    publishProgress(i);

                    // adding a 'sleep' to simulate a more difficult file operation
                    try {
                        Thread.sleep(250); // this is hard!
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                successful = false;
            } finally { // ugly ugly code, but we need to make sure the file is closed
                try {
                    fOut.close();
                } catch (IOException | NullPointerException ex) {
                    ex.printStackTrace();
                }
            }
            return successful;
        }

        /**
         * Once the thread is done executing: reset the progress bar, re-enable the buttons, and let
         * the user know if we were successful in writing the file.
         *
         * @param successful Boolean returned from 'doInBackground' letting us know if the file
         *                   operations were successful.
         */
        @Override
        protected void onPostExecute(Boolean successful) {
            resetProgressBar();
            enableButtons();
            if (successful) {
                makeToast("Finished writing file"); // mmmmmmm...toast...
            } else {
                makeToast("Unable to write file");
            }
        }
    }

    /**
     * This class reads through a file, line-by-line, and stores it in a list of Strings. It uses
     * the number of lines in the file to determine its progress, and subsequently makes use of the
     * progress bar to keep the user informed. Also, it uses the 'Thread.sleep(x)' method to
     * simulate a more difficult task. Because of this, it implements AsyncTask, which allows it to
     * run in the background, thus freeing up the main thread so that the UI can continue to run
     * smoothly while these operations are performing.
     *
     * @author John Walker
     * @see AsyncTask
     */
    private class FileReaderTask extends AsyncTask<File, Integer, ArrayList<String>> {

        // the number of lines in the file (see method 'getNumberLines')
        private int fileNumberLines;

        /**
         * Before the thread starts: enable the progress bar and disable the buttons so that the
         * user doesn't get any funny ideas... (like trying to read and write at the same time).
         */
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            disableButtons();
        }

        /**
         * Opens the file and reads it into memory. It also keeps the progress bar updated. Whether
         * or not the file was actually able to be opened and read will be handled in the method
         * 'onPostExecute'.
         *
         * @param file File object that is to be read
         * @return     An ArrayList of Strings containing the contents of the file
         */
        @Override
        protected ArrayList<String> doInBackground(File[] file) {
            // get the number of lines in the file so that we can use our progress bar
            fileNumberLines = getNumberLines(file[0]);

            // the data we read from the text file will be stored in this array
            ArrayList<String> numbers = new ArrayList<>();

            // lets get to reading
            BufferedReader br = null;
            try {
                FileInputStream fStream = new FileInputStream(file[0]);
                br = new BufferedReader(new InputStreamReader(fStream));

                String buffer;      // temporary storage for data read from file
                int lineNumber = 0; // what line are we on? (for progress bar)
                while ((buffer = br.readLine()) != null) {
                    numbers.add(buffer);

                    publishProgress(++lineNumber);

                    // adding a 'sleep' to simulate a more difficult file operation
                    try {
                        Thread.sleep(250); // this is hard!
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    br.close();
                } catch (IOException | NullPointerException ex) {
                    ex.printStackTrace();
                }
            }
            return numbers;
        }

        /**
         * Updates the progress bar with the current percentage of the progress of the file being
         * read.
         *
         * @param i the current line being read
         */
        @Override
        protected void onProgressUpdate(Integer... i) {
            setProgressPercent((int)(((float)i[0] / fileNumberLines) * 100));
        }

        /**
         * Once the thread is done executing: reset the progress bar, re-enable the buttons, and
         * populate the list if the data was able to be read. The user is notified if there was a
         * problem.
         *
         * @param results this is the ArrayList returned by the method 'doInBackground'
         */
        @Override
        protected void onPostExecute(ArrayList<String> results) {
            // get rid of the progress bar until it is needed again
            resetProgressBar();

            // re-enable the buttons
            enableButtons();

            // finally, if we were able to load anything then put it into the listView
            if (!results.isEmpty()) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        MainActivity.this,
                        android.R.layout.simple_list_item_1,
                        results
                );
                listView.setAdapter(adapter);

                // let our user know that everything was successful
                makeToast("Finished reading file");
            } else {
                // let the user know that the file was unable to be read
                makeToast("Unable to load file");
            }
        }

        /**
         * Computes the number of lines in a file.
         *
         * @param file the file to be read
         * @return     the number of lines in the file
         */
        private int getNumberLines(File file) {
            LineNumberReader reader = null;
            try {
                reader = new LineNumberReader(new FileReader(file));
                while ((reader.readLine()) != null); // empty body intended
                return reader.getLineNumber();
            } catch (IOException ex) {
                ex.printStackTrace();
                return -1;
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
}
