# Zole

Bezsaistes Zoles kāršu spēle Android telefonam. Tu spēlē pret diviem botiem (viegls, vidējs, grūts). Minimālistisks dizains, melns OLED režīms pēc noklusējuma.

## Lejupielāde

APK ir pieejams [Releases](../../releases) sadaļā. Katra būvējuma APK atrodams arī GitHub Actions darba artefaktā `zole-apk`.

## Moduļi

| Modulis | Saturs |
|---|---|
| `:engine` | Tīrs Kotlin spēles dzinējs: kārtis, noteikumi, solīšana, gājieni, punktu skaitīšana, pules, partijas vēsture. Bez Android atkarībām. |
| `:ai` | Boti: viegls (noteikumi), vidējs (heiristika), grūts (Monte Carlo ar determinizāciju). |
| `:core:ui` | Tēma, krāsas, Inter fonts, kāršu un vēdekļa komponentes, gājiena taimeris. |
| `:core:data` | Iestatījumi (DataStore), aktīvās partijas saglabāšana (JSON), vēsture un statistika (Room). |
| `:feature:menu` | Sākuma ekrāns, jaunā spēle, noteikumi. |
| `:feature:game` | Spēles galds: solīšana, kāršu nolikšana, izspēle, dales rezultāts. |
| `:feature:stats` | Punktu tabula un statistika. |
| `:feature:settings` | Iestatījumi. |
| `:app` | Aktivitāte, navigācija, atkarību sasaiste. |

## Būvēšana

Nepieciešams JDK 17+ un Android SDK (compileSdk 35).

```
./gradlew :engine:test :ai:test     # dzinēja un botu testi
./gradlew assembleDebug             # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:recordRoborazziDebug # ekrānuzņēmumi uz app/build/screenshots/
```

### Release parakstīšana

`assembleRelease` paraksta ar atslēgu no vides mainīgajiem `ZOLE_KEYSTORE_PATH`, `ZOLE_KEYSTORE_PASSWORD`, `ZOLE_KEY_ALIAS`, `ZOLE_KEY_PASSWORD`. Ja to nav, izmanto debug atslēgu.

GitHub Actions izmanto secrets `ZOLE_KEYSTORE_BASE64` (keystore base64 formātā), `ZOLE_KEYSTORE_PASSWORD`, `ZOLE_KEY_ALIAS`, `ZOLE_KEY_PASSWORD`. Bez tiem katrs CI būvējums tiek parakstīts ar citu debug atslēgu, un jaunu versiju nevar uzinstalēt pāri vecajai.

Jaunu relīzi izveido, uzliekot tagu: `git tag v0.1 && git push origin v0.1`.

## Konfigurācija kodā

- Gājiena laiks: `TurnTimer.durationMs` failā `feature/game/.../GameViewModel.kt` (noklusējums 15 000 ms).
- Punktu vērtības un noteikumu varianti: `Rules` failā `engine/.../Rules.kt`.

## Licences

Fonts Inter ir licencēts ar SIL Open Font License 1.1 (`core/ui/src/main/assets/inter-ofl.txt`).
