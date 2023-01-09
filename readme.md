# Hello!

This is a Condo Miniapps playground for Android, it is still under development, but already allows you to feel the real process of interaction with the application.

You can find the cordova app itself in the MainCordovaApplication folder, where in the www folder there is an example of interaction with the native api and you can develop something of your own.


___
# Content
1. [Getting started](#getting_started)
2. [Common methods.](#common_methods)
3. [Testing.](#testing)

   3.1 [Testing in Demo environment](#testing-demo)

   3.2 [Testing in Production environment](#testing-production)
4. [Publishing.](#publishing)


---
# Getting started <a name="getting_started"></a>

1. Installing the necessary dependencies:

- make sure you have the latest version of Android Studio [installed](https://developer.android.com/studio/install)

- for Windows systems, [install](https://learn.microsoft.com/en-us/windows/wsl/install) wsl subsystem, you need it to use linux commands from cmd like:

        wsl <linux_command>

- [install](https://github.com/nvm-sh/nvm#installing-and-updating) nvm, node and npm 

- cordova installation:

        npm install -g cordova
- open Android Studio, choose project folder to open, then open sdk manager

![SDK manager](./screenshots/sdk_mgr.png)

- project is running under android 12 (api level 32), install required dependenies

![SDK manager](./screenshots/sdk_mgr1.png)
![SDK manager](./screenshots/sdk_mgr2.png)
![SDK manager](./screenshots/sdk_mgr3.png)

2. Editing the application

- open the project directory and go to the /MainCordovaApplication/www subdirectory

  it will contain your application code, edit it freely

3. Launching and testing the application

- open project folder with Android Studio, wait until indexing is complete, then choose real or virtual device and click "run app"

![Run app](./screenshots/run_app.png)

- if you have this error

![Run app](./screenshots/license.png)

accept license agreements by:

        cd ~/Library/Android/sdk/tools/bin/
        ./sdkmanager --licenses

- "updateCordovaProjectToDemo" subtask runs during project build (file app/build.gradle, 66 line), this subtask automatically builds final cordova app file 'www.zip' (MainCordovaApplication/platforms/ios/www.zip) and copies it into app/src/main/res/raw/www.zip to use it by android app

 ---
# Common methods <a name="common_methods"></a>
- authorization

  function requestServerAuthorizationByUrl(miniapp_server_init_auth_url, custom_params_reserver_for_future_use, success, error)

  example:

            cordova.plugins.condo.requestServerAuthorizationByUrl('https://miniapp.d.doma.ai/oidc/auth', {}, function(response) {
                console.log('recive authorication result => ', JSON.stringify(response));
                window.location.reload();
            }, function(error) {
                console.log(error);
            });

- obtaining a current resident/address

  function getCurrentResident(success, error)

  example:

            cordova.plugins.condo.getCurrentResident(function(response) {
                console.log("current resident\address => ", JSON.stringify(response));
            }, function(error) {
                console.log(error);
            });

- application closing

  function closeApplication(success, error)

  example:

            cordova.plugins.condo.closeApplication(function(response) {}, function(error) {});


---

# Testing  <a name="testing"></a>

## Demo environment  <a name="testing-demo"></a>

1. Open Chrome and enter into url field 'chrome://inspect/#devices'
Choose WebView in ai.doma.miniappdemo and lick "inspect"

   ![inspect](./screenshots/inspect.png)

2. Yay! Standard Chrome debugging tools are connected to your mini-application

   ![console](./screenshots/console.png)

## Production environment  <a name="testing-production"></a>

1. Take www.zip archive:

        MainCordovaApplication/platforms/ios/www.zip

If it doesn't exist create it from folder:

        /MainCordovaApplication/platforms/ios/www

2. [Install](https://play.google.com/store/apps/details?id=ai.doma.client) Doma app from Google Play

3. The app you downloaded has built-in functionality for debugging mini-applications. To turn it on & off, you can use these deeplinks:

- [Click here to switch on](https://mobile.doma.ai/api/mobile/partners/miniapps/enable-local.html)
- [Click here to switch off](https://mobile.doma.ai/api/mobile/partners/miniapps/disable-local.html)

5. Now, on the main application screen in the list of mini-applications, the last button allows you to download or replace a previously downloaded mini-application from files. When you click on it, you need to select the previously taken www.zip archive.

6. The application loaded in this way has a built-in js console, which is accessible by clicking on the button at the bottom right of the open mini-application and is able to show a lot of additional information, including various errors.

---
# Publishing <a name="publishing"></a>
To publish the mini-application, send the 'www.zip' archive you received during the testing phase to the people at Doma with whom you interact.   
        