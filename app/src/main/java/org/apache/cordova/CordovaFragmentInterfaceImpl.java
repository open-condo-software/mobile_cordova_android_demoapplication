package org.apache.cordova;

import static androidx.navigation.fragment.FragmentKt.findNavController;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CordovaFragmentInterfaceImpl implements CordovaInterface {
    private static final String TAG = "CordovaInterfaceImpl";
    protected AppCompatActivity activity;
    protected Fragment fragment;
    protected ExecutorService threadPool;
    protected PluginManager pluginManager;

    protected ActivityResultHolder savedResult;
    protected CallbackMap permissionResultCallbacks;
    public CordovaPlugin activityResultCallback;
    protected String initCallbackService;
    protected int activityResultRequestCode;
    protected boolean activityWasDestroyed = false;
    protected Bundle savedPluginState;

    public CordovaFragmentInterfaceImpl(AppCompatActivity activity, Fragment fragment) {
        this(activity, fragment, Executors.newCachedThreadPool());
    }

    public CordovaFragmentInterfaceImpl(AppCompatActivity activity, Fragment fragment, ExecutorService threadPool) {
        this.activity = activity;
        this.fragment = fragment;
        this.threadPool = threadPool;
        this.permissionResultCallbacks = new CallbackMap();
    }

    @Override
    public void startActivityForResult(CordovaPlugin command, Intent intent, int requestCode) {
        setActivityResultCallback(command);
        try {
            fragment.startActivityForResult(intent, requestCode);
        } catch (RuntimeException e) { // E.g.: ActivityNotFoundException
            activityResultCallback = null;
            throw e;
        }
    }

    @Override
    public void setActivityResultCallback(CordovaPlugin plugin) {
        // Cancel any previously pending activity.
        if (activityResultCallback != null) {
            activityResultCallback.onActivityResult(activityResultRequestCode, AppCompatActivity.RESULT_CANCELED, null);
        }
        activityResultCallback = plugin;
    }

    @Override
    public AppCompatActivity getActivity() {
        return activity;
    }

    @Override
    public Context getContext() {
        return activity;
    }

    @Override
    public Object onMessage(String id, Object data) {
        if ("exit".equals(id)) {
            findNavController(fragment).popBackStack();
        }
        return null;
    }

    @Override
    public ExecutorService getThreadPool() {
        return threadPool;
    }

    /**
     * Dispatches any pending onActivityResult callbacks and sends the resume event if the
     * Activity was destroyed by the OS.
     */
    public void onCordovaInit(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        Log.d("CORDOVA","onCordovaInit");
        if (savedResult != null) {
            Log.d("CORDOVA","savedResult = "+ savedResult.resultCode);
            onActivityResult(savedResult.requestCode, savedResult.resultCode, savedResult.intent);
        } else if(activityWasDestroyed) {
            // If there was no Activity result, we still need to send out the resume event if the
            // Activity was destroyed by the OS
            activityWasDestroyed = false;
            if(pluginManager != null)
            {
                CoreAndroid appPlugin = (CoreAndroid) pluginManager.getPlugin(CoreAndroid.PLUGIN_NAME);
                if(appPlugin != null) {
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("action", "resume");
                    } catch (JSONException e) {
                        LOG.e(TAG, "Failed to create event message", e);
                    }
                    appPlugin.sendResumeEvent(new PluginResult(PluginResult.Status.OK, obj));
                }
            }

        }
    }

    /**
     * Routes the result to the awaiting plugin. Returns false if no plugin was waiting.
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent intent) {
        CordovaPlugin callback = activityResultCallback;
        Log.d("CORDOVA","onActivityResult ");

        if(callback == null && initCallbackService != null) {
            Log.d("CORDOVA","callback:" + callback + " initCallbackService = "+initCallbackService);
            // The application was restarted, but had defined an initial callback
            // before being shut down.
            savedResult = new ActivityResultHolder(requestCode, resultCode, intent);
            if (pluginManager != null) {
                Log.d("CORDOVA","pluginManager exist");

                callback = pluginManager.getPlugin(initCallbackService);

                if(callback != null) {
                    Log.d("CORDOVA","callback:"+callback.getServiceName());

                    callback.onRestoreStateForActivityResult(savedPluginState.getBundle(callback.getServiceName()),
                            new ResumeCallback(callback.getServiceName(), pluginManager));
                }
            }
        }
        activityResultCallback = null;

        if (callback != null) {
            LOG.d(TAG, "Sending activity result to plugin");
            initCallbackService = null;
            savedResult = null;
            callback.onActivityResult(requestCode, resultCode, intent);
            return true;
        }
        LOG.w(TAG, "Got an activity result, but no plugin was registered to receive it" + (savedResult != null ? " yet!" : "."));
        return false;
    }

    /**
     * Call this from your startActivityForResult() overload. This is required to catch the case
     * where plugins use Activity.startActivityForResult() + CordovaInterface.setActivityResultCallback()
     * rather than CordovaInterface.startActivityForResult().
     */
    public void setActivityResultRequestCode(int requestCode) {
        activityResultRequestCode = requestCode;
    }

    /**
     * Saves parameters for startActivityForResult().
     */
    public void onSaveInstanceState(Bundle outState) {
        if (activityResultCallback != null) {
            String serviceName = activityResultCallback.getServiceName();
            outState.putString("callbackService", serviceName);
        }
        if(pluginManager != null){
            outState.putBundle("plugin", pluginManager.onSaveInstanceState());
        }

    }

    /**
     * Call this from onCreate() so that any saved startActivityForResult parameters will be restored.
     */
    public void restoreInstanceState(Bundle savedInstanceState) {
        initCallbackService = savedInstanceState.getString("callbackService");
        savedPluginState = savedInstanceState.getBundle("plugin");
        activityWasDestroyed = true;
    }

    private static class ActivityResultHolder {
        private int requestCode;
        private int resultCode;
        private Intent intent;

        public ActivityResultHolder(int requestCode, int resultCode, Intent intent) {
            this.requestCode = requestCode;
            this.resultCode = resultCode;
            this.intent = intent;
        }
    }

    /**
     * Called by the system when the user grants permissions
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        Pair<CordovaPlugin, Integer> callback = permissionResultCallbacks.getAndRemoveCallback(requestCode);
        if(callback != null) {
            callback.first.onRequestPermissionResult(callback.second, permissions, grantResults);
        }
    }

    public void requestPermission(CordovaPlugin plugin, int requestCode, String permission) {
        String[] permissions = new String [1];
        permissions[0] = permission;
        requestPermissions(plugin, requestCode, permissions);
    }

    @SuppressLint("NewApi")
    public void requestPermissions(CordovaPlugin plugin, int requestCode, String [] permissions) {
        int mappedRequestCode = permissionResultCallbacks.registerCallback(plugin, requestCode);
        fragment.requestPermissions(permissions, mappedRequestCode);
    }

    public boolean hasPermission(String permission)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            int result = activity.checkSelfPermission(permission);
            return PackageManager.PERMISSION_GRANTED == result;
        }
        else
        {
            return true;
        }
    }
}
