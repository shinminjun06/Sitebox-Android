package com.sitebox.app;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.*;
/** Synthetic provider in test APK only; no production provider or storage permission. */
public final class BackupTestProvider extends ContentProvider {
 static final Uri DOCUMENT=Uri.parse("content://com.sitebox.app.test.backup/backup.json");
 private File file(Uri uri){if(!DOCUMENT.equals(uri))throw new IllegalArgumentException();return new File(getContext().getFilesDir(),"synthetic-backup.json");}
 public boolean onCreate(){return true;}
 public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.parseMode(mode));}
 public String getType(Uri uri){file(uri);return "application/json";}
 public Cursor query(Uri uri,String[] p,String s,String[] a,String o){file(uri);return null;}
 public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
 public int update(Uri uri,ContentValues values,String s,String[] a){throw new UnsupportedOperationException();}
 public int delete(Uri uri,String s,String[] a){return file(uri).delete()?1:0;}
}
