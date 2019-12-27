package com.lzq.serialcamera;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.lzq.serialcamera.util.HexDump;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import android_serialport_api.SerialPort;
import android_serialport_api.SerialPortFinder;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private StringBuffer stringBuffer;

    private InputStream inputStream;
    private OutputStream outputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stringBuffer = new StringBuffer();
        textView = findViewById(R.id.tv_log);
        textView.setMovementMethod(ScrollingMovementMethod.getInstance());


        //////////////////////////////////////////////////////////////////////////////////////////
        /*SerialPortFinder serialPortFinder = new SerialPortFinder();
        String[] entryValues = serialPortFinder.getAllDevicesPath();
        stringBuffer.append("entry size: "+entryValues.length+"\n");
        textView.setText(stringBuffer.toString());
        for(int i=0;i<entryValues.length;i++){
            stringBuffer.append("entryValue: "+entryValues[i]+"\n");
            textView.setText(stringBuffer.toString());
        }*/


        try {
            SerialPort serialPort = new SerialPort(new File("/dev/ttsy1"), 115200, 0);
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("hehe", "找不到改设备文件： " + e.getMessage());
        }

        writeAndRead();
    }

    @Override
    protected void onResume() {
        super.onResume();
        writeAndRead();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isDoing = false;
    }

    private boolean isDoing = false;
    private int cmdSerialNumber = 0x00;

    private void writeAndRead() {
        if (isDoing) {
            return;
        }
        isDoing = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isDoing) {
                    if (cmdSerialNumber > 0x10) {
                        cmdSerialNumber = 0x00;
                    }

                    cmdSerialNumber++;

                    byte[] write = new byte[]{(byte) 0xAA, 0x01, (byte) cmdSerialNumber, 0x09};
                    stringBuffer.append("写命令：" + HexDump.toHexString(write) + "\n");
                    updateTextview();
                    try {
                        //
                        outputStream.write(write);
                        outputStream.flush();

                        //
                        int readCount = 0;
                        while (readCount == 0) {
                            readCount = inputStream.available();
                            SystemClock.sleep(100);
                        }
                        stringBuffer.append("可读字节长度：" + readCount + "\n");
                        updateTextview();
                        byte[] read = new byte[readCount];
                        inputStream.read(read);
                        String readStr = HexDump.toHexString(read);
                        stringBuffer.append("读到的数据：" + readStr + "\n");
                        updateTextview();
                        String tem = readStr.substring(4, readStr.length() - 4);
                        tem = new String(HexDump.hexStringToByteArray(tem));
                        stringBuffer.append("读取到的温度：" + tem + "\n");
                        updateTextview();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    for (int i = 0; i < 60; i++) {
                        if (isDoing) {
                            SystemClock.sleep(1000);
                        } else {
                            break;
                        }
                    }
                }
            }
        }).start();
    }


    private void updateTextview() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(stringBuffer.toString());
            }
        });
    }
}
