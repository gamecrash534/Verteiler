package web

import (
	"html/template"
	"log"
	"net/http"
	"strconv"
	"verteiler/config"
)

func RunServer(conf config.Configuration) {
	layoutTmpl = template.Must(template.ParseFS(files, "templates/layout.gohtml", "templates/browse.gohtml"))

	mux := http.NewServeMux()
	mux.HandleFunc("/browse", browse)

	log.Fatal(http.ListenAndServe(":"+strconv.FormatInt(int64(conf.Port), 10), mux))
}

func browse(w http.ResponseWriter, r *http.Request) {
	renderPage(w, PageData{
		LayoutTmpl: LayoutTmpl{
			AppName:     "Verteiler",
			Title:       "Browse",
			IsBrowsing:  true,
			IsSearching: false,
			IsAdmin:     true,
			IsOnAdmin:   false,
			ShowCredits: true,
			ShowFooter:  true,
		},
		BrowseTmpl: BrowseTmpl{
			Breadcrumb: "/",
			HasParent:  false,
			Entries: []BrowseEntry{
				{Href: "/browse/file.txt", Class: "file", Icon: "[file]", Name: "file.txt", ShowSize: true, ShowDate: true, Size: "12 KB", Date: "2024-01-01"},
			},
			Empty: false,
		},
	})
}
