/******************************************************************
*
*Copyright (C) 2016  Amlogic, Inc.
*
*Licensed under the Apache License, Version 2.0 (the "License");
*you may not use this file except in compliance with the License.
*You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing, software
*distributed under the License is distributed on an "AS IS" BASIS,
*WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*See the License for the specific language governing permissions and
*limitations under the License.
******************************************************************/
package com.droidlogic.mediacenter;

import java.util.List;
import java.util.Map;

import org.cybergarage.util.Debug;

import com.droidlogic.mediacenter.dlna.DMRError;
import com.droidlogic.mediacenter.dlna.DmpFragment;
import com.droidlogic.mediacenter.dlna.DmpService;
import com.droidlogic.mediacenter.dlna.DmpStartFragment;
import com.droidlogic.mediacenter.dlna.MediaCenterService;
import com.droidlogic.mediacenter.dlna.PrefUtils;
import com.droidlogic.mediacenter.dlna.DmpFragment.FreshListener;
import com.droidlogic.mediacenter.dlna.DmpService.DmpBinder;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Group;
import androidx.viewpager.widget.PagerAdapter;

import static com.droidlogic.mediacenter.SettingsFragment.KEY_DEVICE_NAME;

public class MediaCenterActivity extends Activity  implements FreshListener {
        private static final String TAG = "DLNA";
        private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
        private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
        private PrefUtils mPrefUtils;
        private Animation animation;
        //private DmpBinder mBinder;
        private DmpService mService;
        private boolean mStartDmp;
        private ServiceConnection mConn = null;
        private TextView mDeviceName = null;
        private Fragment mCallbacks;
        private Context mContent;
        private Group mMenuGroup;

        @Override
        protected void onCreate ( Bundle savedInstanceState ) {
            super.onCreate ( savedInstanceState );
            setContentView ( R.layout.activity_main );
            mPrefUtils = new PrefUtils ( this );
            animation = ( AnimationSet ) AnimationUtils.loadAnimation ( this, R.anim.refresh_btn );
            mDeviceName = ( TextView ) findViewById ( R.id.device_name );
            mMenuGroup = findViewById(R.id.menu_group);
            mContent = this;
            checkNet();
            LogStart();
            PermissionUtils.verifyStoragePermissions(this);
        }

        private void startDmpService() {
            mConn = new ServiceConnection() {
                @Override
                public void onServiceConnected ( ComponentName name, IBinder service ) {
                    DmpBinder mBinder = ( DmpBinder ) service;
                    mService = mBinder.getServiceInstance();
                }
                @Override
                public void onServiceDisconnected ( ComponentName name ) {
                    mService.forceStop();
                }
            };
            getApplicationContext().bindService ( new Intent ( mContent, DmpService.class ), mConn, Context.BIND_AUTO_CREATE );
            mStartDmp = true;
        }



        /**
         * @Description TODO
         * @return
         */
        public PrefUtils getPref() {
            return mPrefUtils;
        }


        @Override
        protected void onDestroy() {
            stopMediaCenterService();
            stopDmpService();
            super.onDestroy();
        }

        private void stopDmpService() {
            if ( mStartDmp && mConn != null ) {
                mStartDmp = false;
                getApplicationContext().unbindService ( mConn );
            }
        }

        @Override
        protected void onResume() {
            super.onResume();
            checkNet();
            /*mRefreshView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    animation.startNow();
                }
            });*/
            showDeviceName();
        }
        @Override
        protected void onPause() {
            super.onPause();
        }

        public void showDeviceName() {
            String serviceName = mPrefUtils.getString ( KEY_DEVICE_NAME, getString ( R.string.config_default_name ) );
            mDeviceName.setText ( serviceName );
        }

        public void startMediaCenterService() {
            Intent intent = new Intent ( mContent, MediaCenterService.class );
            startService ( intent );
        }

        private void stopMediaCenterService() {
            Intent intent = new Intent ( mContent, MediaCenterService.class );
            stopService ( intent );
        }

        /* (non-Javadoc)
         * @see com.droidlogic.mediacenter.DmpFragment.FreshListener#startSearch()
         */
        @Override
        public void startSearch() {
            if ( mService != null )
            { mService.startSearch(); }
        }
        @Override
        public void startDMP(){
            if ( mService != null )
                mService.restartDmp();
        }

        @Override
        public void stopDMP(){
            if ( mService != null )
                mService.forceStop();
        }
        /* (non-Javadoc)
         * @see com.droidlogic.mediacenter.DmpFragment.FreshListener#getFullList()
         */
        @Override
        public List<String> getDevList() {
            if ( mService == null )
            { return null; }
            return mService.getDevList();
        }

        /* (non-Javadoc)
         * @see com.droidlogic.mediacenter.FreshListener#getDevIcon(java.lang.String)
         */
        @Override
        public String getDevIcon ( String path ) {
            if ( mService == null )
            { return null; }
            return mService.getDeviceIcon ( path );
        }

        public List<Map<String, Object>> getBrowseResult ( String didl_str, List<Map<String, Object>> list, int itemTypeDir, int itemImgUnsel ) {
            if ( mService == null )
            { return null; }
            return mService.getBrowseResult ( didl_str, list, itemTypeDir, itemImgUnsel );
        }

        public String actionBrowse ( String mediaServerName, String item_id,
        String flag ) {
            if ( mService == null )
            { return null; }
            return mService.actionBrowse ( mediaServerName, item_id, flag );
        }

        @Override
        public boolean onKeyDown ( int keyCode, KeyEvent event ) {
            if ( keyCode == KeyEvent.KEYCODE_BACK ) {
                mCallbacks = getFragmentManager().findFragmentById ( R.id.frag_detail );
                if ( mCallbacks instanceof Callbacks ) {
                    ( ( Callbacks ) mCallbacks ).onBackPressedCallback();
                } else if (mMenuGroup.getVisibility() == View.VISIBLE) {
                    mMenuGroup.setVisibility(View.INVISIBLE);
                    Fragment fragment = getFragmentManager().findFragmentById(R.id.main_fragment);
                    getFragmentManager().beginTransaction().show(fragment).commitAllowingStateLoss();
                } else {
                    stopMediaCenterService();
                    stopDmpService();
                    MediaCenterActivity.this.finish();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MENU) {
                Fragment fragment = getFragmentManager().findFragmentById(R.id.main_fragment);
                if (mMenuGroup.getVisibility() != View.VISIBLE) {
                    mMenuGroup.setVisibility(View.VISIBLE);
                    getFragmentManager().beginTransaction().hide(fragment).commitAllowingStateLoss();
                }
                return true;
            }
            return super.onKeyDown ( keyCode, event );
        }

        private void checkNet() {
            ConnectivityManager mConnectivityManager = ( ConnectivityManager ) mContent.getSystemService ( Context.CONNECTIVITY_SERVICE );
            NetworkInfo wifiInfo = mConnectivityManager.getNetworkInfo ( ConnectivityManager.TYPE_WIFI );
            NetworkInfo ethInfo = mConnectivityManager.getNetworkInfo ( ConnectivityManager.TYPE_ETHERNET );
            NetworkInfo mobileInfo = mConnectivityManager.getNetworkInfo ( ConnectivityManager.TYPE_MOBILE );
            if ( ethInfo == null && wifiInfo == null && mobileInfo == null ) {
                Intent mIntent = new Intent();
                mIntent.addFlags ( Intent.FLAG_ACTIVITY_NEW_TASK );
                mIntent.setClass ( mContent, DMRError.class );
                startActivity ( mIntent );
                return;
            }
            if ( ( ethInfo != null && ethInfo.isConnectedOrConnecting() ) ||
                    ( wifiInfo != null && wifiInfo.isConnectedOrConnecting() ) ||
            ( mobileInfo != null && mobileInfo.isConnectedOrConnecting() ) ) {
                startMediaCenterService();
                startDmpService();
            } else {
                Intent mIntent = new Intent();
                mIntent.addFlags ( Intent.FLAG_ACTIVITY_NEW_TASK );
                mIntent.setClass ( mContent, DMRError.class );
                startActivity ( mIntent );
            }
        }

    public void updateServerName(String name) {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.main_fragment);
        if (fragment instanceof HomeFragment) {
            HomeFragment homeFragment = (HomeFragment) fragment;
            homeFragment.updateServerName(name);
        }
    }

    public interface Callbacks {
            public void onBackPressedCallback();
        }

        public void LogStart() {
            if ( PrefUtils.getProperties ( "rw.app.dlna.debug", "false" ).equals("true") ) {
                org.cybergarage.util.Debug.on(); //LOG ON
            } else {
                org.cybergarage.util.Debug.off();  //LOG OFF
            }
        }
        private BroadcastReceiver mHomeKeyReceiver =
            new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.i(TAG, "onReceive: action: " + action);
                if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                    String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                    Log.i(TAG, "reason: " + reason);
                    if (SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason)) {
                        Log.i(TAG, "homekey stop service");
                        stopMediaCenterService();
                        stopDmpService();
                        MediaCenterActivity.this.finish();
                    }
                }

            }
        };
}
