package edu.iiit.speech.healthcare;

import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class HealthcareActivity extends Activity implements
		OnTouchListener, RecognitionListener {
	static {
		System.loadLibrary("pocketsphinx_jni");
	}
	/**
	 * Recognizer task, which runs in a worker thread.
	 */
	RecognizerTask rec;
	/**
	 * Thread in which the recognizer task runs.
	 */
	Thread rec_thread;
	/**
	 * Time at which current recognition started.
	 */
	Date start_date;
	/**
	 * Number of seconds of speech.
	 */
	float speech_dur;
	/**
	 * Are we listening?
	 */
	boolean listening;
	/**
	 * Progress dialog for final recognition.
	 */
	ProgressDialog rec_dialog;
	/**
	 * Performance counter view.
	 */
	TextView performance_text;
	/**
	 * Editable text view.
	 */
	TextView edit_text;

	/**
	 * Editable text view.
	 */
	ListView disease_list;
	
	/**
	 * List view to show numbers
	 */
	//ListView numlist;
	//ArrayAdapter<String> listAdapter;

	/**
	 * Initialize text parsing module
	 */
	NLUParser nluParser = new NLUParser();

	DialogManager dialogManager = null;

	boolean in_progress = false;

	TextToSpeech tts;

	/**
	 * Respond to touch events on the Speak button.
	 * 
	 * This allows the Speak button to function as a "push and hold" button, by
	 * triggering the start of recognition when it is first pushed, and the end
	 * of recognition when it is released.
	 * 
	 * @param v
	 *            View on which this event is called
	 * @param event
	 *            Event that was triggered.
	 */
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			start_date = new Date();
			this.listening = true;
			this.rec.start();
			break;
		case MotionEvent.ACTION_UP:
			Date end_date = new Date();
			long nmsec = end_date.getTime() - start_date.getTime();
			this.speech_dur = (float) nmsec / 1000;
			if (this.listening) {
				Log.d(getClass().getName(), "Showing Dialog");
				this.rec_dialog = ProgressDialog.show(
						HealthcareActivity.this, "",
						"Recognizing speech...", true);
				this.rec_dialog.setCancelable(false);
				this.listening = false;
			}
			this.rec.stop();
			break;
		default:
			;
		}
		/* Let the button handle its own state */
		return false;
	}

	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		this.rec = new RecognizerTask();
		this.rec_thread = new Thread(this.rec);
		this.listening = false;
		ImageButton b = (ImageButton) findViewById(R.id.Button01);
		b.setOnTouchListener(this);
		this.performance_text = (TextView) findViewById(R.id.PerformanceText);
		this.edit_text = (TextView) findViewById(R.id.TextView01);
		this.edit_text.setMovementMethod(new ScrollingMovementMethod());
		this.disease_list = (ListView) findViewById(R.id.diseaselist);
		//this.numlist = (ListView) findViewById(R.id.numberList);
		//listAdapter = new ArrayAdapter<String>(this, R.layout.main, R.id.EditText03);
		//numlist.setAdapter(listAdapter);
		final Context that = this;
		this.rec.setRecognitionListener(this);
		this.rec_thread.start();

		/* Initializing TTS */
		tts = new TextToSpeech(getApplicationContext(),
				new TextToSpeech.OnInitListener() {

					@Override
					public void onInit(int status) {
						if (status == TextToSpeech.SUCCESS) {
							int result = tts.setLanguage(Locale.ENGLISH);
							if (result == TextToSpeech.LANG_MISSING_DATA
									|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
								Log.e("TTS", "This language is not supported!");
							}
						}
					}
				});

		/* testing db with synonyms */
		dialogManager = new DialogManager(this);
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		speakOut(getString(R.string.greeting_text));
	}

	@Override
	public void onDestroy() {
		// Don't forget to shutdown tts!
		if (tts != null) {
			tts.stop();
			tts.shutdown();
		}
		super.onDestroy();
	}
	
	public void speakOut (String text) {
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
	}

	/** Called when partial results are generated. */
	public void onPartialResults(Bundle b) {
		final HealthcareActivity that = this;
		final String hyp = b.getString("hyp");
		that.edit_text.post(new Runnable() {
			public void run() {
				// that.edit_text.setText(hyp);
			}
		});
	}

	/** Called with full results are generated. */
	public void onResults(Bundle b) {
		final String hyp = b.getString("hyp");
		final HealthcareActivity that = this;
		this.edit_text.post(new Runnable() {
			public void run() {
				Log.i("Output ######## ", hyp);
				String hyp1 = hyp.replaceAll("_", " ");
				String facultyContext = nluParser
						.getContextInfo(hyp1);
				Log.i("Parser", facultyContext);
				boolean info_complete = false;
				if (in_progress) {
					appendDialogText(hyp1, "U");
					info_complete = dialogManager.manage(facultyContext);
				} else {
					setDialogText(hyp1, "U");
					info_complete = dialogManager.init(facultyContext);
					in_progress = true;
				}
				if (info_complete) {
					// get results from db
					Log.i("info ######## ", "info complete");
					in_progress = false;
					//appendDialogText(getString(R.string.dialog_end_text), "S");
					dialogManager.showResults();
				}
				// String facultyInfo = db.verifyFacultyInfo(facultyContext);
				// Log.i("Number", facultyInfo);
				// that.phnumber_text.setText(facultyInfo);
				Date end_date = new Date();
				long nmsec = end_date.getTime() - that.start_date.getTime();
				float rec_dur = (float) nmsec / 1000;
				that.performance_text.setText(String.format(
						"%.2f seconds %.2f xRT", that.speech_dur, rec_dur
								/ that.speech_dur));
				Log.d(getClass().getName(), "Hiding Dialog");
				that.rec_dialog.dismiss();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.reset:
			in_progress = false;
			setDialogText(getString(R.string.greeting_text), "S");
			disease_list.setAdapter(null);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onError(int err) {
		final HealthcareActivity that = this;
		that.edit_text.post(new Runnable() {
			public void run() {
				that.rec_dialog.dismiss();
			}
		});
	}

	private void setDialogText(String text, String tag) {
		this.edit_text.setText(getFormattedText(text, tag) + "\n");
		if ("S".equals(tag)) {
			speakOut(text);
		}
	}

	private void appendDialogText(String text, String tag) {
		this.edit_text.setText(
				this.edit_text.getText()
						+ getFormattedText(text, tag).toString() + "\n");
		if ("S".equals(tag)) {
			speakOut(text);
		}
	}

	private Spanned getFormattedText(String text, String tag) {
		Spanned ftext = null;
		if ("S".equals(tag)) {
			ftext = Html.fromHtml("<font color='red'><b>" + tag + ": </b>" + text
					+ "</font>");
		} else {
			ftext = Html.fromHtml("<font color='green'><b>" + tag + ": </b>" + text
					+ "</font>");
		}
		return ftext;
	}
}