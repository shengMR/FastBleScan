package com.cys.fastblescan;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.text.TextUtils;

import com.cys.fastblescan.bean.ScanDevice;
import com.cys.fastblescan.callback.FastBleScanCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class FastBleScannerImplLollipop extends FastBleScanner {

    protected FastBleScannerImplLollipop() {
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

        if (mBluetoothLeScanner == null) {
            mBluetoothLeScanner = adapter.getBluetoothLeScanner();
        }
        if (mBluetoothLeScanner == null) {
            synchronized (this) {
                isStartScan = false;
                isScanning = false;
            }

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (mCallback != null) {
                        mCallback.onScanFailure(SCAN_ERROR_BLUETOOTH_SCANNER_NULL);
                    }
                }
            });
            return false;
        } else {
            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            List<ScanFilter> scanFilters = null;
            if (mFilterUuidList != null && mFilterUuidList.size() > 0) {
                scanFilters = new ArrayList<>();
                for (int i = 0; i < mFilterUuidList.size(); i++) {
                    UUID uuid = mFilterUuidList.get(i);
                    ScanFilter.Builder builder = new ScanFilter.Builder();
                    builder.setServiceUuid(new ParcelUuid(uuid));
                    ScanFilter build = builder.build();
                    scanFilters.add(build);
                }
            }
            mBluetoothLeScanner.startScan(scanFilters, scanSettings, mScanCallback);
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

        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mScanCallback);
        }

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

        if (mScanCallback == null) {
            mScanCallback = new ScanCallback() {

                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    if (!isScanning) {
                        return;
                    }

                    if (!TextUtils.isEmpty(mFilterName) && TextUtils.isEmpty(result.getDevice().getName())) {
                        return;
                    }

                    if (!TextUtils.isEmpty(mFilterName) && !TextUtils.isEmpty(result.getDevice().getName())
                            && !mFilterName.equals(result.getDevice().getName())) {
                        return;
                    }

                    if (isIgnoreSame) {
                        if (mIgnoreSameMaps != null) {
                            if (mIgnoreSameMaps.containsKey(result.getDevice().getAddress())) {
                                return;
                            }
                        }
                    }
                    ScanDevice scanDevice = new ScanDevice();
                    scanDevice.device = result.getDevice();
                    scanDevice.deviceName = result.getDevice().getName();
                    scanDevice.rssi = result.getRssi();
                    if (result.getScanRecord() != null && result.getScanRecord().getBytes() != null) {
                        scanDevice.scanRecord = result.getScanRecord().getBytes();
                    }
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

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    if (mCallback != null) {
                        mCallback.onScanFailure(errorCode);
                    }
                }
            };
        }
        return this;
    }
}
