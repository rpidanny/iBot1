package com.dannytech.ibot;


import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.dannytech.ibot.MjpegInputStream;
import com.dannytech.ibot.MjpegView;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;


import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;



public class LoginActivity extends Activity implements SensorEventListener {

	private SensorManager sensorManager;
	
	String ipaddress="192.168.1.5";
	int port=5010;
	String URL = "http://192.168.1.10:8070/videofeed";
	private MjpegView mv;
	
	TextView xaxis;
    TextView yaxis;
    TextView zaxis;
	
    public int flagbtnF=0,hndbrkflag=0;
    boolean connectflag=false;
    boolean cameraflag=true;
    DoRead DoRead1;
    private static final int RESULT_SETTINGS = 1;
    
    String previousmsg="";
    boolean datastream=true;
    boolean secondaryControl=false;
    int griprelease =1;
    
    private Socket clientSocket=null;
	private DataOutputStream outToServer=null;
	
	 Button conb;
	 ImageView statusimg,camera;
	 TextView txtstatus;
	 RelativeLayout myrelativelayout;
	 LinearLayout mylinearlayout;
	 @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
	        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	        setContentView(R.layout.activity_login);
	        
	        showUserSettings();
	       
	        //Button forwardbtn = (Button)findViewById(R.id.forwardbtn);
	        //Button handbreak = (Button)findViewById(R.id.hndbrkBtn);
	        myrelativelayout=(RelativeLayout) findViewById(R.id.myLayout);
	        mylinearlayout=(LinearLayout) findViewById(R.id.linearLayout1);
	        mylinearlayout.setVisibility(View.GONE);
	        
	        
	        //FrameLayout frame = (FrameLayout) findViewById(R.id.frame);
	        //camera.setImageResource(R.drawable.camcover);
	        //frame.addView(camera, 0);
	       mv = new MjpegView(this);
	        
	        statusimg= (ImageView) findViewById(R.id.imageView1);
	        statusimg.setOnTouchListener(new View.OnTouchListener() {

		        public boolean onTouch(View v, MotionEvent event) {
		        	
		        		final int action = event.getAction();
		            switch (action & MotionEvent.ACTION_MASK) {

		                case MotionEvent.ACTION_DOWN: {
		                	if(connectflag && secondaryControl)
		                	{
		                		if(griprelease==1)
		                		{
		                			sendData("1");
		                			griprelease=3;
		                		}
		                		else if(griprelease==3)
		                		{
		                			sendData("3");
		                			griprelease=1;
		                		}
		                		
		                	}
		                    break;
		                }

		              
		                case MotionEvent.ACTION_UP:{
		                	if(connectflag && secondaryControl)
		                	{
		                		sendData("2");
		                	}
		                    break;
		                }
		            }
		            
		            return true;
		            
		          
		        }

		       

		    });
	        
	        
	        
	        txtstatus=(TextView)findViewById(R.id.txtstatus);
	        
	        conb= (Button)findViewById(R.id.button1);
	        conb.setOnClickListener(new OnClickListener() {
	    
				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					//connect();
					MyTask task = new MyTask();
			        task.execute();
				}
	        });
	        
	        FrameLayout frame = (FrameLayout) findViewById(R.id.frame);
	        camera= (ImageView) findViewById(R.id.camcover);
	        
	        frame.setOnClickListener(new OnClickListener() {
	    	    
	    				@Override
	    				public void onClick(View arg0) {
	    					
	    				    if(cameraflag){
	    				    	camera.setVisibility(View.GONE);
	    				        FrameLayout frame = (FrameLayout) findViewById(R.id.frame);
	    				        //frame.removeAllViews();
	    				        frame.addView(mv, 0);
	    				        cameraflag=false;
	    				       connectCam();
	    				     
	    				        
	    				    }
	    				    else
	    				    {
	    				    	destoryCam();
	    				    	FrameLayout frame = (FrameLayout) findViewById(R.id.frame);
	    				        frame.removeView(mv);
	    				        mv.stopPlayback();
	    				        
	    				    	camera.setVisibility(View.VISIBLE);
	    				    	//cameraflag=true;
	    				    	
	    				    }
	    				}
	    	        });
	 
	        
	        xaxis=(TextView)findViewById(R.id.xaxis);
	        yaxis=(TextView)findViewById(R.id.yaxis);
	        zaxis=(TextView)findViewById(R.id.zaxis);
	        		
	      sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
	      sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
	   }
	 
	
	 public void connectCam()
	 {
		  DoRead1 = new DoRead();
	     DoRead1.execute(URL);
	     // new DoRead().execute(URL);
	      
	 }
	 
	 public void destoryCam()
	 {
		 //DoRead1.cancel(true);
		 if(DoRead1.cancel(true))
		 {
			 cameraflag=true;
		 }
		 else
		 {
			 cameraflag=false;
		 }
	 }
	 
	 
	 public void connect()
	 {
		 TextView view = (TextView) findViewById(R.id.errorMsg);
		 try {
			clientSocket= new Socket(ipaddress,port);
			outToServer = new DataOutputStream(clientSocket.getOutputStream()); 
			
			connectflag=true;
			
			view.setText("");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			 view.setText("Don't know about host: hostname");
			 connectflag=false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			  view.setText("Couldn't get I/O for the connection to: hostname");
			  connectflag=false;
		}
	 }
	       
	 
	 public void sendData(String msg)
	 {
		 TextView view = (TextView) findViewById(R.id.errorMsg);
		 if (clientSocket != null && outToServer != null) {
	            try {
	               if(datastream){
	            	outToServer.writeBytes(msg);
	            	}
	               else{
	            	   if(msg.equals(previousmsg))
	            	   {
	            	   }
	            	   else
	            	   {
	            		   outToServer.writeBytes(msg);
	            	   }
	               }
	            	previousmsg=msg;
	            } catch (IOException e) {
	            	 view.setText("IOException:  " + e);
	            }
	           
	        }
		 
	 }
	
	 public void closeSocket()
	 {
		 TextView view = (TextView) findViewById(R.id.errorMsg);
		 
		 if (clientSocket != null && outToServer != null) {
	            try {
	                outToServer.close();
	                clientSocket.close();
	            } catch (UnknownHostException e) {
	                view.setText("Trying to connect to unknown host: " + e);
	            } catch (IOException e) {
	                view.setText("IOException:  " + e);
	            }
	        }
	 }
	 
	 
	
	 
	   @Override
	   public void onAccuracyChanged(Sensor arg0, int arg1) {
	   }

	   @Override
	   public void onSensorChanged(SensorEvent event) {
	        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
	                    xaxis.setText(String.valueOf(event.values[0]));
	                    yaxis.setText(String.valueOf(event.values[1]));
	                    zaxis.setText(String.valueOf(event.values[2]));
	                    
	                   /* if(flagbtnF==1)
	                    {
	                    	 sendData("F");
	                    }
	                    if(hndbrkflag==1)
	                    {
	                    	sendData("H");
	                    }
	                    */
	                    if(connectflag){
	                    if(event.values[1]>-2 && event.values[1]<2.5 && event.values[0]>-1 && event.values[0]<7)
	                    {
	                    	if(secondaryControl){
	                    		sendData("S");
	                    	}
	                    	else{
	                    		sendData("H");
	                    	}
	                    }
	                    else {
	                    	{
	                    
		                    if(event.values[1]<=-2)
		                    {
		                    	if(secondaryControl){
		                    		sendData("W");
		                    	}
		                    	else{
		                    		sendData("R");
		                    	}
		                    }
		                    else if(event.values[1]>2.5)
		                    {
		                    	if(secondaryControl){
		                    		sendData("E");
		                    	}
		                    	else{
		                    		sendData("L");
		                    	}
		                    }
		                    }
	                    
	                    	 {
	 		                    if(event.values[0]<=-1)
	 		                    {
	 		                    	if(secondaryControl){
	 		                    		sendData("D");
	 		                    	}
	 		                    	else{
	 		                    		sendData("F");
	 		                    	}
	 		                    }
	 		                    else if(event.values[0]>=7)
	 		                    {
	 		                    	if(secondaryControl){
	 		                    		sendData("U");
	 		                    	}
	 		                    	else{
	 		                    		sendData("B");
	 		                    	}
	 		                    }
	                    	 }
	        }
	                    }
	            }
	   }
	
	   @Override
		public void onPause() {
		   
		   sensorManager.unregisterListener(this);
		   super.onPause();
	   }
	   
	   
	   @Override
	   public void onResume() {
			super.onResume();
			sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
	   }
	 
	   /*
	   @Override
	   public void onDestroy(){
		   super.onDestroy();
		   mTcpClient.stopConnection();
	   }
	  */
	   
	   
	   @Override
		public boolean onCreateOptionsMenu(Menu menu) {
			//getMenuInflater().inflate(R.menu.login, menu);
		   getMenuInflater().inflate(R.menu.login, menu);
		     menu.add(1, 1, 0, "Disconnect");
		     menu.add(1, 2, 1, "Exit");
			return true;
		}
		
		@Override
	    public boolean onOptionsItemSelected(MenuItem item)
	    {
	    
	     switch(item.getItemId())
	     {
	     case 1:
	    	 closeSocket();
	    	 connectflag=false;
	    	 conb.setVisibility(View.VISIBLE);
	    	 statusimg.setImageResource(R.drawable.disconnected);
	    	 mylinearlayout.setVisibility(View.GONE);
	    	 myrelativelayout.setBackgroundColor(Color.parseColor("#585858"));
	    	 txtstatus.setText("Disconnected");
	    	 
	      return true;
	     case 2:
	    	 Intent intent = new Intent(getApplicationContext(), MainActivity.class);
	 	    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	 	    startActivity(intent);
	      return true;
	     case R.id.action_settings:
				Intent i = new Intent(this, UserSettingActivity.class);
				startActivityForResult(i, RESULT_SETTINGS);
				break;

	     }
	     return super.onOptionsItemSelected(item);

	    }
		
		@Override
		protected void onActivityResult(int requestCode, int resultCode, Intent data) {
			super.onActivityResult(requestCode, resultCode, data);

			switch (requestCode) {
			case RESULT_SETTINGS:
				showUserSettings();
				break;

			}

		}

		private void showUserSettings() {
			SharedPreferences sharedPrefs = PreferenceManager
					.getDefaultSharedPreferences(this);

			ipaddress=sharedPrefs.getString("prefIpaddress", "NULL");
			port=Integer.parseInt(sharedPrefs.getString("prefPort", "8050"));
			
			datastream=sharedPrefs.getBoolean("prefDataStream", true);
			
			URL="http://"+sharedPrefs.getString("prefIpCamaddress", "NULL");
			
			secondaryControl=sharedPrefs.getBoolean("prefSecondaryControl", false);
			
			/*StringBuilder builder = new StringBuilder();

			builder.append("\n Username: "
					+ sharedPrefs.getString("prefUsername", "NULL"));

			builder.append("\n Send report:"
					+ sharedPrefs.getBoolean("prefSendReport", false));

			builder.append("\n Sync Frequency: "
					+ sharedPrefs.getString("prefSyncFrequency", "NULL"));
			*/
		//	TextView settingsTextView = (TextView) findViewById(R.id.textUserSettings);

			//settingsTextView.setText(builder.toString());
		}
		
		
		
		public void onDestroy() {
		    super.onDestroy(); 
		    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
		    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    startActivity(intent);
		    //System.exit(0);
		}
		
		
	   private class MyTask extends AsyncTask<String, String, String>{

			@Override
			protected String doInBackground(String... params) {
			
				connect();
				
				return null;
				
			}
			
			protected void onPostExecute(String result) {
				  if(connectflag){
				sendData("ibot ready..");
				
				StringBuilder builder = new StringBuilder();

				builder.append(ipaddress+":"+ Integer.toString(port));

				txtstatus.setText(builder.toString());
				
				conb.setVisibility(View.GONE);
				
				statusimg.setImageResource(R.drawable.connected);
				mylinearlayout.setVisibility(View.VISIBLE);
				myrelativelayout.setBackgroundColor(Color.parseColor("#E8E8E8"));
				  }
				  else{
				  txtstatus.setText("Connection Failed!");
				  }
				  }
				
			}
	   
	   
	   

	    public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
	        protected MjpegInputStream doInBackground(String... url) {
	            //TODO: if camera has authentication deal with it and don't just not work
	            HttpResponse res = null;
	            DefaultHttpClient httpclient = new DefaultHttpClient();     
	            
	           // Log.d(TAG, "1. Sending http request");
	            try {
	                res = httpclient.execute(new HttpGet(URI.create(url[0])));
	              //  Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
	                if(res.getStatusLine().getStatusCode()==401){
	                    //You must turn off camera User Access Control before this will work
	                    return null;
	                }
	                return new MjpegInputStream(res.getEntity().getContent());  
	            } catch (ClientProtocolException e) {
	                e.printStackTrace();
	              //  Log.d(TAG, "Request failed-ClientProtocolException", e);
	                //Error connecting to camera
	            } catch (IOException e) {
	                e.printStackTrace();
	              //  Log.d(TAG, "Request failed-IOException", e);
	                //Error connecting to camera
	            }

	            return null;
	        }

	        protected void onPostExecute(MjpegInputStream result) {
	            mv.setSource(result);
	            mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
	            mv.showFps(true);
	        }
	    }
	    
	    
}