package config

import (
	"errors"
	"github.com/pelletier/go-toml/v2"
	"os"
	"path/filepath"
)

type Configuration struct {
	host            string `toml:"server.host"`
	port            int16  `toml:"server.port"`
	dataDirectory   string `toml:"server.data_directory"`
	rootUriAsRawUri bool   `toml:"server.root_path_as_raw_path"`

	logLevel     string `toml:"logging.level"`
	logAccess    bool   `toml:"logging.log_access"`
	logToFile    bool   `toml:"logging.log_to_file"`
	logDirectory string `toml:"logging.log_directory"`

	adminEnabled bool   `toml:"admin.enabled"`
	adminToken   string `toml:"admin.token"`

	chunkedUploadsEnabled bool `toml:"admin.chunked_uploads.enabled"`
	chunkSize             int  `toml:"admin.chunked_uploads.chunk_size"`
	tempUploadDir         *os.File
	uploadSessionTtl      int `toml:"admin.chunked_uploads.session_ttl"`

	title          string `toml:"ui.title"`
	showFileSizes  bool   `toml:"ui.show_file_sizes"`
	showDates      bool   `toml:"ui.show_dates"`
	dragDropUpload bool   `toml:"ui.drag_drop_upload"`
	enablePreview  bool   `toml:"ui.enable_preview"`

	footerEnabled bool `toml:"ui.footer.enabled"`
	showCredits   bool `toml:"ui.footer.show_credits"`

	useCustomResources       bool   `toml:"ui.resources.use_custom_resources"`
	customResourcesDirectory string `toml:"ui.resources.custom_resources_directory"`

	allowDirectoryListing bool     `toml:"security.allow_directory_listing"`
	allowedExtensions     []string `toml:"security.allowed_extensions"`

	enableCaching bool `toml:"performance.enable_caching"`
	cacheMaxAge   int  `toml:"performance.cache_max_age"`
	minifyFiles   bool `toml:"performance.minify_files"`
}

var DefaultConfig = Configuration{
	host:                     "0.0.0.0",
	port:                     2987,
	dataDirectory:            "./data",
	rootUriAsRawUri:          false,
	logLevel:                 "INFO",
	logAccess:                true,
	logToFile:                false,
	logDirectory:             "./logs",
	adminEnabled:             true,
	adminToken:               "%s",
	chunkedUploadsEnabled:    true,
	chunkSize:                5242880,
	tempUploadDir:            getTempDir(),
	uploadSessionTtl:         900000,
	title:                    "Verteiler",
	showFileSizes:            true,
	showDates:                true,
	dragDropUpload:           true,
	enablePreview:            true,
	footerEnabled:            true,
	showCredits:              true,
	useCustomResources:       false,
	customResourcesDirectory: "./web",
	allowDirectoryListing:    true,
	allowedExtensions:        nil,
	enableCaching:            true,
	cacheMaxAge:              3600,
	minifyFiles:              true,
}

func getTempDir() *os.File {
	f, e := os.CreateTemp("", "verteiler")
	if e != nil {
		panic(e)
	}

	return f
}

func ReadConfig(path string) (string, error) {
	_, err := os.Stat(path)

	if errors.Is(err, os.ErrNotExist) {
		out, error := WriteConfig(path)
		return out, error
	} else {
		return "", nil
	}
}

func WriteConfig(path string) (string, error) {
	f, err := os.Create(path)

	if err != nil {
		return "", err
	}

	conf, err2 := toml.Marshal(DefaultConfig)

	if err2 != nil {
		return "", err
	}

	f.Write(conf)
	return string(conf), nil
}

func getConfigPath(args []string) (string, error) {
	path := os.Getenv("VERTEILER_CONFIG")
	if !(len(path) > 0) {
		return filepath.Abs(path)
	}

	for i, a := range args {
		if (a == "-c" || a == "--config") && len(args) > i+1 {
			return filepath.Abs(args[i+1])
		}
	}

	return filepath.Abs("./config.toml")
}
