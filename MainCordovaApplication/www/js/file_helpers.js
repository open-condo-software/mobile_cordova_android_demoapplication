function onClickInput(v) {
    var captureSuccess = function(mediaFiles) {
            var i, path, len;
            for (i = 0, len = mediaFiles.length; i < len; i += 1) {
                console.log(mediaFiles[i])
                path = mediaFiles[i].fullPath;

                var xhr = new XMLHttpRequest()
                xhr.open('GET', path)
                var index = i
                xhr.onload = function (r) {
                    console.log(r)
                    var content = xhr.response;
                    var blob = new Blob([content]);
                    console.log(blob);
                    file = new File([blob], mediaFiles[index].name, { type: mediaFiles[index].type })

                    var dt  = new DataTransfer();
                    dt.items.add(file);
                    var file_list = dt.files;

                    v.target.files = file_list
                }
                xhr.responseType = 'blob'
                xhr.send()
            }
        };

        // capture error callback
        var captureError = function(error) {
            console.log('Error code: ' + error.code);
        };

        // start video capture
        navigator.device.capture.captureImage(captureSuccess, captureError, { limit : 1 });

        return false
}

function onClickVideoInput(v) {
    var captureSuccess = function(mediaFiles) {
        var i, path, len;
        for (i = 0, len = mediaFiles.length; i < len; i += 1) {
            console.log(mediaFiles[i])
            path = mediaFiles[i].fullPath;

            var xhr = new XMLHttpRequest()
            xhr.open('GET', path)
            var index = i
            xhr.onload = function (r) {
                console.log(r)
                var content = xhr.response;
                var blob = new Blob([content]);
                console.log(blob);
                file = new File([blob], mediaFiles[index].name, { type: mediaFiles[index].type })

                var dt  = new DataTransfer();
                dt.items.add(file);
                var file_list = dt.files;

                v.target.files = file_list
            }
            xhr.responseType = 'blob'
            xhr.send()
        }
    };

    // capture error callback
    var captureError = function(error) {
        console.log('Error code: ' + error.code);
    };

    // start video capture
    navigator.device.capture.captureVideo(captureSuccess, captureError, { limit : 1 });

    return false
}

