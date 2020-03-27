package com.example.faseblescan;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cys.fastblescan.FastBleScanner;
import com.cys.fastblescan.bean.ScanDevice;
import com.cys.fastblescan.callback.FastBleScanCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements FastBleScanCallback,
        SwipeRefreshLayout.OnRefreshListener {

    private static final int MENU_ID_SETTING = 931;
    private RecyclerView idRcvDevice;
    private List<ScanDevice> mDeviceList = new ArrayList<>();
    private DeviceAdapter adapterDevice;
    private SwipeRefreshLayout idSrlDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getString(R.string.app_name));

        // 控件
        idSrlDevice = findViewById(R.id.id_srl_device);
        idSrlDevice.setOnRefreshListener(this);
        idRcvDevice = findViewById(R.id.id_rcv_device);
        idRcvDevice.setLayoutManager(new LinearLayoutManager(this));
        adapterDevice = new DeviceAdapter();
        idRcvDevice.setAdapter(adapterDevice);

        // 蓝牙6.0扫描 需要检测地理位置权限
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            } else {
                scan();
                idSrlDevice.setRefreshing(true);
            }
        }else{
            scan();
            idSrlDevice.setRefreshing(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 设置回调为空，防止内存泄漏
        FastBleScanner.getInstance().setScanCallback(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ID_SETTING, 0, getString(R.string.ac_main_menu_setting));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_SETTING:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!FastBleScanner.getInstance().isBluetoothEnabled()) {
            FastBleScanner.getInstance().enableBluetooth();
        }
    }

    @Override
    public void onRefresh() {
        scan();
    }

    public void scan() {
        mDeviceList.clear();
        adapterDevice.replaceData(mDeviceList);
        // 获取设置表的设置信息
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean scan_enable = sp.getBoolean("scan_name_enable", false);
        String scan_name = sp.getString("scan_name", "");
        if (!scan_enable) {
            scan_name = "";
        }

        // FastBleScanner库 使用 开始扫描
        FastBleScanner.getInstance()
                .setFilterName(scan_name) // 设置过滤名称
                .setIgnoreSame(true) // 设置扫描结果不重复
                .setScanTime(3) // 设置扫描时间
                .setScanCallback(this) // 设置回调
                .startScan(); // 开始扫描
    }


    @Override
    public void onStartScan() {
        Toast.makeText(this, getString(R.string.ac_main_start_scan), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLeScan(final ScanDevice scanDevice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeviceList.add(scanDevice);
                Collections.sort(mDeviceList, new Comparator<ScanDevice>() {
                    @Override
                    public int compare(ScanDevice o1, ScanDevice o2) {
                        if (o1.rssi < o2.rssi) {
                            return 1;
                        } else if (o1.rssi == o2.rssi) {
                            return 0;
                        }
                        return -1;
                    }
                });
                adapterDevice.replaceData(mDeviceList);
            }
        });
    }

    @Override
    public void onStopScan() {
        Toast.makeText(MainActivity.this, getString(R.string.ac_main_stop_scan), Toast.LENGTH_SHORT).show();
        if (idSrlDevice != null) {
            idSrlDevice.setRefreshing(false);
        }
    }

    @Override
    public void onScanFailure(int errorCode) {
        Toast.makeText(MainActivity.this, getString(R.string.ac_main_scan_error), Toast.LENGTH_SHORT).show();
    }
}
