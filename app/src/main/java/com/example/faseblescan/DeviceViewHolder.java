package com.example.faseblescan;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class DeviceViewHolder extends RecyclerView.ViewHolder {

    public TextView textViewByName;
    public TextView textViewByMac;
    public TextView textViewByScanRecordInt;
    public TextView textViewByScanRecordHex;


    public DeviceViewHolder(View itemView) {
        super(itemView);
        textViewByName = itemView.findViewById(R.id.id_tv_name);
        textViewByMac = itemView.findViewById(R.id.id_tv_mac);
        textViewByScanRecordInt = itemView.findViewById(R.id.id_tv_scanrecord_int);
        textViewByScanRecordHex = itemView.findViewById(R.id.id_tv_scanrecord_hexint);
    }
}
