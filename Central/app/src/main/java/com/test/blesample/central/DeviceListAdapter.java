package com.test.blesample.central;

import android.bluetooth.le.ScanResult;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by itanbarpeled on 28/01/2018.
 */

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder>  {


    public interface DevicesAdapterListener {
        void onDeviceItemClick(String deviceName, String deviceAddress);
    }

    /*
    class Temp {
        String name;
        String address;

        public Temp(String name, String address) {
            this.name = name;
            this.address = address;
        }
    }
    */

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView mDeviceNameView;
        TextView mDeviceNameAddressView;

        ViewHolder(View view) {

            super(view);
            mDeviceNameView = (TextView) view.findViewById(R.id.device_name);
            mDeviceNameAddressView = (TextView) view.findViewById(R.id.device_address);
        }

    }

    /* メンバ変数 */
    private ArrayList<ScanResult>		mDeviceList = new ArrayList<ScanResult>();
    private DevicesAdapterListener mListener;


    public DeviceListAdapter(DevicesAdapterListener listener) {
        mListener = listener;

        /*
        Temp device = new Temp("Pixel", "AA:12:AA:12:AA:12");
        mArrayList.add(device);
        device = new Temp("Galaxy S8", "BB:23:BB:23:BB:23");
        mArrayList.add(device);
        notifyDataSetChanged();
        */
    }

    public DeviceListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_device_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        ScanResult scanResult = mDeviceList.get(position);
        final String deviceName = scanResult.getDevice().getName();
        final String deviceAddress = scanResult.getDevice().getAddress();

        /*
        Temp scanResult = mArrayList.get(position);
        final String deviceName = scanResult.name;
        final String deviceAddress = scanResult.address;
        */

        if (TextUtils.isEmpty(deviceName)) {
            holder.mDeviceNameView.setText("");
        } else {
            holder.mDeviceNameView.setText(deviceName);
        }

        if (TextUtils.isEmpty(deviceAddress)) {
            holder.mDeviceNameAddressView.setText("");
        } else {
            holder.mDeviceNameAddressView.setText(deviceAddress);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(deviceName) && !TextUtils.isEmpty(deviceAddress)) {
                    if (mListener != null) {
                        mListener.onDeviceItemClick(deviceName, deviceAddress);
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDeviceList.size();
    }

    public void addDevice(ScanResult scanResult) {
        addDevice(scanResult, true);
    }

    /**
     * Add a ScanResult item to the adapter if a result from that device isn't already present.
     * Otherwise updates the existing position with the new ScanResult.
     */
    public void addDevice(ScanResult scanResult, boolean notify) {

        if (scanResult == null) {
            return;
        }

        int existingPosition = getPosition(scanResult.getDevice().getAddress());

        if (existingPosition >= 0) {
            // Device is already in list, update its record.
            mDeviceList.set(existingPosition, scanResult);
        } else {
            // Add new Device's ScanResult to list.
            mDeviceList.add(scanResult);
        }

        if (notify) {
            notifyDataSetChanged();
        }

    }

    public void addDevice(List<ScanResult> scanResults) {
        if (scanResults != null) {
            for (ScanResult scanResult : scanResults) {
                addDevice(scanResult, false);
            }
            notifyDataSetChanged();
        }
    }


    /**
     * Search the adapter for an existing device address and return it, otherwise return -1.
     */
    private int getPosition(String address) {
        int position = -1;
        for (int i = 0; i < mDeviceList.size(); i++) {
            if (mDeviceList.get(i).getDevice().getAddress().equals(address)) {
                position = i;
                break;
            }
        }
        return position;
    }

	public void clearDevice() {
		mDeviceList.clear();
		notifyDataSetChanged();
	}
}
