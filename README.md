# 3sat-Mediathek für Cloudstream

Inoffizieller deutscher Provider für [Cloudstream](https://github.com/recloudstream/cloudstream).
Er verwendet die Website und die Player-API von [3sat](https://www.3sat.de/).

## In Cloudstream installieren

Unter **Einstellungen → Erweiterungen → Repository hinzufügen** diese Adresse eintragen:

```text
https://raw.githubusercontent.com/nerooo190/cloudstream-3sat/builds/repo.json
```

Danach **ThreeSat** aus dem Repository installieren. Der Provider erscheint als
**3sat Mediathek**. Der [automatische Build](https://github.com/nerooo190/cloudstream-3sat/actions)
veröffentlicht das Plugin und künftige Updates auf dem Branch `builds`.

## Funktionen

- Empfehlungen, Dokumentationen, Wissen, Kultur und Kabarett
- Suche mit Umlauten, Sonderzeichen und weiteren Ergebnisseiten
- Einzelvideos und auf Sendungsseiten sichtbare Videoeinträge
- 3sat-Livestream
- HLS, DASH und direkte MP4/WebM-Streams, soweit von 3sat angeboten
- Untertitel und getrennt beschriftete Tonspuren, einschließlich Audiodeskription
- Aktuelle Player-Tokens und Stream-Adressen bei jedem Abspielaufruf

## Eigenes GitHub-Repository veröffentlichen

1. Ein **öffentliches** GitHub-Repository, beispielsweise `cloudstream-3sat`, anlegen und diese Dateien auf den Branch `main` hochladen. Auch versteckte Dateien wie `.github/workflows/build.yml` übernehmen.
2. Unter **Actions** den Workflow **Build and publish** ausführen bzw. den automatischen Lauf nach dem Push abwarten.
3. Der Workflow legt den Branch `builds` selbst an. Dort stehen anschließend `ThreeSat.cs3`, `plugins.json` und ein bereits auf dein Repository angepasstes `repo.json`.
4. In Cloudstream **Einstellungen → Erweiterungen → Repository hinzufügen** öffnen und folgende Adresse eintragen, mit deinem GitHub-Namen und Repository-Namen:

   ```text
   https://raw.githubusercontent.com/DEIN-NAME/cloudstream-3sat/builds/repo.json
   ```

5. Im hinzugefügten Repository **ThreeSat** installieren; der Provider erscheint als **3sat Mediathek**.

Der Workflow verwendet das eingebaute `GITHUB_TOKEN`; ein persönlicher Token ist nicht erforderlich. Er benötigt Schreibrechte für den Branch `builds`. Organisationsregeln oder Branchregeln müssen dies zulassen.

Für einen Fork lässt sich die Quelldatei `repo.json` auf das eigene Repository anpassen. Der Workflow erzeugt die Datei auf **`builds`** automatisch mit der richtigen Adresse:

```bash
python3 scripts/generate_repo.py DEIN-GITHUB-NAME/DEIN-REPOSITORY
```

Ein Push auf `main` oder `master` aktualisiert das veröffentlichte Plugin. Bei Änderungen am Provider die Ganzzahl `version` in `ThreeSat/build.gradle.kts` erhöhen, damit Cloudstream das Update erkennt. Pull Requests bauen und testen das Plugin ohne Veröffentlichung.

## Direktinstallation

Die Datei `ThreeSat.cs3` lässt sich auch über die lokale Plugin-Installation von Cloudstream importieren. Im Actions-Lauf liegt sie zusätzlich im Artefakt **cloudstream-3sat**. Für automatische Updates das Repository wie oben hinzufügen.

## Lokal bauen

Benötigt werden JDK 21 sowie ein Android SDK mit Plattform 35 und Build Tools 35.0.0. `ANDROID_HOME` setzen oder `sdk.dir=/pfad/zum/android-sdk` in einer unversionierten `local.properties` hinterlegen.

```bash
chmod +x gradlew
./gradlew :ThreeSat:testDebugUnitTest make makePluginsJson
```

Ausgaben:

```text
ThreeSat/build/ThreeSat.cs3
build/plugins.json
```

Für einen Fork beim lokalen Erzeugen von `plugins.json` vorher `GITHUB_REPOSITORY=DEIN-GITHUB-NAME/DEIN-REPOSITORY` setzen. Ohne diese Variable wird `nerooo190/cloudstream-3sat` verwendet. In GitHub Actions wird das aktuelle Repository automatisch erkannt.

Die Parser-Tests laufen ohne Netzwerk. Ein zusätzlicher Test prüft die aktuelle Suche, ein Suchergebnis und den Livestream gegen die 3sat-API:

```bash
THREESAT_LIVE_TEST=1 ./gradlew :ThreeSat:testDebugUnitTest --rerun-tasks
```

Dieser Test prüft die Auflösung der Stream-Adressen. Die Wiedergabe und das Casting müssen zusätzlich auf dem jeweiligen Cloudstream-Gerät geprüft werden.

## Umfang und Verfügbarkeit

Die Startbereiche und Sendungsseiten zeigen die im abgerufenen HTML enthaltenen Einträge. Die Suche unterstützt weitere Ergebnisseiten. Das dynamische Nachladen vollständiger Sendungsarchive ist derzeit nicht implementiert. Videos ohne eigenes Player-Element können nicht abgespielt werden.

Es gelten die von 3sat gesetzten Verfügbarkeitsfristen, regionalen Beschränkungen und Jugendschutzzeiten. DRM-geschützte Formate werden übersprungen. Website- oder API-Änderungen können Anpassungen am Provider erfordern.

## Aufbau

```text
ThreeSat/src/main/kotlin/de/cloudstream/threesat/
  ThreeSatPlugin.kt      Registrierung in Cloudstream
  ThreeSatProvider.kt    Startseite, Suche, Details und Wiedergabe
  ThreeSatParser.kt      HTML-/JSON-Auswertung
ThreeSat/src/test/       Parser-Tests und optionaler API-Test
.github/workflows/      Build und Veröffentlichung auf builds
scripts/generate_repo.py
```

Entwicklerdokumentation: [Cloudstream – Getting started](https://recloudstream.github.io/csdocs/devs/gettingstarted/).

Dieses Projekt ist weder ein offizielles 3sat-Angebot noch mit 3sat oder ZDF verbunden. Die Namen und das vom Anbieter geladene Logo gehören ihren jeweiligen Rechteinhabern.
