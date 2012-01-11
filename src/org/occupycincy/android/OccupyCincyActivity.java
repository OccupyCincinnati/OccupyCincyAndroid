package org.occupycincy.android;

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
//import android.content.res.Resources;
//import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.apache.commons.lang3.StringEscapeUtils;

public class OccupyCincyActivity extends Activity {
	
	private ArrayList<HashMap<String, String>> feedItems;
	private ProgressDialog progress = null;
	private DownloadFileTask downloadTask = null;
	private AlertDialog alert = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		
		TextView link = (TextView)findViewById(R.id.lblOccupy);
		link.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_occupy))));			
			}
		});
		
		Button btn = (Button)findViewById(R.id.btnGetInvolved);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_getInvolved))));			
			}
		});
		
		btn = (Button)findViewById(R.id.btnDonate);
		btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_donate))));			
			}
		});
		
		getOccupyFeed(false);
    }
    
//    @Override
//    public void onStart() {
//    	super.onStart();
//        getOccupyFeed();
//    }
        
//    @Override
//    public void onResume() {
//    	super.onResume();
//        getOccupyFeed();
//    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	if(downloadTask != null) {
    		downloadTask.cancel(true);
    	}
    	
    	if( alert != null ) {
    		alert.dismiss();
    		alert = null;
    	}
    	
    	// remove loading progress dialog
    	setLoading(false);
    }

    private void processFeed() {

    	feedItems = new ArrayList<HashMap<String, String>>();
    	
    	File file = new File(getFullPath(getString(R.string.occupy_feed_file)));
    	if( file.exists() )
    		parseOccupyFeed();
    	
        populateBlogView();
        
        setLoading(false);
    }

//    private void setStatus(String text) {
//    	TextView tv = (TextView)findViewById(R.id.textView1);
//    	if(tv != null)
//    		tv.setText(text);
//    }
    
    private void setToast(int resId) {
    	setToast(getString(resId));
    }
    
    private void setToast(String text) {
    	Context context = getApplicationContext();
    	int duration = Toast.LENGTH_SHORT;

    	Toast toast = Toast.makeText(context, text, duration);
    	toast.setGravity(Gravity.BOTTOM, 0, 0);
    	toast.show();
    }

    private void showAlert(String text) {
    	
    	if( alert != null ) {
    		alert.dismiss();
    		alert = null;
    	}

    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(text)
    	       .setCancelable(false)
    	       .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   dialog.dismiss();
    	           }
    	       });
    	alert = builder.create();
    	alert.show();
    }

    private void startFileDownload(String URL, String fileName) {
    	DownloadTaskParams dtp = new DownloadTaskParams();
    	dtp.URL = URL;
    	dtp.filename = fileName;
    	
        setLoading(true);

    	downloadTask = new DownloadFileTask();
    	downloadTask.execute(dtp);
    }
    
    private void getOccupyFeed(Boolean force) {
    	// get NOW() minus one hour
    	Calendar cal = Calendar.getInstance();
    	cal.setTime(new Date());
    	cal.add(Calendar.MINUTE, -getResources().getInteger(R.integer.autoupdate_minutes));
    	    	
		File file = new File(getFullPath(getString(R.string.occupy_feed_file)));
		// if file doesn't exist or is older than one hour ago we want to download it again.
		if(force || !file.exists() || file.lastModified() < cal.getTimeInMillis()) {
	    	startFileDownload(getString(R.string.url_occupy_feed), getString(R.string.occupy_feed_file));
		}
		else
			// load any old info we might have
			processFeed();
    }
    
    private void parseOccupyFeed() {
    	
    	try {
	  		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	  		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	  		
	  		Document doc = dBuilder.parse(new File(getFullPath(getString(R.string.occupy_feed_file))));
	  		
	  		doc.getDocumentElement().normalize();
	   
	  		NodeList nList = doc.getElementsByTagName("item");
	   
	  		for (int i = 0; i < nList.getLength(); i++) {
	   
	  		   Node nNode = nList.item(i);
	  		   if (nNode.getNodeType() == Node.ELEMENT_NODE) {
	   
	  		      Element eElement = (Element) nNode;
	   
	  		      HashMap<String, String> item = new HashMap<String, String>();
	  		      
	  		      item.put("title", getTagValue("title", eElement));
	  		      item.put("link", getTagValue("link", eElement));
	  		      item.put("description", StringEscapeUtils.unescapeXml(getTagValue("description", eElement)));
	   
	  		      feedItems.add(item);
	  		   }
	  		}
    	} catch (Exception e) {
    		showAlert(e.getMessage());
    	}
    }
    
    private static String getTagValue(String sTag, Element eElement) {
    	NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
     
    	Node nValue = (Node) nlList.item(0);
     
    	return nValue.getNodeValue();
    }
    
    private void populateBlogView() {
    	ListView lv = (ListView)findViewById(R.id.lvBlog);
    	ListAdapter la = null;
    	if(feedItems.isEmpty()) {
    		la = new ArrayAdapter<Object>(getApplicationContext(), android.R.layout.simple_list_item_1, new String[] {"No data. Try again later."});
    		lv.setOnItemClickListener(null);
    	}
    	else {
	    	la = new SimpleAdapter(getApplicationContext(), feedItems, R.layout.blog_row, 
	    			new String[] {"title", "description"}, 
	    			new int[] {R.id.txtTitle, R.id.txtDescription}); 

	    	lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
	    	    public void onItemClick(AdapterView<?> parent, View v, int position, long id)
	    	    {
	    	        //setToast(feedItems.get(position).get("title"));
	    	    	startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(feedItems.get(position).get("link"))));
	    	    }
	    	});
    	}
    	lv.setAdapter(la);
    }
    
    
    private void setLoading(Boolean on) {
    	if(progress != null) {
    		progress.dismiss();
    		progress = null;
    	}
    	
    	if(on) {
			progress = ProgressDialog.show(this, "", getString(R.string.loading), true);
    	}
    }

    
	private class DownloadTaskParams {
		public String URL;
		public String filename;
	}
	
	private class DownloadTaskResult {
		public Boolean success;
		public String errorMessage;
		public String fileName;
		
		DownloadTaskResult(String filename) {
			success = false;
			fileName = filename;
		}
	}
    
    private class DownloadFileTask extends AsyncTask<DownloadTaskParams, Void, DownloadTaskResult> {
        /** The system calls this to perform work in a worker thread and
          * delivers it the parameters given to AsyncTask.execute() */
        protected DownloadTaskResult doInBackground(DownloadTaskParams... dtp) {
            return downloadFile(dtp[0].URL, dtp[0].filename);
        }
        
        /** The system calls this to perform work in the UI thread and delivers
          * the result from doInBackground() */
        protected void onPostExecute(DownloadTaskResult result) {
        	if(result.success) {
        		if( replaceOldFile(result.fileName) ) {
	        		setToast(R.string.download_complete);
	        		processFeed();
        		}
        		else 
        			setLoading(false);
        	}
        	else {
        		showAlert(result.errorMessage);
        		processFeed();
        	}
        }
    }
    
    private Boolean replaceOldFile(String filename) {
    	Boolean retVal = false;
    	
    	try {
    		File oldFile = new File(getFullPath(filename));
    		if( oldFile.exists() )
    			oldFile.delete();
    		
    		File newFile = new File(getFullPath(filename, ".new"));
    		newFile.renameTo(oldFile);
    		
    		retVal = true;
    	}
    	catch(Exception ex) {
    		showAlert(ex.getMessage());
    	}
    	
    	return retVal;
    }

    private DownloadTaskResult downloadFile(String strURL, String fileName) {
    	DownloadTaskResult retVal = new DownloadTaskResult(fileName);
    	
    	HttpURLConnection urlConnection = null;
		try {
			ConnectivityManager conn = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

			NetworkInfo info = conn.getActiveNetworkInfo();
			if( info == null || !info.isConnected() )
				throw new Exception(getString(R.string.error_not_connected));
				
			
			URL url = new URL(strURL);
			urlConnection = (HttpURLConnection) url.openConnection();
			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			
			retVal.success = saveFile(in, fileName);
		}
		catch(Exception ex)
		{
			retVal.errorMessage = ex.getMessage();
		}
	    finally {
	    	if(urlConnection != null)
	    		urlConnection.disconnect();
	    }
		
		return retVal;
    }
    
    private Boolean saveFile(InputStream inStream, String fileName) throws Exception {
    	Boolean retVal = false;
    	
    	// check if ExternalStorage is available
    	String state = Environment.getExternalStorageState();
    	if (Environment.MEDIA_MOUNTED.equals(state)) {
    		File file = new File(getFullPath(fileName, ".new"));
    		
    		if(!file.getParentFile().exists())
    			file.getParentFile().mkdirs();
    		
    		if( file.exists() ) 
    			file.delete();
    		
    		file.createNewFile();
    		
    		BufferedOutputStream buf = new BufferedOutputStream(new FileOutputStream(file));
    		try {
	    		int read = 0;
	    		byte[] bytes = new byte[1024];
	    	 
	    		while ((read = inStream.read(bytes)) != -1) {
	    			if( downloadTask.isCancelled() )
	    				break;
	    			buf.write(bytes, 0, read);
	    		}

	    		retVal = !downloadTask.isCancelled();
    		}
    		finally {
	    		inStream.close();
	    		buf.flush();
	    		buf.close();
    		}
    	}
    	else
    		throw new Exception(getString(R.string.error_write_ext_storage));
    	
    	return retVal;
    }
    
    private String getFullPath(String fileName) {
    	return getFullPath(fileName, "");
    }
    
    private String getFullPath(String fileName, String ext) {
		File exRoot = Environment.getExternalStorageDirectory();
		return exRoot.getAbsolutePath() + getString(R.string.file_directory).replace("<package_name>", this.getClass().getPackage().getName()) + fileName + ext;
    }
}