package com.oren.mybikelife.data;

import android.app.Activity;
import com.oren.mybikelife.util;
import com.oren.util.bluetooth.UseGattAttributes;
import com.oren.xml.Document;
import com.oren.xml.Element;
import com.oren.xml.XmlParser;

import java.io.File;
import java.io.FileOutputStream;

public class Config {
    private static Element elRoot;
    private static int newBikeId = 0;
    public static Element selectedElBike;
    public static float selectWeelSize = 0;
    public static Bike selectedBike;
    private static String selectedId = "0";

    private static void createConfig() {
        elRoot = new Element("bikelife");
        Element elusers = elRoot.aChild("users");
        elusers.aChild("user").sAttr("id","anonymous");
        Element elbikes = elRoot.aChild("bikes").sAttr("selectedId","0");
        elbikes.aChild("bike").sAttr("name","My bicycle").sAttr("id", "0").sAttr("wheelType", "25-622").sAttr("wheelDes","-(700x25C)").sAttr("wheelSize","672")
                .sAttr("photo", ""); //.sAttr("deviceName","").sAttr("deviceAddress","").sAttr("connectDevice","true");
        elbikes.gChild("bike").aChild("device").sAttr("deviceName","").sAttr("deviceAddress","").sAttr("connectDevice","true");
        saveConfig();
    }
    public static void loadConfig(Activity a) {
        if(util.dataPath == null) util.dataPath = a.getFilesDir().getAbsolutePath() + File.separator;
        File file = new File(util.dataPath+"config.xml");
        if(!file.exists()) {
            createConfig();
        } else {
            try {
                Document doc = new XmlParser().parse(file.getAbsolutePath());
                elRoot = doc.gRootElem();
            } catch (Exception ee) {
                android.util.Log.w("xy", ee.toString());
                createConfig();
            }
        }
        for(Element el : elRoot.gChild("bikes").gChildren()) {
            int id = Integer.parseInt(el.gAttrValue("id"));
            if(id > newBikeId) newBikeId = id+1;
        }
        setSelectedBike();
//        if(!"".equals(selectedBike.getDeviceAddress()) && selectedBike.getConnectDevice()) {
//            UseGattAttributes.connect(selectedBike.getDeviceAddress());
//        }
    }
    public static void setSelectedBike() {
        setSelectedBike(elRoot.gChild("bikes").gAttrValue("selectedId"));
    }
    public static void setSelectedBike(String id) {
        for(Element el : elRoot.gChild("bikes").gChildren()) {
            if(el.gAttrValue("id").equals(id)) {
                selectedElBike = el;
                android.util.Log.d("xy",el.gAttrValue("wheelType") + "::"+ el.gAttrValue("name"));
                if(selectedElBike.gAttr("deviceName") != null) {
                    Element elDevice = selectedElBike.aChild("device");
                    elDevice.sAttr("deviceName", el.gAttrValue("deviceName"));
                    elDevice.sAttr("deviceAddress", el.gAttrValue("deviceAddress"));
                    el.rAttr("deviceName");
                    el.rAttr("deviceAddress");
                    saveConfig();
                }
//                android.util.Log.d("xy", "11111????==> "+el.gAttrValue("connectDevice"));
                String strconn = el.gAttrValue("connectDevice");
                if(strconn == null) {
                    el.sAttr("connectDevice", "false");
                    strconn = "false";
                }

                selectedBike = new Bike(id, el.gAttrValue("name"),el.gAttrValue("wheelType")
                        ,el.gAttrValue("wheelDes"),Integer.parseInt(el.gAttrValue("wheelSize")),el.gAttrValue("photo"), Boolean.parseBoolean(strconn));
                selectWeelSize = selectedBike.getWheelSize(); //Float.parseFloat(el.gAttrValue("wheelSize"));
                String address=null, address1=null;
                if(selectedBike.getConnectDevice()) {
                    int ii = 0;
                    for(Element elDevice : selectedElBike.gChildren("device")) {
                        if(!elDevice.gAttrValue("deviceAddress").equals("")) {
                            if(ii == 0)
                                address =  elDevice.gAttrValue("deviceAddress");
                            else address1 =  elDevice.gAttrValue("deviceAddress");
                        }
                    }
                    ii++;
                    android.util.Log.d("xy",address + "::"+ address1);
                    UseGattAttributes.connect(address, address1);
                    break;
                }
            }
        }
    }
    public static void removeDevice(String id, Element device) {
        for(Element el : elRoot.gChild("bikes").gChildren()) {
            if(el.gAttrValue("id").equals(id)) {
                el.rChild(device);
                return;
            }
        }
    }
    public static void removeBike(String id) {
        for(Element el : elRoot.gChild("bikes").gChildren()) {
            if(el.gAttrValue("id").equals(id)) {
                elRoot.gChild("bikes").rChild(el);
                return;
            }
        }
    }
    public static Element getBike(String id) {
        for(Element el : elRoot.gChild("bikes").gChildren()) {
            if(el.gAttrValue("id").equals(id)) {
                return el;
            }
        }
        return null;
    }
    public static Element getBikes() {
        return  elRoot.gChild("bikes");
    }
    public static void modifySave(Bike bike) {
        for(Element el : elRoot.gChild("bikes").gChildren()) {
            if(el.gAttrValue("id").equals(selectedId)) {
                el.sAttr("name", bike.getName());
                el.sAttr("wheelType", bike.getWheelType());
                el.sAttr("wheelDes", bike.getWheelDes());
                el.sAttr("wheelSize", bike.getWheelSize()+"");
                el.sAttr("photo", bike.getPhoto());
                el.sAttr("connectDevice", bike.getConnectDevice() ? "true" : "false");
                android.util.Log.d("xy", "33333????==> "+bike.getConnectDevice()+"::::"+el.gAttrValue("connectDevice"));
                saveConfig();
                break;
            }
        }
    }
    public static void saveConfig() {
        FileOutputStream out;
        try {
            out = new FileOutputStream(util.dataPath+"config.xml");
            elRoot.gDoc().xmlToOutputStream(out);
            out.close();
        } catch (Exception e) {
            android.util.Log.w("xy", "saveConfig => "+e.toString());
        }
    }
    public static boolean isDevice(String deviceAddress) {
        for(Element elDevice : selectedElBike.gChildren("device")) {
            if(elDevice.gAttrValue("deviceAddress").equals(deviceAddress)) {
                return true;
            }
        }
        return false;
    }
}
