package com.example.mdp_andriod;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

// NOT using
public class BluetoothConnection {

    private String MY_TAG = "BluetoothConnection: ";

    private static volatile BluetoothConnection INSTANCE = null;

    //UUID (Standard ID)
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private String APP_NAME = MY_UUID.toString();

    static final int STATE_MESSAGE_RECEIVED = 0;
    static final int STATE_WRITE = 1;
    static final int STATE_CONNECTED=100;
    static final int STATE_DISCONNECTING=101;
    static final int STATE_CONNECTION_LOST=102;
    static final int STATE_CONNECTION_ERROR=404;

    // UI Thread
    private Handler handler;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice mDevice;


    // Thread class
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private Connected_SendReceive_Thread connected_sendReceive_thread;

    //  State of BluetoothService
    private State mState;

    ProgressDialog mProgressDialog;

    Context mContext;

    Intent connectionStatus;

    //  Available states of BluetoothService
    public enum State {
        NONE, LISTEN, CONNECTING, CONNECTED
    }


    public BluetoothConnection(Context context)
    {
        // Initializing handler
        this.handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                return false;
            }
        });
        //this.bluetoothAdapter = bluetoothAdapter;

        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mContext = context;

        mState = State.NONE;
    }


    /**
     * Get instance of BluetoothService to be used
     * @return An instance of BluetoothService
     */
    public static BluetoothConnection getInstance(Context context)  {
        if (INSTANCE == null) {
            synchronized (BluetoothConnection.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BluetoothConnection(context);
                }
            }
        }
        return INSTANCE;
    }


    public void connectedSocket(BluetoothSocket socket)
    {
        if (socket.isConnected()) {
            connected_sendReceive_thread = new Connected_SendReceive_Thread(socket);
            connected_sendReceive_thread.start();
        }
    }

    // sending message across bluetooth
    synchronized void sendServiceMessage(int status_message, Object object)
    {
        handler.obtainMessage(status_message, -1, -1,object).sendToTarget();
    }
    synchronized void sendServiceMessage(int status_message) {
        handler.obtainMessage(status_message, -1, -1).sendToTarget();
    }

    public void registerNewHandlerCallback(Handler.Callback callback) {
        handler = new Handler(callback);
    }

    //listen for incoming bluetooth connections
    synchronized void listen() {
        //  Start listening on socket
        if (acceptThread == null && mState != State.LISTEN) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
    }

    // connect to a chosen bluetooth device
    synchronized void connect(BluetoothDevice device) {
        disconnect();
        if (connectThread == null) {
            connectThread = new ConnectThread(device);
            connectThread.start();
        }
    }

    // disconnect all devices and reset all thread
    public synchronized void disconnect() {
        if (connected_sendReceive_thread != null) {
            connected_sendReceive_thread.cancel();
            connected_sendReceive_thread = null;
        }

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        mState = State.NONE;

        //  Listen to incoming Bluetooth connections, if not already doing so
        listen();
    }

    /**
     * Perform relevant actions on connected BluetoothSocket
     * @param mmSocket Connected socket
     */
    private synchronized void manageConnectedSocket(BluetoothSocket mmSocket) {
        if (mmSocket.isConnected()) {
            connected_sendReceive_thread = new Connected_SendReceive_Thread(mmSocket);
            connected_sendReceive_thread.start();
        }
    }

    public State getState() {
        return mState;
    }


    // get the devices name from the connected thread
    String getConnectedDeviceName() {
        if (mState == State.CONNECTED) {
            return connected_sendReceive_thread.bSocket.getRemoteDevice().getName();
        } else {
            return "No Device";
        }
    }


    //Send message from local device to remote device
    public synchronized void sendMessageToRemoteDevice(String message) {
        //  Create temporary object
        synchronized (this) {
            if (mState != State.CONNECTED) return;
        }
        connected_sendReceive_thread.write(message);
    }



    // This device provide connection to other devices
    private class AcceptThread extends Thread{

        private BluetoothServerSocket serverSocket;

        public AcceptThread()
        {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mState = BluetoothConnection.State.LISTEN;
        }

        @Override
        public void run() {

            BluetoothSocket bSocket = null;

            // always liston for incoming bluetooth
            while (mState != BluetoothConnection.State.CONNECTED)
            {
                try {
                    bSocket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                // there is a connection
                if(bSocket != null)
                {

                    Log.d(MY_TAG, "Accept Thread connected");

                    sendServiceMessage(STATE_CONNECTED, bSocket.getRemoteDevice().getName());

                    //start the send and receive thread
                    connectedSocket(bSocket);

                    // close
                    //cancel();

                    break;
                }
            }

        }

        // Closes the connect socket and causes the thread to finish.
        void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(MY_TAG, "Could not close the connect socket", e);
            }
        }
    }

    // Connecting to targeted device
    private class ConnectThread extends Thread
    {
        //private BluetoothDevice device;
        private BluetoothSocket bSocket;

        public ConnectThread(BluetoothDevice device)
        {

            mDevice = device;


        }

        @Override
        public void run() {

            try {
                bSocket = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
                sendServiceMessage(STATE_CONNECTION_ERROR);
            }

            mState = BluetoothConnection.State.CONNECTING;

            // stop discovering devices to save resources
            bluetoothAdapter.cancelDiscovery();

            try {
                bSocket.connect();

                sendServiceMessage(STATE_CONNECTED, mDevice.getName());

                connectedSocket(bSocket);

                Log.d(MY_TAG, "ConnectThread connected");

            } catch (IOException e) {
//                e.printStackTrace();
//                sendServiceMessage(STATE_CONNECTION_ERROR);

                try {
                    bSocket.close();
                    Log.d(MY_TAG, "RUN: ConnectThread socket closed.");
                } catch (IOException e1) {
                    Log.e(MY_TAG, "RUN: ConnectThread: Unable to close connection in socket."+ e1.getMessage());
                }
                Log.d(MY_TAG, "RUN: ConnectThread: could not connect to UUID."+ MY_UUID);
                try {
                    MainActivity mBluetoothActivityActivity = (MainActivity) mContext;
                    mBluetoothActivityActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "Failed to connect to the Device.", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception z) {
                    z.printStackTrace();
                }
            }

            try {
                mProgressDialog.dismiss();
            } catch(NullPointerException e){
                e.printStackTrace();
            }

            synchronized (BluetoothConnection.this) {
                connectThread = null;
            }

        }

        // Closes the client socket and causes the thread to finish.
        void cancel() {
            try {
                sendServiceMessage(STATE_DISCONNECTING, mDevice.getName());
                bSocket.close();
            } catch (IOException e) {
                Log.e(MY_TAG, "Could not close the client socket", e);
                sendServiceMessage(STATE_CONNECTION_ERROR);
            }
        }
    }


    // connected device can send and receive string values
    private class Connected_SendReceive_Thread extends Thread
    {
        private final BluetoothSocket bSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private byte[] buffer;

        Connected_SendReceive_Thread(BluetoothSocket socket)
        {
            bSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            connectionStatus = new Intent("ConnectionStatus");
            connectionStatus.putExtra("Status", "connected");
            connectionStatus.putExtra("Device",mDevice );

            try {
                tempIn = bSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                tempOut = bSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tempIn;
            outputStream = tempOut;
            mState = BluetoothConnection.State.CONNECTED;
        }

        @Override
        public void run() {

            buffer = new byte[256];

            int numOfBytes;
            String message;

            while (mState == BluetoothConnection.State.CONNECTED)
            {
                try { // read input string
                    numOfBytes = inputStream.read(buffer);
                    message = new String(buffer).substring(0, numOfBytes);

                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, numOfBytes, -1, message).sendToTarget();

                } catch (IOException e) {
                    e.printStackTrace();
                    sendServiceMessage(STATE_CONNECTION_LOST);
                    break;
                }
            }


        }

        // message that is send to the trageted device
        public void write(String inMessage)
        {
            try {
                outputStream.write(inMessage.getBytes());
                outputStream.flush();

                handler.obtainMessage(STATE_WRITE, inMessage.getBytes().length, -1, inMessage).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(MY_TAG, "Error occurred when sending data", e);
                sendServiceMessage(STATE_CONNECTION_ERROR);
            }
        }

        // Call this method from the main activity to shut down the connection.
        void cancel() {
            try {
                sendServiceMessage(STATE_DISCONNECTING);
                bSocket.close();
            } catch (IOException e) {
                Log.e(MY_TAG, "Could not close the connect socket", e);
                sendServiceMessage(STATE_CONNECTION_ERROR);
            }
        }
    }




}
