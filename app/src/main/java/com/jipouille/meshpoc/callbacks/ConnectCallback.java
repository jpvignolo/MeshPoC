package com.jipouille.meshpoc.callbacks;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

/**
 * Created by Jean-Philippe on 22/05/2016.
 */
public interface ConnectCallback {

    public void connectionSucceed(BluetoothDevice device, BluetoothSocket socket);
    public void connectionFailed();
}
