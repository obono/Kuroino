/*
 * Copyright (C) 2012 OBN-soft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.obnsoft.kuroino;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.AlertDialog;
import android.app.Application;
import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.format.DateFormat;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

public class MyApplication extends Application {

    private static final String FILENAME_WORK   = "work.csv";
    private static final String FILENAME_UPLOAD = "rollbook.csv";
    private static final String EXT_FILEPATH_UPLOAD =
        ".RollBook" + File.separator + FILENAME_UPLOAD;

    private SheetData mData;

    public MyApplication() {
        super();
        mData = new SheetData();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mData.cellSize = (int) (48f * getResources().getDisplayMetrics().density);
        mData.fileEncode = getText(R.string.file_encoding).toString();
        try {
            mData.importDataFromFile(openFileInput(FILENAME_WORK));
        } catch (FileNotFoundException e) {
            Calendar calFrom = new GregorianCalendar();
            int year = calFrom.get(Calendar.YEAR);
            int month = calFrom.get(Calendar.MONTH);
            calFrom.clear();
            calFrom.set(year, month, 1);
            Calendar calTo = new GregorianCalendar(
                    year, month, calFrom.getActualMaximum(Calendar.DAY_OF_MONTH));
            mData.createNewData(calFrom, calTo, 1, null,
                    getResources().getStringArray(R.array.sample_members));
        }
        cleanSheetDataToUpload();
    }

    @Override
    public void onTerminate() {
        cleanSheetDataToUpload();
        super.onTerminate();
    }

    protected SheetData getSheetData() {
        return mData;
    }

    protected boolean saveSheetData() {
        boolean ret = false;
        try {
            ret = mData.exportDataToFile(openFileOutput(FILENAME_WORK, MODE_PRIVATE));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return ret;
    }

    protected Intent prepareSheetDataToUpload() {
        Intent intent = null;
        boolean isExternal = isMountedExternalStorage();
        File file = getUploadFile(isExternal);
        FileOutputStream stream = null;
        try {
            if (isExternal) {
                file.getParentFile().mkdirs();
                stream = new FileOutputStream(file.getPath());
            } else {
                stream = openFileOutput(file.getName(), MODE_WORLD_READABLE);
            }
            if (stream != null && mData.exportDataToFile(stream)) {
                intent = new Intent(Intent.ACTION_SEND);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setType("text/comma-separated-values");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                if (!isExternal) {
                    showToast(this, R.string.msg_uploadcaution);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return intent;
    }

    protected void cleanSheetDataToUpload() {
        File file = getUploadFile(false);
        file.delete();
        if (isMountedExternalStorage()) {
            file = getUploadFile(true);
            file.delete();
            file.getParentFile().delete();
        }
    }

    private File getUploadFile(boolean isExternal) {
        if (isExternal) {
            return new File(Environment.getExternalStorageDirectory(), EXT_FILEPATH_UPLOAD);
        }
        return getFileStreamPath(FILENAME_UPLOAD);
    }

    private boolean isMountedExternalStorage() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /*----------------------------------------------------------------------*/

    public static CharSequence getDateString(Context context, Calendar cal) {
        return DateFormat.getDateFormat(context).format(cal.getTime());
    }

    public static final char IDEOGRAPHICS_SPACE = 0x3000;

    public static String trimUni(String s){
        int len = s.length();
        int st = 0;
        char[] val = s.toCharArray();

        while (st < len && (val[st] <= ' ' || val[st] == IDEOGRAPHICS_SPACE)) {
            st++;
        }
        while (st < len && (val[len - 1] <= ' ' || val[len - 1] == IDEOGRAPHICS_SPACE)) {
            len--;
        }
        return (st > 0 || len < s.length()) ? s.substring(st, len) : s;
    }

    public static void showDatePickerDialog(
            Context context, Calendar cal, DatePickerDialog.OnDateSetListener listener) {
        DatePickerDialog dlg = new DatePickerDialog(context, listener,
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dlg.setIcon(R.drawable.ic_dialog_date);
        dlg.show();
    }

    public static void showYesNoDialog(
            Context context, int iconId, int titleId, int msgId,
            DialogInterface.OnClickListener listener) {
        showYesNoDialog(context, iconId, context.getText(titleId), msgId, listener);
    }

    public static void showYesNoDialog(
            Context context, int iconId, CharSequence title, int msgId,
            DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(context)
                .setIcon(iconId)
                .setTitle(title)
                .setMessage(msgId)
                .setPositiveButton(android.R.string.yes, listener)
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    public static void showShareDialog(
            final Context context, int iconId, CharSequence title, CharSequence msg) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, title.toString() + "\n\n" + msg);
        new AlertDialog.Builder(context)
                .setIcon(iconId)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.button_share, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            context.startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            showToast(context, R.string.msg_error);
                        }
                    }
                })
                .show();
    }

    public static void showSingleChoiceDialog(
            Context context, int iconId, int titleId, String[] items,
            int choice, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(context)
                .setIcon(iconId)
                .setTitle(titleId)
                .setSingleChoiceItems(items, choice, listener)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static void showMultiChoiceDialog(
            Context context, int iconId, int titleId, String[] items,
            final boolean[] choices, DialogInterface.OnClickListener listener) {
        DialogInterface.OnMultiChoiceClickListener
        listener2 = new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                choices[which] = isChecked;
            }
        };
        new AlertDialog.Builder(context)
                .setIcon(iconId)
                .setTitle(titleId)
                .setMultiChoiceItems(items, choices, listener2)
                .setPositiveButton(android.R.string.ok, listener)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static void showCustomDialog(
            Context context, int iconId, int titleId, View view,
            DialogInterface.OnClickListener listener) {
        final AlertDialog dlg = new AlertDialog.Builder(context)
                .setIcon(iconId)
                .setTitle(titleId)
                .setView(view)
                .setPositiveButton(android.R.string.ok, listener)
                .create();
        if (listener != null) {
            dlg.setButton(AlertDialog.BUTTON_NEGATIVE, context.getText(android.R.string.cancel),
                    (DialogInterface.OnClickListener) null);
        }
        if (view instanceof EditText) {
            view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        dlg.getWindow().setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    }
                }
            });
        }
        dlg.show();
    }

    public static void showToast(Context context, int msgId) {
        Toast.makeText(context, msgId, Toast.LENGTH_SHORT).show();
    }

    /*----------------------------------------------------------------------*/

}
