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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;

import com.obnsoft.kuroino.SheetData.EntryData;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int MENU_GID_OPTION    = 1;
    private static final int MENU_GID_HEADER    = 2;
    private static final int MENU_GID_SIDE      = 3;

    private static final int MENU_ID_ADDDATE        = 1;
    private static final int MENU_ID_MODIFYDATE     = 2;
    private static final int MENU_ID_DELETEDATE     = 3;
    private static final int MENU_ID_INFODATE       = 4;
    private static final int MENU_ID_ADDMEMBER      = 5;
    private static final int MENU_ID_MODIFYMEMBER   = 6;
    private static final int MENU_ID_MOVEUPMEMBER   = 7;
    private static final int MENU_ID_MOVEDOWNMEMBER = 8;
    private static final int MENU_ID_DELETEMEMBER   = 9;
    private static final int MENU_ID_INFOMEMBER     = 10;
    private static final int MENU_ID_INSERTMEMBER   = 11;
    private static final int MENU_ID_CREATE         = 12;
    private static final int MENU_ID_IMPORT         = 13;
    private static final int MENU_ID_EXPORT         = 14;
    private static final int MENU_ID_ABOUT          = 15;

    private static final int REQUEST_ID_CREATE = 1;
    private static final int REQUEST_ID_IMPORT = 2;
    private static final int REQUEST_ID_EXPORT = 3;

    private int mTargetRow = SheetData.POS_GONE;
    private int mTargetCol = SheetData.POS_GONE;

    private HeaderScrollView    mHeader;
    private SideScrollView      mSide;
    private SheetScrollView     mSheet;
    private SheetData           mData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mHeader = (HeaderScrollView) findViewById(R.id.view_head);
        mSide = (SideScrollView) findViewById(R.id.view_side);
        mSheet = (SheetScrollView) findViewById(R.id.view_main);

        registerForContextMenu(mHeader);
        registerForContextMenu(mSide);

        mData = ((MyApplication) getApplication()).getSheetData();
        mData.cellSize = (int) (48f * getResources().getDisplayMetrics().density);
        mData.fileEncode = getText(R.string.file_encoding).toString();

        Calendar calNow = new GregorianCalendar();
        int year = calNow.get(Calendar.YEAR);
        int month = calNow.get(Calendar.MONTH);
        int day = calNow.get(Calendar.DAY_OF_MONTH);
        calNow.clear();
        calNow.set(year, month, day);

        if ((new File(getLocalFileName())).exists()) {
            mData.importDataFromFile(getLocalFileName());
        } else {
            Calendar calFrom = new GregorianCalendar(year, month, 1);
            Calendar calTo = new GregorianCalendar(
                    year, month, calFrom.getActualMaximum(Calendar.DAY_OF_MONTH));
            mData.createNewData(calFrom, calTo, 1, null,
                    getResources().getStringArray(R.array.sample_members));
        }
        mHeader.setSheetData(mData);
        mSide.setSheetData(mData);
        mSheet.setSheetData(mData);

        int col = mData.searchDate(calNow, false);
        if (col >= mData.dates.size()) {
            col = mData.dates.size() - 1;
        }
        handleFocus(null, SheetData.POS_KEEP, col, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshViews();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (true) {
            mData.exportDataToFile(getLocalFileName());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean ret = super.onCreateOptionsMenu(menu);
        menu.add(MENU_GID_OPTION, MENU_ID_ADDDATE, Menu.NONE, R.string.menu_adddate)
            .setIcon(android.R.drawable.ic_menu_today);
        menu.add(MENU_GID_OPTION, MENU_ID_ADDMEMBER, Menu.NONE, R.string.menu_addmember)
            .setIcon(android.R.drawable.ic_menu_my_calendar);
        menu.add(MENU_GID_OPTION, MENU_ID_CREATE, Menu.NONE, R.string.menu_new)
            .setIcon(android.R.drawable.ic_menu_agenda);
        menu.add(MENU_GID_OPTION, MENU_ID_IMPORT, Menu.NONE, R.string.menu_import)
            .setIcon(android.R.drawable.ic_menu_set_as);
        menu.add(MENU_GID_OPTION, MENU_ID_EXPORT, Menu.NONE, R.string.menu_export)
            .setIcon(android.R.drawable.ic_menu_save);
        menu.add(MENU_GID_OPTION, MENU_ID_ABOUT, Menu.NONE, R.string.menu_version)
            .setIcon(android.R.drawable.ic_menu_info_details);
        return ret;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getGroupId() == MENU_GID_OPTION) {
            if (executeFunction(item.getItemId())) {
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v == mHeader) {
            menu.setHeaderTitle(MyApplication.getDateString(this, mData.dates.get(mTargetCol)));
            menu.add(MENU_GID_HEADER, MENU_ID_MODIFYDATE, Menu.NONE, R.string.menu_modify);
            menu.add(MENU_GID_HEADER, MENU_ID_DELETEDATE, Menu.NONE, R.string.menu_delete);
            menu.add(MENU_GID_HEADER, MENU_ID_INFODATE, Menu.NONE, R.string.menu_info);
            menu.add(MENU_GID_HEADER, MENU_ID_ADDDATE, Menu.NONE, R.string.menu_adddate);
        } else if (v == mSide) {
            menu.setHeaderTitle(mData.entries.get(mTargetRow).name);
            menu.add(MENU_GID_SIDE, MENU_ID_MODIFYMEMBER, Menu.NONE, R.string.menu_modify);
            if (mTargetRow > 0) {
                menu.add(MENU_GID_SIDE, MENU_ID_MOVEUPMEMBER, Menu.NONE, R.string.menu_moveup);
            }
            if (mTargetRow < mData.entries.size() - 1) {
                menu.add(MENU_GID_SIDE, MENU_ID_MOVEDOWNMEMBER, Menu.NONE, R.string.menu_movedown);
            }
            menu.add(MENU_GID_SIDE, MENU_ID_DELETEMEMBER, Menu.NONE, R.string.menu_delete);
            menu.add(MENU_GID_SIDE, MENU_ID_INFOMEMBER, Menu.NONE, R.string.menu_info);
            menu.add(MENU_GID_SIDE, MENU_ID_INSERTMEMBER, Menu.NONE, R.string.menu_insertmember);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == MENU_GID_HEADER ||
                item.getGroupId() == MENU_GID_SIDE) {
            if (executeFunction(item.getItemId())) {
                return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_ID_CREATE:
            if (resultCode == RESULT_OK) {
                handleFocus(null, SheetData.POS_GONE, SheetData.POS_GONE, false);
                mSheet.scrollTo(0, 0);
            }
            break;
        case REQUEST_ID_IMPORT:
            if (resultCode == RESULT_OK) {
                String path = data.getStringExtra(MyFilePickerActivity.INTENT_EXTRA_SELECTPATH);
                mData.importDataFromFile(path);
                refreshViews();
                handleFocus(null, SheetData.POS_GONE, SheetData.POS_GONE, false);
                mSheet.scrollTo(0, 0);
            }
            break;
        case REQUEST_ID_EXPORT:
            if (resultCode == RESULT_OK) {
                String path = data.getStringExtra(MyFilePickerActivity.INTENT_EXTRA_SELECTPATH);
                mData.exportDataToFile(path);
            }
            break;
        }
    }

    /*----------------------------------------------------------------------*/

    protected void handleFocus(View v, int row, int col, boolean scroll) {
        if (v != mHeader) {
            mHeader.setFocus(col, scroll);
        }
        if (v != mSide) {
            mSide.setFocus(row, scroll);
        }
        if (v != mSheet) {
            mSheet.setFocus(row, col);
        }
    }

    protected void handleMouseDown(View v) {
        if (v != mHeader) {
            mHeader.fling(0);
        }
        if (v != mSide) {
            mSide.fling(0);
        }
        if (v != mSheet) {
            mSheet.fling(0, 0);
        }
    }

    protected void handleClick(View v, int row, int col, boolean extra) {
        if (v == mHeader) {
            if (extra) {
                mTargetCol = col;
                openContextMenu(mHeader);
            } else {
                handleFocus(v, SheetData.POS_KEEP, col, false);
            }
        } else if (v == mSide) {
            if (extra) {
                mTargetRow = row;
                openContextMenu(mSide);
            } else {
                handleFocus(v, row, SheetData.POS_KEEP, false);
            }
        } else if (v == mSheet) {
            mTargetRow = row;
            mTargetCol = col;
            handleFocus(v, row, col, false);
            String[] symbolStrs = getResources().getStringArray(R.array.symbol_strings);
            ArrayList<String> attends = mData.entries.get(row).attends;
            if (symbolStrs.length == 0 || attends.size() - 1 < col) {
                return;
            }
            String attend = attends.get(col);
            if (attend == null) {
                attends.set(col, symbolStrs[0]);
            } else {
                boolean match = false;
                for (int i = 0; i < symbolStrs.length - 1; i++) {
                    if (symbolStrs[i].equals(attend)) {
                        attends.set(col, symbolStrs[i + 1]);
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    attends.set(col, null);
                }
            }
        }
    }

    protected void handleScroll(View v, int l, int t) {
        if (v == mHeader) {
            mSheet.scrollTo(l, mSheet.getScrollY());
        } else if (v == mSide) {
            mSheet.scrollTo(mSheet.getScrollX(), t);
        } else if (v == mSheet) {
            mHeader.scrollTo(l, 0);
            mSide.scrollTo(0, t);
        }
    }

    private boolean executeFunction(int menuId) {
        switch (menuId) {
        case MENU_ID_ADDDATE:
            addDate();
            return true;
        case MENU_ID_MODIFYDATE:
            modifyDate(mTargetCol);
            return true;
        case MENU_ID_DELETEDATE:
            deleteDate(mTargetCol);
            return true;
        case MENU_ID_INFODATE:
            showDateStats(mTargetCol);
            return true;
        case MENU_ID_ADDMEMBER:
            addMember(mData.entries.size());
            return true;
        case MENU_ID_MODIFYMEMBER:
            modifyMember(mTargetRow);
            return true;
        case MENU_ID_MOVEUPMEMBER:
            moveMember(mTargetRow, -1);
            return true;
        case MENU_ID_MOVEDOWNMEMBER:
            moveMember(mTargetRow, 1);
            return true;
        case MENU_ID_DELETEMEMBER:
            deleteMember(mTargetRow);
            return true;
        case MENU_ID_INFOMEMBER:
            showMemberStats(mTargetRow);
            return true;
        case MENU_ID_INSERTMEMBER:
            addMember(mTargetRow);
            return true;
        case MENU_ID_CREATE:
            startWizardActivity();
            return true;
        case MENU_ID_IMPORT:
            startFilePickerActivityToImport();
            return true;
        case MENU_ID_EXPORT:
            startFilePickerActivityToExport();
            return true;
        case MENU_ID_ABOUT:
            showVersion();
            return true;
        }
        return false;
    }

    /*----------------------------------------------------------------------*/

    private void addDate() {
        DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                Calendar cal = new GregorianCalendar(year, month, day);
                if (mData.insertDate(cal)) {
                    refreshViews();
                    handleFocus(null, SheetData.POS_KEEP, mData.searchDate(cal, true), true);
                }
            }
        };
        MyApplication.showDatePickerDialog(this, new GregorianCalendar(), listener);
    }

    private void modifyDate(final int col) {
        DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                Calendar cal = new GregorianCalendar(year, month, day);
                mData.moveDate(col, cal);
                handleFocus(null, SheetData.POS_KEEP, mData.searchDate(cal, true), true);
            }
        };
        MyApplication.showDatePickerDialog(this, mData.dates.get(col), listener);
    }

    private void deleteDate(final int col) {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                mData.deleteDate(col);
                refreshViews();
                handleFocus(null, SheetData.POS_KEEP, SheetData.POS_GONE, false);
            }
        };
        MyApplication.showYesNoDialog(
                this, android.R.drawable.ic_dialog_alert,
                MyApplication.getDateString(this, mData.dates.get(col)),
                R.string.msg_delete, listener);
    }

    private void showDateStats(int col) {
        class SymbolStats {
            int count = 0;
            StringBuffer buf = new StringBuffer();
        }
        LinkedHashMap<String, SymbolStats> map = new LinkedHashMap<String, SymbolStats>();
        String[] symbols = getResources().getStringArray(R.array.symbol_strings);
        for (String key : symbols) {
            map.put(key, new SymbolStats());
        }
        for (EntryData entry : mData.entries) {
            String key = entry.attends.get(col);
            if (key != null) {
                SymbolStats stats = map.get(key);
                if (stats == null) {
                    stats = new SymbolStats();
                    map.put(key, stats);
                }
                stats.count++;
                stats.buf.append("\n - ").append(entry.name);
            }
        }
        StringBuffer msgBuf = new StringBuffer();
        for (String key : map.keySet()) {
            if (msgBuf.length() > 0) {
                msgBuf.append("\n\n");
            }
            SymbolStats stats = map.get(key);
            msgBuf.append(key).append(": ")
                .append(String.format(getText(R.string.text_member_count).toString(), stats.count))
                .append(stats.buf);
        }
        MyApplication.showShareDialog(this, android.R.drawable.ic_dialog_info,
                MyApplication.getDateString(this, mData.dates.get(col)), msgBuf);
    }

    /*----------------------------------------------------------------------*/

    private void addMember(final int row) {
        final EditText editText = new EditText(this);
        editText.setSingleLine();
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                mData.insertEntry(row, editText.getText().toString());
                refreshViews();
                handleFocus(null, row, SheetData.POS_KEEP, true);
            }
        };
        MyApplication.showCustomDialog(
                this, android.R.drawable.ic_dialog_info,
                R.string.msg_newmembername, editText, listener);
    }

    private void modifyMember(final int row) {
        final EditText editText = new EditText(this);
        editText.setSingleLine();
        String name = mData.entries.get(row).name;
        editText.setText(name);
        editText.setSelection(name.length());
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                mData.modifyEntry(row, editText.getText().toString());
                refreshViews();
            }
        };
        MyApplication.showCustomDialog(
                this, android.R.drawable.ic_dialog_info,
                R.string.msg_newmembername, editText, listener);
    }

    private void moveMember(int row, int distance) {
        if (mData.moveEntry(row, distance)) {
            handleFocus(null, row + distance, SheetData.POS_KEEP, true);
        }
    }

    private void deleteMember(final int row) {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                mData.deleteEntry(row);
                refreshViews();
                handleFocus(null, SheetData.POS_GONE, SheetData.POS_KEEP, false);
            }
        };
        MyApplication.showYesNoDialog(
                this, android.R.drawable.ic_dialog_alert,
                mData.entries.get(row).name, R.string.msg_delete, listener);
    }

    private void showMemberStats(int row) {
        class SymbolStats {
            int count = 0;
        }
        LinkedHashMap<String, SymbolStats> map = new LinkedHashMap<String, SymbolStats>();
        String[] symbols = getResources().getStringArray(R.array.symbol_strings);
        for (String key : symbols) {
            map.put(key, new SymbolStats());
        }
        for (String key : mData.entries.get(row).attends) {
            if (key != null) {
                SymbolStats stats = map.get(key);
                if (stats == null) {
                    stats = new SymbolStats();
                    map.put(key, stats);
                }
                stats.count++;
            }
        }
        StringBuffer msgBuf = new StringBuffer();
        msgBuf.append(MyApplication.getDateString(this, mData.dates.get(0))).append(" - ")
            .append(MyApplication.getDateString(this, mData.dates.get(mData.dates.size() - 1)));
        for (String key : map.keySet()) {
            msgBuf.append('\n').append(key).append(": ").append(String.format(
                    getText(R.string.text_times_count).toString(), map.get(key).count));
        }
        MyApplication.showShareDialog(this, android.R.drawable.ic_dialog_info,
                mData.entries.get(row).name, msgBuf);
    }

    private void showVersion() {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View aboutView = inflater.inflate(R.layout.about, null);
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                    getPackageName(), PackageManager.GET_META_DATA);
            TextView textView = (TextView) aboutView.findViewById(R.id.text_about_version);
            textView.setText("Version " + packageInfo.versionName);

            StringBuilder buf = new StringBuilder();
            InputStream in = getResources().openRawResource(R.raw.license);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String str;
            while((str = reader.readLine()) != null) {
                buf.append(str).append('\n');
            }
            textView = (TextView) aboutView.findViewById(R.id.text_about_message);
            textView.setText(buf.toString());
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        MyApplication.showCustomDialog(this, android.R.drawable.ic_dialog_info,
                R.string.menu_version, aboutView, null);
    }

    /*----------------------------------------------------------------------*/

    private void startWizardActivity() {
        final Intent intent = new Intent(this, WizardActivity.class);
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                startActivityForResult(intent, REQUEST_ID_CREATE);
            }
        };
        MyApplication.showYesNoDialog(
                this, android.R.drawable.ic_dialog_alert,
                R.string.menu_new, R.string.msg_newsheet, listener);
    }

    private void startFilePickerActivityToImport() {
        final Intent intent = new Intent(this, MyFilePickerActivity.class);
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_TITLEID, R.string.title_import);
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_EXTENSION, "csv");
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                startActivityForResult(intent, REQUEST_ID_IMPORT);
            }
        };
        MyApplication.showYesNoDialog(
                this, android.R.drawable.ic_dialog_alert,
                R.string.menu_import, R.string.msg_newsheet, listener);
    }

    private void startFilePickerActivityToExport() {
        Intent intent = new Intent(this, MyFilePickerActivity.class);
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_TITLEID, R.string.title_export);
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_EXTENSION, "csv");
        intent.putExtra(MyFilePickerActivity.INTENT_EXTRA_WRITEMODE, true);
        startActivityForResult(intent, REQUEST_ID_EXPORT);
    }

    /*----------------------------------------------------------------------*/

    private void refreshViews() {
        mHeader.refreshView();
        mSide.refreshView();
        mSheet.refreshView();
    }

    private String getLocalFileName() {
        return getFilesDir() + File.pathSeparator + "work.csv";
    }
}