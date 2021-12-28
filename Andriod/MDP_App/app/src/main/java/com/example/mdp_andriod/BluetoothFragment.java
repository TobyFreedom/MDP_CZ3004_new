package com.example.mdp_andriod;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BluetoothFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class BluetoothFragment extends Fragment {

    static final int STATE_MESSAGE_RECEIVED = 0;
    static final int STATE_WRITE = 1;
    static final int STATE_CONNECTED=100;
    static final int STATE_DISCONNECTED=101;
    static final int STATE_CONNECTION_LOST=102;
    static final int STATE_CONNECTION_ERROR=404;

    static final String CONNECTED = "Connected";
    static final String DISCONNECTED = "Disconnected";


    // for debugging
    private static String MY_TAG = " BluetoothFragment: ";

    // Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private Set<BluetoothDevice> discoveredDevices;

    //NEW
    private String connStatus;
    BluetoothServices mBluetoothConnection;
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //public static BluetoothDevice mBTDevice;

    private String connectedDeviceName;
    private Switch bluetooth_switch;
    private TextView tv_connectionStatus;
    private TextView tv_deviceName;
    private static BluetoothDevice mDevice;


    // Unconnected devices
    private LinearLayout mUnconnectedLayout;
    private Button btn_scan_bluetooth;
    private ArrayAdapter<String> pairedDevicesListAdapter;
    private ArrayAdapter<String> discoveredDevicesListAdapter;
    private ListView pairedDevicesListView;
    private ListView discoveredDevicesListView;

    // connected devices
    private LinearLayout mConnectedLayout;
    //private TextView mConnectedDeviceText;
    private Button mDisconnectButton;
    private EditText mSendBluetoothMessage;
    private ImageButton mSendBluetoothMessageButton;
    private ListView mBluetoothMessages;
    private static ArrayAdapter<String> mBluetoothMessagesListAdapter;

   // static BluetoothConnection bluetoothConnection;
//    boolean retryConnection = false;

    // Declaration Variables
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static Context context;

    ProgressDialog myDialog;

    boolean isDisconnectBtn = false;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BluetoothFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BluetoothFragment newInstance(String param1, String param2) {
        BluetoothFragment fragment = new BluetoothFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }


    public BluetoothFragment() {
        // Required empty public constructor
    }

    boolean retryConnection = false;
    Handler reconnectionHandler = new Handler();

    Runnable reconnectionRunnable = new Runnable() {
        @Override
        public void run() {
            // Magic here
            try {
                if (BluetoothServices.BluetoothConnectionStatus == false) {
                    startBTConnection(mDevice, myUUID);
                    Toast.makeText(getContext(), "Reconnection Success", Toast.LENGTH_SHORT).show();

                }
                reconnectionHandler.removeCallbacks(reconnectionRunnable);
                retryConnection = false;
            } catch (Exception e) {
                Toast.makeText(getContext(), "Failed to reconnect, trying in 5 second", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
        //OLD
        discoveredDevices = new HashSet<>();
//
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
//        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
//        getActivity().registerReceiver(broadcastReceiver, intentFilter);
//
//
////        bluetoothConnection = new BluetoothConnection();
//
//        // getinstance using synchronized
//        bluetoothConnection = BluetoothConnection.getInstance(getContext());
//
//        //  Register handler callback to handle BluetoothService messages
//        bluetoothConnection.registerNewHandlerCallback(bluetoothMessageHandler);

        mBluetoothConnection = new BluetoothServices(getContext());
        checkBluetoothPermission();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_bluetooth, container, false);

        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        //  Instantiate layouts
        mUnconnectedLayout = view.findViewById(R.id.bluetooth_unconnected_container);
        mConnectedLayout = view.findViewById(R.id.bluetooth_connected_container);


        bluetooth_switch = view.findViewById(R.id.switch_bluetooth);
        btn_scan_bluetooth = view.findViewById(R.id.btn_scan_bluetooth);
        tv_deviceName = view.findViewById(R.id.tv_deviceName);
        tv_connectionStatus = view.findViewById(R.id.tv_connectionStatus);
        pairedDevicesListView = view.findViewById(R.id.LV_bluetooth_paired_device);
        discoveredDevicesListView = view.findViewById(R.id.LV_bluetooth_discovered_device);

        //  Instantiate connected layout
        mDisconnectButton = view.findViewById(R.id.bluetooth_disconnect_button);
        //mConnectedDeviceText = view.findViewById(R.id.bluetooth_connected_device);
        mBluetoothMessages = view.findViewById(R.id.bluetooth_messages);
        mBluetoothMessagesListAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1);
        mBluetoothMessages.setAdapter(mBluetoothMessagesListAdapter);
        mSendBluetoothMessage = view.findViewById(R.id.bluetooth_message);
        mSendBluetoothMessageButton = view.findViewById(R.id.send_bluetooth_message_button);

        // get shared preferences
//        sharedPreferences = getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
//        mBluetoothMessagesListAdapter.add(sharedPreferences.getString("message", ""));


        // set the adapter for paired devices
        pairedDevicesListAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1);
        pairedDevicesListView.setAdapter(pairedDevicesListAdapter);

        // set the adapter for discovered devices
        discoveredDevicesListAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1);
        discoveredDevicesListView.setAdapter(discoveredDevicesListAdapter);

        getPairedDevices();

        //NEW
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getContext().registerReceiver(mBroadcastReceiver4, filter);

        IntentFilter filter2 = new IntentFilter("ConnectionStatus");
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mBroadcastReceiver5, filter2);

//        //  Get paired devices and display on list
//        pairedDevices = bluetoothAdapter.getBondedDevices();
//        if (pairedDevices.size() > 0) {
//            for (BluetoothDevice pairedDevice : pairedDevices) {
//                pairedDevicesListAdapter.add(pairedDevice.getName());
//            }
//        }

        Log.d("bluetoothAdapter", Boolean.toString(bluetoothAdapter.isEnabled()));
        if(bluetoothAdapter.isEnabled())
        {
            bluetooth_switch.setChecked(true);
            //getPairedDevices();
            pairedDevicesListView.setVisibility(View.VISIBLE);
        }
        else
        {
            updateDisconnectStatus();
            pairedDevicesListView.setVisibility(View.GONE);
        }
        bluetooth_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(bluetooth_switch.isChecked())
                {
                    pairedDevicesListView.setVisibility(View.VISIBLE);
                    checkBluetoothPermission();

                    //getPairedDevices();

                    if(bluetoothAdapter == null)
                    {
                        bluetooth_switch.setChecked(false);
                        Toast.makeText(getContext(), "Bluetooth is not supported in this device.", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        if(bluetoothAdapter.isEnabled() == false)
                        {
                            //getPairedDevices();
                            Intent enableBTintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivity(enableBTintent);
                            //startActivityForResult(enableBTintent,1);

                            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                            getContext().registerReceiver(mBroadcastReceiver1, BTIntent);

                            IntentFilter discoverIntent = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
                            getContext().registerReceiver(mBroadcastReceiver2, discoverIntent);

                        }
                        if (bluetoothAdapter.isEnabled()) {
                            Log.d(MY_TAG, "enableDisableBT: disabling Bluetooth");
                            bluetoothAdapter.disable();

                            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                            getContext().registerReceiver(mBroadcastReceiver1, BTIntent);
                        }
                        //getPairedDevices();

                    }
                }
                else
                {
                    bluetoothAdapter.disable();
                    discoveredDevicesListAdapter.clear();
                    discoveredDevicesListAdapter.notifyDataSetChanged();
//                    pairedDevicesListAdapter.clear();
//                    pairedDevicesListAdapter.notifyDataSetChanged();
                    pairedDevicesListView.setVisibility(View.GONE);
                    updateDisconnectStatus();
                }
            }
        });


        //  Scan button action
        btn_scan_bluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(MY_TAG,"btn_scan click");
                //sets the device to be discoverable for five minutes
                Intent discoverableIntent =
                        new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoverableIntent);

                // clear list adapter and hashset
                discoveredDevicesListAdapter.clear();
                discoveredDevices.clear();
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                checkBluetoothPermission();
                bluetoothAdapter.startDiscovery();
//                Log.d(MY_TAG, "Discovery started.");
//                btn_scan_bluetooth.setText("Scanning");
//                btn_scan_bluetooth.setEnabled(false);

                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                getContext().registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);

//                Log.d(MY_TAG, "Discovery finished.");
//                btn_scan_bluetooth.setText("Scan For Devices");
//                btn_scan_bluetooth.setEnabled(true);

            }

        });

        //  onClick for paired devices in list view
//        pairedDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                String chosenDeviceName = ((TextView) view).getText().toString();
//                for (BluetoothDevice pairedDevice : pairedDevices) {
//                    if (pairedDevice.getName().equalsIgnoreCase(chosenDeviceName)) {
//                        connect(pairedDevice);
//                        break;
//                    }
//                }
//            }
//        });

        // paired devices on click
        pairedDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                String chosenDeviceName = ((TextView) view).getText().toString();
                for (BluetoothDevice pairedDevice : pairedDevices) {
                    if (pairedDevice.getName().equalsIgnoreCase(chosenDeviceName)) {
                        //bluetoothConnection.connect(pairedDevice);
                        mDevice = pairedDevice;
                        showLog("pairedDevicesListView on click "+mDevice.getName());
                        isDisconnectBtn = false;
                        startConnection();
                        break;
                    }
                }
            }
        });

        // discovered devices on clicked
        discoveredDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String chosenDeviceName = ((TextView) view).getText().toString();
                for (BluetoothDevice device : discoveredDevices) {
                    if (device.getName().equalsIgnoreCase(chosenDeviceName)) {
                        discoveredDevicesListAdapter.clear();
                        discoveredDevices.clear();
                        //bluetoothConnection.connect(device);
                        mDevice = device;
                        isDisconnectBtn = false;
                        startConnection();
                        break;
                    }
                }
            }
        });

        //  Disconnect button action
        mDisconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //bluetoothConnection.disconnect();
                BluetoothFragment.printMessage("BT_DC");
                //TODO: Disconnect function
                mBluetoothConnection.disconnect();


                isDisconnectBtn = true;

            }
        });

        //  Send Message button action
        mSendBluetoothMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                String message = mSendBluetoothMessage.getText().toString().trim();
//                if (message.length() != 0) {
//                    mSendBluetoothMessage.setText("");
//                    bluetoothConnection.sendMessageToRemoteDevice(message);
//                }

                showLog("Clicked sendTextBtn");
                String sentText = "" + mSendBluetoothMessage.getText().toString();

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("message", sharedPreferences.getString("message", "") + '\n' + sentText);
                editor.commit();
                //MessageReceivedTV.setText(sharedPreferences.getString("message", ""));
                //mBluetoothMessagesListAdapter.add("Team 27: " + sendingMessage);
                mBluetoothMessagesListAdapter.add("Team 27: " + sentText);
                mSendBluetoothMessage.setText("");

                if (BluetoothServices.BluetoothConnectionStatus == true) {
                    byte[] bytes = sentText.getBytes(Charset.defaultCharset());
                    BluetoothServices.write(bytes);
                }
                showLog("Exiting sendTextBtn");

            }
        });

        myDialog = new ProgressDialog(getContext());
        myDialog.setMessage("Waiting for other device to reconnect...");
        myDialog.setCancelable(false);
        myDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });


//        if (bluetoothConnection.getState() == BluetoothConnection.State.CONNECTED) {
//            updateConnectedStatus(bluetoothConnection.getConnectedDeviceName());
//        }

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //  Stop Bluetooth discovery
        bluetoothAdapter.cancelDiscovery();

        //  Unregister receivers
        //getActivity().unregisterReceiver(broadcastReceiver);
    }

    private void getPairedDevices()
    {
        // set bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //  Get paired devices and display on list
        pairedDevices = bluetoothAdapter.getBondedDevices();
        Log.d("pairedDevices", Integer.toString(pairedDevices.size()));
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice pairedDevice : pairedDevices) {
                pairedDevicesListAdapter.add(pairedDevice.getName());
            }
            pairedDevicesListAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Handle Bluetooth broadcast events
     */
//    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            final String action = intent.getAction();
//            if (action != null) {
//                Log.d(MY_TAG, "BroadcastReceiver: action: " + action);
//                switch (action) {
//                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
//                        //  Bluetooth device discovery started
//                        Log.d(MY_TAG, "Discovery started.");
//                        btn_scan_bluetooth.setText("Scanning");
//                        btn_scan_bluetooth.setEnabled(false);
//                        break;
//                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
//                        //  Bluetooth device discovery completed
//                        Log.d(MY_TAG, "Discovery finished.");
//                        btn_scan_bluetooth.setText("Scan For Devices");
//                        btn_scan_bluetooth.setEnabled(true);
//                        break;
//                    case BluetoothDevice.ACTION_FOUND:
//                        //  Bluetooth device discovered, get information from Intent
//                        try {
//                            Log.d(MY_TAG, "Action found ");
//                            mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                            String deviceName = mDevice.getName();
//                            Log.d(MY_TAG, "the device name: " + mDevice.getName());
//                            //Log.d(MY_TAG, "the store device name: " + storeDeviceName);
//                            if(deviceName == null) {
//                                Log.d(MY_TAG, "the device address: " + mDevice.getAddress());
//
//                                break;
//                            }
//                            else if(discoveredDevices.contains(mDevice) == false && pairedDevices.contains(mDevice) == false)
//                            {// block repeated devices and paired devices from adding to the list
//                                // add the device name
//                                discoveredDevicesListAdapter.add(deviceName);
//                                // update the adapter
//                                discoveredDevicesListAdapter.notifyDataSetChanged();
//                                // update the hashset
//                                discoveredDevices.add(mDevice);
//                            }
////                            else if (!deviceName.equals(storeDeviceName)) { // to prevent duplicate list
////                                storeDeviceName = deviceName;
////                                // add the device name
////                                discoveredDevicesListAdapter.add(deviceName);
////                                // update the adapter
////                                discoveredDevicesListAdapter.notifyDataSetChanged();
////                                //pairedDevices.add(device);
////                            }
//                        }catch(Exception e){
//                            break;
//                        }
//                        break;
//                }
//            }
//        }
//    };
//
//    private final Handler.Callback bluetoothMessageHandler = new Handler.Callback() {
//        @Override
//        public boolean handleMessage(Message message) {
//            try {
//                Log.d(MY_TAG, "BluetoothFragment Handler");
//                Log.d(MY_TAG, "bluetoothMessageHandler: message.what: " + message.what);
//
//                switch (message.what) {
//                    case STATE_MESSAGE_RECEIVED:
//                        //  Reading message from remote device
//                        String receivedMessage = message.obj.toString();
//                        mBluetoothMessagesListAdapter.add(connectedDeviceName + ": " + receivedMessage);
//                        return false;
//                    case STATE_WRITE:
//                        //  Writing message to remote device
//                        String sendingMessage = message.obj.toString();
//                        mBluetoothMessagesListAdapter.add("Team 27: " + sendingMessage);
//                        return false;
//                    case STATE_CONNECTED:
//                        //  Successfully connected to remote device
//                        String deviceName = message.obj.toString();
//                        Toast.makeText(getContext(), "Connected to remote device: " + deviceName, Toast.LENGTH_SHORT).show();
//                        updateConnectedStatus(deviceName);
//                        //  Get paired devices
//                        //pairedDevices = bluetoothAdapter.getBondedDevices();
//                        //getPairedDevices();
//                        Log.d("device", mDevice.getName());
//                        Log.d("pairedDevices", pairedDevices.toString());
//                        if(pairedDevices.contains(mDevice) == false)
//                        {
//                            pairedDevicesListAdapter.add(deviceName);
//                            pairedDevicesListAdapter.notifyDataSetChanged();
//                            pairedDevices.add(mDevice);
//                        }
//                        //setConnectedState(deviceName);
//                        //  Switch to Arena
//                        //MainActivity.addFragment(MainActivity.ARENA_TAG);
//                        return false;
//                    case STATE_DISCONNECTED:
//                    case STATE_CONNECTION_LOST:
//                        //  Connection to remote device lost
//                        Toast.makeText(getContext(), "Connection to remote device lost", Toast.LENGTH_SHORT).show();
//                        updateDisconnectStatus();
//                        return false;
//                    case STATE_CONNECTION_ERROR:
//                        //  An error occured during connection
//                        Log.e(MY_TAG, "BT_ERROR_OCCURRED: A Bluetooth error occurred");
//                        Toast.makeText(getContext(), "A Bluetooth error occurred", Toast.LENGTH_SHORT).show();
//                        //setDisconnectedState();
//                        updateDisconnectStatus();
//                        return false;
//                    //default:
//                        //return false;
//                }
//            } catch (Throwable t) {
//                Log.e(MY_TAG, null, t);
//            }
//
//            return false;
//        }
//    };

    private void updateConnectedStatus(String deviceName)
    {
        tv_deviceName.setText(deviceName);
        tv_connectionStatus.setText("CONNECTED");

        connectedDeviceName = deviceName;

        btn_scan_bluetooth.setVisibility(View.GONE);
        mUnconnectedLayout.setVisibility(View.GONE);
        mConnectedLayout.setVisibility(View.VISIBLE);

        tv_deviceName.setTextColor(getResources().getColor(R.color.lime_green));
        tv_connectionStatus.setTextColor(getResources().getColor(R.color.lime_green));

        Intent intent = new Intent(getContext(), ArenaFragment.class);
        intent.putExtra("BT_Status", "CONNECTED");

        // update the textview from ArenaFragment
        TextView btStatus = ((Activity)this.getContext()).findViewById(R.id.BTstatus);
        btStatus.setText("CONNECTED");
        btStatus.setTextColor(getResources().getColor(R.color.lime_green));

    }

    private void updateDisconnectStatus()
    {
        tv_deviceName.setText("No Device");
        tv_connectionStatus.setText("DISCONNECTED");

        btn_scan_bluetooth.setVisibility(View.VISIBLE);
        mUnconnectedLayout.setVisibility(View.VISIBLE);
        mConnectedLayout.setVisibility(View.GONE);
        tv_deviceName.setTextColor(getResources().getColor(R.color.red));
        tv_connectionStatus.setTextColor(getResources().getColor(R.color.red));

        mBluetoothMessagesListAdapter.clear();

        // update the textview from ArenaFragment
        TextView btStatus = ((Activity)this.getContext()).findViewById(R.id.BTstatus);
        btStatus.setText("DISCONNECTED");
        btStatus.setTextColor(getResources().getColor(R.color.red));


    }

    // Check Bluetooth permission
    public void checkBluetoothPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            int permissionCheck = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) + ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) + ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION ) + ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION ) + ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.BLUETOOTH_ADMIN ) + ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.BLUETOOTH );

            if(permissionCheck != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(getActivity(), new String[]
                        {
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.BLUETOOTH

                        },1);
            }

        }
    }

    private static void showLog(String message) {
        Log.d(MY_TAG, message);
    }

    // Send message to bluetooth
    public static void printMessage(String message) {
        showLog("Entering printMessage");
        //editor = sharedPreferences.edit();

//        if (bluetoothConnection.getState() == BluetoothConnection.State.CONNECTED) {
//            //byte[] bytes = message.getBytes(Charset.defaultCharset());
//            bluetoothConnection.sendMessageToRemoteDevice(message);
//        }
        if (BluetoothServices.BluetoothConnectionStatus == true) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothServices.write(bytes);
        }
        showLog(message);
//        editor.putString("message", CommsFragment.getMessageReceivedTextView().getText() + "\n" + message);
//        editor.commit();
        //for the message chat List
        mBluetoothMessagesListAdapter.add("Team 27: " + message);
        //ArenaFragment.updateArenaBTMessage(message);
        //refreshMessageReceived();
        showLog("Exiting printMessage");
    }

    // for waypoint message
    public static void printMessage(String name, int x, int y) throws JSONException {
        showLog("Entering printMessage");
        //sharedPreferences();

        JSONObject jsonObject = new JSONObject();
        String message;

        switch(name) {
            //"starting" case:
            case "waypoint":
                jsonObject.put(name, name);
                jsonObject.put("x", Integer.toString(x));
                jsonObject.put("y", Integer.toString(y));
                message = name + " (" + Integer.toString(x) + "," + Integer.toString(y) + ")";
                break;
            case "Obstacle":
                jsonObject.put(name, name);
                jsonObject.put("x", Integer.toString(x));
                jsonObject.put("y", Integer.toString(y));
                message = name + " (" + Integer.toString(x) + "," + Integer.toString(y) + ")";
                break;
            default:
                message = "Unexpected default for printMessage: " + name;
                break;
        }
        //editor.putString("message", CommsFragment.getMessageReceivedTextView().getText() + "\n" + message);
        //editor.commit();
//        if (bluetoothConnection.getState() == BluetoothConnection.State.CONNECTED) {
//            //byte[] bytes = message.getBytes(Charset.defaultCharset());
//            //BluetoothConnectionService.write(bytes);
//            bluetoothConnection.sendMessageToRemoteDevice(message);
//        }
        if (BluetoothServices.BluetoothConnectionStatus == true) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothServices.write(bytes);
        }
        showLog("Exiting printMessage");
        mBluetoothMessagesListAdapter.add("Team 27: " + message);
    }

    // for obstacle message
    public static void printMessage(String name, int x, int y, String direction) throws JSONException {
        showLog("Entering printMessage");
        //sharedPreferences();

        JSONObject jsonObject = new JSONObject();
        String message;

        switch(name) {
            //"starting" case:
            case "waypoint":
                jsonObject.put(name, name);
                jsonObject.put("x", Integer.toString(x));
                jsonObject.put("y", Integer.toString(y));
                message = name + " (" + Integer.toString(x) + "," + Integer.toString(y) + ")";
                break;
            case "Obstacle":
                jsonObject.put(name, name);
                jsonObject.put("x", Integer.toString(x));
                jsonObject.put("y", Integer.toString(y));
                message = name + " (" + Integer.toString(x) + "," + Integer.toString(y) + ","+ direction + ")";
                break;
            case "Obstacle direction change":
                jsonObject.put(name, name);
                jsonObject.put("x", Integer.toString(x));
                jsonObject.put("y", Integer.toString(y));
                message = name + " (" + Integer.toString(x) + "," + Integer.toString(y) + ","+ direction + ")";
                break;
            default:
                message = "Unexpected default for printMessage: " + name;
                break;
        }
        //editor.putString("message", CommsFragment.getMessageReceivedTextView().getText() + "\n" + message);
        //editor.commit();
//        if (bluetoothConnection.getState() == BluetoothConnection.State.CONNECTED) {
//            //byte[] bytes = message.getBytes(Charset.defaultCharset());
//            //BluetoothConnectionService.write(bytes);
//            bluetoothConnection.sendMessageToRemoteDevice(message);
//        }

        if (BluetoothServices.BluetoothConnectionStatus == true) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothServices.write(bytes);
        }
        showLog("Exiting printMessage");
        mBluetoothMessagesListAdapter.add("Team 27: " + message);
    }

    /*
    public static void refreshMessageReceived() {
        String received= sharedPreferences.getString("message", "");
        CommsFragment.getMessageReceivedTextView().setText(sharedPreferences.getString("message", ""));
        //CommsFragment.getMessageReceivedTextView().setText(received);
        /*String[] separated = received.split("\\|");
        if(separated[counter].equals("\nstatus exploring")){
            robotStatusTextView.setText("exploring");
        }if(separated[counter].equals("\nstatus fastest path")){
            robotStatusTextView.setText("fastest path");
        }if(separated[counter].equals("\nstatus turning left")){
            robotStatusTextView.setText("turning left");
        }if(separated[counter].equals("\nstatus turning right")){
            robotStatusTextView.setText("turning right");
        }if(separated[counter].equals("\nstatus moving forward")){
            robotStatusTextView.setText("moving forward");
        }if(separated[counter].equals("\nstatus reversing")){
            robotStatusTextView.setText("reversing");
        }
        counter++;
    }
    */

//    public static void receiveMessage(String message) {
//        showLog("Entering receiveMessage");
//        //sharedPreferences();
//        editor.putString("message", sharedPreferences.getString("message", "") + "\n" + message);
//        editor.commit();
//        showLog("Exiting receiveMessage");
//    }

    public static void refreshMessageReceived() {
        //CommunicationFragment.getMessageReceivedTV().setText(sharedPreferences.getString("message", ""));
        mBluetoothMessagesListAdapter.add(sharedPreferences.getString("message", ""));
        mBluetoothMessagesListAdapter.notifyDataSetChanged();
    }




    public static void receiveMessage(String message) {
        showLog("Entering receiveMessage");
        //sharedPreferences();
        editor.putString("message", sharedPreferences.getString("message", "") + "\n" + message);
        editor.commit();
        showLog("Exiting receiveMessage");
    }


    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(MY_TAG, "mBroadcastReceiver1: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(MY_TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(MY_TAG, "mBroadcastReceiver1: STATE ON");

                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(MY_TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                final int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(MY_TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(MY_TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(MY_TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(MY_TAG, "mBroadcastReceiver2: Connecting...");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(MY_TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }
            }
        }
    };

    // To discover bluetooth devices
    private final BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(MY_TAG, "onReceive: ACTION FOUND.");

            if(action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                if(deviceName == null)
                {
                    Log.d(MY_TAG, "the device address: " + device.getAddress());
                    discoveredDevices.add(device);
                    discoveredDevicesListAdapter.add(device.getAddress());
                }
                else {
                    discoveredDevices.add(device);
                    discoveredDevicesListAdapter.add(device.getName());
                    Log.d(MY_TAG, "the device name: " + device.getName());
                }
                discoveredDevicesListAdapter.notifyDataSetChanged();
                Log.d(MY_TAG, "onReceive: "+ device.getName() +" : " + device.getAddress());
                //mNewDevlceListAdapter = new DeviceAdapter(context, R.layout.activity_device_view, mNewBTDevices);
                //otherDevicesListView.setAdapter(mNewDevlceListAdapter);

            }
        }
    };

    // for bluetooth pairing
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(MY_TAG, "BOND_BONDED.");
                    Toast.makeText(getContext(), "Successfully paired with " + mDevice.getName(), Toast.LENGTH_SHORT).show();
                    BluetoothFragment.this.mDevice = mDevice;
                }
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(MY_TAG, "BOND_BONDING.");
                }
                if(mDevice.getBondState() == BluetoothDevice.BOND_NONE){
                    Log.d(MY_TAG, "BOND_NONE.");
                }
            }
        }
    };

    // for auto reconnecting bluetooth
    private final BroadcastReceiver mBroadcastReceiver5 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            sharedPreferences = getActivity().getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();

            if(status.equals("connected")){
                try {
                    myDialog.dismiss();
                } catch(NullPointerException e){
                    e.printStackTrace();
                }

                Log.d(MY_TAG, "mBroadcastReceiver5: Device now connected to "+mDevice.getName());
                Toast.makeText(getContext(), "Device now connected to "+mDevice.getName(), Toast.LENGTH_LONG).show();
                editor.putString("connStatus", "Connected to " + mDevice.getName());
                //connStatusTextView.setText("Connected to " + mDevice.getName());
                updateConnectedStatus(mDevice.getName());
            }
            else if(status.equals("disconnected") && retryConnection == false){
                Log.d(MY_TAG, "mBroadcastReceiver5: Disconnected from "+mDevice.getName());
                Toast.makeText(getContext(), "Disconnected from "+mDevice.getName(), Toast.LENGTH_LONG).show();
                mBluetoothConnection = new BluetoothServices(getContext());
//                mBluetoothConnection.startAcceptThread();


                sharedPreferences = getActivity().getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
                editor = sharedPreferences.edit();
                editor.putString("connStatus", "Disconnected");
                //TextView connStatusTextView = findViewById(R.id.connStatusTextView);
                //connStatusTextView.setText("Disconnected");
                updateDisconnectStatus();
                editor.commit();

                if(isDisconnectBtn == false) {
                    try {

                        myDialog.show();
                    }catch (Exception e){
                        Log.d(MY_TAG, "BluetoothPopUp: mBroadcastReceiver5 Dialog show failure");
                    }

                    retryConnection = true;
                    reconnectionHandler.postDelayed(reconnectionRunnable, 5000);
                }
//                retryConnection = true;
//                reconnectionHandler.postDelayed(reconnectionRunnable, 5000);

            }
            editor.commit();
        }
    };

    public void startConnection(){
        startBTConnection(mDevice,myUUID);
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(MY_TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection");

        mBluetoothConnection.startClientThread(device, uuid);
    }


    @Override
    public void onPause() {
        Log.d(MY_TAG, "onPause: called");
        super.onPause();
        try {
            getContext().unregisterReceiver(mBroadcastReceiver1);
            getContext().unregisterReceiver(mBroadcastReceiver2);
            getContext().unregisterReceiver(mBroadcastReceiver3);
            getContext().unregisterReceiver(mBroadcastReceiver4);
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mBroadcastReceiver5);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    public static String getBTdeviceName()
    {
        return mDevice.getName();
    }



}