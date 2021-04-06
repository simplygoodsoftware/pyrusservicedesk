package com.pyrus.pyrusservicedesk.log;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.core.util.Consumer;

import com.pyrus.pyrusservicedesk.BuildConfig;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class _L {

    private final static String _TAG = "Logger";
    private final static String _TAG_PREFIX = "PPR.";

    private final static boolean _ENABLED = true;

    private enum _Level {ERROR, WARN, INFO, DEBUG, VERBOSE}

    private final static _Level _LL = _Level.VERBOSE;
    private final static _Level _SL = _Level.VERBOSE;

    private final static long _KEEP_LOGS_FOR = 7 * 24 * 60 * 60 * 1000;
    private final static long _ONE_LOG_INTERVAL = 3 * 60 * 60 * 1000;
    private final static long _KEEP_EXTERNAL_LOGS_FOR = 60 * 60 * 1000;
    private final static float _ZIP_MAGICK_NUMBER = 3f;
    private final static long _ZIP_MAX_TOTAL_SIZE = 9 * 1024 * 1024;

    private static volatile long _log_ts = 0;
    private static volatile String _log_name = null;
    private static volatile PrintWriter _fw = null;
    private static volatile Handler _handler = null;

    private static SimpleDateFormat _dateFormat = new SimpleDateFormat("MMM/dd(Z) HH:mm:ss.SSS", Locale.US);
    private static HashMap<String, Long> pTags = new HashMap<>();

    private static boolean _ll_error, _ll_warn, _ll_info, _ll_debug, _ll_verbose;
    private static boolean _sl_error, _sl_warn, _sl_info, _sl_debug, _sl_verbose;

    private static File filesDir;
    private static File logsDir;

    private static final Set<Consumer<File>> logSubscribers = new HashSet<>();

    static void addLogSubscriber(Consumer<File> subscriber) {
        logSubscribers.add(subscriber);
    }

    static void removeLogSubscriber(Consumer<File> subscriber) {
        logSubscribers.remove(subscriber);
    }

    static void Instantiate(Application application) {
        initLogsDirs(application);
        if (_handler == null) {
            new LogWriterThread().start();
        }
        setConditions();
        _checkFS();
    }

    private static void initLogsDirs(Context appContext) {
        filesDir = appContext.getFilesDir();

        logsDir = new File(filesDir + "/service_desk_logs/");
        logsDir.mkdirs();
    }

    private static void setConditions() {
        _ll_error = _ENABLED && _LL.ordinal() >= _Level.ERROR.ordinal();
        _ll_warn = _ENABLED && _LL.ordinal() >= _Level.WARN.ordinal();
        _ll_info = _ENABLED && _LL.ordinal() >= _Level.INFO.ordinal();
        _ll_debug = _ENABLED && _LL.ordinal() >= _Level.DEBUG.ordinal();
        _ll_verbose = _ENABLED && _LL.ordinal() >= _Level.VERBOSE.ordinal();

        _sl_error = _ENABLED && BuildConfig.DEBUG && _SL.ordinal() >= _Level.ERROR.ordinal();
        _sl_warn = _ENABLED && BuildConfig.DEBUG && _SL.ordinal() >= _Level.WARN.ordinal();
        _sl_info = _ENABLED && BuildConfig.DEBUG && _SL.ordinal() >= _Level.INFO.ordinal();
        _sl_debug = _ENABLED && BuildConfig.DEBUG && _SL.ordinal() >= _Level.DEBUG.ordinal();
        _sl_verbose = _ENABLED && BuildConfig.DEBUG && _SL.ordinal() >= _Level.VERBOSE.ordinal();
    }

    private static class LogWriterThread extends Thread {
        public void run() {
            Looper.prepare();

            _handler = new Handler() {
                public void handleMessage(Message msg) {
                    _checkFS();
                    synchronized (_L.class) {
                        if (_fw != null) {
                            _fw.println((String) msg.obj);
                            _fw.flush();
                        }
                    }
                }
            };
            Looper.loop();
        }
    }

    private static void _checkFS() {
        if (_fw == null || System.currentTimeMillis() - _log_ts > _ONE_LOG_INTERVAL) {
            synchronized (_L.class) {
                if (System.currentTimeMillis() - _log_ts > _ONE_LOG_INTERVAL) {
                    if (_fw != null) {
                        _fw.close();
                        _fw = null;
                    }
                }

                if (_fw == null) {
                    _log_ts = System.currentTimeMillis();
                    try {
                        Calendar c = Calendar.getInstance();
                        c.setTimeInMillis(_log_ts);
                        String logfname = String.format(Locale.US, "%04d%02d%02d-%02d%02d%02d",
                                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
                        _log_name = logfname;
                        _fw = new PrintWriter(logsDir + "/log-" + logfname + ".txt");
                        Log.i(_TAG, "Start writing a new file: " + logsDir + "/log-" + logfname + ".txt");
                    }
                    catch (Exception e) {
                        Log.e(_TAG, "Cannot open logfile for output: " + e.getLocalizedMessage(), e);
                        _fw = null;
                    }
                }
            }
            _clearOldLogs();
        }

    }

    private static void _dumpFilesTreeStats() {
        long totalsize = 0;
        long totalfiles = 0;
        _L.d(_TAG, "Checking app files");
        List<File> allfiles = Utils.getListFiles(filesDir);
        for (File f : allfiles) {
            totalfiles++;
            totalsize += f.length();
        }
        _L.d(_TAG, "App files checked: %d files, %d bytes", totalfiles, totalsize);
    }

    private static void _clearOldLogs() {
        String[] fl = logsDir.list(
                new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String fn) {
                        return (fn.startsWith("log-") && fn.endsWith(".txt"))
                                || fn.endsWith(".hprof");
                    }
                });

        if (fl != null) {
            ArrayList<File> files = new ArrayList<>();
            for (String s : fl) {
                if (_log_name != null && s.contains(_log_name)) {
                    continue;
                }
                File f = new File(logsDir, s);
                Date lastModDate;
                try {
                    lastModDate = new Date(f.lastModified());
                }
                catch (Exception e) {
                    lastModDate = null;
                }
                if (lastModDate != null && lastModDate.getTime() < _log_ts - _KEEP_LOGS_FOR) {
                    _L.d(_TAG, "_clearOldLogs, deleting old log file: " + s);
                    f.delete();
                }
                else {
                    files.add(f);
                }
            }

            if(files.size() > 0) {
                Collections.sort(files);
                long total_size = 0;
                long max_size = (long)((float)_ZIP_MAX_TOTAL_SIZE * _ZIP_MAGICK_NUMBER);
                for (ListIterator<File> it = files.listIterator(files.size()); it.hasPrevious();) {
                    final File f = it.previous();
                    long fs = f.length();
                    if (total_size + fs >= max_size) {
                        _L.d(_TAG, "_clearOldLogs, deleting oversized file: %s", f);
                        f.delete();
                        it.remove();
                    }
                    total_size += fs;
                }
            }
        }
    }

    private static String _getLogDate(String ll, String tag) {
        return _dateFormat.format(new Date()) + " " + ll + " [" + Thread.currentThread().getId() + ":" + tag + "] ";
    }

    private static String _getLogMsg(String pTag, String msg, Object... params) {
        if (msg == null)
            msg = "";

        if (pTag != null) {
            Long osa = pTags.get(pTag);
            if (osa == null) {
                osa = System.currentTimeMillis();
                pTags.put(pTag, osa);
            }
            long sa = osa;
            msg = String.format(Locale.US, "[%s+%.3fs] ", pTag, ((float) (System.currentTimeMillis() - sa)) / 1000) + msg;
        }
        return params.length > 0 ? String.format(msg, params) : msg;
    }

    private static void _log(String s) {
        if (_handler != null) {
            Message msg = Message.obtain(_handler);
            msg.obj = s;
            msg.sendToTarget();
        }
    }

    public static void v(String TAG, String msg, Object... params) {
        V(TAG, null, msg, params);
    }

    public static void V(String TAG, String pTag, String msg, Object... params) {
        if ((_ll_verbose || _sl_verbose) && PLog.initialized) {
            String logMsg = _getLogMsg(pTag, msg, params);

            if (_ll_verbose)
                _log(_getLogDate("V", TAG) + logMsg);

            if (_sl_verbose)
                Log.v(_TAG_PREFIX + TAG, logMsg);
        }

    }


    public static void d(String TAG, String msg, Object... params) {
        D(TAG, null, msg, params);
    }

    public static void D(String TAG, String pTag, String msg, Object... params) {
        if ((_ll_debug || _sl_debug) && PLog.initialized) {
            String logMsg = _getLogMsg(pTag, msg, params);

            if (_ll_debug)
                _log(_getLogDate("D", TAG) + logMsg);

            if (_sl_debug)
                Log.d(_TAG_PREFIX + TAG, logMsg);
        }
    }


    public static void i(String TAG, String msg, Object... params) {
        I(TAG, null, msg, params);
    }

    public static void I(String TAG, String pTag, String msg, Object... params) {
        if ((_ll_info || _sl_info) && PLog.initialized) {
            String logMsg = _getLogMsg(pTag, msg, params);

            if (_ll_info)
                _log(_getLogDate("I", TAG) + logMsg);

            if (_sl_info)
                Log.i(_TAG_PREFIX + TAG, logMsg);
        }

    }

    public static void w(String TAG, String msg, Object... params) {
        W(TAG, null, msg, params);
    }

    public static void w(String TAG, Throwable throwable, String msg, Object... params) {
        W(TAG, throwable, null, msg, params);
    }

    public static void W(String TAG, String pTag, String msg, Object... params) {
        W(TAG, null, pTag, msg, params);
    }

    public static void W(String TAG, Throwable throwable, String pTag, String msg, Object... params) {
        WImpl(TAG, throwable, pTag, msg, params);
    }

    private static void WImpl(String TAG, Throwable throwable, String pTag, String msg, Object... params) {
        if ((_ll_warn || _sl_warn) && PLog.initialized) {
            String logMsg = _getLogMsg(pTag, msg, params);

            if (_ll_warn)
                _log(_getLogDate("W", TAG) + logMsg);

            if (_sl_warn)
                Log.w(TAG, logMsg, throwable);
        }
    }


    public static void e(String TAG, String msg, Object... params) {
        eImpl(TAG, null, msg, params);
    }

    public static void e(String TAG, Throwable throwable, String msg, Object... params) {
        eImpl(TAG, throwable, msg, params);
    }

    private static void eImpl(String TAG, Throwable throwable, String msg, Object... params) {
        if (_ll_error || _sl_error) {
            String logMsg = _getLogMsg(null, msg, params);

            if (_ll_error)
                _log(_getLogDate("E", TAG) + logMsg);

            if (_sl_error)
                Log.e(TAG, logMsg, throwable);
        }
    }

    /**
     * Execute {@link SendLogTask} which packs user's logs and start email
     * intent.
     */
    public static void sendLogs() {
        long now = Calendar.getInstance().getTimeInMillis();
        synchronized (mActiveSendLogTaskLock) {
            if (mActiveSendLogTask == null
                    || mActiveSendTaskStarted == 0
                    || TimeUnit.MILLISECONDS.toSeconds(Math.abs(now - mActiveSendTaskStarted)) > 3 * 60) {
                mActiveSendTaskStarted = now;
                mActiveSendLogTask = new SendLogTask();
                d(_TAG, "starting sendLogTask %s, startTime: %s", mActiveSendLogTask, mActiveSendTaskStarted);
                mActiveSendLogTask.execute();
            }
            else
                d(_TAG, "skipping a new sendLogTask, there is already one started: %s, startTime: %s", mActiveSendLogTask, mActiveSendTaskStarted);
        }
    }

    private static AsyncTask mActiveSendLogTask = null;
    private static long mActiveSendTaskStarted = 0;
    private static final Object mActiveSendLogTaskLock = new Object();

    private static File _zipLogFiles(File[] logs) throws IOException {
        final String tempLogPrefix = "service_desk_android_";
        final String tempLogSuffix = ".zip";

        try {
            Utils.Arrays.forEach(
                    logsDir.listFiles(),
                    new Consumer<File>() {
                        @Override
                        public void accept(File file) {
                            if (file.isFile()
                                    && file.getName().startsWith(tempLogPrefix)
                                    && file.getName().endsWith(tempLogSuffix)
                                    && System.currentTimeMillis() - file.lastModified() > _KEEP_EXTERNAL_LOGS_FOR)
                                file.delete();
                        }
                    }
            );
        }
        catch (Exception e) {
            _L.w(_TAG, e, "_zipLogFiles. Exception occurred while deleting internal logs, gonna skip the offending files");
        }

        File zipfile = File.createTempFile(tempLogPrefix, tempLogSuffix, logsDir);
        zipfile.deleteOnExit();
        ZipOutputStream zipOutputStream = null;
        try {
            zipOutputStream = new ZipOutputStream(new FileOutputStream(zipfile));
            for (File log : logs) {
                ZipEntry entry;
                try {
                    entry = new ZipEntry(log.getName());
                }
                catch (Exception exc) {
                    _L.w(_TAG, exc, "_zipLogFiles, exception occurred while creating a zip entry, gonna skip the offending file");
                    continue;
                }
                zipOutputStream.putNextEntry(entry);
                try {
                    _copy(zipOutputStream, log);
                }
                catch (Exception exc) {
                    _L.w(_TAG, exc, "_zipLogFiles, exception occurred while writing a zip entry, gonna skip the offending file");
                }
                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
            return zipfile;
        }
        finally {
            try {
                if (zipOutputStream != null)
                    zipOutputStream.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void _copy(OutputStream outputStream, File file) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            byte[] buffer = new byte[8096];
            int length;
            while ((length = inputStream.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, length);
            }
        }
        finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class SendLogTask extends AsyncTask<Object, Void, File> {

        @Override
        protected File doInBackground(Object... context) {
            String subj = "ServiceDesk Android Client logs (" + _dateFormat.format(new Date()) + ")";
            String text = Build.DEVICE
                    + " / " + Build.MANUFACTURER
                    + " / " + Build.HARDWARE
                    + " / " + Build.BRAND
                    + " / " + Build.MODEL;

            _L.i(_TAG, "Sending logs...");
            _L.i(_TAG, "Email Subj: %s", subj);
            _L.i(_TAG, "Email Text: %s", text);

            try {
                _clearOldLogs();
            }
            catch(Exception e){
                _L.w(_TAG, e,"failed to clear old logs");
            }

            try {
                _dumpFilesTreeStats();
            }
            catch(Exception e){
                _L.w(_TAG, e, "failed to dump files tree stats");
            }

            synchronized (_L.class) {
                if (_fw != null) {
                    _fw.close();
                }
                _fw = null;
            }

            File[] logs = logsDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File fn) {
                    try {
                        return (fn.getName().startsWith("log-") && fn.getName().endsWith(".txt"))
                                || fn.getName().endsWith(".hprof");
                    }
                    catch (Exception exc) {
                        _L.w(_TAG, exc, "Exception occurred while listing files, gonna skip the offending file.");
                    }
                    return false;
                }
            });

            File temp;
            try {
                temp = _zipLogFiles(logs);
            }
            catch(Exception e){
                _L.e(_TAG, e, "Failed to collect logs");
                return null;
            }

            return temp;
        }

        @Override
        protected void onPostExecute(File logFile){

            if (logFile != null)
                for (Consumer<File> subscriber : logSubscribers)
                    subscriber.accept(logFile);

            d(_TAG, "sending logs finished. Finished sendLogTask %s, startTime: %s", mActiveSendLogTask, mActiveSendTaskStarted);
            synchronized (mActiveSendLogTaskLock) {
                mActiveSendLogTask = null;
                mActiveSendTaskStarted = 0;
            }
        }
    }
}
