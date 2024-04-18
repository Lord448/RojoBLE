package ca.crit.treasurehunter.rojoble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

//@RequiresApi(api = Build.VERSION_CODES.S)
@SuppressLint("InlinedApi")
public class RojoBLE {
    public static final int ROJO_TYPE_WRITE = 1;
    public static final int ROJO_TYPE_NOTIFY = 2;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ENABLE_ADMIN_BT = 2;
    private static final String TAG = "RojoBLE";
    private final int typeCharacteristic;
    private final Context context;
    private BluetoothGattCharacteristic mCharacteristic;
    private BluetoothGatt mGatt;
    private BluetoothDevice mDevice;
    private BluetoothGattDescriptor mDescriptor;
    private RojoGattCallback GattCallback;
    private String mDeviceMacAddress;
    private String strDataSend;
    private String strDataReceived;
    private byte[] mDataBuffer;
    private byte[] mDataReceived;
    private SetNotificationsListener setNotificationsListener;
    private SetGattServerDisconnectedListener setGattServerDisconnectedListener;
    private SetGattServerConnectedListener setGattServerConnectedListener;
    private SetOnGattServerStatusChangedListener setOnGattServerStatusChangedListener;


    private static final String[] btPermissions = {
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN
    };

    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED
    };

    private static final String[] PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED
    };

    /**
     * @brief Constructor of the class it prepares all the attributes to
     *        establish the BLE connection, first it checks the type of
     *        the characteristic and depending if it's write or notify
     *        it creates the corresponding objects
     * @param context  Main context of the Android Application launcher
     * @param characteristicUUID  UUID of the BLE characteristic
     * @param typeCharacteristic  Type of the characteristic object that will be created
     */
    public RojoBLE(Context context, UUID characteristicUUID, int typeCharacteristic, String deviceMacAddress) {
        this.typeCharacteristic = typeCharacteristic;
        this.context = context;
        this.mDeviceMacAddress = deviceMacAddress;
        if (context == null) {
            Log.e(TAG, "Context is null");
            Log.e(TAG, "Class not constructed");
            return;
        }
        if(this.typeCharacteristic == ROJO_TYPE_WRITE) {
            mCharacteristic = new BluetoothGattCharacteristic(
                    characteristicUUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);
            GattCallback = new RojoGattCallback(mCharacteristic, "TRY".getBytes(), characteristicUUID);
            Log.i(TAG, "Write characteristic created");
        }
        else if(this.typeCharacteristic == ROJO_TYPE_NOTIFY) {
            mCharacteristic = new BluetoothGattCharacteristic(
                    characteristicUUID,
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);
            GattCallback = new RojoGattCallback(mCharacteristic, null, characteristicUUID);
            GattCallback.setOnCharacteristicChangedListener(this::onCharacteristicNotificationListener);
            GattCallback.setOnGattServerDisconnected(this::onGattServerDisconnectedListener);
            GattCallback.setOnGattServerConnected(this::onGattServerConnectedListener);
            GattCallback.setOnGattServerStatusChangedListener(this::onGattServerStatusChangedListener);
            Log.i(TAG, "Notify characteristic created");
        }
        else {
            Log.i(TAG, "Could not create the characteristic");
            mCharacteristic = null;
            return;
        }
        mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(this.mDeviceMacAddress);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, btPermissions, REQUEST_ENABLE_BT);
            ActivityCompat.requestPermissions((Activity) context, btPermissions, REQUEST_ENABLE_ADMIN_BT);
        }
        mGatt = mDevice.connectGatt(this.context, false, GattCallback);
        Log.i(TAG, "Finished construct");
    }

    /**
     * @brief Overload constructor of the class
     *        it prepares all the attributes to
     *        establish the BLE connection, first it checks the type of
     *        the characteristic and depending if it's write or notify
     *        it creates the corresponding objects
     * @param context            Main context of the Android Application Launcher
     * @param characteristicUUID UUID of the characteristic that will be created
     * @param typeCharacteristic Type of the characteristic object that will be created
     * @param adapter            Bluetooth adapter internal instance of the Android API
     * @param deviceName         Name of the device that will be connected
     */
    public RojoBLE(Context context, UUID characteristicUUID, int typeCharacteristic, BluetoothAdapter adapter, String deviceName) {
        this.typeCharacteristic = typeCharacteristic;
        this.context = context;
        if (context == null) {
            Log.e(TAG, "Context is null");
            Log.e(TAG, "Class not constructed");
            return;
        }
        if(this.typeCharacteristic == ROJO_TYPE_WRITE) {
            mCharacteristic = new BluetoothGattCharacteristic(
                    characteristicUUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);
            GattCallback = new RojoGattCallback(mCharacteristic, "TRY".getBytes(), characteristicUUID);
            Log.i(TAG, "Write characteristic created");
        }
        else if(this.typeCharacteristic == ROJO_TYPE_NOTIFY) {
            mCharacteristic = new BluetoothGattCharacteristic(
                    characteristicUUID,
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);
            GattCallback = new RojoGattCallback(mCharacteristic, null, characteristicUUID);
            GattCallback.setOnCharacteristicChangedListener(this::onCharacteristicNotificationListener);
            Log.i(TAG, "Notify characteristic created");
        }
        else {
            Log.i(TAG, "Could not create the characteristic");
            return;
        }
        if (mDeviceMacAddress == null) {
            mDeviceMacAddress = searchForMacAddress(this.context, adapter, deviceName);
        }
        mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mDeviceMacAddress);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, btPermissions, REQUEST_ENABLE_BT);
            ActivityCompat.requestPermissions((Activity) context, btPermissions, REQUEST_ENABLE_ADMIN_BT);
        }
        mGatt = mDevice.connectGatt(this.context, false, GattCallback);
    }

    /**
     * @brief Check if the device that it's executing the program can use BLE
     * @note  This method handles also the permissions request for BLE
     * @param context          Main context of the Android Application Launcher
     * @param bluetoothAdapter Bluetooth adapter internal instance of the Android API
     * @return True if the device can use BLE, False if not
     */
    public static boolean checkBLESupport(Context context, BluetoothAdapter bluetoothAdapter) {
        if(bluetoothAdapter != null) {
            if(!bluetoothAdapter.isEnabled()) {
                if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions((Activity) context, btPermissions, REQUEST_ENABLE_BT);
                    ActivityCompat.requestPermissions((Activity) context, btPermissions, REQUEST_ENABLE_ADMIN_BT);
                }
            }
        }
        else {
            Log.e(TAG, "Bluetooth Adapter is null");
            return false;
        }
        return true;
    }

    /**
     * @brief This function search a BLE device from it's name and if found it returns the device MAC address
     * @param context          Main context of the Android application launcher
     * @param bluetoothAdapter Bluetooth adapter internal instance of the Android API
     * @param deviceName       Name of the device that will be searched
     * @return String: MAC address of the device if founded, otherwise it returns null
     */
    public static String searchForMacAddress(Context context, BluetoothAdapter bluetoothAdapter, String deviceName) {
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, PERMISSIONS_STORAGE, 1);
        }
        else if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, PERMISSIONS_LOCATION, 1);
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String temporalDeviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
                if (Objects.equals(temporalDeviceName, deviceName)) {
                    Log.i(TAG, "Mac address founded");
                    return deviceHardwareAddress;
                }
            }
            Log.i(TAG, "Mac address not founded");
            return null;
        }
        Log.i(TAG, "Mac address not founded");
        return null;
    }

    /**
     * @brief Allows the user compare the incoming data (bytes array) with a desired string
     * @param dataReceived  Data received from the gatt server
     * @param dataToCompare Data that will be compared with the incoming data
     * @return True if they are equals, otherwise false
     */
    public static boolean compareStrings(String dataReceived, String dataToCompare) {
        return dataReceived.toLowerCase().trim().equals(dataToCompare.toLowerCase().trim());
    }

    /**
     * @brief Allows the user compare data from the incoming raw data
     * @param value         raw received data (in byte array) that will be compared
     * @param dataToCompare Data that will be compared
     * @return true if they are equals otherwise false
     */
    public static boolean compareIncomingData(byte[] value, String dataToCompare) {
        return compareStrings(getString(value), dataToCompare);
    }

    /**
     * @brief Translates the byte raw data to string
     * @param value Incoming data
     * @return String value of the data encoded in UTF 8
     */
    public static String getString(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }

    /**
     * @brief Send data to the gatt server
     * @note  Only available if the characteristic is ROJO_TYPE_WRITE
     * @param dataBuffer Data that will be sent
     * @return Status of the operation
     */
    public boolean sendData(String dataBuffer) {
        mDataBuffer = dataBuffer.getBytes();
        if(GattCallback.sendData(mDataBuffer, mGatt)) {
            Log.i(TAG, "Data send successfully");
            return true;
        }
        else {
            Log.i(TAG, "Data could not be sent");
            return false;
        }
    }

    /**
     * @brief Getter of the RojoGattCallback Attribute
     * @return RojoGattCallback instance
     */
    public RojoGattCallback getGattCallback() {
        return GattCallback;
    }

    /**
     * --------------------------------------------------------------------------
     *                       GATT SERVER CONNECTION LISTENERS
     * --------------------------------------------------------------------------
     */

    /**
     * @brief Interface that specifies the method that will be handle the onGattServerConnectedListener
     */
    public interface SetGattServerConnectedListener {
        void onGattServerConnectedListener();
    }

    /**
     * @brief Allows the listener of RojoGattCallback executes the onGattServerConnectedListener
     */
    private void onGattServerConnectedListener() {
        if(setGattServerConnectedListener != null) {
            setGattServerConnectedListener.onGattServerConnectedListener();
        }
    }

    /**
     * @brief Setter of the listener
     * @param listener Method that will be called by the listener in the main thread
     *                 (or other one desired by the user)
     */
    public void setOnGattServerConnectedListener(SetGattServerConnectedListener listener) {
        setGattServerConnectedListener = listener;
    }

    /**
     * @brief Interface that specifies the method that will be handle the onGattServerDisconnectedListener
     */
    public interface SetGattServerDisconnectedListener {
        void onGattServerDisconnectedListener();
    }

    /**
     * @brief Allows the listener of RojoGattCallback executes the onGattServerDisconnectedListener
     */
    private void onGattServerDisconnectedListener() {
        if(setGattServerDisconnectedListener != null) {
            setGattServerDisconnectedListener.onGattServerDisconnectedListener();
        }
    }

    /**
     * @brief Setter of the listener
     * @param listener Method that will be called by the listener in the main thread
     *                 (or other one desired by the user)
     */
    public void setOnGattServerDisconnectedListener(SetGattServerDisconnectedListener listener) {
        setGattServerDisconnectedListener = listener;
    }

    /**
     * @brief Interface that specifies the method that will be handle the onGattServerDisconnectedListener
     */
    public interface SetOnGattServerStatusChangedListener {
        void onCharacteristicNotificationListener(int status);
    }

    /**
     * @brief Allows the listener of RojoGattCallback executes the onGattServerDisconnectedListener
     * @param status status of the gatt server connection
     */
    private void onGattServerStatusChangedListener(int status) {
        if(setOnGattServerStatusChangedListener != null) {
            setOnGattServerStatusChangedListener.onCharacteristicNotificationListener(status);
        }
    }

    /**
     * @brief Setter of the listener
     * @param listener Method that will be called by the listener in the main thread
     *                 (or other one desired by the user)
     */
    public void setOnGattServerStatusChangedListeners(SetOnGattServerStatusChangedListener listener) {
        setOnGattServerStatusChangedListener = listener;
    }

    /**
     * --------------------------------------------------------------------------
     *                     CHARACTERISTIC NOTIFICATION LISTENER
     * --------------------------------------------------------------------------
     */

    /**
     * @brief Interface that specifies the method that will be handle the onGattServerDisconnectedListener
     */
    public interface SetNotificationsListener {
        void onCharacteristicNotificationListener(byte[] value);
    }

    /**
     * @brief Interface that specifies the method that will be handle the onGattServerDisconnectedListener
     * @param value Incoming data
     */
    private void onCharacteristicNotificationListener(byte[] value) {
        if(setNotificationsListener != null) {
            setNotificationsListener.onCharacteristicNotificationListener(value);
        }
    }

    /**
     * @brief Setter of the listener
     * @param listener Method that will be called by the listener in the main thread
     *                 (or other one desired by the user)
     */
    public void setOnCharacteristicNotificationListener(SetNotificationsListener listener) {
        setNotificationsListener = listener;
    }
}
