function onClickInput(v) {
    navigator.camera.getPicture(
        function(imageData) {
            fetch(`data:image/jpeg;base64,${imageData}`).then((v2) => {
                return v2.blob()
            }).then((blob) => {
                let currDate = new Date()
                var file = new File([blob], `capture_${currDate.valueOf()}.mp4`, {type: blob.type});

                var dt  = new DataTransfer();
                dt.items.add(file);
                var file_list = dt.files;

                v.files = file_list
            });

        },
        captureError,
        {
            quality: 50,
            destinationType: Camera.DestinationType.DATA_URL,
            sourceType: Camera.PictureSourceType.CAMERA,
            encodingType: Camera.EncodingType.JPEG,
            mediaType: Camera.MediaType.VIDEO
        }
    )

    // capture error callback
    var captureError = function(error) {
        console.log('Error: ' + error);
    };

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
                    file = new File([blob], mediaFiles[index].name, { type: 'video/mp4' })
                    file.end = mediaFiles[index].size
                    console.log(file)

                    if (window._impFiles == undefined) {
                        window._impFiles = [path]
                    } else {
                        window._impFiles.push(path)
                    }

                    var dt  = new DataTransfer();
                    dt.items.add(file);
                    var file_list = dt.files;

                    videoInput.files = file_list
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

