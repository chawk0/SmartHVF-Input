package com.example.testlibrary;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.icu.util.Output;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class TestClass
{
    private BluetoothAdapter ba;
    private Set<BluetoothDevice> pairedDevices;
    private BluetoothDevice inputDevice;
    private UUID uuid;
    private BluetoothSocket bs;
    private OutputStream os;
    private ListenerThread listener;

    private boolean inputReceived;

    public void TestMethod()
    {
        Log.d("Unity", "Yay native function calls!");
    }

    public void ToggleBluetooth(Context context)
    {
        ba = BluetoothAdapter.getDefaultAdapter();
        Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        turnOn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(turnOn);
    }

    public void ClearInput()
    {
        inputReceived = false;
    }

    public boolean GetInput()
    {
        return inputReceived;
    }

    public void InitBluetooth(Context context)
    {
        ba = BluetoothAdapter.getDefaultAdapter();
        Log.d("Unity", "initializing bluetooth...");

        if (!ba.isEnabled())
        {
            Log.d("Unity", "enabling bluetooth...");
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //turnOn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(turnOn);
        }

        Log.d("Unity", "attempting to connect and send data to HC-05...");

        pairedDevices = ba.getBondedDevices();

        for (BluetoothDevice bd : pairedDevices)
        {
            //Log.d("Unity", "paired device name: " + bd.getName());

            if (bd.getName().equals("HC-05"))
            {
                Log.d("Unity", "found the input device");
                inputDevice = bd;
            }
        }

        uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        Log.d("Unity", "creating socket...");
        try
        {
            //bs = inputDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            bs = (BluetoothSocket)inputDevice.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class }).invoke(inputDevice, uuid);
        }
        catch (Exception e)
        {
            Log.d("Unity", "error creating rfcomm socket: " + e.getMessage());
        }

        try
        {
            Log.d("Unity", "connecting...");
            bs.connect();
            Log.d("Unity", "acquiring output stream...");
            os = bs.getOutputStream();

            Log.d("Unity", "sending test message...");
            byte[] buf = "hello from Java".getBytes();
            os.write(buf);

            //bs.close();
        }
        catch (IOException e)
        {
            Log.d("Unity", "error connecting socket: " + e.getMessage());
        }

        Log.d("Unity", "starting listener thread...");
        listener = new ListenerThread(bs);
        listener.start();

        Log.d("Unity", "done!");

        inputReceived = false;
    }


    private class ListenerThread extends Thread
    {
        private InputStream is;
        private OutputStream os;

        public ListenerThread(BluetoothSocket socket)
        {
            try
            {
                is = socket.getInputStream();
                os = socket.getOutputStream();
            }
            catch (Exception e)
            {
                Log.d("Unity", "error acquiring input/output streams in listener thread!");
            }
        }

        public void run()
        {
            byte[] buffer = new byte[256];
            int bytesRead;

            Log.d("Unity", "starting listen loop...");
            while (true)
            {
                try
                {
                    bytesRead = is.read(buffer);
                    if (bytesRead > 0)
                    {
                        buffer[bytesRead] = 0;
                        Log.d("Unity", "data received in listener! (" + bytesRead + " bytes)");
                        //Log.d("Unity", "data: " + new String(buffer));

                        if (buffer[0] == '1')
                        {
                            inputReceived = true;
                            Log.d("Unity", "BUTTON PRESSED!");
                        }
                    }
                }
                catch (IOException e)
                {
                    Log.d("Unity", "exception while reading data: " + e.getMessage());
                    break;
                }
            }
        }
    }
}


