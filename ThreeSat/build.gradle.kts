version = 1

cloudstream {
    description = "3sat-Mediathek: Dokumentationen, Wissen, Kultur, Kabarett und Livestream."
    authors = listOf("3sat Cloudstream Contributors")
    status = 1
    language = "de"
    tvTypes = listOf("Movie", "TvSeries", "Live")
    requiresResources = false
    iconUrl = "https://www.3sat.de/assets/3sat-logo-rot-100~384x216"
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
