package com.cys.fastblescan.callback;

import com.cys.fastblescan.bean.ScanDevice;

public interface FastBleScanCallback {

    void onStartScan();

    void onLeScan(ScanDevice scanDevice);

    void onStopScan();

    void onScanFailure(int errorCode);

}
