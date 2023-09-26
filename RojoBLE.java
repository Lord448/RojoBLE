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
    private SetNotificationsListener setNotificationsListener;
    private BluetoothGatt mGatt;
    private BluetoothDevice mDevice;
    private BluetoothGattDescriptor mDescriptor;
    private RojoGattCallback GattCallback;
    private String mDeviceMacAddress;
    private String strDataSend;
    private String strDataReceived;
    private byte[] mDataBuffer;
    private byte[] mDataReceived;

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
            Log.i(TAG, "Notify characteristic created");
        }
        else {
            Log.i(TAG, "Could not create the characteristic");
            mCharacteristic = null;
            return;
        }
        mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(this.mDeviceMacAddress);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, btPermissions, REQUEST_ENABLE_BT);
            ActivityCompat.requestPermissions((Activity) context, btPermissions, REQUEST_ENABLE_ADMIN_BT);
        }
        mGatt = mDevice.connectGatt(this.context, false, GattCallback);
        Log.i(TAG, "Finished construct");
    }

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
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, btPermissions, REQUEST_ENABLE_BT);
            ActivityCompat.requestPermissions((Activity) context, btPermissions, REQUEST_ENABLE_ADMIN_BT);
        }
        mGatt = mDevice.connectGatt(this.context, false, GattCallback);
    }

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

    public static String searchForMacAddress(Context context, BluetoothAdapter bluetoothAdapter, String deviceName) {
        if(ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
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

    private void onCharacteristicNotificationListener(byte[] value) {
        if(setNotificationsListener != null) {
            setNotificationsListener.onCharacteristicNotificationListener(value);
        }
    }

    public interface SetNotificationsListener {
        void onCharacteristicNotificationListener(byte[] value);
    }

    public void setOnCharacteristicNotificationListener(SetNotificationsListener listener) {
        setNotificationsListener = listener;
    }

    public RojoGattCallback getGattCallback() {
        return GattCallback;
    }
}

