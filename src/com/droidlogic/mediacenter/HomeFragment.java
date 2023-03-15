package com.droidlogic.mediacenter;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;

import com.droidlogic.mediacenter.dlna.PrefUtils;
import com.droidlogic.mediacenter.flip.FlipViewPager;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import static com.droidlogic.mediacenter.SettingsFragment.KEY_DEVICE_NAME;

public class HomeFragment extends Fragment {

    private static final String NETWORK_ETHERNET = "eth0";
    private static final String NETWORK_WLAN = "wlan0";
    private ImageView ivNetwork;
    private TextView tvIpAddress;
    private TextView tvNetworkName;
    private TextView tvServerName;
    private FlipViewPager mFlipViewPager;
    private RadioButton mRbVideo;
    private RadioButton mRbAudio;
    private RadioButton mRbImage;
    private RadioGroup mRbGroup;
    private View mViewPoint;
    private int mPageCurrentIndex = Integer.MAX_VALUE / 2;
    private final Timer mTimer = new Timer();
    private ConnectivityManager connectivityManager;
    private boolean isResume = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        ivNetwork = view.findViewById(R.id.iv_network_status);
        tvIpAddress = view.findViewById(R.id.tv_ip_address);
        tvNetworkName = view.findViewById(R.id.tv_network_name);
        tvServerName = view.findViewById(R.id.tv_server_name);
        mFlipViewPager = view.findViewById(R.id.dmr_flip);
        mRbVideo = view.findViewById(R.id.rb_dmr_video);
        mRbAudio = view.findViewById(R.id.rb_dmr_audio);
        mRbImage = view.findViewById(R.id.rb_dmr_image);
        mRbGroup = view.findViewById(R.id.vp_dmr_indicator);
        mViewPoint = view.findViewById(R.id.img_point);
        initFlipPager();
    }

    @Override
    public void onResume() {
        super.onResume();
        isResume = true;
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        NetworkRequest request = builder.build();
        if (connectivityManager != null) {
            connectivityManager.registerNetworkCallback(request, networkCallback);
        }
        tvServerName.setText(new PrefUtils(getContext()).getString(KEY_DEVICE_NAME, getString(R.string.config_default_name)));
    }

    private void initFlipPager() {
        mFlipViewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return Integer.MAX_VALUE;
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
                return view == o;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                int index = position % 3;
                int res;
                switch (index) {
                    case 0:
                        res = R.drawable.dmr_image;
                        break;
                    case 1:
                        res = R.drawable.dmr_audio;
                        break;
                    default:
                        res = R.drawable.dmr_video;
                        break;
                }
                ImageView imageView = new ImageView(container.getContext());
                imageView.setBackgroundResource(res);
                container.addView(imageView);
                return imageView;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View) object);
            }

        });
        mFlipViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.page_margin));
        mFlipViewPager.setCurrentItem(mPageCurrentIndex);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (!isResume) return;
                    mPageCurrentIndex++;
                    mFlipViewPager.setCurrentItem(mPageCurrentIndex);
                    int index = mPageCurrentIndex % 3;
                    if (index == 0) {
                        mRbImage.setChecked(true);
                    } else if (index == 1) {
                        mRbAudio.setChecked(true);
                    } else {
                        mRbVideo.setChecked(true);
                    }
                    if (getView() != null) {
                        View view = getView().findViewById(mRbGroup.getCheckedRadioButtonId());
                        if (view != null) {
                            FrameLayout.LayoutParams pms = (FrameLayout.LayoutParams) mViewPoint.getLayoutParams();
                            pms.leftMargin = view.getLeft();
                            mViewPoint.setLayoutParams(pms);
                        }
                    }
                });
            }
        }, 500, 5000);
    }

    private void runOnUiThread(Runnable runnable) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(runnable);
        }
    }

    public void updateServerName(String name) {
        if (tvServerName != null) {
            tvServerName.setText(name);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isResume = false;
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        NetworkRequest request = builder.build();
        if (connectivityManager != null) {
            connectivityManager.registerNetworkCallback(request, networkCallback);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTimer.cancel();
    }

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {

        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
                    if (ivNetwork != null) {
                        ivNetwork.post(() -> ivNetwork.setImageResource(R.drawable.ic_ethernet));
                        //tvNetworkName.setText(R.string.network_ethernet);
                        tvNetworkName.setText("ethernet");
                    }
                    updateIpAddress(NETWORK_ETHERNET);
                } else {
                    if (ivNetwork != null) {
                        ivNetwork.post(() -> ivNetwork.setImageResource(R.drawable.ic_wifi));
                    }
                    updateWifiInfo();
                }
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            if (ivNetwork != null) {
                ivNetwork.post(() -> {
                    ivNetwork.setImageResource(R.drawable.ic_no_signal);
                    tvNetworkName.setText("--");
                });
            }
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);

        }
    };

    private void updateIpAddress(String interfaceName) {
        String ipAddress = getLocalIpAddress(interfaceName);
        if (tvIpAddress != null) {
            tvIpAddress.post(() -> tvIpAddress.setText(ipAddress));
        }
    }

    private void updateWifiInfo() {
        Context context = getContext();
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID();
            if (tvNetworkName != null) {
                tvNetworkName.post(() -> tvNetworkName.setText(ssid.replace("\"", "")));
            }
            if (tvIpAddress != null) {
                tvIpAddress.post(() -> tvIpAddress.setText(Formatter.formatIpAddress(wifiInfo.getIpAddress())));
            }
        }
    }

    public String getLocalIpAddress(String interfaceName) {
        try {
            for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements(); ) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.getName().toLowerCase().equals(interfaceName)) {
                    for (Enumeration<InetAddress> addresses = networkInterface.getInetAddresses(); addresses.hasMoreElements(); ) {
                        InetAddress inetAddress = addresses.nextElement();
                        if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
