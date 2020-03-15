package com.example.usbserial.utile;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsbSerialUtile {
    private static final String TAG = "UsbSerialUtile";

   // private UsbManager mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    public UsbManager mUsbManager;
    public UsbSerialPort Dusbport;
    public boolean threadStatus; //线程状态，为了安全终止线程

    static private SerialInputOutputManager mSerialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    public void init(Activity activity){
    mUsbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
    final List <UsbSerialDriver> drivers =
                UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);

       // final List<UsbSerialPort> result = new ArrayList <UsbSerialPort>();
        for (final UsbSerialDriver driver : drivers) {
            final List<UsbSerialPort> ports = driver.getPorts();
            LogUtils.d( String.format("+ %s: %s port%s",
                    driver, Integer.valueOf(ports.size()), ports.size() == 1 ? "" : "s"));
            for (UsbSerialPort cp2102 : ports){
                final UsbDevice device = cp2102.getDriver().getDevice();
                if(device.getVendorId() == 0x10C4 && device.getProductId() == 0xEA60){
                    Dusbport = cp2102;
                    LogUtils.d("vendorid is "+0x10C4+" productid is "+0xEA60);
                }
            }

            //result.addAll(ports);
        }

    }

    //打开端口并且进行设置

    public void setUsbDevice() {
        UsbDeviceConnection connection = mUsbManager.openDevice(Dusbport.getDriver().getDevice());

        //初始化一个connection以实现USB通讯,详见Android USB Host API
        try {
            //开启端口
            Dusbport.open(connection);
            threadStatus = false; //线程状态
            //为sPort设置参数
            Dusbport.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
            try {
                Dusbport.close();
            } catch (IOException e2) {
                // Ignore.
            }
            Dusbport = null;
            return;
        }
    }

    //发送数据
    public boolean sendDataStr(String data) throws IOException {
        LogUtils.i("sendDataString:"+data);
        if (Dusbport != null) {
            Dusbport.write(DataUtils.HexToByteArr(data),200);
        }
        return false;
    }

    public boolean sendDataByte(byte[] data) throws IOException {
        LogUtils.i("sendDataByte:"+Arrays.toString(data));
        if (Dusbport != null) {
            Dusbport.write(data,200);
        }
        return false;
    }

    public void stopIoManager() {
        if (mSerialIoManager != null) {
            LogUtils.i("Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    public void startIoManager(SerialInputOutputManager.Listener mListener) {
        if (mSerialIoManager == null) {
            LogUtils.i("Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(Dusbport, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }


    /**
     * usb设备断开连接
     * */
    public void disconnect(){
        LogUtils.e("usb disconnect...");
        stopIoManager();
        if (Dusbport != null) {
            try {
               Dusbport.close();
            } catch (IOException e) {
                // Ignore.
            }
            Dusbport = null;
        }

        //usbdevice usbmanager
    }

    //校正函数用克进行
    public void Calibrate(int cal_Text) throws IOException {
        //输入的重量单位为克
        //int cal_Text = Integer.parseInt(view_sendascii_cal.getText().toString());
        LogUtils.d("校正的重量是"+cal_Text);
        int crcdata = DataUtils.crccal(cal_Text);
        String crcdata1 = String.format("%02x", crcdata);
        //String cal_hi = Integer.toHexString(cal_Text>>8);

        //String cal_hi1 = String.format("%02x", Integer.parseInt(cal_hi));
        String cal_hi1 = String.format("%02x", cal_Text>>8);
        LogUtils.d("高八位"+cal_hi1);
        //String data = "AD00"+Integer.toHexString(cal_Text>>8)+Integer.toHexString(cal_Text & 0xff)+Integer.toHexString(crcdata);
        //String data = "AD00"+cal_hi1+Integer.toHexString(cal_Text & 0xff)+Integer.toHexString(crcdata);
        String data = "AD00"+cal_hi1+Integer.toHexString(cal_Text & 0xff)+crcdata1;
        LogUtils.d("发送的数据为"+data);
        //serialPortUtils.sendSerialPort_hex(data);
        //dusbport.write(DataUtils.HexToByteArr(data),200);
        sendDataStr(data);
    }

    //校正函数用克进行
    public void Calibrate5g(int cal_Text1) throws IOException {
        //输入的重量单位为克
        //int cal_Text = Integer.parseInt(view_sendascii_cal.getText().toString());
        LogUtils.d("校正的重量是"+cal_Text1);
        int cal_Text = cal_Text1/5;
        int crcdata = DataUtils.crccal(cal_Text);
        String crcdata1 = String.format("%02x", crcdata);
        //String cal_hi = Integer.toHexString(cal_Text>>8);

        //String cal_hi1 = String.format("%02x", Integer.parseInt(cal_hi));
        String cal_hi1 = String.format("%02x", cal_Text>>8);
        LogUtils.d("高八位"+cal_hi1);
        //String data = "AD00"+Integer.toHexString(cal_Text>>8)+Integer.toHexString(cal_Text & 0xff)+Integer.toHexString(crcdata);
        //String data = "AD00"+cal_hi1+Integer.toHexString(cal_Text & 0xff)+Integer.toHexString(crcdata);
        String data = "AD00"+cal_hi1+Integer.toHexString(cal_Text & 0xff)+crcdata1;
        LogUtils.d("发送的数据为"+data);
        //serialPortUtils.sendSerialPort_hex(data);
        //dusbport.write(DataUtils.HexToByteArr(data),200);
        sendDataStr(data);
    }

}
