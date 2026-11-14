package web

import (
	"embed"
	"html/template"
	"net/http"
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
	Empty      bool
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
	BrowseTmpl
}

//go:embed templates
var files embed.FS
var layoutTmpl *template.Template

func renderPage(w http.ResponseWriter, pageData PageData) {
	if err := layoutTmpl.ExecuteTemplate(w, "layout", pageData); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
	}
}
