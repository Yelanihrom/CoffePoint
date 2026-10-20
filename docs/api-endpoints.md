# Запросы приложения и регрессионные проверки

[Оглавление документации](README.md) · [Синхронизация](synchronization.md) · [Модель данных](database.md)

Проверено по исходному коду 19.10.2026. Реестр относится к текущей рабочей копии,
а не гарантированно к опубликованной версии Apps Script.

## Адреса и авторизация

| Сервис | Адрес | Авторизация |
| --- | --- | --- |
| Google Sheets / Apps Script | `WEB_APP_URL` из `GoogleSheetsSyncConfig.kt`, путь `/macros/s/{deploymentId}/exec` | GET: query `token`; POST: поле JSON `token`. Сервер читает `SYNC_TOKEN` из Script Properties |
| Команды Vendista из Android | `https://api.vendista.ru:99/` | Query `token`, источник `VendistaCredentials` |
| Продажи Vendista из Apps Script | `https://api.vendista.ru:99/reports/common` | Query `token`, сервер читает `VENDISTA_TOKEN` из Script Properties |
| Обновление Android | `https://api.github.com/repos/Yelanihrom/CoffePoint/releases/latest`, затем `update.json` и APK из assets | Без авторизации; только публичный канал, см. [обновления](app-updates.md) |

Токены в этой документации и тестовых данных заменены фиктивными значениями.
Смена `WEB_APP_URL` требует переустановки собранной версии приложения.

## Реестр Android-запросов

У Google Sheets один физический URL. Операции разделяются параметрами `action`
и `entity`, поэтому ниже приведены отдельные бизнес-сценарии одного endpoint.

| Метод | Action / путь | Entity / параметры | Что отправляем или получаем | Где используется |
| --- | --- | --- | --- | --- |
| GET | `full_sync` | `entities` отсутствует | Все восемь коллекций | Полная синхронизация |
| GET | `full_sync` | `coffee_station,station_inventory,station_settings,warehouse_stock` | Станции, серверные продажи, остатки, настройки, склад | Обновление списка станций и фоновый импорт без каталога/сотрудников |
| GET | `full_sync` | `warehouse_stock` | Только склад и его минимум/максимум | Кнопка обновления склада |
| GET | `full_sync` | `supply_catalog` | Только каталог | Обновление каталога |
| GET | `full_sync` | `employees,employee_station_assignments,employees_transactions` | Сотрудники, привязки, начисления/выплаты | Экран заработка и синхронизация сотрудников |
| POST | `upsert` | `coffee_station` | Данные станции и локальный `updatedAt`; `salesAfterService` и `salesUpdatedAt` не отправляются | Создание станции, информация, аренда, ручное изменение дат узлов |
| POST | `upsert` | `station_inventory` | `stationId`, все девять остатков, локальный `updatedAt` | Заправка оператора, пополнение снабженца, повторная и фоновая отправка |
| POST | `upsert` | `station_settings` | `stationId`, девять пар `Min`/`Max`, локальный `updatedAt` | Настройки станции |
| POST | `complete_service` | `coffee_station` | `id,lastServiceDate,salesAfterService,salesUpdatedAt` | Завершение заправки оператором; повторная отправка этих полей |
| POST | `complete_service` | `coffee_station` | `id` и выбранные `mixersServiceDate` / `liquidWasteContainerServiceDate` | Завершение обслуживания узлов снабженцем |
| POST | `upsert_batch` | `warehouse_stock` | `data.items`: `itemType,quantity,minQuantity,maxQuantity,updatedAt` | Пополнение/настройки склада, списание со склада, повторная/фоновая отправка. До девяти строк одним запросом |
| POST | `upsert` | `supply_catalog` | Вариант закупки, включая `id,itemType,title,supplier,article,url,maxPrice,isActive,priority,comment,updatedAt` | Создание и редактирование варианта |
| POST | `delete` | `supply_catalog` | `data.id` | Удаление варианта. **Есть в Android, отсутствует в локальном `Router.gs`; серверная успешная обработка не покрыта** |
| POST | `upsert` | `employees` | Одна запись сотрудника с локальным `updatedAt` | Отправка несинхронизированных сотрудников |
| POST | `upsert` | `employee_station_assignments` | Одна привязка, её период действия и локальный `updatedAt` | Привязка сотрудника к станции и повторная отправка |
| POST | `upsert` | `employees_transactions` | Одна операция: ID, сотрудник, тип, сумма, даты, локальный `updatedAt` | Выплата и повторная/фоновая отправка операций |
| POST | `terminals/{id}/commands` | `command_id=2` | Перезагрузка терминала | Управление терминалом |
| POST | `terminals/{id}/commands` | `command_id=15,parameter1=1` | Выключение экрана и игнорирование нажатий | Управление терминалом |
| POST | `terminals/{id}/commands` | `command_id=15,parameter1=0` | Включение экрана | Управление терминалом |

Сотрудники, привязки и операции отправляются отдельными upsert на запись;
пакетная оптимизация сейчас применяется к складу. Отправка остатков станции и
списание склада также остаются разными запросами, а не общей транзакцией.

## Серверные запросы и сохранённый функционал

| Вызов | Параметры | Кто запускает | Статус |
| --- | --- | --- | --- |
| GET Vendista `/reports/common` | `DateFrom`, при необходимости `DateTo`, `token` | Apps Script: продажи после заправки и ежедневные начисления | Продажи станций обновляет серверный триггер раз в 10 минут; прямого запроса продаж из Android нет |
| POST Apps Script `upload_photos` | `stationId`, необязательные Base64 фото | Серверная ветка `Router.gs` | Сохранённый модуль камеры; текущего Android-вызова загрузки в сетевом API нет |

Частота серверных триггеров зависит от установленных триггеров проекта Google;
локальный тест не проверяет наличие или выполнение реального триггера.

## Схема full_sync

| Лист | Поле в `data` | Ключ записи |
| --- | --- | --- |
| `coffee_station` | `stations` | `id` |
| `station_inventory` | `inventories` | `stationId` |
| `station_settings` | `settings` | `stationId` |
| `supply_catalog` | `supplyCatalog` | `id` |
| `warehouse_stock` | `warehouseStock` | `itemType` |
| `employees` | `employees` | `id` |
| `employee_station_assignments` | `employeeStationAssignments` | `id` |
| `employees_transactions` | `employeeTransactions` | `id` |

Точный порядок всех столбцов зафиксирован независимо от рабочего `Config.gs` в
`app/src/test/resources/contracts/sheet-schema.json`. При согласованном изменении
API нужно обновить реализацию, этот контракт, образцы и тесты.

## Как обрабатываются ответы

Apps Script возвращает ошибки внутри JSON, обычно с HTTP `200`:

```json
{"success":false,"error":{"status":409,"message":"Conflict: remote row is newer"}}
```

`success=false` нельзя считать успешной записью только по HTTP-статусу.
Android различает `CONFLICT`, `CONNECTION` и `UNKNOWN`. Ошибки конфигурации
сервера (`SYNC_TOKEN`, заголовки) не являются доказательством отсутствия интернета.
Проверки UI-текста ошибок в этот набор не входят.

Пакет склада сначала проверяет ключи, дубликаты и конфликты всех элементов, а
затем пишет строки. Тест подтверждает отсутствие записей при обнаруженном
конфликте; полноценного отката при сбое самого Google Sheets во время записи нет.

## Запуск перед изменениями и перед APK

Локальные тесты не требуют телефона, эмулятора, рабочих токенов или доступа к
Google/Vendista. Для первого запуска Gradle может понадобиться интернет, чтобы
загрузить тестовые зависимости. Нужны JDK 21, Android SDK проекта и Node.js 18+.

```powershell
.\scripts\Test-Api.ps1 -JavaHome 'D:\Android Studio\jbr'
```

Параметр `-JavaHome` можно не передавать, если настроен `JAVA_HOME` или Java есть
в `PATH`. Отдельные наборы запускаются так:

```powershell
.\gradlew.bat :app:testDebugUnitTest
node --test google-apps-script/tests/http-contract.test.cjs
```

Android-отчёт: `app/build/reports/tests/testDebugUnitTest/index.html`.
Apps Script выводит результат в терминал. Один серверный тест отмечен `TODO`:
обработчик `delete` отсутствует в текущей локальной копии; это известный пробел,
а не подтверждённая работоспособность удаления в опубликованном скрипте.

## Что именно проверяется

- Android использует настоящие Retrofit/Gson/OkHttp и локальный HTTPS MockWebServer.
- Проверяются методы, query-параметры, JSON, отдельные сценарии загрузки, пакет склада,
  локальные даты, состав полей обслуживания, каталог, сотрудники, выплаты и команды терминала.
- Проверяются HTTP-ошибки, ошибка в JSON при HTTP `200`, конфликт, DNS-сбой,
  таймаут, отмена coroutine и некорректный ответ сервера.
- Apps Script исполняется в Node VM с таблицами в памяти: проверяются авторизация,
  все восемь схем, выборочный импорт, upsert, конфликты пакета, изоляция полей станции,
  устаревший результат продаж, формат числа и повтор после Vendista `429`.
- Модуль камеры сохраняется; проверяется только маршрутизация серверного вызова,
  без создания файлов в Google Drive.

Это тесты сетевого слоя и серверных обработчиков, а не полное покрытие проекта.
Запись в Room, очередь WorkManager, восстановление после уничтожения процесса,
взаимодействие двух устройств и интерфейс требуют отдельных интеграционных/UI-тестов.

## Проверка настоящего развертывания после обновления Apps Script

Локальные тесты не находят устаревшее развертывание или неправильно настроенные
Script Properties. Для этого добавлена отдельная проверка только чтения:

```powershell
# Задай COFFEEPOINT_SYNC_TOKEN в окружении, затем:
.\scripts\Test-GoogleSheetsDeployment.ps1 -WebAppUrl 'https://script.google.com/macros/s/DEPLOYMENT_ID/exec'
```

Она делает один GET `full_sync`, проверяет успех, восемь коллекций, поля строк и
числовой `salesAfterService`. Записей и команд терминалу не отправляет, токен и
ответы со всеми данными не печатает. Запускай её после публикации новой версии
скрипта и настройки токенов. Если обновлять существующее развертывание через
«Управление развертываниями», его URL сохраняется.
