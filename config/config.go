package config

import (
	"crypto/rand"
	"errors"
	"os"

	"github.com/pelletier/go-toml/v2"
)

type Server struct {
	Port            int16
	DataDirectory   string
	RootUriAsRawUri bool
}

type Logging struct {
	Level        string
	LogAccess    bool
	LogToFile    bool
	LogDirectory string
}

type Admin struct {
	Enabled               bool
	Token                 string
	ChunkedUploadsEnabled bool
	ChunkSize             int
	TempUploadDir         *os.File `toml:""`
	UploadSessionTtl      int
}

type Ui struct {
	Title          string
	ShowFileSizes  bool
	ShowDates      bool
	DragDropUpload bool
	EnablePreview  bool
	Footer         UiFooter
}

type UiFooter struct {
	Enabled     bool
	ShowCredits bool
}

type Security struct {
	AllowDirectoryListing bool
	AllowedExtensions     []string
}

type Performance struct {
	EnableCaching bool
	CacheMaxAge   int
}

type Config struct {
	Server      Server
	Logging     Logging
	Admin       Admin
	Ui          Ui
	Security    Security
	Performance Performance
}

var DefaultConfig = Config{
	Server: Server{
		Port:            2987,
		DataDirectory:   "./files",
		RootUriAsRawUri: false,
	},
	Logging: Logging{
		Level:        "",
		LogAccess:    true,
		LogToFile:    false,
		LogDirectory: "./logs",
	},
	Admin: Admin{
		Enabled:               true,
		Token:                 rand.Text(),
		ChunkedUploadsEnabled: false,
		ChunkSize:             5242880,
		UploadSessionTtl:      900000,
	},
	Ui: Ui{
		Title:          "Verteiler",
		ShowFileSizes:  true,
		ShowDates:      true,
		DragDropUpload: true,
		EnablePreview:  true,
		Footer: UiFooter{
			Enabled:     true,
			ShowCredits: true,
		},
	},
	Security: Security{
		AllowDirectoryListing: true,
		AllowedExtensions:     nil,
	},
	Performance: Performance{
		EnableCaching: false,
		CacheMaxAge:   0,
	},
}

func getTempDir() *os.File {
	f, e := os.CreateTemp("", "verteiler")
	if e != nil {
		panic(e)
	}

	return f
}

func ReadConfig(path string) (*Config, error) {
	_, err := os.Stat(path)

	if errors.Is(err, os.ErrNotExist) {
		_, err := WriteConfig(path)
		if err != nil {
			return nil, err
		}
	} else if err != nil {
		return nil, err
	}

	res, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}

	var out Config
	out.Admin.TempUploadDir = getTempDir()

	err = toml.Unmarshal(res, &out)
	return &out, err
}

func WriteConfig(path string) ([]byte, error) {
	f, err := os.Create(path)
	if err != nil {
		return nil, err
	}

	conf, err := toml.Marshal(DefaultConfig)
	if err != nil {
		return nil, err
	}

	f.Write(conf)
	return conf, nil
}
