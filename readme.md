Приветствуем! 

Это плейграунд Condo Miniapps для Android, он ещё в процессе разработки, но уже позволяет пощупать реальный процесс взаимодействия с приложением.

Приложение миниаппа - это всегда архив с названием www.zip который лежит в app/src/main/res/raw/www.zip, замените его собственным 
архивом с тем же названием чтобы тестировать собственное приложение.

основные методы
- авторизация

    function requestServerAuthorizationByUrl(miniapp_server_init_auth_url, custom_params_reserver_for_future_use, success, error) 

    пример:

            cordova.plugins.condo.requestServerAuthorizationByUrl('https://miniapp.d.doma.ai/oidc/auth', {}, function(response) {
                console.log('recive authorication result => ', JSON.stringify(response));
                window.location.reload();
            }, function(error) {
                console.log(error);
            });

- получение текущего резидента\адреса

    function getCurrentResident(success, error)

    пример:

            cordova.plugins.condo.getCurrentResident(function(response) {
                console.log("current resident\address => ", JSON.stringify(response));
            }, function(error) {
                console.log(error);
            });

- закрытие приложения

    function closeApplication(success, error)

    пример:

            cordova.plugins.condo.closeApplication(function(response) {}, function(error) {});
        