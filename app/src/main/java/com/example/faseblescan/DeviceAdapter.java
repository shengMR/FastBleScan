package com.example.faseblescan;

import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cys.fastblescan.bean.ScanDevice;
import com.cys.fastblescan.util.ArraysUtils;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceViewHolder> {

    public List<ScanDevice> mDatas = new ArrayList<>();

    public void replaceData(List<ScanDevice> datas) {
        this.mDatas.clear();
        this.mDatas.addAll(datas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_item_device, parent, false);
        return new DeviceViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        ScanDevice scanDevice = mDatas.get(position);
        BluetoothDevice device = scanDevice.device;
        StringBuffer sb = new StringBuffer();
        sb.append(TextUtils.isEmpty(scanDevice.deviceName) ? device.getAddress() : scanDevice.deviceName);
        sb.append("( rssi ");
        sb.append(scanDevice.rssi);
        sb.append(" ) ");
        holder.textViewByName.setText(sb.toString());
        holder.textViewByMac.setText("Mac : "  + device.getAddress());
        holder.textViewByScanRecordInt.setText("ScanRecord : " + ArraysUtils.bytesToString(scanDevice.scanRecord));
        holder.textViewByScanRecordHex.setText("ScanRecord : " + ArraysUtils.bytesToHexString(scanDevice.scanRecord, " "));
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }
}
