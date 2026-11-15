package templates

import (
	"bytes"
	"embed"
	"html/template"
	"log"
	"net/http"
	"path/filepath"
	"strconv"
	"strings"
	"time"
	"verteiler/config"
	"verteiler/storage"
	"verteiler/util"
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
	Breadcrumb template.HTML
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

type SearchTmpl struct {
	Query   string
	Entries []BrowseEntry
}

type PreviewTmpl struct {
	FileName    string
	FilePath    string
	IcnDownload template.HTML
	IcnExternal template.HTML
	Size        string
	Mime        string
	IsImage     bool
	IsVideo     bool
	IsAudio     bool
	IsPdf       bool
	IsText      bool
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

//go:embed *
var files embed.FS

var tmpls *template.Template
var conf *config.Config

var BaseTmpl LayoutTmpl
var Icons = make(map[string]template.HTML)

func InitTmpls(c *config.Config, res *embed.FS) {
	conf = c

	BaseTmpl = LayoutTmpl{
		AppName:     conf.Ui.Title,
		ShowFooter:  conf.Ui.Footer.Enabled,
		ShowCredits: conf.Ui.Footer.ShowCredits,
	}

	registerIcons(res)

	tmpls = template.Must(template.ParseFS(files, "*.gohtml"))
}

func RenderPage(w http.ResponseWriter, tmplName string, pageData PageData) {
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

func FromStorageEntries(entry []storage.Entry) []BrowseEntry {
	entries := make([]BrowseEntry, len(entry))

	for i, o := range entry {
		var href string
		if o.Dir {
			href = "/browse" + o.Path
		} else if util.IsPreviewable(o.Mime) && conf.Ui.EnablePreview {
			href = "/preview" + o.Path
		} else {
			href = "/download" + o.Path
		}

		entries[i] = BrowseEntry{
			Href:     href,
			Class:    util.GetCssClass(o.Dir),
			Icon:     util.ResolveIcon(o.Mime, o.Dir, Icons),
			Name:     o.Name,
			ShowSize: conf.Ui.ShowFileSizes,
			ShowDate: conf.Ui.ShowDates,
			Date:     o.LastMod.Format(time.DateTime),
		}
		if o.Dir {
			entries[i].Size = "-"
		} else {
			entries[i].Size = util.FmtSize(o.Size)
		}
	}

	return entries
}

func OnError(w http.ResponseWriter, code int, msg string, err error) {
	tmpl := BaseTmpl
	tmpl.Title = "Error"

	RenderPage(w, "error", PageData{
		LayoutTmpl: tmpl,
		Data: ErrorTmpl{
			Code:    strconv.FormatInt(int64(code), 10),
			Message: msg,
		},
	})

	if code != 404 {
		log.Printf("An error occurred: %s\n- sent to client: %v %s", err, code, msg)
	}
}

func registerIcons(resources *embed.FS) {
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

		Icons[strings.TrimRight(filepath.Base(e.Name()), filepath.Ext(e.Name()))] = template.HTML(c)
	}
}
