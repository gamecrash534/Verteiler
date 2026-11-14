package web

import (
	"bytes"
	"embed"
	"html/template"
	"log"
	"net/http"
	"strconv"
	"verteiler/storage"
)

type LayoutTmpl struct {
	AppName     string
	Title       string
	IsBrowsing  bool
	IsSearching bool
	IsAdmin     bool
	IsOnAdmin   bool
	ShowCredits bool
	ShowFooter  bool
}

type BrowseTmpl struct {
	Breadcrumb string
	HasParent  bool
	ParentUrl  string
	UpIcon     string
	Entries    []BrowseEntry
}

type BrowseEntry struct {
	Href     string
	Class    string
	Icon     string
	Name     string
	ShowSize bool
	ShowDate bool
	Size     string
	Date     string
}

type PageData struct {
	LayoutTmpl
	Body template.HTML
	Data any
}

type ErrorTmpl struct {
	Code    string
	Message string
}

//go:embed templates
var files embed.FS
var tmpls *template.Template

var baseTmpl LayoutTmpl

func renderPage(w http.ResponseWriter, tmplName string, pageData PageData) {
	var body bytes.Buffer
	if err := tmpls.ExecuteTemplate(&body, tmplName, pageData.Data); err != nil {
		log.Printf("render %s: %v\n", tmplName, err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
	}

	if err := tmpls.ExecuteTemplate(w, "layout", PageData{
		LayoutTmpl: pageData.LayoutTmpl,
		Data:       pageData.Data,
		Body:       template.HTML(body.String()),
	}); err != nil {
		log.Printf("render layout: %v", err)
	}
}

func fromStorageEntries(entry []storage.Entry) []BrowseEntry {
	entries := make([]BrowseEntry, len(entry))

	for i, o := range entry {
		entries[i] = BrowseEntry{
			Href:     "",
			Class:    "",
			Icon:     "",
			Name:     o.Name,
			ShowSize: conf.Ui.ShowFileSizes,
			ShowDate: conf.Ui.ShowDates,
			Size:     strconv.FormatInt(o.Size, 10),
			Date:     o.LastMod.String(),
		}
	}

	return entries
}

func httpError(w http.ResponseWriter, code int, msg string) {
	tmpl := baseTmpl
	tmpl.Title = "Error"

	renderPage(w, "error", PageData{
		LayoutTmpl: tmpl,
		Data: ErrorTmpl{
			Code:    strconv.FormatInt(int64(code), 10),
			Message: msg,
		},
	})
}

func cdt(tmpl LayoutTmpl) LayoutTmpl {
	t := baseTmpl
	t.Title = tmpl.Title
	t.IsAdmin = tmpl.IsAdmin
	t.IsBrowsing = tmpl.IsBrowsing
	t.IsOnAdmin = tmpl.IsOnAdmin
	t.IsSearching = tmpl.IsSearching
	return t
}
