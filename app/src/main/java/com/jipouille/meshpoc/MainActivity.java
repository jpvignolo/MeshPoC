package com.jipouille.meshpoc;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.jipouille.meshpoc.callbacks.ConnectCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements ConnectCallback {

    private static final int MESSAGE_READ = 1;
    private static final int MESSAGE_WRITE = 2;
    private BluetoothAdapter mBluetoothAdapter;
    private BroadcastReceiver mReceiver;
    private ArrayList<BluetoothDevice> mDeviceList;
    private HashMap<BluetoothDevice, BluetoothSocket> mSocketHash;
    private ArrayAdapter<String> mArrayAdapter;
    private boolean clientMode = true;
    private int connectedDevices = 0;
    private int connectedDevicesFailed = 0;

    final String sUuid = "MeshPocUuid";
    final UUID uuid = UUID.nameUUIDFromBytes(sUuid.getBytes());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        mDeviceList = new ArrayList<>();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.startDiscovery();
        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                //Finding devices                 
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice tmpDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDeviceList.add(tmpDevice);
                    // Add the name and address to an array adapter to show in a ListView
                    Log.d("bluetooth","Trouvé "+tmpDevice.getName() + "\n" + tmpDevice.getAddress());

                }
                if (BluetoothDevice.ACTION_UUID.equals(action)) {
                    Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                    //device.createInsecureRfcommSocketToServiceRecord(uuidExtra);
                    // Add the name and address to an array adapter to show in a ListView
                    if (uuidExtra != null) {
                        for (int i = 0; i< uuidExtra.length; i++) {
                            Log.d("bluetooth", "Trouvé uuid " + uuidExtra[i].toString());
                        }
                    }
                    else
                        Log.d("bluetooth","Trouvé uuid null");
                }
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
                {
                    Log.d("bluetooth","Begin Scan");
                }
                if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
                {
                    for (BluetoothDevice device : mDeviceList) {
                        ConnectThread ct = new ConnectThread(device);
                        ct.run();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        //filter.addAction(BluetoothDevice.ACTION_UUID);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
    }

    private void switchToServerMode() {
        clientMode = false;
        Thread listen = new AcceptThread();
        listen.start();
    }

    @Override
    public void onDestroy() {
            unregisterReceiver(mReceiver);
            super.onDestroy();
    }

    @Override
    public void connectionSucceed(BluetoothDevice device, BluetoothSocket socket) {
        connectedDevices++;
        mSocketHash.put(device,socket);
        Log.d("bluetooth","Connection succeed "+connectedDevices);
        if ((connectedDevicesFailed + connectedDevices) == mDeviceList.size()) {
            sendDataToServer("Hello Server");
        }
    }

    private void sendDataToServer(String s) {
        Iterator it = mSocketHash.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            it.remove(); // avoids a ConcurrentModificationException
            BluetoothSocket socket = (BluetoothSocket)pair.getValue();
        }
    }

    @Override
    public void connectionFailed() {
        connectedDevicesFailed++;
        Log.d("bluetooth","Connection failed "+connectedDevicesFailed);
        if (connectedDevicesFailed == mDeviceList.size()) {
            switchToServerMode();
        } else if ((connectedDevicesFailed + connectedDevices) == mDeviceList.size()) {
            sendDataToServer("Hello Server");
        }
    }

    private class ConnectThread extends  Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
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
                connectionSucceed(mmDevice, mmSocket);
                Log.d("bluetooth", "Client connecté");
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    mmSocket.close();
                } catch (IOException closeException) {}
                connectionFailed();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
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
                    try {
                        mmServerSocket.close();
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

    private class ConnectedThread extends  Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        BluetoothSocket mmSocket;
        public ConnectedThread(BluetoothSocket socket, String msg) {

            mmSocket = socket;
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
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
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
                mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {

            }
        }
    }

    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.d("bluetooth ","Message : " + readMessage);
                    break;
                case MESSAGE_WRITE :
                    break;
            }
        }
    };

}
