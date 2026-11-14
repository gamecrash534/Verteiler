package web

import (
	"embed"
	"html/template"
	"log"
	"net/http"
	"path/filepath"
	"strconv"
	"strings"
	"verteiler/config"
	"verteiler/storage"
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
	mux.HandleFunc("/assets/{cat}/{f}", resourceHandler)
	mux.HandleFunc("/browse", browse)
	mux.HandleFunc("/browse/{path}", browse)

	log.Fatal(http.ListenAndServe(":"+strconv.FormatInt(int64(c.Server.Port), 10), mux))
}

func resourceHandler(w http.ResponseWriter, r *http.Request) {
	http.ServeFileFS(w, r, resources, "static/"+r.PathValue("cat")+"/"+r.PathValue("f"))
}

func browse(w http.ResponseWriter, r *http.Request) {
	path := r.PathValue("path")

	en, err := storage.GetDirectoryContents(path)
	if err != nil {
		log.Println("error while trying to get contents:", err)
		httpError(w, 500, "An internal error occurred")
		return
	}

	lt := cdt(LayoutTmpl{
		IsBrowsing: true,
		IsAdmin:    true,
		Title:      "browse",
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
