package storage

import (
	"io"
	"net/http"
	"strings"
	"time"
)

import "os"

var basePath string
var tempDir string

type Entry struct {
	name     string
	path     string
	fullPath string
	dir      bool
	size     int64
	lastMod  time.Time
	mime     string
}

func Init(bp string, td string) {
	basePath = bp
	tempDir = td

	if d, err := os.Stat(bp); os.IsNotExist(err) || !d.IsDir() {
		os.MkdirAll(bp, 0750)
	}
}

func Store(path string, data []byte) error {
	p := Paths(path)
	f, err := os.Create(p)
	if err != nil {
		return err
	}

	defer f.Close()

	if _, err = f.Write(data); err != nil {
		return err
	}

	return nil
}

func Get(path string) (*Entry, error) {
	stat, err := os.Stat(path)
	if err != nil {
		return nil, err
	}

	entry := Entry{
		name:     stat.Name(),
		path:     path,
		fullPath: Paths(path),
		dir:      stat.IsDir(),
		size:     stat.Size(),
		lastMod:  stat.ModTime(),
	}

	if entry.dir {
		return &entry, nil
	}

	f, err := os.Open(entry.fullPath)
	if err != nil {
		return nil, err
	}
	defer f.Close()

	mt, err := ProbeContentType(f)
	if err != nil {
		return nil, err
	}
	entry.mime = *mt

	return &entry, nil
}

func GetFileContents(path string) ([]byte, error) {
	return os.ReadFile(Paths(path))
}

func Paths(keys ...string) string {
	var res strings.Builder
	res.WriteString(basePath)

	for _, key := range keys {
		res.WriteString(string(os.PathSeparator) + key)
	}

	return res.String()
}

func ProbeContentType(r io.Reader) (*string, error) {
	b := make([]byte, 512)
	_, err := r.Read(b)

	if err != nil {
		return nil, err
	}
	res := http.DetectContentType(b)

	return &res, nil
}
