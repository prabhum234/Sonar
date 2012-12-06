package com.uu.Sonar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;
import com.google.tts.TTS;

@SuppressWarnings("deprecation")
public class Sonar extends Activity {
    /** Called when the activity is first created. */
	public static final String SERIAL_COM_LOCK_BASE = "/data/local/serialcom/LCK..";

	protected TextView txtOutput;
	
	protected boolean running;
	protected Process serialComProc;
	protected InputStream in;
	protected OutputStream out;
	MediaPlayer mp;
	 TTS myTts;
	 
	private class Reader extends AsyncTask<Void, String, Void> {		
		String rawData="";
		float floorLevel=100f;		
		@Override
		protected Void doInBackground(Void... params) {
			publishProgress("Waiting for data ...\n");
			while (running) {
				try {
					/* Read a character and append it to txtOutput. */
					int c = in.read();
					if (c != -1) {
						publishProgress(((char) c) + "");
					} else {
						/* Handle end of file (no more data from sensor). */
						publishProgress("\n");						
						break;
					 }
				} catch (IOException ioe) {
		    		/* TODO Handle exception. */
		    		ioe.printStackTrace();
		    		break;
				}
			}
			
			Scanner error = new Scanner(serialComProc.getErrorStream());
			while (error.hasNextLine()) {
				System.out.println(error.nextLine());
			}

			return null;
		}
		
		@Override
		protected void onProgressUpdate(String... s) {
			if(s[0].equals("\n")){
				processData(rawData);				
				rawData = "";
			}
			else{
				rawData += s[0];					
			}
				
		}
		protected void processData(String rawData){			
			int distance = 0;
			float leftVolume=0,rightVolume=0;
			String [] obstacleDistance;		
			try{													
					obstacleDistance = rawData.split(" ");
					if(obstacleDistance.length == 0){					
						obstacleDistance[0] = rawData;
					}
			
					for(int i=0;i<obstacleDistance.length;i++){
						txtOutput.append(String.valueOf(obstacleDistance[i])+" ");
						distance = Integer.parseInt(obstacleDistance[i].substring(1).trim());						
						if(distance <= 0 ||distance > floorLevel){
							txtOutput.setText("");
							continue;							
						}
						
						if(obstacleDistance[i].startsWith("l")){
							leftVolume = VolumeOf(distance);							
						}
						else if(obstacleDistance[i].startsWith("r")){
							rightVolume = VolumeOf(distance);	
						}
						else if(obstacleDistance[i].startsWith("d")){
							 leftVolume = 1;
							 rightVolume = 1;							
							 myTts.speak("Down stairs", 0, null);
						}
						else if(obstacleDistance[i].startsWith("u")){
							 myTts.speak("Up stairs", 0, null);
						}
						else{
							leftVolume = 0;
							rightVolume = 0;
							mp.pause();
							txtOutput.setText("");							
						}
					}
														
				}catch(Exception e){	
					txtOutput.setText(e.toString());
					leftVolume = 0;
					rightVolume = 0;
			}			
			//GenerateFrequency(distance,left,right);
				mp.setVolume(leftVolume,rightVolume);	
				if(!mp.isPlaying())
					mp.start();
				
		}
		protected float VolumeOf(int distance){
			float volume = 0f;
			volume = 1f - ((float)distance/floorLevel);		
			return volume;
		}
		/*protected void GenerateFrequency(int distance,boolean left,boolean right){
			{
				float volume = 0,leftVolume=0,rightVolume=0;
				
				volume = 1.2f - ((float)distance/floorLevel);		
				if(left && right){
					mp.setVolume(leftVolume, rightVolume);	
				}
				else if(right){
					mp.setVolume(0, rightVolume);
				}
				else if(left){
					mp.setVolume(leftVolume, 0);
				}
				else{
					mp.setVolume(0,0);	
					txtOutput.setText("");
				}
				
				if(distance>10 && distance <= floorLevel){											
					if(!mp.isPlaying())
						mp.start();
					//txtOutput.append(Float.toString(volume)+" ");						
				}
				else{						
					mp.setVolume(0,0);	
					mp.pause();
					txtOutput.setText("");
				}						
			}		
		}*/
	};
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);    

        myTts = new TTS(this, ttsInitListener, true);
       
        
         mp = MediaPlayer.create(getBaseContext(), R.raw.wawyalarm);      
    	 mp.setVolume(1f,1f);			 
	     mp.setLooping(true);
	     mp.start();
	     mp.pause();
        txtOutput = (TextView) findViewById(R.id.Output);
        txtOutput.setMovementMethod(new ScrollingMovementMethod());
    }
    private TTS.InitListener ttsInitListener = new TTS.InitListener() {
        public void onInit(int version) {
          myTts.speak("Hello", 0, null);
        }
      };
    @Override
    public void onStart() {
    	super.onStart();
    	
        startSerialCom();
    }

    @Override
	public void onStop() {
    	super.onStop();
    	mp.release();
		stopSerialCom();
	}
	
    protected void startSerialCom() {
    	if (running) {
    		return;
    	}
    	
    	/* Start serial_com process for device /dev/ttyUSB0. */
    	try {
    		serialComProc = Runtime.getRuntime().exec(new String[] { "serial_com", "/dev/ttyUSB0" } );
    	} catch (IOException ioe) {
    		/* TODO Handle exception. */
    		ioe.printStackTrace();
    		return;
    	}

    	in = serialComProc.getInputStream();
    	out = serialComProc.getOutputStream();
    	running = true;
    	new Reader().execute();
    }
    
    protected void stopSerialCom() {
    	if (!running) {
    		return;
    	}
    	
		try {
			/* Read the process PID from the lock file and kill the process .*/
			Scanner s = new Scanner(new File(SERIAL_COM_LOCK_BASE + "ttyUSB0"));
			String pid = s.nextLine();
			Runtime.getRuntime().exec(new String[] { "kill", pid });
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		running = false;
    }
}