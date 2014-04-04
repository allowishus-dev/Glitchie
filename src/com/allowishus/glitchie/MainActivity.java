package com.allowishus.glitchie;

import java.io.File;
import java.io.IOException;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.PdListener;
import org.puredata.core.utils.IoUtils;
import org.puredata.core.utils.PdDispatcher;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AnimationDrawable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends Activity {
	
	private static final String GLITCH1_STATE = "GLITCH1_STATE";
	private static final String GLITCH2_STATE = "GLITCH2_STATE";
	private static final String GLITCH3_STATE = "GLITCH3_STATE";
	
	private static final String BITDEPTH_STATE = "BITDEPTH_STATE";
	private static final String SAMPLERATE_STATE = "SAMPLERATE_STATE";
	
	private static final String DELAY_STATE = "DELAY_STATE";
	private static final String FEEDBACK_STATE = "FEEDBACK_STATE";
	
	private boolean glitch1;
	private boolean glitch2;
	private boolean glitch3;
	
	private boolean bit;
	private boolean sampleRate;
	
	private boolean delay;
	private boolean feedback;
	
	CheckBox glitch1CheckBox;
	CheckBox glitch2CheckBox;
	CheckBox glitch3CheckBox;
	
	CheckBox bitCheckBox;
	CheckBox sampleRateCheckBox;
	
	CheckBox delayCheckBox;
	CheckBox feedbackCheckBox;
	
	AnimationDrawable bodyAni;
	AnimationDrawable backAni;
	
	ImageView bodyView;
	Activity tempMainView;
	
	//■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■ libPD bindings ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
	private static final String TAG = "FXUnit";
	private PdDispatcher dispatcher;
	private PdService pdService = null;
	
	private final ServiceConnection pdConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			pdService = ((PdService.PdBinder)service).getService();
			try {
				initPD();
				loadPatch();
			}
			catch (IOException e) {
				Log.e(TAG, e.toString());
				finish();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			// this method will never be called			
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		if(savedInstanceState == null) {
			glitch1 = false;
			glitch2 = false;
			glitch3 = false;
			
			bit=false;
			sampleRate=false;
			
			delay = false;
			feedback = false;
		}
		else {
			glitch1 = savedInstanceState.getBoolean(GLITCH1_STATE);
			glitch2 = savedInstanceState.getBoolean(GLITCH2_STATE);
			glitch3 = savedInstanceState.getBoolean(GLITCH3_STATE);
			
			bit = savedInstanceState.getBoolean(BITDEPTH_STATE);
			sampleRate = savedInstanceState.getBoolean(SAMPLERATE_STATE);
			
			delay = savedInstanceState.getBoolean(DELAY_STATE);
			feedback = savedInstanceState.getBoolean(FEEDBACK_STATE);
		}
		
		initGUI();
		
		bindService(new Intent(this, PdService.class), pdConnection, BIND_AUTO_CREATE);
	}
	
	private void initGUI() {
		setContentView(R.layout.activity_main);
		
		glitch1CheckBox = (CheckBox) findViewById(R.id.glitch1CheckBox);
		glitch1CheckBox.setOnCheckedChangeListener(glitch1CheckBoxListener);
		
		glitch2CheckBox = (CheckBox) findViewById(R.id.glitch2CheckBox);
		glitch2CheckBox.setOnCheckedChangeListener(glitch2CheckBoxListener);
		
		glitch3CheckBox = (CheckBox) findViewById(R.id.glitch3CheckBox);
		glitch3CheckBox.setOnCheckedChangeListener(glitch3CheckBoxListener);
		
		bitCheckBox = (CheckBox) findViewById(R.id.bitCheckBox);
		bitCheckBox.setOnCheckedChangeListener(bitCheckBoxListener);
		
		sampleRateCheckBox = (CheckBox) findViewById(R.id.sampleRateCheckBox);
		sampleRateCheckBox.setOnCheckedChangeListener(sampleRateCheckBoxListener);
		
		feedbackCheckBox = (CheckBox) findViewById(R.id.feedbackCheckBox);
		feedbackCheckBox.setOnCheckedChangeListener(feedbackCheckBoxListener);
		
		delayCheckBox = (CheckBox) findViewById(R.id.delayCheckBox);
		delayCheckBox.setOnCheckedChangeListener(delayCheckBoxListener);

		bodyView = (ImageView) findViewById(R.id.bodyView);

		bodyView.setBackgroundResource(R.drawable.ani_body);

        bodyAni = (AnimationDrawable) bodyView.getBackground();
		
	}
	
	private void initPD() throws IOException {
		// configure the audio glue
		int sampleRate = AudioParameters.suggestSampleRate();
		//int channels = AudioParameters.suggestInputChannels();
		pdService.initAudio(sampleRate,  2,  2,  10.0f);
		pdService.startAudio();		
		
		// create and install the dispatcher
		dispatcher = new PdUiDispatcher();
		
		dispatcher.addListener("something", new PdListener.Adapter() {
			@Override
			public void receiveFloat(String source, float x) {
				Log.i(TAG, "something : " + x);
				System.out.println(x);
			}
		});
		
		PdBase.setReceiver(dispatcher);		
	}
	
	private void loadPatch() throws IOException {
		File dir = getFilesDir();
		IoUtils.extractZipResource(getResources().openRawResource(R.raw.glitchie) , dir, true);
		File patchFile = new File(dir, "glitchie.pd");
		PdBase.openPatch(patchFile.getAbsolutePath());
	}
	
	private void start() {
		if (!pdService.isRunning()) {
			Intent intent = new Intent(MainActivity.this, MainActivity.class);
			pdService.startAudio(intent, R.drawable.icon, "Glitchie", "Return to Glitchie");
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindService(pdConnection);
	}
	
	//■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■ Change Listeners ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
	private OnCheckedChangeListener glitch1CheckBoxListener = new CheckBox.OnCheckedChangeListener() {
	    
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			bodyAni.start();
			glitch1 = glitch1CheckBox.isChecked();
			PdBase.sendBang("glitch1");
			
		}
	};
	
	private OnCheckedChangeListener glitch2CheckBoxListener = new CheckBox.OnCheckedChangeListener() {
	    
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			bodyAni.start();
			glitch2 = glitch2CheckBox.isChecked();
			PdBase.sendBang("glitch2");
			
		}
	};
	
	private OnCheckedChangeListener glitch3CheckBoxListener = new CheckBox.OnCheckedChangeListener() {
	    
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			bodyAni.start();
			glitch3 = glitch3CheckBox.isChecked();
			PdBase.sendBang("glitch3");			
		}
	};
	
	private OnCheckedChangeListener bitCheckBoxListener = new CheckBox.OnCheckedChangeListener() {
	    
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			bit = bitCheckBox.isChecked();
			PdBase.sendBang("bit");			
		}
	};
	
	private OnCheckedChangeListener sampleRateCheckBoxListener = new CheckBox.OnCheckedChangeListener() {
	    
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			sampleRate = sampleRateCheckBox.isChecked();
			PdBase.sendBang("sampleRate");			
		}
	};
	
	private OnCheckedChangeListener feedbackCheckBoxListener = new CheckBox.OnCheckedChangeListener() {
	    
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			feedback = feedbackCheckBox.isChecked();
			PdBase.sendBang("feedback");			
		}
	};
	
	private OnCheckedChangeListener delayCheckBoxListener = new CheckBox.OnCheckedChangeListener() {
	    
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			delay = delayCheckBox.isChecked();
			PdBase.sendBang("delay");			
		}
	};

	
	//■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■


	
	
	
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	// this method will be called whenever the device changes in any way
	protected void onSaveInstanceState(Bundle outState) {

		super.onSaveInstanceState(outState);
		outState.putBoolean(GLITCH1_STATE, glitch1);
		outState.putBoolean(GLITCH2_STATE, glitch2);
		outState.putBoolean(GLITCH3_STATE, glitch3);
		
		outState.putBoolean(BITDEPTH_STATE, bit);
		outState.putBoolean(SAMPLERATE_STATE, sampleRate);
		
		outState.putBoolean(DELAY_STATE, delay);
		outState.putBoolean(FEEDBACK_STATE, feedback);
		
	}
}
