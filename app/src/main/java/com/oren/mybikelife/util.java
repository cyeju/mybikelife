package com.oren.mybikelife;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import com.oren.xml.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class util {
    public static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault());
    public static String dataPath = null;
//    public static String appRootPath = MainActivity.class..getFilesDir().getAbsolutePath();
    public static boolean saveHistory(Activity a, Element el) {
//        File file = new File(Environment.getDataDirectory().getAbsolutePath() ); //+ File.separator + "bikelife"
//        if(!file.exists()) file.mkdir();
//        file = new File(file.getAbsolutePath()+ File.separator + "data");
//        if(!file.exists()) file.mkdir();
        dataPath = a.getFilesDir().getAbsolutePath()+ File.separator;
        FileOutputStream out;
        try {
            long tms = Long.parseLong(el.gAttrValue("startTimeMillis"));
            out = new FileOutputStream(dataPath + "hisdata_"+timeFormat.format(tms)+".xml");
            el.gDoc().xmlToOutputStream(out);
            out.close();
            return true;
        } catch (Exception e) {
            mailToMe(a, "Debug Report to Developer",e.toString());
            return false;
        }
    }

    public static void mailToMe(final Activity a, final String title, final String str) {
        AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(title);
        builder.setMessage("오류가 났습니다. \n 개발자에게 오류내용을 보내주시면 오류을 해결하는데 도움이 됩니다. \n\n"+str);
        builder.setCancelable(true);
        builder.setPositiveButton(a.getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, title);
                intent.putExtra(Intent.EXTRA_TEXT, str);
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"cjseok@gmail.com"});
                a.startActivity(intent);
            }
        });
        builder.setNegativeButton(a.getString(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }
}
