package config

type Configuration struct {
	host            string
	port            int16
	dataDirectory   string
	rootUriAsRawUri bool

	logLevel     string
	logAccess    bool
	logToFile    bool
	logDirectory string

	adminEnabled bool
	adminToken   string

	chunkedUploadsEnabled bool
	chunkSize             int
	tempUploadDir         string
	uploadSessionTtl      int

	title          string
	showFileSizes  bool
	showDates      bool
	dragDropUpload bool
	enablePreview  bool

	footerEnabled bool
	showCredits   bool

	useCustomResources       bool
	customResourcesDirectory string

	allowDirectoryListing bool
	allowedExtensions     []string

	enableCaching bool
	cacheMaxAge   int
	minifyFiles   bool
}
