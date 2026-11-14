package storage

import (
	"io"
	"log"
	"net/http"
	"path/filepath"
	"strings"
	"time"
)

import "os"

var basePath string
var tempDir string

type Entry struct {
	Name     string
	Path     string
	FullPath string
	Dir      bool
	Size     int64
	LastMod  time.Time
	Mime     string
}

func Init(bp string, td string) {
	var err error
	basePath, err = filepath.Abs(bp)
	if err != nil {
		log.Fatalln(err)
	}
	tempDir = td

	if d, err := os.Stat(bp); os.IsNotExist(err) || !d.IsDir() {
		os.MkdirAll(bp, 0750)
	}
}

func Store(path string, data []byte) error {
	p := Paths(basePath, path)
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
		Name:     stat.Name(),
		Path:     path,
		FullPath: path,
		Dir:      stat.IsDir(),
		Size:     stat.Size(),
		LastMod:  stat.ModTime(),
	}

	if entry.Dir {
		return &entry, nil
	}

	f, err := os.Open(entry.FullPath)
	if err != nil {
		return nil, err
	}
	defer f.Close()

	mt, err := ProbeContentType(f)
	if err != nil {
		return nil, err
	}
	entry.Mime = *mt

	return &entry, nil
}

func GetFileContents(path string) ([]byte, error) {
	return os.ReadFile(Paths(path))
}

func GetDirectoryContents(path string) ([]Entry, error) {
	p := Paths(basePath, path)
	l, err := os.ReadDir(p)

	if err != nil {
		return nil, err
	}

	res := make([]Entry, len(l))
	for i, fe := range l {
		en, err := Get(Paths(p, fe.Name()))
		if err != nil {
			return nil, err
		}

		res[i] = *en
	}

	return res, nil
}

func Paths(keys ...string) string {
	var res strings.Builder
	res.WriteString(keys[0])

	for _, key := range keys[1:] {
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
