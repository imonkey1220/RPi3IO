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

public class GPIOTESTActivity extends Activity {
    private Gpio RESETGpio, mX00Gpio,mX01Gpio,mX02Gpio,mX03Gpio,mX04Gpio,mX05Gpio,mX06Gpio,mX07Gpio,
            mY00Gpio,mY01Gpio,mY02Gpio,mY03Gpio,mY04Gpio,mY05Gpio,mY06Gpio,mY07Gpio;
    DatabaseReference  mXINPUT,mYOUTPUT,mFriend,presenceRef,lastOnlineRef,connectedRef,connectedRefF;
    Map<String, Object> input = new HashMap<>();
    Map<String, Object> alert = new HashMap<>();
    Map<String, Object> log = new HashMap<>();
    Map<String,String> pinName = new HashMap<>();
    String memberEmail,deviceId;
    public static final String devicePrefs = "devicePrefs";

    public MySocketServer mServer;
    private static final int SERVER_PORT = 9402;

    DatabaseReference mLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));
        //GPIO: BCM12, BCM13, BCM16, BCM17, BCM18, BCM19, BCM20, BCM21, BCM22, BCM23, BCM24, BCM25, BCM26, BCM27, BCM4, BCM5, BCM6
        pinName.put("RESET","BCM26");
        pinName.put("X00","BCM4");
        pinName.put("X01","BCM17");
        pinName.put("X02","BCM27");
        pinName.put("X03","BCM22");
        pinName.put("X04","BCM5");
        pinName.put("X05","BCM6");
        pinName.put("X06","BCM13");
        pinName.put("X07","BCM19");

        pinName.put("Y00","BCM18");
        pinName.put("Y01","BCM23");
        pinName.put("Y02","BCM24");
        pinName.put("Y03","BCM25");
        pinName.put("Y04","BCM12");
        pinName.put("Y05","BCM16");
        pinName.put("Y06","BCM20");
        pinName.put("Y07","BCM21");

        EventBus.getDefault().register(this);
        SharedPreferences settings = getSharedPreferences(devicePrefs, Context.MODE_PRIVATE);
        memberEmail = settings.getString("memberEmail", null);
        deviceId = settings.getString("deviceId", null);

        if (memberEmail == null) {
            memberEmail = "RPI3IO@test.com";
            deviceId = "RPI3IOtest";
            startServer();
        }else{
            init();
            deviceOnline();
        }
        mXINPUT = FirebaseDatabase.getInstance().getReference("/LOG/GPIO/" + deviceId+"/X/");
        mYOUTPUT = FirebaseDatabase.getInstance().getReference("/LOG/GPIO/" + deviceId+"/Y/");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        if (mY00Gpio != null) {
            try {
                mY00Gpio.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mY00Gpio = null;
            }
        }

        if (mY01Gpio != null) {
            try {
                mY01Gpio.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mY01Gpio = null;
            }
        }

        if (mY02Gpio != null) {
            try {
                mY02Gpio.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mY02Gpio = null;
            }
        }

        if (mY03Gpio != null) {
            try {
                mY03Gpio.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mY03Gpio = null;
            }
        }

        if (mY04Gpio != null) {
            try {
                mY04Gpio.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mY04Gpio = null;
            }
        }

        if (mY05Gpio != null) {
            try {
                mY05Gpio.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mY05Gpio = null;
            }
        }

        if (mY06Gpio != null) {
            try {
                mY06Gpio.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mY06Gpio = null;
            }
        }

        if (mY07Gpio != null) {
            try {
                mY07Gpio.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mY07Gpio = null;
            }
        }


        if (mX00Gpio != null) {
            try {
                mX00Gpio.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mX00Gpio = null;
            }
        }

        if (mX01Gpio != null) {
            try {
                mX01Gpio.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mX01Gpio = null;
            }
        }
        if (mX02Gpio != null) {
            try {
                mX02Gpio.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mX02Gpio = null;
            }
        }

        if (mX03Gpio != null) {
            try {
                mX03Gpio.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mX03Gpio = null;
            }
        }
        if (mX04Gpio != null) {
            try {
                mX04Gpio.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mX04Gpio = null;
            }
        }

        if (mX05Gpio != null) {
            try {
                mX05Gpio.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mX05Gpio = null;
            }
        }
        if (mX06Gpio != null) {
            try {
                mX06Gpio.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mX06Gpio = null;
            }
        }

        if (mX07Gpio != null) {
            try {
                mX07Gpio.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mX07Gpio = null;
            }
        }
    }

    private  void init(){
        PeripheralManagerService service = new PeripheralManagerService();

        try {
            RESETGpio = service.openGpio(pinName.get("RESET"));
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
        try {
            mY00Gpio = service.openGpio(pinName.get("Y00"));
            mY00Gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mY00Gpio.setValue(false);
        } catch (IOException e) {
            e.printStackTrace();
            alert("Y_output:"+memberEmail+"->Y00=Err");
        }

        try {

            mY01Gpio = service.openGpio(pinName.get("Y01"));
            mY01Gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mY01Gpio.setValue(false);
        } catch (IOException e) {
            e.printStackTrace();
            alert("Y_output:"+memberEmail+"->Y01=Err");
        }

        try {
            mY02Gpio = service.openGpio(pinName.get("Y02"));
            mY02Gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mY02Gpio.setValue(false);
        } catch (IOException e) {
            e.printStackTrace();
            alert("Y_output:"+memberEmail+"->Y02=Err");
        }

        try {
            mY03Gpio = service.openGpio(pinName.get("Y03"));
            mY03Gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mY03Gpio.setValue(false);
        } catch (IOException e) {
            e.printStackTrace();
            alert("Y_output:"+memberEmail+"->Y03=Err");
        }

        try {
            mY04Gpio = service.openGpio(pinName.get("Y04"));
            mY04Gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mY04Gpio.setValue(false);
        } catch (IOException e) {
            e.printStackTrace();
            alert("Y_output:"+memberEmail+"->Y04=Err");
        }

        try {
            mY05Gpio = service.openGpio(pinName.get("Y05"));
            mY05Gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mY05Gpio.setValue(false);
        } catch (IOException e) {
            e.printStackTrace();
            alert("Y_output:"+memberEmail+"->Y05=Err");
        }

        try {
            mY06Gpio = service.openGpio(pinName.get("Y06"));
            mY06Gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mY06Gpio.setValue(false);
        } catch (IOException e) {
            e.printStackTrace();
            alert("Y_output:"+memberEmail+"->Y06=Err");
        }

        try {
            mY07Gpio = service.openGpio(pinName.get("Y07"));
            mY07Gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mY07Gpio.setValue(false);
        } catch (IOException e) {
            e.printStackTrace();
            alert("Y_output:"+memberEmail+"->Y07=Err");
        }


        try {
            mX00Gpio = service.openGpio(pinName.get("X00"));
            mX00Gpio.setDirection(Gpio.DIRECTION_IN);
            mX00Gpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            mX00Gpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {

                    try {
                        input.clear();
                        input.put("X00", mX00Gpio.getValue());
                        input.put("memberEmail",memberEmail);
                        input.put("timeStamp", ServerValue.TIMESTAMP);
                        mXINPUT.push().setValue(input);
                        alert("X_input:"+memberEmail+"->X00="+mX00Gpio.getValue());
                        log("X_input:"+memberEmail+"->X00="+mX00Gpio.getValue());
                    } catch (IOException e) {
                        e.printStackTrace();
                        alert("X_input:"+memberEmail+"->X00=Err");
                    }
                    return true;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            alert("X_input:"+memberEmail+"->X00=Err");
        }

        try {
            mX01Gpio = service.openGpio(pinName.get("X01"));
            mX01Gpio.setDirection(Gpio.DIRECTION_IN);
            mX01Gpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            mX01Gpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {

                    try {
                        input.clear();
                        input.put("X01", mX01Gpio.getValue());
                        input.put("memberEmail",memberEmail);
                        input.put("timeStamp", ServerValue.TIMESTAMP);
                        mXINPUT.push().setValue(input);
                        log("X_input:"+memberEmail+"->X00="+mX01Gpio.getValue());
                        alert("X_input:"+memberEmail+"->X01="+mX01Gpio.getValue());
                    } catch (IOException e) {
                        e.printStackTrace();
                        alert("X_input:"+memberEmail+"->X01=Err");
                    }
                    return true;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            alert("X_input:"+memberEmail+"->X01=Err");
        }


        try {
            mX02Gpio = service.openGpio(pinName.get("X02"));
            mX02Gpio.setDirection(Gpio.DIRECTION_IN);
            mX02Gpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            mX02Gpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {

                    try {
                        input.clear();
                        input.put("X02", mX02Gpio.getValue());
                        input.put("memberEmail",memberEmail);
                        input.put("timeStamp", ServerValue.TIMESTAMP);
                        mXINPUT.push().setValue(input);
                        alert("X_input:"+memberEmail+"->X02="+mX02Gpio.getValue());
                        log("X_input:"+memberEmail+"->X02="+mX02Gpio.getValue());
                    } catch (IOException e) {
                        e.printStackTrace();
                        alert("X_input:"+memberEmail+"->X02=Err");
                    }
                    return true;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            alert("X_input:"+memberEmail+"->X02=Err");
        }

        try {
            mX03Gpio = service.openGpio(pinName.get("X03"));
            mX03Gpio.setDirection(Gpio.DIRECTION_IN);
            mX03Gpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            mX03Gpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {

                    try {
                        input.clear();
                        input.put("X03", mX03Gpio.getValue());
                        input.put("memberEmail",memberEmail);
                        input.put("timeStamp", ServerValue.TIMESTAMP);
                        mXINPUT.push().setValue(input);
                        alert("X_input:"+memberEmail+"->X03="+mX03Gpio.getValue());
                        log("X_input:"+memberEmail+"->X03="+mX03Gpio.getValue());
                    } catch (IOException e) {
                        e.printStackTrace();
                        alert("X_input:"+memberEmail+"->X03=Err");
                    }
                    return true;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            alert("X_input:"+memberEmail+"->X03=Err");
        }

        try {
            mX04Gpio = service.openGpio(pinName.get("X04"));
            mX04Gpio.setDirection(Gpio.DIRECTION_IN);
            mX04Gpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            mX04Gpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {

                    try {
                        input.clear();
                        input.put("X04", mX04Gpio.getValue());
                        input.put("memberEmail",memberEmail);
                        input.put("timeStamp", ServerValue.TIMESTAMP);
                        mXINPUT.push().setValue(input);
                        alert("X_input:"+memberEmail+"->X04="+mX04Gpio.getValue());
                        log("X_input:"+memberEmail+"->X04="+mX04Gpio.getValue());
                    } catch (IOException e) {
                        e.printStackTrace();
                        alert("X_input:"+memberEmail+"->X04=Err");
                    }
                    return true;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            alert("X_input:"+memberEmail+"->X04=Err");
        }

        try {
            mX05Gpio = service.openGpio(pinName.get("X05"));
            mX05Gpio.setDirection(Gpio.DIRECTION_IN);
            mX05Gpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            mX05Gpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {

                    try {
                        input.clear();
                        input.put("X05", mX01Gpio.getValue());
                        input.put("memberEmail",memberEmail);
                        input.put("timeStamp", ServerValue.TIMESTAMP);
                        mXINPUT.push().setValue(input);
                        alert("X_input:"+memberEmail+"->X05="+mX06Gpio.getValue());
                        log("X_input:"+memberEmail+"->X05="+mX06Gpio.getValue());
                    } catch (IOException e) {
                        e.printStackTrace();
                        alert("X_input:"+memberEmail+"->X05=Err");
                    }
                    return true;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            alert("X_input:"+memberEmail+"->X05=Err");
        }


        try {
            mX06Gpio = service.openGpio(pinName.get("X06"));
            mX06Gpio.setDirection(Gpio.DIRECTION_IN);
            mX06Gpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            mX06Gpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {

                    try {
                        input.clear();
                        input.put("X06", mX06Gpio.getValue());
                        input.put("memberEmail",memberEmail);
                        input.put("timeStamp", ServerValue.TIMESTAMP);
                        mXINPUT.push().setValue(input);
                        alert("X_input:"+memberEmail+"->X06="+mX06Gpio.getValue());
                        log("X_input:"+memberEmail+"->X06="+mX06Gpio.getValue());
                    } catch (IOException e) {
                        e.printStackTrace();
                        alert("X_input:"+memberEmail+"->X06=Err");
                    }
                    return true;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            alert("X_input:"+memberEmail+"->X06=Err");
        }

        try {
            mX07Gpio = service.openGpio(pinName.get("X07"));
            mX07Gpio.setDirection(Gpio.DIRECTION_IN);
            mX07Gpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            mX07Gpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {

                    try {
                        input.clear();
                        input.put("X07", mX07Gpio.getValue());
                        input.put("memberEmail",memberEmail);
                        input.put("timeStamp", ServerValue.TIMESTAMP);
                        mXINPUT.push().setValue(input);
                        alert("X_input:"+memberEmail+"->X07="+mX07Gpio.getValue());
                        log("X_input:"+memberEmail+"->X07="+mX07Gpio.getValue());
                    } catch (IOException e) {
                        e.printStackTrace();
                        alert("X_input:"+memberEmail+"->X07=Err");
                    }
                    return true;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            alert("X_input:"+memberEmail+"->X07=Err");
        }
        mYOUTPUT.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.child("Y00").getValue() != null) {
                    if (dataSnapshot.child("Y00").getValue().equals(true)) {
                        try {
                            mY00Gpio.setValue(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                            alert("Y_output:"+memberEmail+"->Y00=Err");
                        }
                    } else {
                        try {
                            mY00Gpio.setValue(false);
                        } catch (IOException e) {
                            e.printStackTrace();
                            alert("Y_output:"+memberEmail+"->Y00=Err");
                        }
                    }
                }
                if (dataSnapshot.child("Y01").getValue() != null) {
                    if (dataSnapshot.child("Y01").getValue().equals(true)) {
                        try {
                            mY01Gpio.setValue(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                            alert("Y_output:"+memberEmail+"->Y01=Err");
                        }
                    } else {
                        try {
                            mY01Gpio.setValue(false);
                        } catch (IOException e) {
                            e.printStackTrace();
                            alert("Y_output:"+memberEmail+"->Y01=Err");
                        }
                    }
                }
                if (dataSnapshot.child("Y02").getValue() != null) {
                    if (dataSnapshot.child("Y02").getValue().equals(true)) {
                        try {
                            mY02Gpio.setValue(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                            alert("Y_output:"+memberEmail+"->Y02=Err");
                        }
                    } else {
                        try {
                            mY02Gpio.setValue(false);
                        } catch (IOException e) {
                            e.printStackTrace();
                            alert("Y_output:"+memberEmail+"->Y02=Err");
                        }
                    }
                }
                if (dataSnapshot.child("Y03").getValue() != null) {
                    if (dataSnapshot.child("Y03").getValue().equals(true)) {
                        try {
                            mY03Gpio.setValue(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                            alert("Y_output:"+memberEmail+"->Y03=Err");
                        }
                    } else {
                        try {
                            mY03Gpio.setValue(false);
                        } catch (IOException e) {
                            e.printStackTrace();
                            alert("Y_output:"+memberEmail+"->Y03=Err");
                        }
                    }
                }

                if (dataSnapshot.child("Y04").getValue() != null) {
                    if (dataSnapshot.child("Y04").getValue().equals(true)) {
                        try {
                            mY04Gpio.setValue(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                            alert("Y_output:"+memberEmail+"->Y04=Err");
                        }
                    } else {
                        try {
                            mY04Gpio.setValue(false);
                        } catch (IOException e) {
                            e.printStackTrace();
                            alert("Y_output:"+memberEmail+"->Y04=Err");
                        }
                    }
                }
                if (dataSnapshot.child("Y05").getValue() != null) {
                    if (dataSnapshot.child("Y05").getValue().equals(true)) {
                        try {
                            mY05Gpio.setValue(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                            alert("Y_output:"+memberEmail+"->Y05=Err");
                        }
                    } else {
                        try {
                            mY05Gpio.setValue(false);
                        } catch (IOException e) {
                            e.printStackTrace();
                            alert("Y_output:"+memberEmail+"->Y05=Err");
                        }
                    }
                }
                if (dataSnapshot.child("Y06").getValue() != null) {
                    if (dataSnapshot.child("Y06").getValue().equals(true)) {
                        try {
                            mY06Gpio.setValue(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                            alert("Y_output:"+memberEmail+"->Y06=Err");
                        }
                    } else {
                        try {
                            mY06Gpio.setValue(false);
                        } catch (IOException e) {
                            e.printStackTrace();
                            alert("Y_output:"+memberEmail+"->Y06=Err");
                        }
                    }
                }
                if (dataSnapshot.child("Y07").getValue() != null) {
                    if (dataSnapshot.child("Y07").getValue().equals(true)) {
                        try {
                            mY07Gpio.setValue(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                            alert("Y_output:"+memberEmail+"->Y07=Err");
                        }
                    } else {
                        try {
                            mY07Gpio.setValue(false);
                        } catch (IOException e) {
                            e.printStackTrace();
                            alert("Y_output:"+memberEmail+"->Y07=Err");
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
            i = new Intent(this,GPIOTESTActivity.class);
            startActivity(i);
        }
    }

    //device online check
    private void deviceOnline(){
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
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
        mFriend= FirebaseDatabase.getInstance().getReference("/devices/"+memberEmail.replace(".", "_")+"/"+deviceId+"/friend");
        mFriend.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    final DatabaseReference  presenceRefF= FirebaseDatabase.getInstance().getReference("/friend/"+childSnapshot.getValue().toString().replace(".", "_")+"/"+deviceId+"/connection");
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
}

