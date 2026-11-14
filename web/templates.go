package web

import (
	"bytes"
	"embed"
	"fmt"
	"html/template"
	"log"
	"net/http"
	"strconv"
	"strings"
	"time"
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
	UpIcon     template.HTML
	Entries    []BrowseEntry
}

type BrowseEntry struct {
	Href     string
	Class    string
	Icon     template.HTML
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
			Icon:     resolveIcon(o.Mime, o.Dir),
			Name:     o.Name,
			ShowSize: conf.Ui.ShowFileSizes,
			ShowDate: conf.Ui.ShowDates,
			Date:     o.LastMod.Format(time.DateTime),
		}
		if o.Dir {
			entries[i].Size = "-"
		} else {
			entries[i].Size = fmtSize(o.Size)
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

func fmtSize(b int64) string {
	const u = 1024
	if b < u {
		return fmt.Sprintf("%dB", b)
	}

	d, e := int64(u), 0
	for n := b / u; n >= u; n /= u {
		d *= u
		e++
	}

	return fmt.Sprintf("%.1f%ciB", float64(b)/float64(d), "KMGTPE"[e])
}

func resolveIcon(mime string, dir bool) template.HTML {
	if dir {
		return icons["folder"]
	}

	switch strings.Split(mime, "/")[0] {
	case "text":
		return icons["text"]
	case "image":
		return icons["image"]
	case "video":
		return icons["video"]
	case "audio":
		return icons["audio"]
	}

	if mime == "application/json" || mime == "application/xml" {
		return icons["text"]
	}
	if mime == "application/pdf" {
		return icons["pdf"]
	}

	if strings.Contains(mime, "zip") || strings.Contains(mime, "tar") || strings.Contains(mime, "tar") {
		return icons["archive"]
	}

	return icons["file"]
}
