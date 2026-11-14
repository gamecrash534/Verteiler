package util

import (
	"html/template"
	"strings"
)

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

	if mime == "application/json" || mime == "application/xml" || mime == "application/pdf" || mime == "image/svg+xml" {
		return true
	}

	return false
}

func IsText(mime string) bool {
	if split(mime) == "text" {
		return true
	}

	if mime == "application/json" || mime == "application/xml" {
		return true
	}

	return false
}

func IsImage(mime string) bool {
	return split(mime) == "image"
}

func IsVideo(mime string) bool {
	return split(mime) == "video"
}

func IsAudio(mime string) bool {
	return split(mime) == "audio"
}

func IsPdf(mime string) bool {
	return mime == "application/pdf"
}

func split(mime string) string {
	return strings.Split(mime, "/")[0]
}
