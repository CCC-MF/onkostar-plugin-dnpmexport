# Onkostar Plugin "onkostar-plugin-dnpmexport"

Aufgabe dieses Plugins ist es, die Inhalte der DNPM-Formulare in die Datenstruktur des bwHC-Datenmodells zu wandeln und
anhand festgelegter Regeln die Notwendigkeit zum Export zu prüfen und diesen durchzuführen.

Hierzu verwendet das Plugin die Library `bwhc-dto-java`, eine Rückportierung der bwhC-DTOs für die Programmiersprache Java.

## Einstellungen

Zum Betrieb dieses Plugins ist die Angabe der URL der Zielanwendung erforderlich.

Dies lässt sich initial durch folgende Datenbankanfrage anlegen, später dann in den allgemeinen Einstellungen von Onkostar auch ändern.

```
INSERT INTO einstellung (name, wert, kategorie, beschreibung) VALUES('dnpmexport_url', 'http://localhost:9000/bwhc/etl/api/MTBFile', 'DNPM', 'DNPM-Export - URL');
INSERT INTO einstellung (name, wert, kategorie, beschreibung) VALUES('dnpmexport_prefix', 'TEST', 'DNPM', 'DNPM-Export - Prefix');
```

## Anonymisierung

Sämtliche **Prozedur-IDs** werden anonymisiert, indem aus der bekannten Prozedur-ID ein SHA256-Hash gebildet wird und dieser - zuzüglich Exportprefix - als
40 Zeichen lange, Base32-codierte Zeichenkette verwendet wird.

Somit ist das Erlangen einer direkten Kenntnis über die tatsächliche Prozedur-ID nicht möglich.

Die **ID eines Patienten** wird nicht anonymisiert und kann in nachgelagerten Schritten pseudonymisiert werden.