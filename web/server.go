package web

import (
	"embed"
	"html/template"
	"io/fs"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"verteiler/config"
	"verteiler/storage"
	"verteiler/util"
)

//go:embed static
var resources embed.FS
var conf config.Config

var icons = make(map[string]template.HTML)

func RunServer(c config.Config) {
	conf = c
	baseTmpl = LayoutTmpl{
		AppName:     conf.Ui.Title,
		ShowFooter:  conf.Ui.Footer.Enabled,
		ShowCredits: conf.Ui.Footer.ShowCredits,
	}
	registerIcons()

	tmpls = template.Must(template.ParseFS(files, "templates/*.gohtml"))

	mux := http.NewServeMux()
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, "/browse", 301)
	})
	mux.HandleFunc("/assets/{cat}/{f}", resourceHandler)
	mux.HandleFunc("/browse", browse)
	mux.HandleFunc("/browse/{path...}", browse)
	mux.HandleFunc("/preview/{path...}", preview)
	mux.HandleFunc("/raw/{path...}", raw)
	mux.HandleFunc("/download/{path...}", download)

	log.Fatal(http.ListenAndServe(":"+strconv.FormatInt(int64(c.Server.Port), 10), mux))
}

func resourceHandler(w http.ResponseWriter, r *http.Request) {
	http.ServeFileFS(w, r, resources, "static/"+r.PathValue("cat")+"/"+r.PathValue("f"))
}

func raw(w http.ResponseWriter, r *http.Request) {
	serveFile(w, r, false)
}

func download(w http.ResponseWriter, r *http.Request) {
	serveFile(w, r, true)
}

func preview(w http.ResponseWriter, r *http.Request) {
	path := r.PathValue("path")

	d, err := storage.Get(path)
	if err != nil {
		onError(w, 500, "Internal Error", err)
	} else if d.Dir {
		onError(w, 403, "Requested path is a directory", fs.ErrInvalid)
	}

	lt := cdt(LayoutTmpl{
		IsBrowsing: false,
		Title:      "preview / " + path,
	})

	renderPage(w, "preview", PageData{
		LayoutTmpl: lt,
		Data: PreviewTmpl{
			FileName:    d.Name,
			FilePath:    d.Path,
			IcnDownload: icons["download"],
			IcnExternal: icons["external"],
			Size:        util.FmtSize(d.Size),
			Mime:        d.Mime,
			IsImage:     util.IsImage(d.Mime),
			IsVideo:     util.IsVideo(d.Mime),
			IsAudio:     util.IsAudio(d.Mime),
			IsPdf:       util.IsPdf(d.Mime),
			IsText:      util.IsText(d.Mime),
		},
	})
}

func browse(w http.ResponseWriter, r *http.Request) {
	path := r.PathValue("path")

	en, err := storage.GetDirectoryContents(path)
	if err != nil {
		onError(w, 500, "An internal error occurred", err)
		return
	}

	lt := cdt(LayoutTmpl{
		IsBrowsing: true,
		Title:      path,
	})

	renderPage(w, "browse", PageData{
		LayoutTmpl: lt,
		Data: BrowseTmpl{
			Breadcrumb: "/",
			UpIcon:     icons["up"],
			HasParent:  false,
			Entries:    fromStorageEntries(en),
		},
	})
}

func registerIcons() {
	r, err := resources.ReadDir("static/icons")
	if err != nil {
		log.Fatalln("could not read icons:", err)
	}

	for _, e := range r {
		if e.IsDir() {
			continue
		}
		c, err := resources.ReadFile("static/icons/" + e.Name())
		if err != nil {
			log.Fatalln("could not read icon:", err)
		}

		icons[strings.TrimRight(filepath.Base(e.Name()), filepath.Ext(e.Name()))] = template.HTML(c)
	}
}

func serveFile(w http.ResponseWriter, r *http.Request, download bool) {
	path := r.PathValue("path")

	if ex, err := storage.Exists(path); err != nil {
		onError(w, 500, "Internal Error", err)
	} else if !ex {
		onError(w, 404, "Not Found", os.ErrNotExist)
	}

	d, err := storage.Get(path)
	if err != nil {
		onError(w, 500, "Internal Error", err)
	} else if d.Dir {
		onError(w, 403, "Requested path is a directory", fs.ErrInvalid)
	}

	if download {
		w.Header().Set("Content-Disposition", "attachment; filename\""+d.Name+"\"")
	} else {
		w.Header().Set("Content-Disposition", "inline; filename\""+d.Name+"\"")
	}

	http.ServeFile(w, r, d.FullPath)
}
