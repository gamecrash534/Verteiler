package routes

import (
	"html/template"
	"io/fs"
	"net/http"
	"strings"
	"verteiler/config"
	"verteiler/storage"
	"verteiler/util"
	"verteiler/web/templates"
)

func BaseMux(conf *config.Config) *http.ServeMux {
	mux := http.NewServeMux()

	mux.HandleFunc("/{$}", func(w http.ResponseWriter, r *http.Request) {
		if conf.Security.AllowDirectoryListing {
			http.Redirect(w, r, "/browse", 301)
		} else {
			templates.OnError(w, 401, "Not Allowed", nil)
		}
	})

	if conf.Security.AllowDirectoryListing {
		mux.HandleFunc("/browse", Browse)
		mux.HandleFunc("/browse/{path...}", Browse)
		mux.HandleFunc("/search", Search)
	}
	if conf.Ui.EnablePreview {
		mux.HandleFunc("/preview/{path...}", Preview)
	}
	mux.HandleFunc("/raw/{path...}", Raw)
	mux.HandleFunc("/download/{path...}", Download)

	return mux
}

func Raw(w http.ResponseWriter, r *http.Request) {
	serveFile(w, r, false)
}

func Download(w http.ResponseWriter, r *http.Request) {
	serveFile(w, r, true)
}

func Preview(w http.ResponseWriter, r *http.Request) {
	path := r.PathValue("path")

	d, err := storage.Get(path)
	if err != nil {
		templates.OnError(w, 500, "Internal Error", err)
		return
	} else if d.Dir {
		templates.OnError(w, 403, "Requested path is a directory", fs.ErrInvalid)
		return
	}

	lt := cdt(templates.LayoutTmpl{
		IsBrowsing: false,
		Title:      "preview / " + path,
	})

	templates.RenderPage(w, "preview", templates.PageData{
		LayoutTmpl: lt,
		Data: templates.PreviewTmpl{
			FileName:    d.Name,
			FilePath:    d.Path,
			IcnDownload: templates.Icons["download"],
			IcnExternal: templates.Icons["external"],
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

func Browse(w http.ResponseWriter, r *http.Request) {
	path := r.PathValue("path")

	en, err := storage.GetDirectoryContents(path)
	if err != nil {
		templates.OnError(w, 500, "An internal error occurred", err)
		return
	}

	lt := cdt(templates.LayoutTmpl{
		IsBrowsing: true,
		Title:      path,
	})

	templates.RenderPage(w, "browse", templates.PageData{
		LayoutTmpl: lt,
		Data: templates.BrowseTmpl{
			Breadcrumb: breadcrumb(path),
			UpIcon:     templates.Icons["up"],
			HasParent:  false,
			Entries:    templates.FromStorageEntries(en),
		},
	})
}

func Search(w http.ResponseWriter, r *http.Request) {
	query := r.URL.Query().Get("q")

	var en []storage.Entry
	if query != "" {
		var err error
		en, err = storage.SearchDirectoryContents(query)
		if err != nil {
			templates.OnError(w, 500, "An internal error occurred", err)
			return
		}
	}

	lt := cdt(templates.LayoutTmpl{
		IsSearching: true,
		Title:       query,
	})

	templates.RenderPage(w, "search", templates.PageData{
		LayoutTmpl: lt,
		Data: templates.SearchTmpl{
			Query:   query,
			Entries: templates.FromStorageEntries(en),
		},
	})
}

func serveFile(w http.ResponseWriter, r *http.Request, download bool) {
	path := r.PathValue("path")

	if ex, err := storage.Exists(path); err != nil {
		templates.OnError(w, 500, "Internal Error", err)
		return
	} else if !ex {
		templates.OnError(w, 404, "Not Found", nil)
		return
	}

	d, err := storage.Get(path)
	if err != nil {
		templates.OnError(w, 500, "Internal Error", err)
		return
	} else if d.Dir {
		templates.OnError(w, 403, "Requested path is a directory", fs.ErrInvalid)
		return
	}

	if download {
		w.Header().Set("Content-Disposition", "attachment; filename\""+d.Name+"\"")
	} else {
		w.Header().Set("Content-Disposition", "inline; filename\""+d.Name+"\"")
	}

	http.ServeFile(w, r, d.FullPath)
}

func breadcrumb(path string) template.HTML {
	const base = "<a href='/browse/'>/</a>"
	if path == "" || path == "/" {
		return base
	}

	var res strings.Builder
	var cpath strings.Builder

	res.WriteString(base)
	parts := strings.Split(path, "/")
	for i, p := range parts {
		cpath.WriteString(p)
		if i == len(parts)-1 {
			res.WriteString(p + "/")
			continue
		}

		res.WriteString("<a href='/browse/" + cpath.String() + "'>" + p + "/</a>")

		cpath.WriteString("/")
	}

	return template.HTML(res.String())
}

func cdt(tmpl templates.LayoutTmpl) templates.LayoutTmpl {
	t := templates.BaseTmpl
	t.Title = tmpl.Title
	t.IsAdmin = tmpl.IsAdmin
	t.IsBrowsing = tmpl.IsBrowsing
	t.IsOnAdmin = tmpl.IsOnAdmin
	t.IsSearching = tmpl.IsSearching
	return t
}
