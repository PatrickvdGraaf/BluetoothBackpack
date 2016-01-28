package graaf.nl.bluetoothbackpack.controllers;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.ParcelUuid;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Created by Patrick van de Graaf on 26-1-2016.
 */
public class BluetoothController {
    private static BluetoothController bluetoothController;

    private BluetoothAdapter mBluetoothAdapter;
    private OutputStream outputStream;
    private InputStream inStream;

    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;

    private BluetoothListener bluetoothListener;

    public static BluetoothController getInstance(Activity activity) {
        if (bluetoothController == null) {
            bluetoothController = new BluetoothController(activity);
        }
        return bluetoothController;
    }

    public void setBluetoothListener(BluetoothListener bluetoothListener) {
        this.bluetoothListener = bluetoothListener;
    }

    private BluetoothController(Activity activity) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            System.out.println("adapter is null");
        } else if (!mBluetoothAdapter.isEnabled()) {
            System.out.println("adapter is not enabled");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, 1);
        } else {
            System.out.println("adapter is not null && enabled");

            try {
                Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();

                if (bondedDevices.size() > 0) {
                    BluetoothDevice device = null;
                    for (BluetoothDevice f : bondedDevices) {
                        if (f.getName().contains("Adafruit"))
                            device = f;
                    }
                    if (device != null) {
                        ParcelUuid[] uuids = device.getUuids();
                        BluetoothSocket socket;
                        socket = device.createRfcommSocketToServiceRecord(uuids[0].getUuid());
                        socket.connect();
                        outputStream = socket.getOutputStream();
                        inStream = socket.getInputStream();
                    }

                    beginListenForData();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
            IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
            IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            BroadcastReceiver mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    if (BluetoothDevice.ACTION_FOUND.equals(action) || (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action))) {
                        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                        // If there are paired devices
                        if (pairedDevices.size() > 0) {
                            // Loop through paired devices
                            for (BluetoothDevice d : pairedDevices) {
                                // Add the name and address to an array adapter to show in a ListView
                                System.out.println(d.getName() + "\n" + d.getAddress());
                            }
                        } else {
                            if (device != null) {
                                System.out.println(device.getName() + "\n" + device.getAddress());
                            }
                        }
                    } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                        System.out.println("Done searching");
                    } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                        System.out.println("Device is about to disconnect");
                    } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                        System.out.println("Device has disconnected");
                    }
                }
            };
            activity.registerReceiver(mReceiver, filter1);
            activity.registerReceiver(mReceiver, filter2);
            activity.registerReceiver(mReceiver, filter3);
        }
    }

    public void write(String s) {
        try {
            if (outputStream != null) {
                byte[] b = s.getBytes(StandardCharsets.US_ASCII);
                outputStream.write(b);

                Log.e("WRITE", "COMPLETED");
            } else {
                Log.e("WRITE", "OUTPUSTREAM WAS NULL");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void beginListenForData() {
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = inStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            inStream.read(packetBytes);
                            int i;
                            for (i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                readBuffer[readBufferPosition++] = b;
                            }

                            if (i == bytesAvailable) {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                String data = new String(encodedBytes, StandardCharsets.US_ASCII);
                                filterTextMessage(data);
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    public void requestHelp() {
        Location location = LocationController.getInstance().getCurrentLocation();
        String string = "IN GEVAAR," + Double.toString(location.getLatitude()) + "," + Double.toString(location.getLongitude()) + ",ENDDANGERDATA";
        write(string);
        LocationController.getInstance().getDangerLocations().add(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    private void filterTextMessage(String data) throws UnsupportedEncodingException {

        Log.e("BLUETOOTH", "Received Data: " + data);

        if (data.contains("GPS GEGEVENS")) {
            readBufferPosition = 0;
            if (LocationController.getInstance() != null) {
                requestHelp();
            }
        } else if (data.contains("IN GEVAAR") && data.contains("ENDDANGERDATA")) {
            readBufferPosition = 0;
            String[] items = data.split(",");
            LatLng dangerLocation = new LatLng(Double.parseDouble(items[1]), Double.parseDouble(items[2]));
            LocationController.getInstance().getDangerLocations().add(dangerLocation);
        }
    }

}
