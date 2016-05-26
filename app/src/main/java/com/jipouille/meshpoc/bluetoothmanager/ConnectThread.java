package com.jipouille.meshpoc.bluetoothmanager;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import com.jipouille.meshpoc.MainActivity;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Jean-Philippe on 25/05/2016.
 */

public class ConnectThread extends  Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final Handler mHandler;

    public ConnectThread(BluetoothDevice device, Handler handler, UUID uuid) {
        this.mHandler = handler;
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
            Pair <BluetoothDevice,BluetoothSocket> tmpPair = new Pair<>(mmDevice, mmSocket);
            Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_CONNECTED_TO_SERVER, tmpPair);
            msg.sendToTarget();
            Log.d("bluetooth", "Client connecté");
        } catch (IOException e) {
            e.printStackTrace();
            try {
                mmSocket.close();
            } catch (IOException closeException) {
            }
            Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_CONNECTION_TO_SERVER_FAIL);
            msg.sendToTarget();
        }
    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
        }
    }
}

