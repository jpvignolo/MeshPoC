package com.jipouille.meshpoc;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.jipouille.meshpoc.bluetoothmanager.AcceptThread;
import com.jipouille.meshpoc.bluetoothmanager.ConnectThread;
import com.jipouille.meshpoc.bluetoothmanager.ConnectedThread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_CLIENT_CONNECTED = 3;
    public static final int MESSAGE_CONNECTED_TO_SERVER = 4;
    public static final int MESSAGE_CONNECTION_TO_SERVER_FAIL = 5;

    private BluetoothAdapter mBluetoothAdapter;
    private BroadcastReceiver mReceiver;
    private ArrayList<BluetoothDevice> mServerDeviceList;
    private HashMap<BluetoothDevice, BluetoothSocket> mClientSocketHash;
    private HashMap<BluetoothDevice, BluetoothSocket> mServerSocketHash;
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

        mServerDeviceList = new ArrayList<>();
        mClientSocketHash = new HashMap<>();
        mServerSocketHash = new HashMap<>();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.startDiscovery();
        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                //Finding devices                 
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice tmpDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mServerDeviceList.add(tmpDevice);
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
                    Log.d("bluetooth","End Scan");
                    if (mServerDeviceList.size() >0 )
                        for (BluetoothDevice device : mServerDeviceList) {
                            ConnectThread ct = new ConnectThread(device, mHandler, uuid);
                            ct.run();
                        }
                    else {
                        switchToServerMode();
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
        Log.d("bluetooth","Switching to server mode");
        clientMode = false;
        Thread listen = new AcceptThread(mBluetoothAdapter, uuid, mHandler);
        listen.start();
    }

    @Override
    public void onDestroy() {
            unregisterReceiver(mReceiver);
            super.onDestroy();
    }

    public void connectionSucceed(BluetoothDevice device, BluetoothSocket socket) {
    }

    private void sendDataToServer(String s) {
        Log.d("bluetooth","sendDataToServer "+s);
        Iterator it = mServerSocketHash.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            it.remove(); // avoids a ConcurrentModificationException

            Log.d("bluetooth","sending to "+((BluetoothDevice)pair.getKey()).getName());
            BluetoothSocket socket = (BluetoothSocket)pair.getValue();
            ConnectedThread ct = new ConnectedThread(socket, mHandler);
            ct.start();
            ct.write(s.getBytes());
        }
    }

    public void connectionFailed() {
    }

    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            byte[] buf;
            String message;
            Pair<BluetoothDevice,BluetoothSocket> tmpPair;
            switch (msg.what) {
                case MESSAGE_READ:
                    // construct a string from the valid bytes in the buffer
                    buf = (byte[]) msg.obj;
                     message = new String(buf, 0, msg.arg1);
                    Log.d("bluetooth ","Message Received : " + message);
                    break;
                case MESSAGE_WRITE :
                    buf = (byte[]) msg.obj;
                    message = new String(buf, 0, msg.arg1);
                    Log.d("bluetooth ","Message Sended : " + message);
                    break;
                case MESSAGE_CLIENT_CONNECTED :
                    tmpPair = (Pair<BluetoothDevice, BluetoothSocket>) msg.obj;
                    mClientSocketHash.put(tmpPair.first,tmpPair.second);
                    Log.d("bleutooth","New client connected "+tmpPair.first.getName());
                    break;
                case MESSAGE_CONNECTED_TO_SERVER :
                    connectedDevices++;
                    tmpPair = (Pair<BluetoothDevice, BluetoothSocket>) msg.obj;
                    mServerSocketHash.put(tmpPair.first,tmpPair.second);
                    Log.d("bluetooth","Connection succeed /failed => "+connectedDevices+" "+connectedDevicesFailed+", deviceList size => "+ mServerDeviceList.size());
                    if ((connectedDevicesFailed + connectedDevices) == mServerDeviceList.size()) {
                        sendDataToServer("Hello Server");
                    }
                    break;
                case MESSAGE_CONNECTION_TO_SERVER_FAIL :
                    connectedDevicesFailed++;
                    Log.d("bluetooth","Connection failed "+connectedDevicesFailed);
                    if (connectedDevicesFailed == mServerDeviceList.size()) {
                        switchToServerMode();
                    } else if ((connectedDevicesFailed + connectedDevices) == mServerDeviceList.size()) {
                        sendDataToServer("Hello Server");
                    }
                    break;
            }
        }
    };

}
