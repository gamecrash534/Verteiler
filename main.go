package main

import (
	"errors"
	"log"
	"os"
	"verteiler/config"
	"verteiler/storage"
	"verteiler/web"
)

var conf config.Config

func main() {
	conf := *getConf()

	storage.Init(conf.Server.DataDirectory, conf.Admin.TempUploadDir)
	web.RunServer(conf)
}

func getConf() *config.Config {
	env := os.Getenv("VERTEILER_CONFIG")
	if env != "" {
		conf, err := config.ReadConfig(env)
		if err != nil {
			log.Fatalln(err)
		}

		return conf
	}

	args := os.Args
	if len(args) >= 2 && (args[0] == "-c" || args[0] == "--config") {
		conf, err := config.ReadConfig(args[1])
		if err != nil {
			log.Fatalln("Could not get config:", err)
		}

		return conf
	}

	if _, err := os.Stat("./config.toml"); errors.Is(err, os.ErrNotExist) {
		_, err := config.WriteConfig("./config.toml")
		if err != nil {
			log.Fatalln("Could not write default config:", err)
		}

		return &config.DefaultConfig
	}

	conf, err := config.ReadConfig("./config.toml")
	if err != nil {
		log.Fatalln("Could not read config:", err)
	}

	return conf
}
