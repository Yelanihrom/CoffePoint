# Интеграция с Google Sheets

Пакет отвечает за обмен Android с HTTP API Apps Script. Репозитории вызывают
его для отправки и загрузки, а Compose-экраны не формируют JSON самостоятельно.
Это пакет модуля `:app`, а не отдельная Gradle-feature.

## Точки входа

| Файл / папка | Ответственность |
| --- | --- |
| [GoogleSheetsSyncManager.kt](GoogleSheetsSyncManager.kt) | Фасад для вызовов из репозиториев |
| [GoogleSheetsRemoteDataSource.kt](GoogleSheetsRemoteDataSource.kt) | Actions, выбор сущностей, преобразования JSON/моделей и типизация ошибок отправки |
| [GoogleSheetsImportService.kt](GoogleSheetsImportService.kt) | Защита от импорта поверх pending-данных и запись полученного набора в DAO |
| [GoogleSheetsStationSyncService.kt](GoogleSheetsStationSyncService.kt) | Поиск ожидающих станций, выбор частичной/общей отправки и обновление локальных статусов |
| [GoogleSheetsSyncConfig.kt](GoogleSheetsSyncConfig.kt) | URL опубликованного `/exec` и токен клиента |
| [GoogleSheetsSyncMessages.kt](GoogleSheetsSyncMessages.kt) | Общие сообщения по типу ошибки |
| [api](api) | Retrofit-интерфейс GET и POST |
| [dto](dto) | Контракты оболочки запросов и ответов |
| [network](network) | Создание HTTP-клиента и Retrofit |
| [worker](worker) | Периодическая/разовая доотправка изменений |

`GoogleSheetsSyncResult`, `GoogleSheetsEntitySyncResult` и
`GoogleSheetsFullSyncBundle` объявлены в файле RemoteDataSource.

## Правила изменений

- Упавший POST не должен стирать локальную запись или менять её `updatedAt`
  на время повтора. Sync-флаги остаются локальными и не уходят в Sheets.
- Операторские и снабженческие поля отправляются через `complete_service`;
  не заменять их отправкой всей станции.
- Для склада используется один `upsert_batch`, а не запрос на каждую позицию.
- Успешный HTTP `200` проверяется вместе с `success` внутри JSON.
- Сохранение DTO не гарантирует сохранение в Room: эти результаты нужно
  обработать на уровне сервиса/репозитория.
- Отмена coroutine должна распространяться вызывающему коду.

Полные правила: [синхронизация](../../../../../../../../../docs/synchronization.md).
Реестр: [API](../../../../../../../../../docs/api-endpoints.md).
Сервер: [Apps Script](../../../../../../../../../google-apps-script/README.md).

## Проверка

Из корня репозитория выполнить `scripts/Test-Api.ps1`.
Android-тесты используют MockWebServer и фиктивные токены; серверные тесты
исполняют локальные `.gs` в памяти. После публикации сервера дополнительно
нужна проверка `Test-GoogleSheetsDeployment.ps1`.
