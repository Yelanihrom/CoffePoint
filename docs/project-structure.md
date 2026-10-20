# Карта папок проекта

Корень Kotlin-кода: `app/src/main/java/ru/coffeepoint`.
Проект содержит один Gradle-модуль `:app`; `feature` и `core` — пакеты внутри него,
а не отдельные Gradle-модули.

```text
CoffePoint/
  app/
    build.gradle.kts
    src/main/
      AndroidManifest.xml
      java/ru/coffeepoint/
        MainActivity.kt
        core/
          auth/
          data/
            local/{dao,entity}/
            mapper/
            preferences/
            remote/{api,dto,network,repository}/
            repository/
          notification/
          status/
          sync/google/{api,dto,network,worker}/
          util/
          worker/
        feature/
          auth/
          camera/
          dashboard/
          earnings/
          service/
          station/
          supplycatalog/
          warehouse/
        navigation/
        shared/component/
        ui/theme/
      res/
    src/test/
    src/androidTest/
  google-apps-script/
    tests/
  docs/
  scripts/
  gradle/
```

## Android-код

| Путь относительно Kotlin-корня | Что здесь искать |
| --- | --- |
| `MainActivity.kt` | Запрос разрешения уведомлений, регистрация обоих фоновых расписаний, запуск Compose |
| `navigation` | Маршруты экранов, аргументы станции/варианта, переходы по ролям |
| `core/auth` | Enum ролей и сохранение выбранной роли |
| `core/data/local` | `AppDatabase`, создание БД и миграции |
| `core/data/local/entity` | Структура локальных таблиц Room |
| `core/data/local/dao` | SQL-запросы, выборка неотправленных записей, обновление sync-статусов |
| `core/data/mapper` | Преобразования Room Entity и моделей станции |
| `core/data/preferences` | Время последнего импорта и сохранённое состояние заработка |
| `core/data/repository` | Сохранение станции, склада, каталога, сотрудников и операций |
| `core/data/remote` | Прямые команды терминалам Vendista; не импорт продаж |
| `core/sync/google` | Вся транспортная интеграция с Apps Script, её DTO и преобразования |
| `core/sync/google/worker` | Фоновая отправка локальных изменений |
| `core/worker` | Фоновая загрузка данных и запуск проверки уведомлений |
| `core/notification` | Условия, роли, тексты и канал уведомлений |
| `core/status` | Состояние станции по продажам и времени после заправки |
| `core/util` | Общие преобразования времени |
| `shared/component` | Общие кнопки, нижняя панель действий, счётчики, индикатор синхронизации и ошибки |
| `ui/theme` | Цвета, типографика, тема Compose |

## Экраны

| Feature | Ответственность |
| --- | --- |
| `auth` | Выбор роли и ввод пароля |
| `dashboard` | Список станций, главное меню управляющего, плашки склада/поставки и их фильтры |
| `station` | Карточка, создание и настройки станции, компоненты остатков |
| `service` | Шаги оператора и снабженца, выбор источника товара, контроль помола |
| `warehouse` | Остатки склада, пополнение и настройка минимумов/максимумов |
| `supplycatalog` | Варианты закупки, редактирование и краткая информация по позиции |
| `earnings` | История начислений/выплат, взаиморасчёт и выплата управляющим |
| `camera` | Сохранённый модуль фотографий; сейчас обязательные фото отключены в рабочем процессе |

Внутри features используются `ui`, `presentation`, `model`, `component` там,
где они нужны. Не каждая feature обязана иметь все эти подпапки.

## Ресурсы, сервер и проверки

| Путь от корня репозитория | Назначение |
| --- | --- |
| `app/src/main/res/drawable-nodpi` | Растровые иконки WebP и схема кнопок аппарата |
| `app/src/main/res/drawable` | Векторные Android-ресурсы |
| `app/src/main/res/mipmap-*` | Иконка приложения |
| `app/src/test` | JVM-тесты сетевых запросов и JSON-контракты |
| `app/src/androidTest` | Инструментальные тесты; полноценного UI-набора пока нет |
| `google-apps-script` | Код, вручную синхронизируемый с проектом Apps Script |
| `config/environments` | Несекретные идентификаторы отдельного тестового стенда |
| `google-apps-script/tests` | Серверные тесты Node.js на таблицах в памяти |
| `scripts` | Локальные проверки, генерация версии в документации, подготовка тестового сервера и запуск clasp |
| `.github/workflows` | Проверка документации, тесты и сборка CI APK |
| `AGENTS.md` | Краткие инструкции и карта контекста для кодирующего агента |
| `gradle` | Wrapper, настройки JVM Gradle и каталог версий библиотек |
| `docs` | Описание архитектуры, процессов и эксплуатации |

`app/build`, `.gradle`, `.idea` и `.codex` содержат генерируемые или локальные
данные и не являются исходниками приложения. `local.properties` и
`release-signing.properties` также остаются локальными.

## Быстрый поиск

| Задача | Начать с |
| --- | --- |
| Изменить действие кнопки на станции | `feature/station/ui/StationScreen.kt`, затем соответствующая ViewModel |
| Проверить поля завершения заправки | `core/sync/google/GoogleSheetsRemoteDataSource.kt`, `google-apps-script/SheetsRepository.gs` |
| Понять причины редких уведомлений | `core/worker/StationAttentionWorker.kt`, затем `core/notification` |
| Изменить состав складских позиций | `feature/warehouse/model/WarehouseStockItem.kt`, затем БД и API |
| Проверить начисление заработка | `google-apps-script/DailyEarnings.gs` |
| Добавить новый экран | Feature, затем `navigation/Routes.kt` и `AppNavigation.kt` |
| Выпустить APK | [Руководство по релизам](releases.md) |
