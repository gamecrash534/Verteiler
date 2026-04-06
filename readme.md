# Verteiler
A small software providing a simple file browser to quickly share and distribute files

## Usage
### Installation
Probably the best way to run this software is through the docker container: `git.gamecrash.dev/game.crash/verteiler:latest`

If you want to run it from the blank jar, you need to build it yourself - as there is currently no up-to-date version of the jar being uploaded.

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
| `server.host`                             | `VERTEILER_HOST`                       | `0.0.0.0`              | The host IP to bind to                                                                                                                                                 |
| `server.port`                             | `VERTEILER_PORT`                       | `2987`                 | The port the server will listen on                                                                                                                                     |
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

## Building
... is as straightforward as with any simple maven project. You can simply run `mvn clean package`, and it will output a `Verteiler-[VERSION].jar` file
under `target/`.