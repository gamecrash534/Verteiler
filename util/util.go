package util

import (
	"fmt"
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

func GetCssClass(dir bool) string {
	if dir {
		return "directory"
	} else {
		return "file"
	}
}
