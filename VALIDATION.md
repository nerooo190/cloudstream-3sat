# Prüfprotokoll

Geprüft am 30.10.2026 in einer Linux-Cloud-Umgebung mit Java 21, Gradle 8.12,
Android SDK 35 und Build Tools 35.0.0.

```text
THREESAT_LIVE_TEST=1 gradle :ThreeSat:testDebugUnitTest make makePluginsJson
BUILD SUCCESSFUL
```

- Sechs Parser-Tests erfolgreich: Hauptplayer, eingebettete Clips, Empfehlungen,
  URL-Auflösung, Suchkarten, Tonspuren, Untertitel und DRM-Filter.
- Ein Netzwerktest erfolgreich: aktuelle 3sat-Suche, daraus ein Video mit zehn
  Stream-Adressen sowie der Livestream mit vier Stream-Adressen.
- Das erzeugte `ThreeSat.cs3` enthält `classes.dex` und ein gültiges
  `manifest.json` mit `de.cloudstream.threesat.ThreeSatPlugin` als Einstiegspunkt.
- Repository-Manifest und Platzhalterprüfung des Python-Hilfsskripts geprüft.
- Die Veröffentlichung auf `builds` wurde lokal mit einem Git-Remote simuliert:
  erste Veröffentlichung und anschließendes Update erfolgreich.

Der Status der GitHub-Builds ist unter
https://github.com/nerooo190/cloudstream-3sat/actions einsehbar. Wiedergabe, Casting
und Bedienung auf einem Android-Gerät wurden hier nicht geprüft. Die Netzwerktests
bestätigen die API-Auflösung; die Verfügbarkeit der CDN-Streams hängt weiterhin
von Region, Sendezeit und Rechten ab.
