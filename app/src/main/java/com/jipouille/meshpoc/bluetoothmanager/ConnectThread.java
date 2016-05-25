package com.jipouille.meshpoc.bluetoothmanager;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.jipouille.meshpoc.callbacks.ConnectCallback;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Jean-Philippe on 25/05/2016.
 */

public class ConnectThread extends  Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private ConnectCallback callback;

    public ConnectThread(BluetoothDevice device, ConnectCallback callback, UUID uuid) {
        this.callback = callback;
        BluetoothSocket tmp = null;
        mmDevice = device;
        try {
            tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mmSocket = tmp;
    }

    public void run() {
        try {
            mmSocket.connect();
            callback.connectionSucceed(mmDevice, mmSocket);
            Log.d("bluetooth", "Client connecté");
        } catch (IOException e) {
            e.printStackTrace();
            try {
                mmSocket.close();
            } catch (IOException closeException) {
            }
            callback.connectionFailed();
        }
    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
        }
    }
}

