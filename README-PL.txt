COOL-VPN v1.1.1 – LOGO + KLAWIATURA + TV

Ten pakiet zawiera finalne zmiany do projektu COOL-VPN.

Ważne: pliki należy skopiować do istniejącego repozytorium ProxyBox-master,
które zawiera już wcześniejsze zmiany COOL-VPN.

Najważniejsze zmiany:
1. Nowe logo COOL-VPN w ekranie logowania.
2. Nowa ikona aplikacji na Androidzie.
3. Nowy banner aplikacji dla Android TV.
4. Android TV uruchamia ekran logowania COOL-VPN jako jedyny launcher TV.
5. Klawiatura ekranowa nie powinna zasłaniać pól login/hasło – adjustResize,
   obsługa IME insets oraz automatyczne przewijanie do aktywnego pola.
6. Poprawiona obsługa pilota/fokusu na ekranie logowania TV.
7. Zachowane wylogowanie i czyszczenie sesji/konfiguracji COOL-VPN.
8. Wersja aplikacji: 1.1.1 (versionCode 3).

Po skopiowaniu plików:
  git add .
  git commit -m "COOL-VPN v1.1.1 logo keyboard TV final"
  git pull origin main --rebase
  git push origin main

Następnie GitHub Actions zbuduje APK.
