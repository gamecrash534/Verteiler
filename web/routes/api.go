package routes

import (
	"encoding/json"
	"log"
	"net/http"
	"time"
	"verteiler/config"
	"verteiler/storage"
)

type HealthRes struct {
	Timestamp int64 `json:"timestamp"`
}

type ListRes struct {
	Path    string      `json:"path"`
	Entries []DataEntry `json:"entries"`
}

type DataEntry struct {
	Name         string `json:"name"`
	Path         string `json:"path"`
	Directory    bool   `json:"directory"`
	Size         int64  `json:"size"`
	LastModified int64  `json:"lastModified"`
	MimeType     string `json:"mimeType"`
}

func ApiMux(conf *config.Config) *http.ServeMux {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", health)
	mux.HandleFunc("GET /info/{path...}", info)
	if conf.Security.AllowDirectoryListing {
		mux.HandleFunc("GET /list/{path...}", list)
	}

	return mux
}

func health(w http.ResponseWriter, r *http.Request) {
	writeRes(HealthRes{
		Timestamp: time.Now().UnixMilli(),
	}, w, 200)
}

func info(w http.ResponseWriter, r *http.Request) {
	path := r.PathValue("path")
	if !checkExists(w, path) {
		return
	}

	d, err := storage.Get(path)
	if err != nil {
		log.Println("An error occurred:", err)
		http.Error(w, "An internal error occurred", 500)
		return
	}

	en := DataEntry{
		Name:         d.Name,
		Path:         d.Path,
		Directory:    d.Dir,
		Size:         d.Size,
		LastModified: d.LastMod.Unix(),
		MimeType:     d.Mime,
	}

	writeRes(en, w, 200)
}

func list(w http.ResponseWriter, r *http.Request) {
	path := r.PathValue("path")
	if !checkExists(w, path) {
		return
	}

	id, err := storage.IsDir(path)
	if err != nil {
		log.Println("An error occurred:", err)
		http.Error(w, "An internal error occurred", 500)
		return
	}
	if !id {
		http.Error(w, "Path is not a directory", 400)
		return
	}

	sen, err := storage.GetDirectoryContents(path)
	if err != nil {
		log.Println("An error occurred:", err)
		http.Error(w, "An internal error occurred", 500)
		return
	}

	en := make([]DataEntry, len(sen))
	for i, e := range sen {
		en[i] = DataEntry{
			Name:         e.Name,
			Path:         e.Path,
			Directory:    e.Dir,
			Size:         e.Size,
			LastModified: e.LastMod.Unix(),
			MimeType:     e.Mime,
		}
	}

	writeRes(ListRes{
		Path:    path,
		Entries: en,
	}, w, 200)
}

func writeRes(d any, w http.ResponseWriter, code int) {
	res, err := json.Marshal(d)
	if err != nil {
		log.Println("An error occurred:", err)
		http.Error(w, "An internal error occurred", 500)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	_, err = w.Write(res)
	if err != nil {
		log.Println("An error occurred:", err)
		http.Error(w, "An internal error occurred", 500)
	}
}

func checkExists(w http.ResponseWriter, p string) bool {
	ex, err := storage.Exists(p)
	if err != nil {
		log.Println("An error occurred:", err)
		http.Error(w, "An internal error occurred", 500)
		return false
	}

	if !ex {
		http.Error(w, "Not Found", 404)
		return false
	}

	return true
}
