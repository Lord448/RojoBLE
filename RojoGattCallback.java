import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.util.Log;

import java.util.UUID;

/**
 * @brief Implementation of the BluetoothGattCallback and overrides some methods
 * @note You will need to create an object per characteristic
 */
@SuppressLint("MissingPermission")
public class RojoGattCallback extends BluetoothGattCallback {
    private static final String TAG = "RojoGattCallback";
    private final UUID chUUID;
    private final byte[] dataBuffer;
    private BluetoothGattCharacteristic characteristic;
    private BluetoothGattDescriptor mDescriptor;
    private OnCharacteristicChangedListener mOnCharacteristicChangedListener;
    private byte[] dataReceived;

    /**
     * @brief The constructor of the class
     * @param characteristic The characteristic that will be handled in the instance of the class
     * @param dataBuffer The data buffer that will be sent to the characteristic in case it has the property "Write"
     * @note If dataBuffer its null the class will assume that the characteristic is a NOTIFY one
     */
    public RojoGattCallback(BluetoothGattCharacteristic characteristic, byte[] dataBuffer, UUID chUUID) {
        this.chUUID = chUUID;
        this.characteristic = characteristic;
        this.dataBuffer = dataBuffer;
    }

    /**
     * @brief This method is local and is called by the onCharacteristicWrite as a way to receive data
     * @note  This method doesn't follow the serial algorithm of BLE communication need implementation of
     *        Queues for that behave
     * @param mGatt Instance of the gatt class
     */
    private void readData(BluetoothGatt mGatt) {
        if (characteristic != null)
            mGatt.readCharacteristic(characteristic);
        else
            Log.i(TAG, "Characteristic is null");
    }

    /**
     * @brief Callback method that is called when the gatt server change his connection
     * @note It will be called when the Gatt class is constructed in the onCreate method
     * @param gatt Gatt class
     * @param status Status of the connection
     * @param newState Parameter not handled
     */
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.i(TAG, "Connected to GATT server.");
            gatt.discoverServices();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.i(TAG, "Disconnected from GATT server.");
            //@TODO ScanBT
        }
    }

    /**
     * @brief This method handles all the services discovered by the discoverServices() method, in case that
     *        the characteristic is for write, it tries to send the string "TRY", in case that the operation is
     *        successful the mCharacteristic attribute acquire the instance of temporalCharacteristic in order
     *        to get the correct service instance in the mCharacteristic instance.
     *        In case that the method detects that the characteristic is a NOTIFY type one, the method scan the
     *        descriptor an calls the enableNotifications method
     * @param gatt Instance of the gatt class that handles the connection
     * @param status Status of the operation
     */
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if(characteristic.getUuid().equals(chUUID))
                    Log.i(TAG, "Starting search of services in CHAR");
            }
            else
            {
                Log.i(TAG, "Services discovered successfully");
            }
            for(BluetoothGattService service : gatt.getServices()) {
                Log.i(TAG, "For service: " + service.getUuid().toString());
                BluetoothGattCharacteristic temporalCharacteristic = service.getCharacteristic(characteristic.getUuid());
                if (temporalCharacteristic != null) {
                    //For write characteristics
                    if(dataBuffer != null) {
                        temporalCharacteristic.setValue(dataBuffer);
                        if(gatt.writeCharacteristic(temporalCharacteristic)) {
                            Log.i(TAG, "Characteristic Selected: " + temporalCharacteristic.getUuid().toString());
                            characteristic = temporalCharacteristic;
                            return;
                        }
                        else {
                            Log.i(TAG, "Failed to write for: " + temporalCharacteristic.getUuid().toString());
                        }
                    }
                    //For the characteristics that notifies
                    for (BluetoothGattDescriptor descriptor : temporalCharacteristic.getDescriptors()) {
                        if(descriptor != null) {
                            Log.i(TAG, "Characteristic can handle notifies: " + temporalCharacteristic.getUuid());
                            Log.d(TAG, "Descriptor UUID founded: " + descriptor.getUuid());
                            characteristic = temporalCharacteristic;
                            mDescriptor = descriptor;
                            enableNotifications(gatt);
                        }
                    }
                }
                else {
                    Log.i(TAG, "Failed to get characteristic.");
                }
            }
        }
        else {
            Log.i(TAG, "Failed to discover services: " + status);
        }
    }

    /**
     * @brief This callback method handle the read request for a characteristic and is called by
     *        the system
     * @param gatt gatt class instance
     * @param characteristic characteristic that will be handled
     * @param status status of the operation
     */
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.i(TAG, "Characteristic read successfully.");
            dataReceived = characteristic.getValue();
        } else {
            Log.i(TAG, "Failed to read characteristic: " + status);
        }
    }

    /**
     * @brief Same as the onCharacteristicRead, just for write operation
     * @param gatt gatt class instance
     * @param characteristic characteristic that will be handled
     * @param status status of the operation
     */
    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.i(TAG, "Characteristic written successfully.");
            readData(gatt);
        } else {
            Log.i(TAG, "Failed to write characteristic: " + status);
        }
    }

    /**
     * @brief When a gatt server change the value in a characteristic this method is called
     *        so the onCharacteristicChangedListener can be called on the main thread and
     *        the data can be handled
     * @param gatt gatt class
     * @param characteristic characteristic that will be handled
     */
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        Log.i(TAG, "Characteristic changed.");
        byte[] value = characteristic.getValue();
        if(mOnCharacteristicChangedListener != null) {
            mOnCharacteristicChangedListener.onCharacteristicChanged(value);
        }
    }

    /**
     * @brief this method only notifies the status
     * @param gatt gatt class instance
     * @param descriptor Instance of the descriptor that will be handled
     * @param status status of the operation
     */
    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        if(status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "Descriptor written successfully");
        }
        else {
            Log.e(TAG, "Failed to set notification value");
        }
    }

    /**
     * @brief This method can be called in the main thread (or other one) to send data to the gatt
     *        server
     * @note  It has to called as a part of the MyGattCallback instance in order to specify what characteristic
     *        will be written
     * @param mDataBuffer byte array that contains the data that will be sent to the gatt server
     * @param mGatt gatt class instance
     */
    public boolean sendData(byte[] mDataBuffer, BluetoothGatt mGatt) {
        if(characteristic != null) {
            characteristic.setValue(mDataBuffer);
            boolean success = mGatt.writeCharacteristic(characteristic);
            if(success) {
                Log.i(TAG, "Data send successfully");
                return true;
            }
            else {
                Log.i(TAG, "Failed to send data");
                return false;
            }
        }
        else {
            Log.i(TAG, "Characteristic is null");
            return false;
        }
    }

    /**
     * @brief Getter fot the dataReceived attribute
     * @note this code is not proved yet and iy may not work
     * @return The dataReceive value
     */
    public byte[] getDataReceived() {
        return dataReceived;
    }

    /**
     * @brief Interface that specifies the method that will be handle the OnCharacteristicChangedListener
     */
    public interface OnCharacteristicChangedListener {
        void onCharacteristicChanged(byte[] value);
    }

    /**
     * @brief Implementation of the interface
     * @param listener Method that will be called by the listener in the main thread
     *                 (or other one desired by the user)
     */
    public void setOnCharacteristicChangedListener(OnCharacteristicChangedListener listener) {
        mOnCharacteristicChangedListener = listener;
    }

    /**
     * @brief Enable the notifications of a desired characteristic and descriptor
     *        contained in the mCharacteristic and mDescriptor attributes
     * @param gatt gatt class instance
     */
    private void enableNotifications(BluetoothGatt gatt) {
        if (gatt == null) {
            Log.e(TAG, "Gatt object is null");
            return;
        }
        else if(characteristic == null) {
            Log.e(TAG, "Characteristic object is null");
            return;
        }
        else if(mDescriptor == null) {
            Log.e(TAG, "Descriptor object is null");
            return;
        }
        // Check if characteristic has NOTIFY property
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
            Log.e(TAG, "Characteristic does not have NOTIFY property");
            return;
        }
        // Enable local notifications
        Log.i(TAG, "For characteristic: " + characteristic.getUuid().toString());
        gatt.setCharacteristicNotification(characteristic, true);
        // Enable remote notifications
        mDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(mDescriptor);
    }
}

