package com.cys.fastblescan.bean;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * 扫描结果类
 */
public class ScanDevice implements Parcelable {
    public BluetoothDevice device;
    public String deviceName;
    public int rssi;
    public byte[] scanRecord;

    public ScanDevice() {

    }

    public ScanDevice(Parcel in) {
        device = in.readParcelable(BluetoothDevice.class.getClassLoader());
        deviceName = in.readString();
        rssi = in.readInt();
        scanRecord = in.createByteArray();
    }

    public static final Creator<ScanDevice> CREATOR = new Creator<ScanDevice>() {
        @Override
        public ScanDevice createFromParcel(Parcel in) {
            return new ScanDevice(in);
        }

        @Override
        public ScanDevice[] newArray(int size) {
            return new ScanDevice[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(device, flags);
        dest.writeString(deviceName);
        dest.writeInt(rssi);
        dest.writeByteArray(scanRecord);
    }

    @Override
    public String toString() {
        return "ScanDevice{" +
                "name=" + deviceName +
                ", mac='" + device.getAddress() +
                '}';
    }
}
