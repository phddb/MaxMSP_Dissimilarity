package ca.davebenson.MaxMSP;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;


import com.cycling74.max.*;

/**
 * Max/MSP object for collecting dissimilarity ratings
 * @author Dave Benson
 *
 */
public class Dissimilarity extends MaxObject {

	/**
	 * 
	 */
	boolean qVerbose = true;

	/**
	 * Where the stimuli are stored.  Must be audio files readable by Max/MSP.
	 */
	File stimuliPath;


	/**
	 * Where the subject responses are stored and read from
	 */
	File resultsPath;


	/**
	 * 
	 */
	ArrayList<Comparison> comparisons = new ArrayList<Comparison>();


	/**
	 * The trial currently under way.
	 */
	Iterator<Comparison> comparisonsIter;
	Comparison current;

	private boolean done;

	/**
	 * 
	 */
	private File[] stimuli;



	/**
	 * 
	 * @param stimuli  stimulus directory
	 * @param results  results file
	 */
	public Dissimilarity(String stimuli, String results){
		try{
			this.stimuliPath = new File(stimuli);
			this.resultsPath = new File(results);

			if(!stimuliPath.isDirectory()){
				throw new Exception("First argument must be a directory.  (First argument: "+stimuli+")");
			}
		} catch (Exception e) {
			bail(e.getMessage());
		}


		declareInlets( new int[]{DataTypes.FLOAT} );
		declareOutlets( new int[]{
				DataTypes.ALL,
				DataTypes.ALL,
				DataTypes.INT,
				DataTypes.ANYTHING} );


		setInletAssist(new String[] {"Inlet 1"});
		setOutletAssist(new String[] {
				"File 1", "File 2","Comparisons left", "Bang on object initialization"});

//		init();

	}

	/**
	 * Set up the object.
	 * 
	 * First, check to see if the results file already exists.
	 * If it does, this experiment has already been begun.
	 * Read in the setup and experiment progress data from it.
	 * 
	 * If not, read in the list of audio files and generate a
	 * list of comparison objects for them.
	 * 
	 */
	@SuppressWarnings("unused")
	private void init(){

		comparisons.clear();
		done = false;
		
		//TODO
		// if results file already exists, read in comparison data
		if(false){//resultsPath.exists()){
			post("Reults file found -- experiment in progress... resuming.");
		}else{

			// otherwise, look at the audio files and generate an array of comparison objects
			stimuli = stimuliPath.listFiles(new AudioFileFilter());
			Arrays.sort(stimuli);

			/**
			 * Create a comparison object for each pairwise comparison between audio files
			 * At the moment, only a half matrix is used (each stimulus is paired with each 
			 * other stimulus only once. 
			 * 
			 * To do a full matrix of comparisons, only this block of code should need to 
			 * be modified.
			 */
			Random random = new Random();	// used to randomize presentation order

			for(int i = 0; i < stimuli.length; i++){
				if(qVerbose){
					post("Read file: "+stimuli[i]);
				}

				for(int j = i+1; j < stimuli.length; j++){
					if(random.nextBoolean()){	// randomize stimulus order
						comparisons.add(new Comparison(stimuli[i],stimuli[j]));
					} else {
						comparisons.add(new Comparison(stimuli[j],stimuli[i]));
					}
				}
			}

			Collections.shuffle(comparisons);	// randomize comparison order
			comparisonsIter = comparisons.iterator();

		}


		int remaining = comparisons.size();
		outlet(2,remaining);

		outletBang(3);
		getNextComparison();


	}

	public void bang(){
		init();
	}
	

	/**
	 * Create the results matrix, write it
	 */
	@SuppressWarnings("unchecked")
	private void calculateResultsMatrix() {

		// format for results matrix
		/**
		{
		    "Description" : "Timbre data for 18 syn intru 88 subs",

		    "StimulusNames" : [
		        "Stimulus00",
		        "Stimulus01",
		        "Stimulus02",
		        "Stimulus03",
		        "Stimulus04",
		        "Stimulus05",
		        "Stimulus06",
		        "Stimulus07",
		        "Stimulus08",
		        "Stimulus09",
		        "Stimulus10",
		        "Stimulus11",
		        "Stimulus12",
		        "Stimulus13",
		        "Stimulus14",
		        "Stimulus15",
		        "Stimulus16",
		        "Stimulus17"
		    ],

		    "SubjectData" : {

		        "Subject000" : [
		            [2.000],
		            [2.000, 4.000],
		            [6.000, 7.000, 6.000],
		            [5.000, 1.000, 7.000, 5.000],
		            [8.000, 7.000, 6.000, 2.000, 5.000],
		            [8.000, 8.000, 9.000, 3.000, 4.000, 4.000],
		            [3.000, 3.000, 3.000, 5.000, 4.000, 7.000, 5.000],
		            [8.000, 8.000, 8.000, 5.000, 7.000, 3.000, 6.000, 8.000],
		            [6.000, 5.000, 3.000, 8.000, 6.000, 7.000, 8.000, 6.000, 8.000],
		            [3.000, 4.000, 3.000, 6.000, 5.000, 7.000, 7.000, 4.000, 5.000, 2.000],
		            [5.000, 5.000, 4.000, 6.000, 8.000, 6.000, 6.000, 6.000, 8.000, 6.000, 6.000],
		            [5.000, 6.000, 4.000, 2.000, 6.000, 4.000, 4.000, 3.000, 7.000, 6.000, 6.000, 7.000],
		            [7.000, 8.000, 7.000, 3.000, 4.000, 5.000, 4.000, 8.000, 5.000, 2.000, 8.000, 8.000, 7.000],
		            [7.000, 6.000, 7.000, 3.000, 3.000, 2.000, 3.000, 5.000, 3.000, 5.000, 5.000, 8.000, 4.000, 4.000],
		            [3.000, 1.000, 4.000, 6.000, 5.000, 8.000, 7.000, 2.000, 8.000, 3.000, 5.000, 4.000, 4.000, 8.000, 6.000],
		            [5.000, 8.000, 5.000, 2.000, 4.000, 6.000, 6.000, 5.000, 7.000, 6.000, 6.000, 4.000, 2.000, 4.000, 3.000, 6.000],
		            [3.000, 7.000, 3.000, 7.000, 6.000, 6.000, 7.000, 1.000, 8.000, 4.000, 5.000, 2.000, 7.000, 7.000, 3.000, 2.000, 7.000]
		        ],
		        
		        */
		
		

		JSONObject JSONResults = new JSONObject();
		
		JSONResults.put("Description", "Generated by "+this.getClass().getName());

		/**
		 *  make stimuli names field
		 */
		JSONArray JSONStimulusNames = new JSONArray();
		for(File f : stimuli){
			JSONStimulusNames.add(f.getName());
		}
		JSONResults.put("StimulusNames", JSONStimulusNames);
		
		/**
		 * Make dissimilarity mat
		 */
		// stimuli

		JSONArray outer = new JSONArray();
		for(int i = 0; i < stimuli.length; i++){
			JSONArray inner = new JSONArray();
			String vertFn = stimuli[i].getName();
			for(int j = 0; j < i; j++){
				String horFn = stimuli[j].getName();
				
				// find the comparison between the two files in question
				Comparison comp = null;
				for(Comparison c : comparisons){
					if(	c.getFile1Name().equals(vertFn) && c.getFile2Name().equals(horFn) 
						||
						c.getFile2Name().equals(vertFn) && c.getFile1Name().equals(horFn) ){
						comp = c;	// found it!
						break;
					}
				}
				//post("Error: no comparison found between files ");
				//post("\t"+vertFn);
				//post("\t"+horFn);
				inner.add(comp.getRating());
			}
			if(inner.size() > 0){
				outer.add(inner);
			}
		}
		
		JSONObject subject = new JSONObject();
		subject.put(resultsPath.getName(), outer);	// use output filename as "subject"
		
		JSONResults.put("SubjectData", subject);
		
		
		/**
		 * Make comparisons field
		 */
		JSONObject JSONComparisons = comparisons2JSON();
		JSONResults.put("Comparisons", JSONComparisons);

		writeResults(JSONResults);
		done = true;
		post("Done! "+comparisons.size()+" stimuli compared.");
	}

	/**
	 * Convert comparisons to a JSON object
	 */
	@SuppressWarnings("unchecked")
	private JSONObject comparisons2JSON() {
		JSONObject JSONResults = new JSONObject();

		JSONArray JSONComparisons = new JSONArray();
		for(Comparison c : comparisons){
			JSONComparisons.add(c.toJSONObject());
		}

		JSONResults.put("Comparisons", JSONComparisons);
		return JSONResults;
	}



	private void writeResults(JSONObject JSONResults){



		Writer out = null;
		try {
			resultsPath.delete();
			out = new OutputStreamWriter(new FileOutputStream(resultsPath));
			JSONResults.writeJSONString(out);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * We received a file1Play message. Increment the counter.
	 */
	public void file1Play(){
		current.incFile1Plays();
	}

	/**
	 * We received a file2Play message. Increment the counter.
	 */
	public void file2Play(){
		current.incFile2Plays();
	}

	/**
	 * Start the timer
	 */
	public void start(){
		//TODO implement this
	}

	/**
	 * Stop the timer (pause the experiment)..
	 *
	 */
	public void stop(){
		//TODO implement
	}


	public void inlet(float rating){		
		if(!done){
			current.setRating(rating);
			if(qVerbose){
				post(current.toJSONObject().toJSONString());
			}
			
			writeResults(comparisons2JSON());
			getNextComparison();
		}
	}

	private void getNextComparison() {
		if(comparisonsIter.hasNext()){
			// write out partial results
			current = comparisonsIter.next();
			outlet(0, (new File(stimuliPath,current.getFile1Name())).toString());
			outlet(1, (new File(stimuliPath,current.getFile2Name())).toString());
		} else {
			calculateResultsMatrix();
		} 
		
		int remaining =0;
		if(!done){
			remaining = comparisons.size() - comparisons.indexOf(current);
		}
		outlet(2,remaining);
	}

	class Comparison {

		String file1;
		String file2;

		float rating = -1;
		float time = 0;		// time taken in seconds
		int file1Plays = 0;
		int file2Plays = 0; // number of times each file was played


		public Comparison(File f1, File f2){
			this.file1 = f1.getName();
			this.file2 = f2.getName();
		}

		public float getRating() {
			return rating;
		}

		/**
		 * 
		 * @return base name of file 2
		 */
		public String getFile2Name() {
			return file2;
		}

		/**
		 * @return base name of file 1
		 */
		public String getFile1Name() {
			return file1;
		}

		public void setRating(float rating) {
			this.rating = rating;
		}

		public void incFile1Plays(){
			file1Plays++;
		}

		public void incFile2Plays(){
			file2Plays++;
		}

		/**
		 * @return true if this comparison has been completed
		 *
		public boolean done(){
			return rating > 0;
		}
		 */

		@SuppressWarnings("unchecked")
		public JSONObject toJSONObject(){
			JSONObject obj = new JSONObject();
			obj.put("file1", file1);
			obj.put("file2", file2);
			if(rating >= 0){
				obj.put("rating", new Double(rating));
			}else{
				obj.put("rating", null);
			}
			obj.put("time", new Double(time));
			obj.put("file1Plays", new Integer(file1Plays));
			obj.put("file2Plays", new Integer(file2Plays));
			return obj;
		}

	}


	class AudioFileFilter implements FilenameFilter {

		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".wav");
		}
	}
}
