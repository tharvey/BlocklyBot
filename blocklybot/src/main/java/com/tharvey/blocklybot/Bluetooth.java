package com.tharvey.blocklybot;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.UUID;

public class Bluetooth extends Mobbob {
    private final static String TAG = Bluetooth.class.getSimpleName();

    // Well-known SPP UUID
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket mmSocket = null;

    private class ConnectThread extends Thread {

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket to connect with the device using SDP lookup of UUID
            try {
                tmp = device.createRfcommSocketToServiceRecord(SPP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mDevice);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            char[] bufferc = new char[1024];
            int n;
            String inString = "";

            // Keep listening to the InputStream until an exception occurs
            while (mConnectionState == connectionStateEnum.isConnected) {
                try {
                    // Read from the InputStream until we have a CRLF terminated string
                    bytes = mmInStream.read(buffer);
                    String chunk = new String(buffer, 0, bytes);
                    if (chunk.contains("\r\n")) {
                        inString += chunk;
                        Log.d(TAG, "Sending '" + inString + "'");
                        onSerialReceived(inString);
                    } else {
                        inString += chunk;
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        public String getStringFromInputStream(InputStream stream, String charsetName) throws IOException
        {
            int n = 0;
            char[] buffer = new char[1024 * 4];
            InputStreamReader reader = new InputStreamReader(stream, charsetName);
            StringWriter writer = new StringWriter();
            while (-1 != (n = reader.read(buffer))) writer.write(buffer, 0, n);
            return writer.toString();
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    public void onConectionStateChange(connectionStateEnum theConnectionState) {
        switch (theConnectionState) {
            case isConnected:
                Log.i(TAG, "Connected");
                break;
            case isConnecting:
                Log.i(TAG, "Connecting");
                break;
            case isToScan:
                Log.i(TAG, "Scan");
                break;
            case isScanning:
                Log.i(TAG, "Scanning");
                break;
            case isDisconnecting:
                Log.i(TAG, "isDisconnecting");
                break;
            default:
                break;
        }
    }

    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private BluetoothDevice mDevice;

    public Bluetooth(Context context, Handler handler, BluetoothDevice device) {
        super(handler, device.getName(), device.getAddress());
        mDevice = device;

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(connectionStateEnum.isScanning);
        connect();
    }

    public synchronized int connect() {
        Log.d(TAG, "connect " + toString());

        // Cancel any thread attempting to make a connection
        if (mConnectionState == connectionStateEnum.isConnecting) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(mDevice);
        mConnectThread.start();
        setState(connectionStateEnum.isConnecting);
        return 0;
    }

    public synchronized void disconnect() {
        Log.d(TAG, "disconnect " + toString());

        // Cancel any thread attempting to make a connection
        if (mConnectionState == connectionStateEnum.isConnecting) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // close connection
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Socket close() failed", e);
        }
        setState(connectionStateEnum.isNull);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {
        Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        setState(connectionStateEnum.isConnected);
    }

    public void serialSend(String theString){
        if (mConnectionState == connectionStateEnum.isConnected) {
            Log.d(TAG, ">> " + theString);
            mConnectedThread.write(theString.getBytes());
        }
    }
}
