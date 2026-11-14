package config

import (
	"errors"
	"os"

	"github.com/pelletier/go-toml/v2"
)

type Configuration struct {
	Port            int16  `toml:"server.port"`
	DataDirectory   string `toml:"server.data_directory"`
	RootUriAsRawUri bool   `toml:"server.root_path_as_raw_path"`

	LogLevel     string `toml:"logging.level"`
	LogAccess    bool   `toml:"logging.log_access"`
	LogToFile    bool   `toml:"logging.log_to_file"`
	LogDirectory string `toml:"logging.log_directory"`

	AdminEnabled bool   `toml:"admin.enabled"`
	AdminToken   string `toml:"admin.token"`

	ChunkedUploadsEnabled bool `toml:"admin.chunked_uploads.enabled"`
	ChunkSize             int  `toml:"admin.chunked_uploads.chunk_size"`
	TempUploadDir         *os.File
	UploadSessionTtl      int `toml:"admin.chunked_uploads.session_ttl"`

	Title          string `toml:"ui.title"`
	ShowFileSizes  bool   `toml:"ui.show_file_sizes"`
	ShowDates      bool   `toml:"ui.show_dates"`
	DragDropUpload bool   `toml:"ui.drag_drop_upload"`
	EnablePreview  bool   `toml:"ui.enable_preview"`

	FooterEnabled bool `toml:"ui.footer.enabled"`
	ShowCredits   bool `toml:"ui.footer.show_credits"`

	UseCustomResources       bool   `toml:"ui.resources.use_custom_resources"`
	CustomResourcesDirectory string `toml:"ui.resources.custom_resources_directory"`

	AllowDirectoryListing bool     `toml:"security.allow_directory_listing"`
	AllowedExtensions     []string `toml:"security.allowed_extensions"`

	EnableCaching bool `toml:"performance.enable_caching"`
	CacheMaxAge   int  `toml:"performance.cache_max_age"`
	MinifyFiles   bool `toml:"performance.minify_files"`
}

var DefaultConfig = Configuration{
	Port:                     2987,
	DataDirectory:            "./data",
	RootUriAsRawUri:          false,
	LogLevel:                 "INFO",
	LogAccess:                true,
	LogToFile:                false,
	LogDirectory:             "./logs",
	AdminEnabled:             true,
	AdminToken:               "%s",
	ChunkedUploadsEnabled:    true,
	ChunkSize:                5242880,
	TempUploadDir:            getTempDir(),
	UploadSessionTtl:         900000,
	Title:                    "Verteiler",
	ShowFileSizes:            true,
	ShowDates:                true,
	DragDropUpload:           true,
	EnablePreview:            true,
	FooterEnabled:            true,
	ShowCredits:              true,
	UseCustomResources:       false,
	CustomResourcesDirectory: "./web",
	AllowDirectoryListing:    true,
	AllowedExtensions:        nil,
	EnableCaching:            true,
	CacheMaxAge:              3600,
	MinifyFiles:              true,
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
		out, err := WriteConfig(path)
		return out, err
	} else {
		return "", nil
	}
}

func WriteConfig(path string) (string, error) {
	f, err := os.Create(path)
	if err != nil {
		return "", err
	}

	conf, err := toml.Marshal(DefaultConfig)
	if err != nil {
		return "", err
	}

	f.Write(conf)
	return string(conf), nil
}
