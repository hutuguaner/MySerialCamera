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
import java.util.List;

import android_serialport_api.SerialPort;
import android_serialport_api.SerialPortFinder;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private StringBuffer stringBuffer;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stringBuffer = new StringBuffer();
        textView = findViewById(R.id.tv_log);
        textView.setMovementMethod(ScrollingMovementMethod.getInstance());
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            stringBuffer.append("当前无可用驱动\n");
            textView.setText(stringBuffer.toString());
        } else {
            stringBuffer.append("当前可用驱动：" + availableDrivers.size() + "个\n");
            textView.setText(stringBuffer.toString());
            for (UsbSerialDriver driver : availableDrivers) {
                stringBuffer.append("-------------------------\n");
                stringBuffer.append("驱动对应端口数量： " + driver.getPorts().size() + "个\n");
                stringBuffer.append("驱动设备名称： " + driver.getDevice().getDeviceName() + "\n");
                stringBuffer.append("驱动设备制造商： " + driver.getDevice().getManufacturerName() + "\n");
                stringBuffer.append("驱动产品名称： " + driver.getDevice().getProductName() + "\n");
                stringBuffer.append("驱动串口号： " + driver.getDevice().getSerialNumber() + "\n");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    stringBuffer.append("驱动版本： " + driver.getDevice().getVersion() + "\n");
                }
                stringBuffer.append("--------------------------\n");
                textView.setText(stringBuffer.toString());
            }

            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection usbDeviceConnection = manager.openDevice(driver.getDevice());
            if (usbDeviceConnection == null) {
                stringBuffer.append("open device failed \n");
                textView.setText(stringBuffer.toString());
                return;
            } else {
                final UsbSerialPort usbSerialPort = driver.getPorts().get(0);
                try {
                    usbSerialPort.open(usbDeviceConnection);
                    usbSerialPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    usbSerialPort.setDTR(true);
                    usbSerialPort.setRTS(true);
                    stringBuffer.append("open device success \n");
                    textView.setText(stringBuffer.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                    stringBuffer.append("open device exception \n");
                    textView.setText(stringBuffer.toString());
                }

                try {
                    //55 48 01 32 00 02 23
                    byte[] write = HexDump.hexStringToByteArray("AA0117040010012C");
                    //byte[] write = new byte[]{0x55,0x48,0x01,0x32,0x00,0x02,0x23};

                    String writeStr = HexDump.toHexString(write);
                    stringBuffer.append("write data: "+writeStr+"\n");
                    textView.setText(stringBuffer.toString());
                    int resultWrite = usbSerialPort.write(write, 2000);
                    stringBuffer.append("result write: " + resultWrite + "\n");
                    textView.setText(stringBuffer.toString());

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (true) {
                                byte[] read = new byte[10];
                                int resultRead = 0;
                                try {
                                    resultRead = usbSerialPort.read(read, 100);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                stringBuffer.append("result read: " + resultRead + "\n");
                                for (int i = 0; i < read.length; i++) {
                                    stringBuffer.append(read[i] + " ");
                                }
                                stringBuffer.append("\n");
                                stringBuffer.append("read data: "+HexDump.toHexString(read)+"\n");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        textView.setText(stringBuffer.toString());
                                    }
                                });
                                SystemClock.sleep(5000);
                            }
                        }
                    }).start();


                } catch (IOException e) {
                    e.printStackTrace();
                    stringBuffer.append("write exception: " + e.getMessage() + "\n");
                    textView.setText(stringBuffer.toString());
                }
            }


        }

        //////////////////////////////////////////////////////////////////////////////////////////
        SerialPortFinder serialPortFinder = new SerialPortFinder();
        String[] entryValues = serialPortFinder.getAllDevicesPath();
        for(int i=0;i<entryValues.length;i++){
            Log.i("hehe"," entryValue: "+entryValues[i]);
        }
        /*try {
            SerialPort serialPort = new SerialPort(new File("/dev/bus/usb/001/005"),115200,0);
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("hehe","找不到改设备文件： "+e.getMessage());
        }*/
    }
}
