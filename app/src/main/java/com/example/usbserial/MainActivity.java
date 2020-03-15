package com.example.usbserial;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.DialogTitle;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.usbserial.utile.DataUtils;
import com.example.usbserial.utile.LogUtils;
import com.example.usbserial.utile.UsbSerialUtile;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private EditText view_sendascii;
    private EditText view_sendhex;
    private TextView view_recive;

    private TextView textView_wei;
    private EditText view_sendascii_cal;
    //static private UsbSerialPort dusbport;
    private UsbSerialUtile usbSerialUtile;

    private static final String TAG = "MainActivity";

    static private SerialInputOutputManager mSerialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private byte[] mBuffer = new byte[10];

   // private UsbSerialUtile usbSerialUtile;
   // private byte[] mBuffer = {0,0};
  //  private Handler handler = new Handler();
    private int weight_num = 0;
    private int i =0;



    //读写
    //Git的源码中使用了ExecutorService来异步执行线程
    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {
                @Override
                public void onRunError(Exception e) {
                    LogUtils.d("Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                   // LogUtils.i("onReceiver data="+Arrays.toString(data));
                    LogUtils.i("onReceiver data="+DataUtils.ByteArrToHex(data)+" lenght is "+data.length);
                    if(i<10){
                        if (i == 0 & DataUtils.Byte2Hex(data[0]).equals("AA")){
                            mBuffer[0] = data[0];
                            i++;
                        }else if (i != 0 & DataUtils.Byte2Hex(mBuffer[0]).equals("AA")){
                            mBuffer[i] = data[0];
                            i++;
                        }

                    }else {i=0;}

                   // byte[] buffer = new byte[10];
                   // buffer = data;
                   // instance.runOnUiThread(new Runnable() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (i == 0){
                                view_recive.setText(DataUtils.ByteArrToHex(mBuffer));

                            //串口协议解析
                            Log.d(TAG, DataUtils.Byte2Hex(mBuffer[0]) + ":::" + DataUtils.Byte2Hex(mBuffer[9]));
                            if ((DataUtils.Byte2Hex(mBuffer[0]).equals("AA")) & (DataUtils.Byte2Hex(mBuffer[9]).equals("FF"))) {

                                Log.d(TAG, "接收格式正确");
                                int sum = DataUtils.Byte2int(mBuffer[1]) + DataUtils.Byte2int(mBuffer[2]) + DataUtils.Byte2int(mBuffer[3]) + DataUtils.Byte2int(mBuffer[4]) + DataUtils.Byte2int(mBuffer[5]) + DataUtils.Byte2int(mBuffer[6]);
                                int sum1 = DataUtils.Byte2int(mBuffer[7]) * 256 + DataUtils.Byte2int(mBuffer[8]);
                                if (sum == sum1) {
                                    weight_num = DataUtils.Byte2int(mBuffer[4]) * 65536 + DataUtils.Byte2int(mBuffer[5]) * 256 + DataUtils.Byte2int(mBuffer[6]);
                                    String code_weight;
                                    if (mBuffer[3] == 0){
                                        code_weight = " " + weight_num*5;//kg为单位,标记1g为精度的时候
                                    }else {
                                        code_weight = "-" + weight_num*5;//kg为单位
                                    }
                                    textView_wei.setText(String.valueOf(code_weight));

//                            //去皮并且确定k值
//                            serialPortUtils.sendSerialPort1("A5");
//                            serialPortUtils.sendSerialPort1("A7");
//                            serialPortUtils.sendSerialPort1("AA");
//                            serialPortUtils.sendSerialPort1("AB");
                                } else {
                                    Log.d(TAG, "校验错误");
                                }
                            } else {
                                Log.d(TAG, "接收格式错误");
                            }
                        }
                    }

                    });
                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        view_sendascii = (EditText)findViewById(R.id.view_sendascii);
        view_sendhex = (EditText)findViewById(R.id.view_sendhex);
        view_recive = (TextView)findViewById(R.id.view_recive);
        view_sendascii_cal = (EditText)findViewById(R.id.view_sendascii_cal);

        textView_wei = (TextView)findViewById(R.id.textView_wei);

        Button btsend_ascii = (Button) findViewById(R.id.btsend_ascii);
        Button btsend_hex = (Button) findViewById(R.id.btsend_hex);
        Button button_zero = (Button) findViewById(R.id.btsend_ascii_zero);
        Button btsend_ascii_cal = (Button)findViewById(R.id.btsend_ascii_cal);


        btsend_ascii.setOnClickListener(this);
        btsend_hex.setOnClickListener(this);
        btsend_ascii_cal.setOnClickListener(this);
        button_zero.setOnClickListener(this);


        usbSerialUtile = new UsbSerialUtile();
        usbSerialUtile.init(this);
        usbSerialUtile.setUsbDevice();
        usbSerialUtile.startIoManager(mListener);

     //   setUsbDevice();
      //  startIoManager();



        //去皮并且确定k值
        try {
            usbSerialUtile.sendDataStr("A300A2A4A5");
        } catch (IOException e) {
            e.printStackTrace();
        }


        //定时发送查询指令
        new Thread(new Runnable() {
            @Override
            public void run() {
                //发送A2查询指令
                try {
                    while (true){
                        Thread.sleep(200);
                        //serialPortUtils.sendSerialPort_hex("A300A2A4A5");
                        usbSerialUtile.sendDataStr("A300A2A4A5");}
                }catch (InterruptedException e){
                    LogUtils.d("发送查询指令线程错误" );
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();



    }



    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btsend_ascii:
               String inText = view_sendascii.getText().toString();
                //String inputText = DataUtils.asciiToHex(inText);
                LogUtils.d("send value is: "+inText);
                //sendData(inText.getBytes());
               // sendData(DataUtils.HexToByteArr(inText));
                try {
                    //dusbport.write(DataUtils.HexToByteArr(inText),200);
                    usbSerialUtile.sendDataStr(inText);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.btsend_hex:
                LogUtils.d("send value is: "+DataUtils.HexToByteArr("A300A2A4A5"));
                try {
                    //dusbport.write(DataUtils.HexToByteArr("A300A2A4A5"),200);
                    usbSerialUtile.sendDataStr("A300A2A4A5");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //  case R.id.btsend_hex:
          //      String intText = view_sendhex.getText().toString();
                //String inputText = DataUtils.asciiToHex(inText);
                //serialPortUtils.sendSerialPort_hex(intText);
         //       sendData(intText.getBytes());
          //      break;

            case R.id.btsend_ascii_zero:
//                serialPortUtils.sendSerialPort1("A5");
//                serialPortUtils.sendSerialPort1("AA");
//                serialPortUtils.sendSerialPort1("A7");
//                serialPortUtils.sendSerialPort1("B2");

                //去皮并且确定k值
                for (int i=0;i<5;i++) {
                    try {
                        //dusbport.write(DataUtils.HexToByteArr("AA00A9ABA8"),200);
                        usbSerialUtile.sendDataStr("AA00A9ABA8");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                break;

            case R.id.btsend_ascii_cal:
                LogUtils.d("this calibrate button");
                try {
                    int cal_Text = Integer.parseInt(view_sendascii_cal.getText().toString());
                    usbSerialUtile.Calibrate5g(cal_Text);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       usbSerialUtile.stopIoManager();
        usbSerialUtile.disconnect();
    }

}


