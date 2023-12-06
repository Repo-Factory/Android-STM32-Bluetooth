package com.example.bluetooth;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Path;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String SERIAL_INDICATOR = "00001101-0000-1000-8000-00805F9B34FB";
    private static final String TARGET_DEVICE = "WM1";
    private static final UUID DEVICE_UUID = UUID.fromString(SERIAL_INDICATOR);
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVER_BT = 1;

    TextView btAvailability;
    TextView btPairedDevices;
    TextView btData;
    Button btOnButton;

    BluetoothAdapter btAdapter;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeBluetooth();
        initializeBluetoothButton();

        BluetoothDevice device = getDeviceAddress(btAdapter).orElse(null);
        if (device == null) {
            btData.setText("00:00");
            return;
        }
        BluetoothSocket socket = createBluetoothSocket(device);
        connectBluetoothSocket(socket);
    }

    private void initializeViews() {
        btAvailability = findViewById(R.id.statusBluetoothTv);
        btData = findViewById(R.id.data);
        btPairedDevices = findViewById(R.id.pairedTv);
        btOnButton = findViewById(R.id.onBtn);
    }

    @SuppressLint("MissingPermission")
    private void initializeBluetooth() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btAvailability.setText(btAdapter == null ?
                "Bluetooth is not available on this device" :
                "Bluetooth available"
        );
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @SuppressLint("MissingPermission")
    private void initializeBluetoothButton() {
        btOnButton.setOnClickListener(v -> {
            if (!btAdapter.isEnabled()) {
                showToast("Turning On Bluetooth...");
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            } else {
                showToast("Bluetooth is already on");
            }
        });
    }

    @SuppressLint("MissingPermission")
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) {
        try {
            return device.createRfcommSocketToServiceRecord(DEVICE_UUID);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressLint("MissingPermission")
    private void connectBluetoothSocket(BluetoothSocket socket) {
        try {
            socket.connect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public class BluetoothTask extends AsyncTask<BluetoothSocket, Void, Void> {

        private BluetoothSocket socket;

        @Override
        protected Void doInBackground(BluetoothSocket... sockets) {
            handleSocket(sockets);
            return null;
        }

        private void handleSocket(BluetoothSocket... sockets) {
            if (sockets.length == 0 || sockets[0] == null) {
                Log.e("BluetoothTask", "BluetoothSocket is null or empty.");
                return;
            }

            socket = sockets[0];

            try {
                performBluetoothOperations();
            } catch (IOException e) {
                Log.e("BluetoothTask", "Error during Bluetooth operations", e);
            } finally {
                closeBluetoothSocket();
            }
        }

        private void performBluetoothOperations() throws IOException {
            if (socket == null) {
                Log.e("BluetoothTask", "BluetoothSocket is null.");
                return;
            }

            InputStream inputStream = socket.getInputStream();
            btData.setText(inputStream.toString());
        }

        private void closeBluetoothSocket() {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.e("BluetoothTask", "Error closing BluetoothSocket", e);
                }
            }
        }
    }

    //toast message function
    private void showToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingPermission")
    private Optional<BluetoothDevice> getDeviceAddress(final BluetoothAdapter btAdapter) {
        if (btAdapter == null) {
            // Handle the case where BluetoothAdapter is null (Bluetooth not supported on the device)
            return Optional.empty();
        }

        // Ensure Bluetooth is enabled before attempting to get bonded devices
        if (!btAdapter.isEnabled()) {
            // Handle the case where Bluetooth is not enabled
            return Optional.empty();
        }

        // Fetch the set of paired (bonded) devices
        Set<BluetoothDevice> pairedDevices = null;

        try {
            pairedDevices = btAdapter.getBondedDevices();
        } catch (final Exception e) {
            pairedDevices = Collections.emptySet();
            showToast("No Paired Devices");
        }

        if (pairedDevices.isEmpty()) {
            // Handle the case where there are no paired devices
            return Optional.empty();
        }

        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals(TARGET_DEVICE)) {
                return Optional.of(device);
            }
        }
        return Optional.empty();
    }
}