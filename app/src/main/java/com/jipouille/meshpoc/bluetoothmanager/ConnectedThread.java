package com.jipouille.meshpoc.bluetoothmanager;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.jipouille.meshpoc.MainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Jean-Philippe on 25/05/2016.
 */

public class ConnectedThread extends  Thread {
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    BluetoothSocket mmSocket;
    Handler mHandler;
    public ConnectedThread(BluetoothSocket socket, Handler handler) {

        mmSocket = socket;
        mHandler = handler;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the BluetoothSocket input and output streams
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {

        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {

        byte[] buffer = new byte[1024];
        int bytes;

        // Keep listening to the InputStream while connected
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);

                // Send the obtained bytes to the UI Activity
                mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
            } catch (IOException e) {

                //connectionLost();
                // Start the service over to restart listening mode
                //BluetoothConnection.this.start();
                break;
            }
        }
    }

    public void write(byte[] buffer) {

        try {
            mmOutStream.write(buffer);

            // Share the sent message back to the UI Activity
            mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, buffer.length, -1, buffer).sendToTarget();
        } catch (IOException e) {
            Log.d("bluetooth","Writing failed "+e.getMessage());
        }
    }
}
