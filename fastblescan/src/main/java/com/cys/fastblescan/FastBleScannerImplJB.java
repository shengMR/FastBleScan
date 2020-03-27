package com.cys.fastblescan;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.RequiresPermission;

import com.cys.fastblescan.bean.ScanDevice;
import com.cys.fastblescan.callback.FastBleScanCallback;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class FastBleScannerImplJB extends FastBleScanner {

    protected FastBleScannerImplJB() {
        super();
    }

    @Override
    public synchronized boolean startScan() {
        synchronized (this) {
            if (isStartScan || isScanning) {
                return true;
            }
            isStartScan = true;
        }

        if (mIgnoreSameMaps != null) {
            mIgnoreSameMaps.clear();
        }

        // 蓝牙未打开返回失败
        if (!this.isBluetoothEnabled()) {
            isStartScan = false;
            if (mCallback != null) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onScanFailure(SCAN_ERROR_BLUETOOTH_IS_DISABLE);
                    }
                });
            }
            return false;
        }

        final BluetoothAdapter adapter = getBluetoothAdapter();

        if (!adapter.startLeScan((UUID[]) mFilterUuidList.toArray(), mLeScanCallback)) {
            synchronized (this) {
                isStartScan = false;
                isScanning = false;
            }
            if (mCallback != null) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onScanFailure(SCAN_ERROR_BLUETOOTH_SCAN_BY_ANDROID_K);
                    }
                });
            }
        } else {
            synchronized (this) {
                isScanning = true;
            }
            if (mCallback != null) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onStartScan();
                    }
                });
            }
        }

        if (mScanTimeout > 0) {
            final long codeTimes = mScanTimeout;
            Observable.interval(1, 1, TimeUnit.SECONDS)
                    .take(codeTimes)
                    .map(new Function<Long, Long>() {
                        @Override
                        public Long apply(Long aLong) throws Exception {
                            return codeTimes - aLong;
                        }
                    })
                    .observeOn(Schedulers.io())
                    .subscribe(new Observer<Long>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            mScanDisposable = d;
                        }

                        @Override
                        public void onNext(Long aLong) {

                        }

                        @Override
                        public void onError(Throwable e) {
                            stopScan();
                        }

                        @Override
                        public void onComplete() {
                            stopScan();
                        }
                    });
        }
        return true;
    }

    @Override
    public synchronized void stopScan() {
        super.stopScan();

        if (mScanDisposable != null) {
            mScanDisposable.dispose();
        }

        final BluetoothAdapter adapter = getBluetoothAdapter();
        adapter.stopLeScan(mLeScanCallback);

        synchronized (this) {
            isStartScan = false;
            isScanning = false;
        }

        if (mCallback != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onStopScan();
                }
            });
        }
    }

    @Override
    public FastBleScanner setScanCallback(FastBleScanCallback callback) {
        this.mCallback = callback;

        if (mLeScanCallback == null) {
            mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

                @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN})
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

                    if (!isScanning) {
                        return;
                    }

                    if (!TextUtils.isEmpty(mFilterName) && TextUtils.isEmpty(device.getName())) {
                        return;
                    }

                    if (!TextUtils.isEmpty(mFilterName) && !TextUtils.isEmpty(device.getName())
                            && !mFilterName.equals(device.getName())) {
                        return;
                    }

                    if (isIgnoreSame) {
                        if (mIgnoreSameMaps != null) {
                            if (mIgnoreSameMaps.containsKey(device.getAddress())) {
                                return;
                            }
                        }
                    }

                    ScanDevice scanDevice = new ScanDevice();
                    scanDevice.device = device;
                    scanDevice.deviceName = device.getName();
                    scanDevice.rssi = rssi;
                    scanDevice.scanRecord = scanRecord;
                    if (isIgnoreSame) {
                        if (mIgnoreSameMaps != null) {
                            mIgnoreSameMaps.put(scanDevice.device.getAddress(), scanDevice);
                        }
                    }

                    Observable.just(scanDevice)
                            .observeOn(Schedulers.io())
                            .subscribe(new Observer<ScanDevice>() {
                                @Override
                                public void onSubscribe(Disposable d) {

                                }

                                @Override
                                public void onNext(ScanDevice scanDevice) {
                                    if (mCallback != null) {
                                        mCallback.onLeScan(scanDevice);
                                    }
                                }

                                @Override
                                public void onError(Throwable e) {

                                }

                                @Override
                                public void onComplete() {

                                }
                            });
                }
            };
        }
        return this;
    }
}
