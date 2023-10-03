package com.example.samplerojoble;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * This project implements a "UART" type connection to make more easy use the BLE
 * But the RojoBLE and RojoGattCallback classes can handle multiples characteristics
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    //Generate random UUIDs at: https://www.uuidgenerator.net/
    private static final UUID txChUUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E"); //ESP32 UUID TX
    private static final UUID rxChUUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E"); //ESP32 UUID RX
    private static final String TAG = "MainActivity";
    private static final String deviceName = "RojoBLE";
    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private String deviceMacAddress;
    //Write characteristic
    private RojoBLE rojoTX;
    //Notify characteristic
    private RojoBLE rojoRX;
    private String strValue;
    private Button btnON_OFF;
    private TextView txtStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnON_OFF = (Button) findViewById(R.id.btnON_OFF);
        txtStatus = (TextView) findViewById(R.id.txtStatus);

        //Step 1 -- Check if the mobile supports BLE
        if(!RojoBLE.checkBLESupport(this, bluetoothAdapter)) {
            Toast.makeText(getApplicationContext(), "Your device doesn't support bluetooth", Toast.LENGTH_LONG).show();
            finish();
        }
        //Step 2 -- Get the mac address of the device
        deviceMacAddress = RojoBLE.searchForMacAddress(this, bluetoothAdapter, deviceName);
        //Step 3 -- Handle the null pointer exception
        if(deviceMacAddress == null) {
            //If the mac address is null it means that the device is not connected to your mobile
            Log.e(TAG, "ESP32 not paired");
            //Handle here the error
            Toast.makeText(getApplicationContext(), "Connect the device", Toast.LENGTH_LONG).show();
            finish();
        }
        //Step 4 -- If the mac address has been found you proceed to instance the RojoBLE objects
        else {
            /*
            Similar to UART, the internal characteristic rojoTX will be connected to the RX characteristic
            of the ESP32 and rojoRX will be connected to TX characteristic of the ESP32.
            ROJO_TYPE_WRITE means that the object rojoTX will be a writable characteristic
            (long short story, you can send information from rojoTX and receive from rojoRX).
             */
            rojoTX = new RojoBLE(this, rxChUUID, RojoBLE.ROJO_TYPE_WRITE, deviceMacAddress);
            rojoRX = new RojoBLE(this, txChUUID, RojoBLE.ROJO_TYPE_NOTIFY, deviceMacAddress);
            /*
            In order to receive information from the object rojoRX you will need to set a listener called
             ******************       onCharacteristicNotificationListener()   **************************
             */
            rojoRX.setOnCharacteristicNotificationListener(this::onCharacteristicNotificationListener);
        }
        //Button handler
        btnON_OFF.setOnClickListener(this::onClick);
    }

    /**
     * Listener that handle the data receive from the Notify characteristic
     * @param value: array that contains the incoming data
     */
    public void onCharacteristicNotificationListener(byte[] value) {
        strValue = new String(value, StandardCharsets.UTF_8); //Parsing to string
        String status = "ON";
        Log.i(TAG, "Receive data!");
        //To compare strings is recommended to use the toLowerCase() and the trim() methods with equals
        int sample = 0;
        //Compare data way
        if(RojoBLE.compareStrings(strValue, "Sample!")) {
            //Do some stuff
            sample++;
        }
        //Second compare data way
        else if(RojoBLE.compareIncomingData(value, "This is a more efficient way!")) {
            //Do some stuff here
            sample--;
        }
        //Real sample code
        else if(RojoBLE.compareStrings(strValue, "ESP32 ON")) {
            Log.i(TAG,"Led turned ON");
            status = "ON";
        }
        else if(RojoBLE.compareIncomingData(value, "ESP32 OFF")) {
            Log.i(TAG, "Led tuned off");
            status = "OFF";
        }
        /*
        If you want to modify a textview or another graphic resource from any listener
        you'll need to set a new thread for the action
         */
        /*
        Since the new thread is handled as a new class, (more properly an inner class) to use an
        external variable on it, you will need to declare a final variable that
        will keep the same value during all the thread execution.
         */
        String finalStatus = status;
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n") //Suppress for a literal string assignment
            @Override
            public void run() {
                txtStatus.setText(finalStatus);
            }
        });
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.btnON_OFF) {
            if(btnON_OFF.getText().toString().equals("ON")) {
                rojoTX.sendData("ON");
                btnON_OFF.setText("OFF");
            }
            else if(btnON_OFF.getText().toString().equals("OFF")) {
                rojoTX.sendData("OFF");
                btnON_OFF.setText("ON");
            }
        }
    }

    /**
     * Shows to the user how to send information with the class
     */
    public void SendInformation() {
        rojoTX.sendData("String to send");
    }
}