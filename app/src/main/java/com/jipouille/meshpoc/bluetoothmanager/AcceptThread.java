package com.jipouille.meshpoc.bluetoothmanager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
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


public class AcceptThread extends Thread {
    private final BluetoothServerSocket mmServerSocket;
    private final BluetoothAdapter mBluetoothAdapter;
    private final Handler mHandler;

    public AcceptThread(BluetoothAdapter btAdapter, UUID uuid, Handler handler) {
        // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        BluetoothServerSocket tmp = null;
        mHandler = handler;
        mBluetoothAdapter = btAdapter;
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("MeshPoc", uuid);
        } catch (IOException e) {
        }
        mmServerSocket = tmp;
    }

    public void run() {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned
        while (true) {
            try {
                Log.d("bluetooth","Server waiting for connection");
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                break;
            }
            // If a connection was accepted
            if (socket != null) {
                // Do work to manage the connection (in a separate thread)
                Log.d("bluetooth", "Server connected!");

                Pair<BluetoothDevice,BluetoothSocket> tmpPair = new Pair<>(socket.getRemoteDevice(), socket);
                Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_CLIENT_CONNECTED, tmpPair);
                msg.sendToTarget();
                try {
                    mmServerSocket.close();
                    ConnectedThread ct = new ConnectedThread(socket, mHandler);
                    ct.run();
                } catch (IOException e) {
                    break;
                }
                break;
            }
        }
    }

    /**
     * Will cancel the listening socket, and cause the thread to finish
     */
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) {
        }
    }
}
