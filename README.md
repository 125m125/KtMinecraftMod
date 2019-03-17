# KtMinecraftMod
Diese Minecraft Modifikation ermöglicht es, aus Minecraft heraus mit Kadcontrade zu interagieren.

## Installation
- Zuerst muss Liteloader 1.12 oder 1.12.2 installiert werden: https://www.liteloader.com/explore/docs/user:install
- Anschließend kann die Mod aus den Releases ( https://github.com/125m125/KtMinecraftMod/releases ) heruntergeladen und in .minecraft/mods/1.12 bzw. .minecraft/mods/1.12 abgelegt werden.
- Wenn nun das Liteloader 1.12 bzw. 1.12.2-Profil gestartet wird, kann die Mod genutzt werden, sobald einer Welt (lokal oder online) beigetreten wurde

## Anmeldung
- Nach der Installation kann die Benutzeroberfläche mit F12 aktiviert werden (in den Einstellungen änderbar)
- Dort wird nach einer Benutzer ID, Token ID und einem Token gefragt. Diese können in den Einstellungen von Kadcontrade ( https://kt.125m125.de/einstellungen ) erstellt werden.
- Optional kann auch noch ein Passwort für die Mod eingegeben werden. Dieses wird dazu verwendet das Token auf der Festplatte zu verschlüsseln. **Dies ist nicht das Passwort für Minecraft oder Kadcontrade!**
- Danach kann die Anmeldung durch einen Klick auf Benutzer verwenden abgeschlossen werden.
- Bei zukünftigen Anmeldungen wird nur noch nach dem Passwort für die Mod gefragt.

#### Anmeldung mit Zertifikat
Anstelle von dem Login mit Token unterstützt Kadcontrade auch die Verwendung von Klientzertifikaten.
Diese müssen im Ordner .minecraft/liteconfig/common mit dem Namen certificate.p12 abgelegt werden.
Bei der Anmeldung wird nun alternativ nach dem Passwort für das Zertifikat gefragt.

## Verwendung
Nach der Anmeldung gelangt man mit F12 in die Übersicht.
Dort werden die neusten Nachrichten und der aktuelle Itemstand angezeigt.
Durch Benutzung des Mausrades kann in der Itemliste hoch- und runtergescrollt werden.
Durch einen Klick auf ein Item wird die Detailansicht für dieses geöffnet.
Dort wird der aktuelle Handelspreis, die Preisentwicklung, die derzeit aktiven Handelsanfragen und Auszahlungsanfragen angezeigt.
Außerdem können dort neue Auszahlungsanfragen oder Handelsanfragen erstellt werden.

Wenn das Token/Zertifikat nicht alle möglichen Berechtigungen hat, stehen einige Funktionen möglicherweise nicht zur Verfügung.



## FAQ
### Wenn ich versuche mich anzumelden, erscheint die Meldung "Kadcontrade funktioniert nicht mit der mitgelieferten JRE. Bitte stelle Minecraft so ein, dass es eine JRE>1.8u151 verwendet."
Die für https://kt.125m125.de benötigten Verschlüsselungsalgorithmen funktionieren mit der in Minecraft mitgelieferten JRE möglicherweise nicht.
In diesem Fall muss für Minecraft manuell eine andere JRE eingestellt werden.
Dafür muss, sofern es noch nicht geschehen, zuerst eine JRE1.8 auf dem Computer installiert werden ( https://java.com/de/download/help/download_options.xml ).
Anschließend kann im Launcher unter Profile->LiteLoader 1.12 bzw. 1.12.2->Java-Programmdatei auf die installierte javaw.exe verwiesen werden (Windows default ist C:\Program Files\Java\jre1.8.0_<Version>\bin\javaw.exe).
Danach sollte diese Warnung nicht mehr auftreten.
Beachte, dass nach einem Java-Update der Pfad unter Umständen neu gesetzt werden muss.
