package util

import (
	"fmt"
	"html/template"
	"strings"
)

func FmtSize(b int64) string {
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

func ResolveIcon(mime string, dir bool, icons map[string]template.HTML) template.HTML {
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

func IsPreviewable(mime string) bool {
	switch strings.Split(mime, "/")[0] {
	case "text":
		return true
	case "image":
		return true
	case "video":
		return true
	case "audio":
		return true
	}

	if mime == "application/json" || mime == "application/xml" || mime == "application/pdf" {
		return true
	}

	return false
}

func GetCssClass(dir bool) string {
	if dir {
		return "directory"
	} else {
		return "file"
	}
}
