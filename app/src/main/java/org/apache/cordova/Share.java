package org.apache.cordova;

import android.content.Intent;
import android.net.Uri;
import android.util.Base64;

import org.apache.cordova.camera.FileProvider;
import org.apache.cordova.engine.SystemWebChromeClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class echoes a string called from JavaScript.
 */
public class Share extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("share")) {
            String text = args.getString(0);
            String title = args.getString(1);
            String mimetype = args.getString(2);

            try {
                JSONArray base64Files = args.getJSONArray(3);
                if (base64Files.length() > 0) {
                    ArrayList<Uri> filesToShare = new ArrayList<>();

                    for (int i = 0; i < base64Files.length(); i++) {
                        JSONObject fileObj = base64Files.getJSONObject(i);
                        String s = fileObj.getString("base64");
                        String fileName = fileObj.getString("name");

                        s = s.substring(s.indexOf("base64,") + 7);

                        byte[] fileBytes = Base64.decode(s, Base64.DEFAULT);
                        File file = new File(cordova.getActivity().getCacheDir(), fileName);
                        if (file.exists()) {
                            file.delete();
                        }
                        file.createNewFile();

                        FileOutputStream fos = new FileOutputStream(file);

                        fos.write(fileBytes);
                        fos.flush();
                        fos.close();

                        Uri uri = FileProvider.getUriForFile(
                                cordova.getContext(),
                                "org.apache.cordova.provider",
                                file
                        );
                        filesToShare.add(uri);

                    }


                    this.shareFiles(filesToShare, text, title, mimetype, callbackContext);
                } else {
                    this.share(text, title, mimetype, callbackContext);
                }
            } catch (Exception e) {
                this.share(text, title, mimetype, callbackContext);
            }

            return true;
        }
        return false;
    }

    private void share(String text, String title, String mimetype, CallbackContext callbackContext) {
        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
            sendIntent.setType(mimetype);
            cordova.getActivity().startActivity(Intent.createChooser(sendIntent, title));
            callbackContext.success();
        } catch (Error e) {
            callbackContext.error(e.getMessage());
        }

    }

    private void shareFiles(
            ArrayList<Uri> files,
            String text,
            String title,
            String mimetype,
            CallbackContext callbackContext
    ) {
        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
//            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
            sendIntent.setType("*/*");
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cordova.getActivity().startActivity(Intent.createChooser(sendIntent, title));
            callbackContext.success();
        } catch (Error e) {
            callbackContext.error(e.getMessage());
        }
    }
}