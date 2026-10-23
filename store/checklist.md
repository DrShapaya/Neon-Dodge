# Чек-лист публикации Neon Dodge 1.7 в RuStore

## 1. Готово в репозитории

- [x] APK 1.7 собран, `versionCode = 8`, `versionName = 1.7`
- [x] Подпись релиза настроена для v1 + v2 + v3
- [x] Иконка 512×512 (`store/icon-512.png`)
- [x] Четыре актуальных скриншота 1080×2400 (`store/screenshots/`)
- [x] Карточка приложения обновлена под 1.7 (`store/listing-ru.md`)
- [x] Политика конфиденциальности (`store/privacy-policy-ru.md`)
- [x] В Git версионируются Markdown-тексты и скриншоты карточки
- [x] Комплект APK 1.7 лежит в `dist/NeonDodge-1.7.apk`

## 2. Перед отправкой в RuStore

- [ ] Указать контактный e-mail в `listing-ru.md` и `privacy-policy-ru.md`
- [ ] Опубликовать политику конфиденциальности по публичному URL
- [ ] Проверить установку APK 1.7 на реальном устройстве или эмуляторе
- [ ] Сделать резервную копию `keystore/release.jks` и паролей
- [ ] Проверить, что в консоли RuStore выбран тот же package name и ключ подписи

## 3. Загрузка карточки

1. Загрузить `dist/NeonDodge-1.7.apk`.
2. Заполнить название, краткое и полное описание из `listing-ru.md`.
3. Загрузить `store/icon-512.png`.
4. Загрузить скриншоты в порядке: меню → игра → пауза → game over.
5. Указать категорию «Игры → Аркады», возрастной рейтинг 6+ и теги из карточки.
6. В декларации безопасности указать: данные не собираются и не передаются; рекламы и сторонних SDK нет; используется только вибрация.
7. Отправить приложение на модерацию.

## 4. Проверка APK

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.1.0\apksigner.bat" `
  verify --min-sdk-version 23 --verbose dist\NeonDodge-1.7.apk

& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.1.0\aapt2.exe" `
  dump badging dist\NeonDodge-1.7.apk | Select-Object -First 1
```

Ожидается: `package name='com.isaev.neondodge' versionCode='8' versionName='1.7'`.
