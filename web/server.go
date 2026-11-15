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

	mux := routes.BaseMux(conf)
	mux.Handle("/api/", http.StripPrefix("/api", routes.ApiMux(conf)))
	mux.HandleFunc("/assets/{cat}/{f}", resourceHandler)
	mux.HandleFunc("/favicon.svg", favicon)

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
