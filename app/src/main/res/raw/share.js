cordova.define("nl.madebymark.share.Share", function(require, exports, module) {
    module.exports = function(text,title,mimetype,success,error){
      if(typeof text !== "string") {
        text = "";
      }
      if(typeof title !== "string") {
        title = "Share";
      }
      if(typeof mimetype !== "string") {
        mimetype = "text/plain";
      }
      cordova.exec(success,error,"Share","share",[text,title,mimetype]);
      return true;
    };

    module.exports = function(data,success,error){
        (async () => await processing(data,success,error))();

        return true;
    };

    async function processing(data,success,error) {
        var title = data.title
        var text = data.text
        var url = data.url
        var mimetype = "text/plain"

        var files = data.files
        window.tmpVar = data.files
        var fileNames = []
        var filesToShare = []

        var command = "share"
        if (files != undefined) {
            for (let index = 0; index < files.length; index++) {
                var file = files[index];
                let result_base64 = await readFileAsDataURL(files[index]);
                fileNames[index] = files[index]
                filesToShare[index] = {
                    base64: result_base64,
                    name: files[index].name
                }
            }
        }

        if (typeof text !== "string") {
            text = "";
        }
        if (typeof url !== "string") {
            url = "";
        }
        if (typeof title !== "string") {
            title = "Share";
        }
        if (typeof mimetype !== "string") {
            mimetype = "text/plain";
        }
        text = (text + " " + url).trim()

        cordova.exec(success,error,"Share",command,[text,title,mimetype, filesToShare]);
    }

    async function readFileAsDataURL(file) {
        let result_base64 = await new Promise((resolve) => {
            let fileReader = new FileReader();
            fileReader.onload = (e) => resolve(fileReader.result);
            fileReader.readAsDataURL(file);
        });
        console.log(result_base64);
        return result_base64;
    }
});
