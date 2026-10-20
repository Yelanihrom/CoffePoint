# CoffeePoint

Android-приложение для работы с кофе-станциями: заправка, снабжение, обслуживание,
склад, каталог закупок и взаиморасчёты с оператором.

## Документация

| Что нужно узнать | Где читать |
| --- | --- |
| С чего начать и как поддерживать документацию | [Оглавление](docs/README.md) |
| Где лежит нужный код | [Карта папок проекта](docs/project-structure.md) |
| Как связаны приложение, БД и внешние сервисы | [Архитектура](docs/architecture.md) |
| Что делают оператор, снабженец и управляющий | [Бизнес-процессы](docs/business-processes.md) |
| Что происходит при сохранении, офлайне и конфликте | [Синхронизация и уведомления](docs/synchronization.md) |
| Какие данные хранятся локально и в Google Sheets | [Модель данных](docs/database.md) |
| Какие запросы есть и как запускать автотесты | [API и проверки](docs/api-endpoints.md) |
| Как собрать APK, назначить версию и опубликовать релиз | [Выпуск приложения](docs/releases.md) |
| Что пока не реализовано или требует осторожности | [Известные ограничения](docs/known-limitations.md) |

## Запуск проекта

1. Открыть корень репозитория в Android Studio и дождаться Gradle Sync.
2. Установить Android SDK, указанный в [app/build.gradle.kts](app/build.gradle.kts).
   Сейчас `compileSdk` и `targetSdk` равны 37, минимальный API устройства — 26.
3. Использовать JDK 21 для Gradle. Android Studio может предоставить его через `jbr`.
4. Проверить `WEB_APP_URL` и `SYNC_TOKEN` в
   [GoogleSheetsSyncConfig.kt](app/src/main/java/ru/coffeepoint/core/sync/google/GoogleSheetsSyncConfig.kt).
   На сервере настроить одноимённое свойство `SYNC_TOKEN`.
5. Выбрать эмулятор или телефон и запустить конфигурацию `app`.

`local.properties`, ключи подписи, пароли и APK исключены из Git.
Значения рабочих токенов в документацию не добавляем.

## Проверки и сборка

В PowerShell из корня проекта, с примером пути к JDK на Windows:

```powershell
.\scripts\Test-Api.ps1 -JavaHome 'D:\Android Studio\jbr'

$env:JAVA_HOME = 'D:\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.
Для release-сборки и обновлений поверх установленного приложения см.
[руководство по выпуску](docs/releases.md).

## Основные части

| Папка | Назначение |
| --- | --- |
| [app](app) | Единственный Android Gradle-модуль: интерфейс, Room, запросы, воркеры |
| [google-apps-script](google-apps-script/README.md) | Серверный код для Google Sheets и Vendista; публикуется отдельно |
| [docs](docs/README.md) | Документация, привязанная к версии исходников |
| [scripts](scripts) | Локальный запуск тестов и проверка развертывания |

Продажи станций и ежедневные начисления получает сервер Apps Script.
Из Android напрямую в Vendista отправляются только команды терминалу.

Документация описывает рабочую копию исходников на 20.10.2026.
Код в GitHub, установленный APK и опубликованное развертывание Apps Script
обновляются отдельными действиями и могут иметь разные версии.
