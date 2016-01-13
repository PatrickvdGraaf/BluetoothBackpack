package graaf.nl.bluetoothbackpack;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "Jon";
    private static UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static String address = "XX:XX:XX:XX:XX:XX";

    Button Connect;
    ToggleButton OnOff;
    TextView Result;
    Handler handler = new Handler();
    EditText setAddressET, setUUIDET;
    Button setAddressBtn, setUUIDBtn;

    TextView addressTv, uuidTv;


    byte delimiter = 10;
    boolean stopWorker = false;
    int readBufferPosition = 0;
    byte[] readBuffer = new byte[1024];
    private String dataToSend;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Connect = (Button) findViewById(R.id.connect);
        OnOff = (ToggleButton) findViewById(R.id.tgOnOff);
        Result = (TextView) findViewById(R.id.msgJonduino);

        setAddressBtn = (Button) findViewById(R.id.setAddressBtn);
        setUUIDBtn = (Button) findViewById(R.id.setUUIDBtn);
        setAddressET = (EditText) findViewById(R.id.setAddressET);
        setUUIDET = (EditText) findViewById(R.id.setUUIDET);

        addressTv = (TextView) findViewById(R.id.addressTV);
        uuidTv = (TextView) findViewById(R.id.uuidTV);

        Connect.setOnClickListener(this);
        OnOff.setOnClickListener(this);

        setAddressBtn.setOnClickListener(this);
        setUUIDBtn.setOnClickListener(this);

        updateInfo();
        CheckBt();
        try {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            Log.e("Jon", device.toString());
        }catch (IllegalArgumentException e){
            Toast.makeText(this, address+ "is not a valid Bluetooth address", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View control) {
        switch (control.getId()) {
            case R.id.connect:
                Connect();
                break;
            case R.id.tgOnOff:
                if (OnOff.isChecked()) {
                    dataToSend = "1";
                    writeData(dataToSend);
                } else if (!OnOff.isChecked()) {
                    dataToSend = "0";
                    writeData(dataToSend);
                }
                break;
            case R.id.setAddressBtn:
                address = setAddressET.getText().toString();
                updateInfo();
                System.out.println(address);
                break;
            case R.id.setUUIDBtn:
                try {
                    MY_UUID = UUID.fromString(setUUIDET.getText().toString());
                    updateInfo();
                }catch (IllegalArgumentException e){
                    Toast.makeText(this, "Invalid UUID: " + setUUIDET.getText().toString(), Toast.LENGTH_SHORT);
                }
                System.out.println(setUUIDET.getText().toString());
                break;
        }
    }

    private void updateInfo(){
        addressTv.setText("Address: " + address);
        uuidTv.setText("UUID: " + MY_UUID.toString());
    }

    private void CheckBt() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Bluetooth Disabled !",
                    Toast.LENGTH_SHORT).show();
        }

        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),
                    "Bluetooth null !", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void Connect() {
        Log.d(TAG, address);
        try {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            Log.e("Jon", device.toString());
            Log.d(TAG, "Connecting to ... " + device);
            mBluetoothAdapter.cancelDiscovery();
            try {
                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                btSocket.connect();
                Log.d(TAG, "Connection made.");
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) {
                    Log.d(TAG, "Unable to end the connection");
                }
                Log.d(TAG, "Socket creation failed");
            }

            beginListenForData();
        }catch (IllegalArgumentException e){
            Toast.makeText(this, address+ "is not a valid Bluetooth address", Toast.LENGTH_SHORT).show();
        }
    }

    private void writeData(String data) {
        if (outStream != null) {
            try {
                outStream = btSocket.getOutputStream();
            } catch (IOException e) {
                Log.d(TAG, "Bug BEFORE Sending stuff", e);
            }

            String message = data;
            byte[] msgBuffer = message.getBytes();

            try {
                outStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "Bug while sending stuff", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            btSocket.close();
        } catch (NullPointerException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void beginListenForData()   {
        try {
            inStream = btSocket.getInputStream();
        } catch (IOException e) {
        }

        Thread workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = inStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            inStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {

                                            if(Result.getText().toString().equals("..")) {
                                                Result.setText(data);
                                            } else {
                                                Result.append("\n"+data);
                                            }

                                                        /* You also can use Result.setText(data); it won't display multilines
                                                        */

                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }
}
