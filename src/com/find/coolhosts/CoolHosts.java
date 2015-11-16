package com.find.coolhosts;


import java.util.LinkedList;
import java.util.Queue;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class CoolHosts extends Activity {  
  
	private boolean root;
	private TextView console;
	private Button ad,customHosts,customIP,clearHosts,help,more;
	private LoadingButton oneKey;
	private ScrollView scrollView;
	
	public static final String TAG="coolhosts";
	private boolean netState=false;
	public static String CACHEDIR;
	public CheckCoolHostsVersion getVersion;
	private ButtonListener btnListener;
	
	
	
	private enum TASK
	{
		DOWNHOSTS,COPYNEWHOSTSFROMWEB,COPYNEWHOSTSFROMLOCAL,DELETEOLDHOSTS,GETURL,GETCHVERSION,GETHOSTSVERSION,AFTERWORK
	}
	private Queue <TASK> taskQueue=null;
	
	@SuppressLint("SetJavaScriptEnabled") 
    @Override  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
        setContentView(R.layout.main);  
        CACHEDIR=getFilesDir().toString();
        Log.v(TAG, CACHEDIR);
        setButtons();
        taskQueue = new LinkedList<TASK>();
        
        try {
        	getVersion=new CheckCoolHostsVersion(CoolHosts.this);
			getVersion.getLocalVersion();
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
        taskQueue.add(TASK.GETHOSTSVERSION);
        taskQueue.add(TASK.GETURL);
        taskQueue.add(TASK.GETCHVERSION);
         
        
        doNextTask();
    }  
	public void setButtons(){
		btnListener=new ButtonListener();
		ad=(Button)findViewById(R.id.ad);
		ad.setOnClickListener(btnListener);
		customHosts=(Button)findViewById(R.id.customehosts);
		customHosts.setOnClickListener(btnListener);
		customIP=(Button)findViewById(R.id.readfromfile);
		customIP.setOnClickListener(btnListener);
		clearHosts=(Button)findViewById(R.id.clearHosts);
		clearHosts.setOnClickListener(btnListener);
		help=(Button)findViewById(R.id.help);
		help.setOnClickListener(btnListener);
		more=(Button)findViewById(R.id.more);
		more.setOnClickListener(btnListener);
		console=(TextView)findViewById(R.id.console);
		console.setMovementMethod(new ScrollingMovementMethod());
		oneKey=(LoadingButton)findViewById(R.id.onekey);
		oneKey.setOnClickListener(btnListener);
		scrollView=(ScrollView)findViewById(R.id.scrollView);
		scrollView.post(new Runnable() {
			   @Override
			   public void run() {
			    // TODO Auto-generated method stub
			    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
			   }
			  });
	}
    public void onResume (){
    	super.onResume();
    	root=RootChecker.hasRoot();
    	oneKey.setCallback(new LoadingButton.Callback() {
            @Override
            public void complete() {
            	String note=Lib.isSuccessed?getString(R.string.updatehostssuccessed):getString(R.string.updatehostsfailed);
                Toast.makeText(getApplicationContext(),note,Toast.LENGTH_SHORT).show();
            }
        });
//    	oneKey.setOnClickListener(btnListener);
    }
    /**Update the console textview
     * 更新console
     * @param textview: 更新哪个textview
     * @param isAppend: 追加还是覆盖
     * @param ids: R.string.值*/
    public void appendOnConsole(TextView textview,boolean isAppend,final int ...id ){
    	if(!isAppend)console.setText("");
    	for(int i:id){
    		console.append(getString(i)+"\n");
    	}
    }
    public void appendOnConsole(TextView textview,boolean isAppend,final String ...strs){
    	if(!isAppend)console.setText("");
    	for(String tempstr:strs)
    		console.append(tempstr+"\n");
    }
  
    
	public TextView getConsole(){return console;}
	public boolean getNetState() {
		return netState;
	}
	/**设置网络状态*/
	public void setNetState(boolean netState) {
		this.netState = netState;
	}  
	public void showVersion(){
		AlertDialog.Builder builderAbout = new AlertDialog.Builder(CoolHosts.this);
		builderAbout.setMessage(getString(R.string.local_version)+Lib.LOCALCHVERSION+"\n"+getString(R.string.remote_version)+Lib.REMOTECHVERSION+"\n"+getString(R.string.updatechnote));
		builderAbout.setTitle(R.string.updatechversion);
		builderAbout.setCancelable(true);
		builderAbout.setPositiveButton("OK", new DialogInterface.OnClickListener(){
			@Override
			public void onClick (DialogInterface dialog, int which){dialog.cancel();}});
		AlertDialog alertAbout = builderAbout.create();
		alertAbout.show();
	}
	public void doNextTask(){
		if(taskQueue!=null && taskQueue.peek()!=null){
			switch(taskQueue.remove()){
			case COPYNEWHOSTSFROMWEB:
				appendOnConsole(getConsole(),true,R.string.copyingnewhosts);
				new FileCopier(CoolHosts.this).execute(CACHEDIR + "/hosts", Lib.HOSTSPATH);
				break;
			case DELETEOLDHOSTS:
				appendOnConsole(getConsole(),true,R.string.deleteoldhosts);
				new FileCopier(CoolHosts.this).execute(null,  Lib.HOSTSPATH);
				break;
			case DOWNHOSTS:
				appendOnConsole(getConsole(),true,R.string.downloadhosts);
				new WebDownloader(CoolHosts.this).execute(Lib.SOURCE,Lib.HOSTSINCACHE);
				break;
			case GETURL:
		        new SendGetApplication(CoolHosts.this).execute(0);
				break;
			case GETCHVERSION:
				new SendGetApplication(CoolHosts.this).execute(1);
				break;
			case GETHOSTSVERSION:
				new GetHostsVersion(CoolHosts.this).execute();
				break;
			case COPYNEWHOSTSFROMLOCAL:
				appendOnConsole(getConsole(),true,R.string.copyingnewhosts);
				new FileCopier(CoolHosts.this).execute(Lib.LOCALCUSTOMHOSTSPATH, Lib.HOSTSPATH);
				break;
			case AFTERWORK:
				appendOnConsole(console,true,getString(R.string.local_version)+Lib.getlocalversion());
				appendOnConsole(console,true,getString(R.string.remote_version)+Lib.getRemoteVersion());
				break;
			default:
				break;
			
			}
		}
	}
	/**cat hosts' content*/
	public void catHosts(){
		Intent catIntent=new Intent(CoolHosts.this,CatHosts.class);
		CoolHosts.this.startActivity(catIntent);
	}
	public void setOneKeyState(int num){
		oneKey.setTargetProgress(num);
	}
	@SuppressLint("NewApi")
	private final class ButtonListener implements View.OnClickListener{
		public void onClick(View v) {
			switch(v.getId()){
			case R.id.onekey:
				if(!root){
					Toast.makeText(CoolHosts.this, R.string.unrooted, Toast.LENGTH_SHORT).show();
				}else{
					//从本地文件更新
					if(Lib.UPDATEMODE==1 && !Lib.LOCALCUSTOMHOSTSPATH.equals("")){
						appendOnConsole(console, false, getString(R.string.readfromfilenote)+Lib.LOCALCUSTOMHOSTSPATH);
						taskQueue.add(TASK.DELETEOLDHOSTS);
						taskQueue.add(TASK.COPYNEWHOSTSFROMLOCAL);
						taskQueue.add(TASK.AFTERWORK);
						doNextTask();
					}else{
						/**判断网络状态*/
						if(getNetState()){
							oneKey.setOnClickDefault();
							oneKey.callOnClick();
							taskQueue.add(TASK.DOWNHOSTS);
							taskQueue.add(TASK.DELETEOLDHOSTS);
							taskQueue.add(TASK.COPYNEWHOSTSFROMWEB);
							taskQueue.add(TASK.AFTERWORK);
							doNextTask();
						}else{
							Toast.makeText(CoolHosts.this, R.string.neterror, Toast.LENGTH_SHORT).show();
						}
					}
				}
				break;
			case R.id.ad:
				Intent intent=new Intent(CoolHosts.this,AdPage.class);
                intent.putExtra("url","http://www.findspace.name");
                CoolHosts.this.startActivity(intent);
				break;
			case R.id.customehosts:
				final EditText et = new EditText(CoolHosts.this);
				new AlertDialog.Builder(CoolHosts.this).setTitle("请输入源地址").setIcon(
					     android.R.drawable.ic_dialog_info).setView(
					    et).setPositiveButton("确定",new DialogInterface.OnClickListener() {
					    	 public void onClick(DialogInterface dialog, int which) {
					    		 Lib.SOURCE=et.getText().toString();
					    		 CoolHosts.this.appendOnConsole(getConsole(), true, R.string.customhostsaddressnote);
//					    		 Toast.makeText(CoolHosts.this, "Host源已经切换，仅此次有效，重启应用后恢复为默认的findspace的源", Toast.LENGTH_SHORT).show();
					    	 }})
					     .setNegativeButton("取消", null).show();
				break;
			case R.id.readfromfile:
				Intent intent3 = new Intent(Intent.ACTION_GET_CONTENT);
				intent3.setType("*/*");
				intent3.addCategory(Intent.CATEGORY_OPENABLE);
				try {
					startActivityForResult(Intent.createChooser(intent3, "Choose File"), Lib.FILE_SELECT_CODE);
				} catch (android.content.ActivityNotFoundException ex) {
					Toast.makeText(CoolHosts.this, "No File Manager Founded", Toast.LENGTH_SHORT).show();
				}
				break;
			case R.id.clearHosts:
				taskQueue.add(TASK.DELETEOLDHOSTS);
				doNextTask();
				break;
			case R.id.help:
				Intent intent2=new Intent(CoolHosts.this,AdPage.class);
                intent2.putExtra("url","http://www.findspace.name/easycoding/503");
                CoolHosts.this.startActivity(intent2);
				break;
			case R.id.more:
				Toast.makeText(CoolHosts.this, "wait for next time~", Toast.LENGTH_SHORT).show();
				break;
			}
		}
		
	}
	 public void onActivityResult(int requestCode, int resultCode, Intent data) {  
	        // TODO Auto-generated method stub  
	        if(resultCode == Activity.RESULT_OK&&requestCode == Lib.FILE_SELECT_CODE){
	        	if(data.getData()!=null){
	        		Uri uri=data.getData();
	        		Lib.LOCALCUSTOMHOSTSPATH=uri.getPath();
	        		Toast.makeText(this, "现在你可以点击一键更新从本地更新了～", Toast.LENGTH_SHORT).show();
	        		Lib.UPDATEMODE=1;
	        	}
	        }
	        super.onActivityResult(requestCode, resultCode, data);  
	    }  
}









