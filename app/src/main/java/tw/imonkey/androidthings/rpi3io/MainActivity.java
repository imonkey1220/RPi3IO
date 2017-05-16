/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tw.imonkey.androidthings.rpi3io;

import android.app.Activity;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import de.greenrobot.event.EventBus;

public class MainActivity extends Activity {
    Handler mHandler = new Handler();//blink
    boolean mLedState = false;//blink

    //GPIO: BCM12, BCM13, BCM16, BCM17, BCM18, BCM19, BCM20, BCM21, BCM22, BCM23, BCM24, BCM25, BCM26, BCM27, BCM4, BCM5, BCM6
    String PiGPIO[]={"BCM4","BCM17","BCM27","BCM22","BCM5","BCM6","BCM13","BCM19",
            "BCM18","BCM23","BCM24","BCM25","BCM12","BCM16","BCM20","BCM21"};
    String GPIOName[]={"X00","X01","X02","X03","X04","X05","X06","X07",
            "Y00","Y01","Y02","Y03","Y04","Y05","Y06","Y07"};
    private Gpio[] GPIO=new Gpio[16];
    private Gpio RESETGpio;
    String RESET="BCM26";
    private Map<String,Gpio> GPIOMap=new HashMap<>();

    DatabaseReference mIOLive,mLog, mXINPUT,mYOUTPUT,mFriend,presenceRef,lastOnlineRef,connectedRef,connectedRefF;
    Map<String, Object> input = new HashMap<>();
    Map<String, Object> log = new HashMap<>();
    String memberEmail,deviceId;
    public static final String devicePrefs = "devicePrefs";
    Map<String, Object> alert = new HashMap<>();

    public MySocketServer mServer;
    private static final int SERVER_PORT = 9402;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));
        EventBus.getDefault().register(this);
        SharedPreferences settings = getSharedPreferences(devicePrefs, Context.MODE_PRIVATE);
        memberEmail = settings.getString("memberEmail", null);
        deviceId = settings.getString("deviceId", null);

        if (memberEmail == null) {
            memberEmail = "test@po-pp.com";
            deviceId = "RPI3IOtest";
            startServer();
            blinkTest();
        }
            init();
            deviceOnline();

        mLog=FirebaseDatabase.getInstance().getReference("/LOG/GPIO/" + deviceId+"/LOG/");
        mXINPUT = FirebaseDatabase.getInstance().getReference("/LOG/GPIO/" + deviceId+"/X/");
        mYOUTPUT = FirebaseDatabase.getInstance().getReference("/LOG/GPIO/" + deviceId+"/Y/");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mBlinkRunnable);
        EventBus.getDefault().unregister(this);
        if ( RESETGpio != null) {
            try {
                RESETGpio.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                RESETGpio = null;
            }
        }
        for(String s:GPIOMap.keySet()) {
            if (GPIOMap.get(s) != null) {
                try {
                    GPIOMap.get(s).close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    GPIOMap.remove(s);
                }
            }
        }
    }

    private  void init(){
        PeripheralManagerService service = new PeripheralManagerService();
        try {
            RESETGpio = service.openGpio(RESET);
            RESETGpio.setDirection(Gpio.DIRECTION_IN);
            RESETGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            RESETGpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {

                    try {
                        if (RESETGpio.getValue()){
                            startServer();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i=0;i<16;i++) {
            final int index = i;
            // X00->INPUT
            if (GPIOName[i].charAt(0) == 'X') {
                try {
                    GPIO[index] = service.openGpio(PiGPIO[index]);
                    GPIO[index].setDirection(Gpio.DIRECTION_IN);
                    GPIO[index].setEdgeTriggerType(Gpio.EDGE_BOTH);
                    GPIO[index].registerGpioCallback(new GpioCallback() {
                        @Override
                        public boolean onGpioEdge(Gpio gpio) {

                            try {
                                input.clear();
                                input.put(GPIOName[index], GPIO[index].getValue());
                                input.put("memberEmail", memberEmail);
                                input.put("timeStamp", ServerValue.TIMESTAMP);
                                mXINPUT.push().setValue(input);
                                alert(GPIOName[index]+":"+GPIO[index].getValue());
                                log(GPIOName[index]+":"+GPIO[index].getValue());

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return true;
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // Y00->OUTPUT
            else if (GPIOName[i].charAt(0) == 'Y') {
                try {
                    GPIO[index] = service.openGpio(PiGPIO[index]);
                    GPIO[index].setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
                    GPIO[index].setValue(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            GPIOMap.put(GPIOName[index], GPIO[index]);
        }

        mYOUTPUT.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                for(String OutputPin :GPIOMap.keySet()) {
                    if (dataSnapshot.child(OutputPin).getValue() != null) {
                        if (dataSnapshot.child(OutputPin).getValue().equals(true)) {
                            try {
                                GPIOMap.get(OutputPin).setValue(true);
                                alert(OutputPin+":"+true);
                                log(OutputPin+":"+true);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                GPIOMap.get(OutputPin).setValue(false);
                                alert(OutputPin+":"+false);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    // websocket server
    private void startServer() {
        InetAddress inetAddress = getInetAddress();
        if (inetAddress == null) {
            return;
        }

        mServer = new MySocketServer(new InetSocketAddress(inetAddress.getHostAddress(), SERVER_PORT));
        mServer.start();
    }

    private static InetAddress getInetAddress() {
        try {
            for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface networkInterface = (NetworkInterface) en.nextElement();

                for (Enumeration enumIpAddr = networkInterface.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();

                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return null;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(SocketMessageEvent event) {
        String message = event.getMessage();
        String[] mArray = message.split(",");
        if (mArray.length==2) {
            SharedPreferences.Editor editor = getSharedPreferences(devicePrefs, Context.MODE_PRIVATE).edit();
            editor.putString("memberEmail", mArray[0]);
            editor.putString("deviceId", mArray[1]);
            editor.apply();
            mServer.sendMessage("echo: " + message);
            Intent i;
            i = new Intent(this,MainActivity.class);
            startActivity(i);
        }
    }

    //device online check
    private void deviceOnline(){
        mIOLive=FirebaseDatabase.getInstance().getReference("/LOG/GPIO/"+deviceId+"/connection");//for log activity
        mIOLive.setValue(true);
        mIOLive.onDisconnect().setValue(null);
        presenceRef = FirebaseDatabase.getInstance().getReference("/FUI/"+memberEmail.replace(".", "_")+"/"+deviceId+"/connection");
        presenceRef.setValue(true);
        presenceRef.onDisconnect().setValue(null);
        lastOnlineRef =FirebaseDatabase.getInstance().getReference("/FUI/"+memberEmail.replace(".", "_")+"/"+deviceId+"/lastOnline");
        lastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP);
        connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    presenceRef.setValue(true);
                    mIOLive.setValue(true);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
        mFriend= FirebaseDatabase.getInstance().getReference("/DEVICE/"+deviceId+"/friend");
        mFriend.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                final DatabaseReference  presenceRefF= FirebaseDatabase.getInstance().getReference("/FUI/"+childSnapshot.getValue().toString().replace(".", "_")+"/"+deviceId+"/connection");
                    presenceRefF.setValue(true);
                    presenceRefF.onDisconnect().setValue(null);
                    connectedRefF = FirebaseDatabase.getInstance().getReference(".info/connected");
                    connectedRefF.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            Boolean connected = snapshot.getValue(Boolean.class);
                            if (connected) {
                                presenceRefF.setValue(true);
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {
                        }
                    });
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void alert(String message){
        NotifyUser.topicsPUSH(deviceId,memberEmail,"智慧機通知",message);
        NotifyUser.IIDPUSH(deviceId,memberEmail,"智慧機通知",message);
        NotifyUser.emailPUSH(deviceId,memberEmail,message);
        NotifyUser.SMSPUSH(deviceId,memberEmail,message);

        DatabaseReference mAlertMaster= FirebaseDatabase.getInstance().getReference("/FUI/"+memberEmail.replace(".", "_")+"/"+deviceId+"/alert");
        alert.clear();
        alert.put("message",message);
        alert.put("timeStamp", ServerValue.TIMESTAMP);
        mAlertMaster.setValue(alert);
        DatabaseReference mFriend= FirebaseDatabase.getInstance().getReference("/DEVICE/"+deviceId+"/friend");
        mFriend.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    DatabaseReference mAlertFriend= FirebaseDatabase.getInstance().getReference("/FUI/"+childSnapshot.getValue().toString().replace(".", "_")+"/"+deviceId+"/alert");
                    mAlertFriend.setValue(alert);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }
    private void log(String message) {
        log.clear();
        log.put("message", message);
        log.put("memberEmail", memberEmail);
        log.put("timeStamp", ServerValue.TIMESTAMP);
        mLog.push().setValue(log);
    }
//blink test
    private void blinkTest(){
        // Post a Runnable that continuously switch the state of the GPIO, blinking the
        // corresponding LED
        mHandler.post(mBlinkRunnable);
    }

    private Runnable mBlinkRunnable = new Runnable() {
        @Override
        public void run() {
            // Exit Runnable if the GPIO is already closed
            if (GPIO[8] == null) {
                return;
            }
            try {
                // Toggle the GPIO state
                mLedState = !mLedState;
                GPIO[8].setValue(mLedState);
                Log.d("blink", "State set to " + mLedState);

                // Reschedule the same runnable in {#INTERVAL_BETWEEN_BLINKS_MS} milliseconds
                mHandler.postDelayed(mBlinkRunnable, 1000);
            } catch (IOException e) {
                Log.e("blink", "Error on PeripheralIO API", e);
            }
        }
    };
}

