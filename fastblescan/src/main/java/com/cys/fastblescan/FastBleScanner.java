package com.cys.fastblescan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.os.Build;

import com.cys.fastblescan.bean.ScanDevice;
import com.cys.fastblescan.callback.FastBleScanCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.reactivex.disposables.Disposable;

/**
 * 具体使用：
 * 开始扫描：
 * FastBleScanner.getInstance()
 * .setFilterName(scan_name) // 设置过滤名称
 * .setIgnoreSame(true) // 设置扫描结果不重复
 * .setScanTime(5) // 设置扫描时间
 * .setScanCallback(this) // 设置回调
 * .startScan(); // 开始扫描
 * 停止扫描：
 * FastBleScanner.getInstance().stopScan();
 *
 * 温馨提示 ：
 * 在android N 中 连续扫描停止，到第五次会出现扫描不到的情况
 */
public class FastBleScanner {

    private static final String TAG = FastBleScanner.class.getSimpleName();

    /**
     * SCAN_ERROR_BLUETOOTH_IS_DISABLE : 表示蓝牙不存在或者未打开
     * SCAN_ERROR_BLUETOOTH_SCANNER_NULL ： 表示BluetoothLeScanner不存在
     * SCAN_ERROR_BLUETOOTH_SCAN_BY_ANDROID_K ： 表示Android 4.4 的开启扫描失败
     */
    public static final int SCAN_ERROR_BLUETOOTH_IS_DISABLE = 0x43F;
    public static final int SCAN_ERROR_BLUETOOTH_SCANNER_NULL = 0x44F;
    public static final int SCAN_ERROR_BLUETOOTH_SCAN_BY_ANDROID_K = 0x45F;
    protected Disposable mScanDisposable;

    protected boolean isStartScan;
    protected boolean isScanning;
    protected boolean isIgnoreSame; // 是否忽略Mac地址一致的设备（即只扫描一次相同的设备）
    protected Map<String, ScanDevice> mIgnoreSameMaps;
    protected String mFilterName; // 过滤的名称
    protected List<UUID> mFilterUuidList; // 过滤的UUID
    protected int mScanTimeout; // 扫描的时间

    protected BluetoothAdapter mBluetoothAdapter;
    protected BluetoothLeScanner mBluetoothLeScanner;
    protected BluetoothAdapter.LeScanCallback mLeScanCallback;
    protected ScanCallback mScanCallback;
    protected FastBleScanCallback mCallback;

    protected static FastBleScanner mThis;

    //region 单例
    protected FastBleScanner() {
        isStartScan = false;
        isScanning = false;
    }

    public static FastBleScanner getInstance() {
        if (mThis == null) {
            synchronized (FastBleScanner.class) {
                if (mThis == null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mThis = new FastBleScannerImplLollipop();
                    } else {
                        mThis = new FastBleScannerImplJB();
                    }
                }
            }
        }

        return mThis;
    }
    //endregion

    /**
     * 开始扫描
     *
     * @return true：开启扫描成功，false：开启扫描失败
     */
    synchronized public boolean startScan() {
        return false;
    }

    /**
     * 停止扫描
     */
    synchronized public void stopScan() {

    }

    /**
     * 设置回调
     *
     * @param callback 扫描回调
     * @return FastBleScanner对象
     */
    public FastBleScanner setScanCallback(FastBleScanCallback callback) {
        return this;
    }

    //region 属性设置
    public boolean isScanning() {
        return isScanning;
    }

    /**
     * 设置扫描名称
     *
     * @param name 扫描名称
     * @return FastBleScanner对象
     */
    public FastBleScanner setFilterName(String name) {
        this.mFilterName = name;
        return this;
    }

    /**
     * 设置是否忽略重复扫描到的设备
     *
     * @param ignoreSame true：停止扫描相同设备，false：继续扫描相同设备
     * @return FastBleScanner对象
     */
    public FastBleScanner setIgnoreSame(boolean ignoreSame) {
        this.isIgnoreSame = ignoreSame;
        if (mIgnoreSameMaps == null) {
            mIgnoreSameMaps = new HashMap<>();
        }
        return this;
    }

    /**
     * 设置扫描时间
     * @param second 时间（单位s）
     * @return FastBleScanner对象
     */
    public FastBleScanner setScanTime(int second) {
        this.mScanTimeout = second;
        return this;
    }

    /**
     * 清除过滤UUID
     *
     * @return FastBleScanner对象
     */
    public FastBleScanner clearFilterUuid() {
        if (mFilterUuidList != null) {
            mFilterUuidList.clear();
        }
        return this;
    }

    /**
     * 设置UUID
     *
     * @param filterUuid 扫描需要识别的UUID
     * @return FastBleScanner对象
     */
    public FastBleScanner setFilterUuid(UUID filterUuid) {
        if (mFilterUuidList == null) {
            mFilterUuidList = new ArrayList<>();
        }
        if (!isExistsUuuidInList(mFilterUuidList, filterUuid)) {
            mFilterUuidList.add(filterUuid);
        }
        return this;
    }

    /**
     * 设置UUID
     *
     * @param filterUuid 扫描需要识别的UUID
     * @return FastBleScanner对象
     */
    public FastBleScanner setFilterUuid(List<UUID> filterUuid) {
        if (mFilterUuidList == null) {
            mFilterUuidList = new ArrayList<>();
        }
        mFilterUuidList.addAll(filterUuid);
        return this;
    }

    private boolean isExistsUuuidInList(List<UUID> list, UUID filterUuid) {
        final List<UUID> uuids = list;
        for (UUID uuid : uuids) {
            if (uuid.toString().equals(filterUuid)) {
                return true;
            }
        }
        return false;
    }
    //endregion

    /**
     * 打开蓝牙
     *
     * @return true：打开成功，false：打开失败
     */
    public boolean enableBluetooth() {
        final BluetoothAdapter adapter = getBluetoothAdapter();
        if (adapter == null)
            return false;
        if (adapter.isEnabled())
            return true;
        return adapter.enable();
    }

    /**
     * 蓝牙是否打开
     *
     * @return true：已经打开，false：已经关闭
     */
    public boolean isBluetoothEnabled() {
        return this.getBluetoothAdapter() != null
                && this.getBluetoothAdapter().isEnabled();
    }

    /**
     * 手机是否具备蓝牙模块
     *
     * @return true：手机支持蓝牙，false：手机不支持蓝牙
     */
    public boolean isSupportBluetooth() {
        return this.getBluetoothAdapter() != null;
    }

    /**
     * 返回蓝牙适配器
     *
     * @return BluetoothAdapter对象
     */
    public BluetoothAdapter getBluetoothAdapter() {
        synchronized (this) {
            if (this.mBluetoothAdapter == null) {
                this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
        }
        return this.mBluetoothAdapter;
    }
}
