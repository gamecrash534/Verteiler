package main

import (
	"verteiler/config"
	"verteiler/web"
)

func main() {
	web.RunServer(config.DefaultConfig)
}
