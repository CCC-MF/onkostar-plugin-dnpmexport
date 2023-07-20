# Onkostar Plugin "onkostar-plugin-dnpmexport"

Aufgabe dieses Plugins ist es, die Inhalte der DNPM-Formulare in die Datenstruktur des bwHC-Datenmodells zu wandeln und
anhand festgelegter Regeln die Notwendigkeit zum Export zu prüfen und diesen durchzuführen.

Hierzu verwendet das Plugin die Library `bwhc-dto-java`, eine Rückportierung der bwhC-DTOs für die Programmiersprache Java.

# Einstellungen

Zum Betrieb dieses Plugins ist die Angabe der URL der Zielanwendung erforderlich.

Dies lässt sich initial durch folgende Datenbankanfrage anlegen, später dann in den allgemeinen Einstellungen von Onkostar auch ändern.

```
INSERT INTO einstellung (name, wert, kategorie, beschreibung) VALUES('dnpmexport_url', 'http://localhost:9000/bwhc/etl/api/MTBFile', 'DNPM', 'DNPM-Export - URL');
```