# MultiMail

Android-приложение для работы с несколькими почтовыми аккаунтами.

Репозиторий: https://github.com/Greznish297477/MultiMail

## Обновления

Приложение проверяет новые версии в [GitHub Releases](https://github.com/Greznish297477/MultiMail/releases).
Чтобы выпустить обновление, увеличьте `versionCode` и `versionName` в `app/build.gradle.kts`, дополните `CHANGELOG.md`, затем отправьте тег формата `v1.2.0`. Workflow GitHub Actions соберёт release APK и прикрепит его к релизу.

### Однократная настройка подписанного APK

Чтобы APK из GitHub Releases устанавливался и обновлялся у пользователей, создайте ключ подписи только один раз и никому его не передавайте:

```powershell
keytool -genkeypair -v -keystore multimail-release.jks -alias multimail -keyalg RSA -keysize 2048 -validity 10000
```

Затем добавьте в **GitHub → Settings → Secrets and variables → Actions** четыре secrets:

- `RELEASE_KEYSTORE_BASE64` — результат команды `certutil -encode multimail-release.jks encoded.txt`, из `encoded.txt` скопируйте Base64-текст без строк `BEGIN/END CERTIFICATE`;
- `RELEASE_STORE_PASSWORD` — пароль keystore;
- `RELEASE_KEY_ALIAS` — `multimail`;
- `RELEASE_KEY_PASSWORD` — пароль ключа.

Не удаляйте `multimail-release.jks`: все будущие APK должны быть подписаны этим же ключом, иначе Android не сможет установить обновление поверх старой версии.

## Что уже работает

- **Google Sign-In** (OAuth) — Gmail + Drive + Chat скоупы
- Безопасное хранение токенов (`EncryptedSharedPreferences`)
- Свайп по аватару для переключения аккаунтов
- Изменение порядка аккаунтов
- Вкладки: Почта / Чат (только Google) / Диски

## Google Client ID

```
730720800458-bh94728mt4hct9frhuigicj2ru8u3blu.apps.googleusercontent.com
```

SHA-1 debug:
```
1C:65:AF:5F:3B:8C:82:C7:5D:29:64:EC:E1:02:28:AC:13:80:47:F9
```

## Как собрать и запустить

1. Открой проект в Android Studio
2. Дождись синхронизации Gradle (нужны новые зависимости)
3. Запусти на эмуляторе или реальном устройстве
4. Нажми **+** → выбери **Google** → войди в аккаунт

## Важно

- В Google Cloud Console должен быть создан OAuth Client ID типа **Android** с package name `com.example.multiaccountmail` и твоим SHA-1.
- Экран согласия OAuth должен быть настроен (хотя бы в тестовом режиме).
- Для тестирования добавь свой Google-аккаунт в список тестовых пользователей (если приложение в статусе Testing).

## Структура новых файлов

```
auth/
  └── GoogleAuthHelper.kt     ← Google Sign-In логика + Client ID
data/
  ├── Account.kt              ← модель с токенами
  ├── AccountRepository.kt
  └── TokenStorage.kt         ← EncryptedSharedPreferences
```

## Следующие шаги

- Получение access_token для реальных запросов к Gmail/Drive API
- Yandex OAuth (нужны Client ID + Secret)
- Mail.ru OAuth
- Реальная загрузка писем и файлов
