package web

import (
	"html/template"
	"log"
	"net/http"
	"strconv"
	"verteiler/config"
	"verteiler/storage"
)

var conf config.Config

func RunServer(c config.Config) {
	conf = c
	baseTmpl = LayoutTmpl{
		AppName:     conf.Ui.Title,
		ShowFooter:  conf.Ui.Footer.Enabled,
		ShowCredits: conf.Ui.Footer.ShowCredits,
	}

	tmpls = template.Must(template.ParseFS(files, "templates/*.gohtml"))

	mux := http.NewServeMux()
	mux.HandleFunc("/browse", browse)

	log.Fatal(http.ListenAndServe(":"+strconv.FormatInt(int64(c.Server.Port), 10), mux))
}

func browse(w http.ResponseWriter, r *http.Request) {
	en, err := storage.GetDirectoryContents("")
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
			HasParent:  false,
			Entries:    fromStorageEntries(en),
		},
	})
}
