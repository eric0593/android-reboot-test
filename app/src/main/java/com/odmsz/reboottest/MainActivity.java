/*Author: longcheer zhengbaojiang
/ Version: v3.0
*/

package com.odmsz.reboottest;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;

import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import android.view.WindowManager;

import static android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP;
//import android.os.SystemProperties;

@SuppressLint("NewApi")
public class MainActivity extends Activity {

    static final String TAG = "RebootTest";
    static final String CONFIG = "CountsAndValue";

    SharedPreferences mPrefs = null;
    SharedPreferences.Editor mEditor = null;

    Button btnStart;
    Button btnStop;
    Button btnReset;
    EditText editWaitTime;
    EditText editCounts;
    TextView tvCountDown;
    TextView mRebootContent;
    int mCounts = 0;
    int mWaitTime = 0;
    int mLeftCounts = 0;
    boolean mStatus;
    AlarmManager mAlarmManager;
    PendingIntent pi;
    RebootTestReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //if (!SystemProperties.get("ro.product.device","ODMSZ").equals("A2110"))
        //{
        //    finish();
       // }
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        btnStart = (Button) findViewById(R.id.start);
        btnStop = (Button) findViewById(R.id.stop);
        btnReset = (Button) findViewById(R.id.reset);
        
        editWaitTime = (EditText) findViewById(R.id.wait_time);
        editCounts = (EditText) findViewById(R.id.counts);
        tvCountDown = (TextView)findViewById(R.id.countdown);

        mPrefs =MainActivity.this.getSharedPreferences(CONFIG, MODE_PRIVATE);
        mEditor =mPrefs.edit();
        mCounts = mPrefs.getInt("counts",0);
        mWaitTime = mPrefs.getInt("wait_time",0);
        mLeftCounts = mPrefs.getInt("left_counts",0);
        mStatus = mPrefs.getBoolean("status",false);

        Log.d(TAG, "oncreate count="+mCounts+"  wait_time="+mWaitTime +" left_counts="+mLeftCounts);

        editCounts.setText(""+mCounts);
        editWaitTime.setText(""+mWaitTime);

        mReceiver = new RebootTestReceiver();
        IntentFilter filter = new IntentFilter("com.odmsz.intent.reboot");
        registerReceiver(mReceiver, filter);

        mRebootContent =(TextView)findViewById(R.id.reboot_content);
        mRebootContent.setText("\r\n"+getString(R.string.text_waittime)+mWaitTime+"  "+getString(R.string.text_counts)+mCounts
                +"  "+getString(R.string.text_leftcounts)+mLeftCounts+"\r\n");

        btnStart.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if (editCounts.getText().toString().equals("0") || editWaitTime.getText().toString().equals("0")
                        ||editCounts.getText().toString().equals("") || editWaitTime.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(),getString(R.string.tip),Toast.LENGTH_LONG).show();
                    return;
                }
                //FileWrite(count);
                //Counts
                mCounts = Integer.parseInt(editCounts.getText().toString());
                mWaitTime = Integer.parseInt(editWaitTime.getText().toString());
                if (mCounts!=mPrefs.getInt("counts",0)||
                        mWaitTime!=mPrefs.getInt("wait_time",0))
                {
                    mLeftCounts = mCounts;
                    mEditor.putInt("counts",mCounts);
                    mEditor.putInt("wait_time",mWaitTime);
                    mEditor.putInt("left_counts",mCounts);
                    mEditor.commit();
                }else
                {
                    mLeftCounts = mPrefs.getInt("left_counts",0);
                }
                mStatus = true;
                mEditor.putBoolean("status",mStatus);

                mRebootContent.setText("\r\n"+getString(R.string.text_waittime)+mWaitTime+"  "+getString(R.string.text_counts)+mCounts
                        +"  "+getString(R.string.text_leftcounts)+mLeftCounts+"\r\n");
                //tvCountDown.setText(getString(R.string.text_countdowning) + waitTime+getString(R.string.second));
                Log.d(TAG, "The Phone will Reboot! count="+mCounts+"  waitTime="+mWaitTime);
                if(mLeftCounts > 0 && mWaitTime > 0){
                    rebootDelay(mWaitTime);
                }
            }
        });
        
        btnStop.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                //Status
                mStatus = false;
                mEditor.putBoolean("status",mStatus);
                mEditor.commit();
                mLeftCounts++;
                cancelReboot();
                Toast.makeText(MainActivity.this,getString(R.string.Stoptip),Toast.LENGTH_LONG).show();
            }
        });

        btnReset.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                //Status
                mStatus = false;
                mEditor.putBoolean("status",mStatus);
                mEditor.putInt("counts",0);
                mEditor.putInt("left_counts",0);
                mEditor.putInt("wait_time",0);
                mEditor.commit();
                cancelReboot();
                mRebootContent.setText("\r\n"+getString(R.string.text_waittime)+0+"  "+getString(R.string.text_counts)+0
                +"  "+getString(R.string.text_leftcounts)+0+"\r\n");
                Toast.makeText(MainActivity.this,getString(R.string.Stoptip),Toast.LENGTH_LONG).show();
            }
        });

        if (mLeftCounts>0 && mStatus==true) {
            rebootDelay(mWaitTime);
        }
    }
    private class RebootTestReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if (action.equals("com.odmsz.intent.reboot")) {
                // TODO - this should understand the interface types
                rebootNow();
            }
        }
    }

    @Override
    protected void onResume() {
        //wakeLock();
        super.onResume();
    }

    @Override
    protected void onPause() {
        //wakeUnlock();
        super.onPause();
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what > 0) {
                Message countDown = new Message();
                countDown.what = --msg.what;
                if(mStatus) {
                    mHandler.sendMessageDelayed(countDown, 1000);
                    tvCountDown.setText(getString(R.string.text_count_down_start) + msg.what +getString(R.string.second));
                }else{
                    tvCountDown.setText(getString(R.string.text_count_down_stop) );
                }
                Log.i(TAG,"mCountDownTime mTimer sendMessage msg.what= "+msg.what);
            } else {
                if (mStatus)
                    tvCountDown.setText(getString(R.string.text_count_reboot_now));
                else
                    tvCountDown.setText(getString(R.string.text_count_down_stop));
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //moveTaskToBack(false);
            if(mStatus)
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    void rebootNow()
    {
        mLeftCounts--;
        mEditor.putInt("left_counts",mLeftCounts);
        if(mLeftCounts==0)
            mEditor.putBoolean("status",false);
        mEditor.commit();
        Intent reboot = new Intent(Intent.ACTION_REBOOT);
        reboot.putExtra("nowait", 1);
        reboot.putExtra("interval", 1);
        reboot.putExtra("window", 0);
        sendBroadcast(reboot);
        Log.d(TAG,"reboot now");
    }

    void rebootDelay(int sec)
    {
        long interval = SystemClock.elapsedRealtime()+sec*1000;
        Intent reboot = new Intent("com.odmsz.intent.reboot");
        pi = PendingIntent.getBroadcast(MainActivity.this, 0, reboot, 0);
        Log.d(TAG,"reboot alarm set");
        mAlarmManager.setExact(ELAPSED_REALTIME_WAKEUP,interval, pi);
        Message msg = new Message();
        msg.what = sec;
        mHandler.sendMessage(msg);
    }

    void cancelReboot()
    {
        tvCountDown.setText(getString(R.string.text_count_down_stop));
        Log.d(TAG,"reboot alarm clear");
        if (pi!=null)
            mAlarmManager.cancel(pi);
    }

}
