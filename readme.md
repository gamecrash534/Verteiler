# Verteiler
A small software providing a simple file browser to quickly share and distribute files

## Usage
### Installation
Probably the best way to run this software is through the docker container:
```shell
docker pull git.gamecrash.dev/game.crash/verteiler:[VERSION]
```
Container tags / versions go after the following scheme:
`major`, `major.minor`, `major.minor.patch`, or any of these, with a `-native` suffix for the native image.
Examples for valid versions: `1-native`, `1.3`, `1.3.1-native`, etc...

> [!TIP]
> Native images are generally smaller and also much more performant in terms of memory usage, startup time and general response time.
> There might be some compatibility issues though - especially as these images currently only support AMD64 as a platform.

All available and valid docker versions can be found [here](https://git.gamecrash.dev/game.crash/-/packages/container/verteiler/versions).

> [!IMPORTANT]
> The `latest`-Tag has been deprecated for a list of reasons, starting with version `1.3.0`.
> Please use any of the versions with the link mentioned above.

For the blank JAR files, please visit the releases section [here](https://git.gamecrash.dev/game.crash/Verteiler/releases).

### Configuration
Once run, this program will automatically generate a configuration file. You can, however, also point it to a custom configuration file by either setting
the `VERTEILER_CONFIG` environment variable to a valid path or by using the `-c` / `--config` command flag.

The available config variables and their descriptions can be found in the table below.
Alternatively, it is also possible to define every configuration through a respective environment variable - which will also override the value set within the
config.

**Table of possible config keys, their respective environment variable name, default value and description:**
<details>
    <summary>Table of Config Values</summary>

| Config Key                                | Environment Variable                   | Default                | Description                                                                                                                                                            |
|-------------------------------------------|----------------------------------------|------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `server.host`                             | `VERTEILER_HOST`                       | `0.0.0.0`              | The host IP to bind to (recommended to use the environment variable when using docker)                                                                                 |
| `server.port`                             | `VERTEILER_PORT`                       | `2987`                 | The port the server will listen on (also recommended to use the environment variable when using docker)                                                                |
| `server.data_directory`                   | `VERTEILER_DATA_DIRECTORY`             | `./data`               | Directory where all files will be served from                                                                                                                          |
| `server.root_path_as_raw_path`            | `VERTEILER_ROOT_AS_RAW`                | `false`                | Whether the root path can also serve any valid file with given path as a raw file. This does not include any file / folder with the name `browse`, for obvious reasons |
| `logging.level`                           | `VERTEILER_LOG_LEVEL`                  | `INFO`                 | The log level to output at                                                                                                                                             |
| `logging.log_access`                      | `VERTEILER_LOG_ACCESS`                 | `true`                 | Defines if all requests and their paths + responses should be logged too                                                                                               |
| `logging.log_to_file`                     | `VERTEILER_LOG_TO_FILE`                | `false`                | If logs should also be saved to a file                                                                                                                                 |
| `logging.log_directory`                   | `VERTEILER_LOG_DIRECTORY`              | `./logs`               | The directory where logs should be saved (only relevant if `log_to_file` is set to `true`)                                                                             |
| `admin.enabled`                           | `VERTEILER_ADMIN_ENABLED`              | `true`                 | If the admin dashboard should be enabled                                                                                                                               |
| `admin.token`                             | `VERTEILER_ADMIN_TOKEN`                | *auto-generated token* | The login token for the admin dashboard                                                                                                                                |
| `admin.chunked_uploads.enabled`           | `VERTEILER_CHUNKED_UPLOADS_ENABLED`    | `true`                 | Whether or not file uploads should be split into chunks                                                                                                                |
| `admin.chunked_uploads.chunk_size`        | `VERTEILER_CHUNK_SIZE`                 | `5242880` *5 MiB*      | The size of a chunk in bytes. Only used when chunked uploads are enabled                                                                                               |
| `admin.chunked_uploads.session_ttl`       | `VERTEILER_SESSION_TTL`                | `900000` *15 min*      | The time in minutes one chunked upload session will persist. After this, it will be invalid, if not already finished                                                   |
| `ui.title`                                | `VERTEILER_TITLE`                      | `Verteiler`            | The page title                                                                                                                                                         |
| `ui.show_file_sizes`                      | `VERTEILER_SHOW_FILE_SIZES`            | `true`                 | Whether to show the size of a file entry                                                                                                                               |
| `ui.show_dates`                           | `VERTEILER_SHOW_DATES`                 | `true`                 | Whether to show the upload date of a file entry                                                                                                                        |
| `ui.drag_drop_upload`                     | `VERTEILER_DRAG_DROP_UPLOAD`           | `true`                 | Whether to enable drag & drop uploads from the admin ui                                                                                                                |
| `ui.enable_preview`                       | `VERTEILER_ENABLE_PREVIEW`             | `true`                 | Whether previewable files should be previewed once clicked on the file entry                                                                                           |
| `ui.footer.enabled`                       | `VERTEILER_FOOTER_ENABLED`             | `true`                 | Whether the footer should be visible                                                                                                                                   |
| `ui.footer.show_credits`                  | `VERTEILER_SHOW_CREDITS`               | `true`                 | Whether to show the `[Served with Verteiler]`-Mark                                                                                                                     |
| `ui.resources.use_custom_resources`       | `VERTEILER_USE_CUSTOM_RESOURCES`       | `false`                | Whether custom web resources should be enabled; allowing to modify the interface design                                                                                |
| `ui.resources.custom_resources_directory` | `VERTEILER_CUSTOM_RESOURCES_DIRECTORY` | `./web`                | Directory to save custom web resources to and serve from                                                                                                               |
| `security.allow_directory_listing`        | `VERTEILER_ALLOW_DIRECTORY_LISTING`    | `true`                 | If file browsing should be enabled                                                                                                                                     |
| `security.allowed_extensions`             | `VERTEILER_ALLOWED_EXTENSIONS`         | `""` *(empty)*         | List of allowed file extensions for file upload; empty means every file type is allowed                                                                                |
| `performance.enable_caching`              | `VERTEILER_ENABLE_CACHING`             | `true`                 | Whether cache controls should be set                                                                                                                                   |
| `performance.cache_max_age`               | `VERTEILER_CACHE_MAX_AGE`              | `3600`                 | Maximum age for cache control                                                                                                                                          |
| `performance.minify_files`                | `VERTEILER_MINIFY_FILES`               | `true`                 | Whether to minify HTML/CSS/JS files; reducing bandwidth                                                                                                                |
</details>

> [!NOTE]
> When using custom web ui resources, please note that all the HTML file template names must stay the same. 
> However, it is possible to add/use custom styling and JS.

> [!NOTE]
> If there are any issues with using custom web resources, it might help turning off the `performance.minify_files` setting.

## API
There are also API endpoints available, to get files, their information, and browse directories.

The (probably) most important one is `/raw/<path>`. This will then directly return the contents of the file at the given path, also enabling the possibility
to serve files for use in other applications.

There are also:

`/api/health`, as a dedicated endpoint, e.g. for uptime monitors.

`curl 0.0.0.0:2987/api/health`
\> `HTTP 200 OK`
```json
{
  "success" : true,
  "timestamp":  1776546140603
}
```

`/api/list/<path>`, to list the contents of the given directory. The output will be a JSON looking like this, but less pretty, like for the other endpoints:

`curl 0.0.0.0:2987/api/list/http/`
\> `HTTP 200 OK`

```json
{
  "success":true,
  "data":{
    "path":"http",
    "entries":[
      {
        "name":"routes",
        "path":"http/routes",
        "directory":true,
        "size":0,
        "lastModified":1775508231,
        "mimeType":""
      },
      {
        "name":"TemplateEngine.java",
        "path":"http/TemplateEngine.java",
        "directory":false,
        "size":4742,
        "lastModified":1775319806,
        "mimeType":"text/x-java-source"
      },
      {
        "name":"WebServer.java",
        "path":"http/WebServer.java",
        "directory":false,
        "size":4806,
        "lastModified":1775500704,
        "mimeType":"text/x-java-source"
      },
      {
        "name":"WebUI.java",
        "path":"http/WebUI.java",
        "directory":false,
        "size":10775,
        "lastModified":1775499489,
        "mimeType":"text/x-java-source"
      }
    ]
  }
}
```
it may also produce this result, whether the directory is empty:

`curl 0.0.0.0:2987/api/list/`
\> `HTTP 404 Not Found`
```json
{
   "success":true,
   "data":"empty"
}
```

For getting more information about a file or directory, you may use `/api/info/<path>`, which will result in a similar output like above:

`curl 0.0.0.0:2987/api/info/http`
\> `HTTP 200 OK`
```json
{
  "success":true,
  "data":{
    "name":"http",
    "path":"http",
    "directory":true,
    "size":0,
    "lastModified":1775508475,
    "mimeType":""
  }
}
```

### Error Messages:

`HTTP 404 Not Found` will occur when a file / directory does not exist at the given path
```json
{
   "success":false,
   "message":"path not found"
}
```

`HTTP 400 Bad Request` will only occur when trying to `/api/list` a file
```json
{
   "success":false,
   "message":"not a directory"
}
```

## Building
... is as straightforward as with any simple maven project. You can simply run `mvn clean package`, and it will output a `Verteiler-[VERSION].jar` file
under `target/`.