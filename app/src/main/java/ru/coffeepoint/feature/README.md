# Экраны и пользовательские сценарии

Features сгруппированы по задачам пользователя внутри одного Android-модуля.
Общие UI-элементы находятся в [shared/component](../shared/component),
переходы — в [navigation](../navigation).

| Feature | Что внутри | Где начинать изменение |
| --- | --- | --- |
| [auth](auth) | Выбор роли и ввод пароля | Экран выбора роли; сохранение роли — `core/auth` |
| [dashboard](dashboard) | Список станций, меню управляющего, карточки, поставка и фильтры | `ui/DashboardScreen.kt`, `presentation/DashboardViewModel.kt` |
| [station](station) | Станция, создание, настройки, отображение остатков | `ui/StationScreen.kt`, `presentation/StationViewModel.kt` |
| [service](service) | Заправка оператора, снабженческий процесс, контроль помола | `presentation/ServiceStep1ViewModel.kt`, отдельные ViewModel последующих шагов |
| [warehouse](warehouse) | Склад, пополнение, настройка пределов | `ui/WarehouseStockScreen.kt`, `presentation/WarehouseStockViewModel.kt` |
| [supplycatalog](supplycatalog) | Варианты закупки и краткая информация | `ui/SupplyCatalogScreen.kt`, `presentation/SupplyCatalogEditViewModel.kt` |
| [earnings](earnings) | Начисления, выплаты, взаиморасчёт | `ui/MonthlyEarningsScreen.kt`, `presentation/MonthlyEarningsViewModel.kt` |
| [camera](camera) | Камера и локальные фото; сохранённый временно отключённый сценарий | Не удалять без отдельного изменения процесса |

В `ui` размещается Compose, в `presentation` — состояние и действия экрана,
в `model` — модели и расчёты отображения, в `component` — составные элементы
конкретной feature. Набор подпапок зависит от потребностей feature.

## Перед изменением экрана

1. Проверить [бизнес-процесс](../../../../../../../docs/business-processes.md)
   и роль пользователя. Представление снабженца у управляющего не меняет его роль.
2. Найти маршрут и callbacks в `AppNavigation`: одинаковые элементы могут
   использоваться в нескольких сценариях.
3. Если меняется сохранение, пройти от ViewModel к репозиторию и connector.
4. Для нового экрана добавить маршрут в `Routes` и точку входа в навигации.
5. Общие лоадеры, ошибки и кнопки использовать из `shared/component`.

Не добавлять работу со Spreadsheet/HTTP непосредственно в Compose.
При ошибке отправки учитывать, что запись уже может быть сохранена локально.
