package web

import (
	"embed"
	"log"
	"net/http"
	"strconv"
	"verteiler/config"
	"verteiler/web/routes"
	"verteiler/web/templates"
)

//go:embed static
var res embed.FS
var conf *config.Config

func RunServer(c *config.Config) {
	conf = c
	templates.InitTmpls(c, &res)

	mux := http.NewServeMux()
	mux.HandleFunc("/{$}", func(w http.ResponseWriter, r *http.Request) {
		if conf.Security.AllowDirectoryListing {
			http.Redirect(w, r, "/browse", 301)
		} else {
			templates.OnError(w, 401, "Not Allowed", nil)
		}
	})
	mux.HandleFunc("/assets/{cat}/{f}", resourceHandler)
	mux.HandleFunc("/favicon.svg", favicon)

	if conf.Security.AllowDirectoryListing {
		mux.HandleFunc("/browse", routes.Browse)
		mux.HandleFunc("/browse/{path...}", routes.Browse)
		mux.HandleFunc("/search", routes.Search)
	}
	if conf.Ui.EnablePreview {
		mux.HandleFunc("/preview/{path...}", routes.Preview)
	}
	mux.HandleFunc("/raw/{path...}", routes.Raw)
	mux.HandleFunc("/download/{path...}", routes.Download)
	if conf.Server.RootUriAsRawUri {
		mux.HandleFunc("/{path...}", routes.Raw)
	}

	log.Fatal(http.ListenAndServe(":"+strconv.FormatInt(int64(c.Server.Port), 10), mux))
}

func resourceHandler(w http.ResponseWriter, r *http.Request) {
	http.ServeFileFS(w, r, res, "static/"+r.PathValue("cat")+"/"+r.PathValue("f"))
}

func favicon(w http.ResponseWriter, r *http.Request) {
	http.ServeFileFS(w, r, res, "static/favicon.svg")
}
