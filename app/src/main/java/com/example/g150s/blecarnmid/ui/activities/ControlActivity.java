package com.example.g150s.blecarnmid.ui.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.g150s.blecarnmid.R;
import com.example.g150s.blecarnmid.ble.BluetoothLeService;
import com.example.g150s.blecarnmid.others.SendTread;
import com.example.g150s.blecarnmid.ui.base.BaseActivity;

import java.util.ArrayList;
import java.util.List;

import static com.example.g150s.blecarnmid.others.Constant.IP;
import static com.example.g150s.blecarnmid.others.Constant.OPEN_BOX;

/**
 * Created by Charon on 2017/5/24.
 */

public class ControlActivity extends BaseActivity implements SensorEventListener {
    private final static String TAG = ControlActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 530;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int DISCONNECTED = 0;
    private static final int CONNECTED = 1;
    private static final int CONNECTING = 2;

    public static void setDelayTime(int delayTime) {
        DELAY_TIME = delayTime;
    }

    private static int DELAY_TIME = 50;
    private int editDegree = 0;
    private int currentDegree = 0;
    private int connectFlag = 0;
    private boolean isClickDisconnect = false;

// TODO: 2017/12/23 关盒子再发一个数据 

    private Vibrator vibrator;
    private static MediaPlayer mp;

    public static int rssi;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeService mBluetoothLeService;
    private Handler mHandler;

    private String mAddress;
    private String mName;

    private ImageView mIvCar;
    private TextView mTvInformation, mTvWifi;
    private EditText mEt,mEtDegree;
    private Button mBt, mBtnOpen,mBtnDegree;

    //盒子开关状态
    private boolean isOpen = false;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        mHandler = new Handler();
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        mp = MediaPlayer.create(this, R.raw.bibibi);
        initDate();
        initView();

        initService();

        initBle();

        initSensor();

    }

    private void initSensor() {
        // 传感器管理器
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        // 注册传感器(Sensor.TYPE_ORIENTATION(方向传感器);SENSOR_DELAY_FASTEST(0毫秒延迟);
        // SENSOR_DELAY_GAME(20,000毫秒延迟)、SENSOR_DELAY_UI(60,000毫秒延迟))
        sm.registerListener(ControlActivity.this,
                sm.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_FASTEST);
    }


    private void initService() {
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        boolean bll = bindService(gattServiceIntent, mServiceConnection,//2
                BIND_AUTO_CREATE);
        if (bll) {
            Log.d("123", "绑定成功");
        } else {
            Log.d("123", "绑定失败");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initBle() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "您的设备不支持蓝牙BLE，将关闭", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                showMessageOKCancel("你必须允许这个权限", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(ControlActivity.this,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
                    }
                });
                return;
            }
            requestPermissions(new String[]{Manifest.permission.WRITE_CONTACTS},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }


    private void initDate() {
        Intent intent = getIntent();
        mAddress = intent.getStringExtra(MainActivity.ADDRESS);
        mName = intent.getStringExtra(MainActivity.NAME);
        isClickDisconnect = false;
    }

    private void initView() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.control_toolbar);
        mToolbar.setTitle(mName);
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectFlag != DISCONNECTED) {
                    showNormalDialog(mName);
                } else {
                    finish();
                }
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTvInformation = (TextView) findViewById(R.id.control_tv_information);
        mTvWifi = (TextView) findViewById(R.id.control_tv_wifi);
        mIvCar = (ImageView) findViewById(R.id.control_img_car);

        mEtDegree = (EditText) findViewById(R.id.control_et_degree);
        mEt = (EditText) findViewById(R.id.control_et);
        mBt = (Button) findViewById(R.id.control_bt);
        mBtnOpen = (Button) findViewById(R.id.control_btn_open);
        mBtnDegree = (Button) findViewById(R.id.control_bt_degree);

        mBtnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isOpen = true;
                new SendTread(OPEN_BOX, IP)
                        .setSendResultListener(new SendTread.SendResultListener() {
                                                   @Override
                                                   public void onSuccess() {
                                                       Log.d(TAG, "onSuccess");
                                                       runOnUiThread(new Runnable() {
                                                           @Override
                                                           public void run() {
                                                               mBtnOpen.setText("一键关盒");
                                                           }
                                                       });
                                                   }

                                                   @Override
                                                   public void onError(String msg) {
                                                       Log.d(TAG, "onError: " + msg);
                                                   }
                                               }
                        ).start();
            }
        });

        mBtnDegree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editDegree = Integer.valueOf(mEtDegree.getText().toString());
            }
        });

        mBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String time = mEt.getText().toString();
                if ("".equals(time)) {
                    setDelayTime(Integer.valueOf(time));
                }
            }
        });

    }

    private void showNormalDialog(final String name) {
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(ControlActivity.this);
        normalDialog.setIcon(R.drawable.car);
        normalDialog.setTitle("断开连接");
        normalDialog.setMessage("点击退出，" + name + "将断开连接，是否断开" + name);
        normalDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //...To-do传入数据库，建立连接，退出
                        writeDate(false);
                        finish();
                    }
                });
        normalDialog.setNegativeButton("关闭",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        // 显示
        normalDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.control, menu);
        if (connectFlag == DISCONNECTED) {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else if (connectFlag == CONNECTING) {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_refresh);
        } else if (connectFlag == CONNECTED) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (connectFlag) {
            case DISCONNECTED:
                //连接
                isClickDisconnect = false;
                connectFlag = CONNECTING;
                invalidateOptionsMenu();
                mBluetoothLeService.connect(mAddress);
                break;
            case CONNECTING:
                connectFlag = DISCONNECTED;
                invalidateOptionsMenu();
                mBluetoothLeService.disconnect();
            case CONNECTED:
                //断开
                isClickDisconnect = true;
                connectFlag = DISCONNECTED;
                invalidateOptionsMenu();
                mBluetoothLeService.disconnect();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {//2

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {//3
                Log.e("123", "Unable to initialize Bluetooth");
                finish();
            } else {
                Log.d("123", "能初始化");
            }
            // 自动连接to the device upon successful start-up
            // 初始化.
            mBluetoothLeService.connect(mAddress);//4
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (!mBluetoothAdapter.isEnabled() || mBluetoothAdapter == null) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        //7
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());//8

        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mAddress);
            if (result) {
                connectFlag = CONNECTED;
            } else {
                connectFlag = DISCONNECTED;
            }
            Log.d("123", "连接结果" + result);
        } else {
            Log.d("123", "mBluetoothLeService为空");
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter
                .addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.READ_RSSI);
        return intentFilter;
    }


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();//接收广播
            Log.d("123", "action:" + action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                //displayConnectState("已连接");
                connectFlag = CONNECTED;
                if (mp.isPlaying()) {
                    mp.release();
                }
                writeDate(true);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
                    .equals(action)) {
                //displayConnectState("未连接");
                //加入是否可以点击

                Log.d("123", isClickDisconnect + "isClickDisconnect");
                if (!isClickDisconnect) {
                    vibrator.vibrate(new long[]{100, 2000, 500, 2500}, -1);
                    mp.start();
                }
                closeUi();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                // 搜索需要的uuid
                initGattCharacteristics(mBluetoothLeService
                        .getSupportedGattServices());
                writeDate(true);
                Toast.makeText(ControlActivity.this, "发现新services", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

            } else if (BluetoothLeService.READ_RSSI.equals(action)) {
                Log.d(TAG, "rssi:" + rssi);
                checkRssi(rssi);
            }
        }
    };

    private void checkRssi(int rssi) {
        mTvWifi.setText("信号强度：" + Math.abs(rssi));
        if (rssi < 0 && rssi > -52) {
            mIvCar.setImageResource(R.drawable.car3);
            mTvInformation.setText("嗯，小车信号满满的哟。");
        } else if (rssi <= -52 && rssi > -75) {
            mIvCar.setImageResource(R.drawable.car2);
            mTvInformation.setText("报告主人，小车信号良好。");
        } else if (rssi <= -75) {
            mIvCar.setImageResource(R.drawable.car1);
            mTvInformation.setText("主人，小车离你有点远了呢。");
        } else if (rssi == 0) {
            mIvCar.setImageResource(R.drawable.car0);
            mTvInformation.setText("小车正在连接哟。");
        } else if (rssi >= 0) {
            mIvCar.setImageResource(R.drawable.car0);
            mTvInformation.setText("哎哟，小车断开连接了哟。");
        }
    }

    private void writeDate(final boolean connect) {
        BluetoothGattCharacteristic characteristic;
        Log.d("123", connect + "flag:" + connectFlag + "");
        if (mBluetoothLeService.mBluetoothGatt != null) {
            mBluetoothLeService.mBluetoothGatt.readRemoteRssi();
        }
        if (connect && mGattCharacteristics != null) {
            for (int i = 0; i < mGattCharacteristics.size(); i++) {
                for (int j = 0; j < mGattCharacteristics.get(i).size(); j++) {
                    if ("0000fff6-0000-1000-8000-00805f9b34fb".equals(mGattCharacteristics.get(i).get(j).getUuid().toString())) {//对应的uuid  92BF01A5-0681-453A-8016-D44DD3E7100B   0000fff1-0000-1000-8000-00805f9b34fb
                        characteristic = mGattCharacteristics.get(i).get(j);
                        //写入的数据
                        write(characteristic, changeDate(currentDegree, rssi));
                        mBluetoothLeService.writeCharacteristic(characteristic);
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (connectFlag == CONNECTED) {
                                    writeDate(true);
                                }
                            }
                        }, DELAY_TIME);
                        Log.d("123", "发送数据成功");
                    }
                }
            }
        } else {
            checkRssi(1);
            Log.d("123", "发送数据失败");
        }
    }

    private byte[] changeDate(int degree, int rssi) {
        byte[] bytes;
        int bai1 = degree / 100;
        int shi1 = (degree - bai1 * 100) / 10;
        int ge1 = degree % 10;
        if (bai1 != 0) {
            bytes = new byte[]{0x41, changeByByte(bai1), changeByByte(shi1), changeByByte(ge1), 0x0D, 0x0A};
        } else if (shi1 != 0) {
            bytes = new byte[]{0x41, 0x00, changeByByte(shi1), changeByByte(ge1), 0x0D, 0x0A};
        } else if (ge1 != 0) {
            bytes = new byte[]{0x41, 0x00, 0x00, changeByByte(ge1), 0x0D, 0x0A};
        } else {
            bytes = new byte[]{0x41, 0x00, 0x00, 0x00, 0x0D, 0x0A};
        }
        for (byte byteArray : bytes) {
            Log.d("123change", "" + byteArray);
        }
        return bytes;
    }

    private byte changeByByte(int num) {
        return (byte) (num + 16 * 3);
    }

    private void write(BluetoothGattCharacteristic characteristic, byte byteArray[]) {
        characteristic.setValue(byteArray);
    }

    private void write(BluetoothGattCharacteristic characteristic, String string) {
        characteristic.setValue(string);
    }

    private void initGattCharacteristics(List<BluetoothGattService> gattServices) {
        if (gattServices == null) {
            return;
        }
        mGattCharacteristics = new ArrayList<>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService
                    .getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
            }
            mGattCharacteristics.add(charas);
        }
    }

    private void closeUi() {
        writeDate(false);
        connectFlag = DISCONNECTED;
        currentDegree = 0;
        invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        closeUi();
        mBluetoothLeService = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION && !isOpen) {
            float degree = event.values[0];
            currentDegree = (int) degree;
        } else {
            currentDegree = editDegree % 360;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
