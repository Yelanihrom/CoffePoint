package ru.coffeepoint.core.sync.google

import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class SalesAnalyticsFailure { NOT_CONFIGURED, API_OUTDATED, UNAUTHORIZED, SERVER_CONFIGURATION, SERVER_ERROR }

class SalesAnalyticsException(val reason: SalesAnalyticsFailure) : IllegalStateException(reason.name)

internal fun salesAnalyticsServerFailure(status: Int?, message: String?): SalesAnalyticsFailure = when {
    status == 401 || status == 403 -> SalesAnalyticsFailure.UNAUTHORIZED
    message == "Unsupported action" ||
        (message?.contains("getSalesAnalytics") == true && message.contains("not defined")) -> SalesAnalyticsFailure.API_OUTDATED
    message?.startsWith("Missing Script Property:") == true ||
        message == "SYNC_TOKEN is not configured" -> SalesAnalyticsFailure.SERVER_CONFIGURATION
    else -> SalesAnalyticsFailure.SERVER_ERROR
}

fun salesAnalyticsErrorMessage(error: Exception): String = when (error) {
    is SalesAnalyticsException -> when (error.reason) {
        SalesAnalyticsFailure.NOT_CONFIGURED -> "Не настроено подключение к серверу аналитики"
        SalesAnalyticsFailure.API_OUTDATED -> "Обновите развёртывание Apps Script: метод аналитики ещё недоступен"
        SalesAnalyticsFailure.UNAUTHORIZED -> "Сервер отклонил доступ. Проверьте токен синхронизации"
        SalesAnalyticsFailure.SERVER_CONFIGURATION -> "Проверьте настройки токенов в Apps Script"
        SalesAnalyticsFailure.SERVER_ERROR -> "Ошибка Apps Script или Vendista. Повторите запрос позже"
    }
    is UnknownHostException -> "Не удалось подключиться к серверу. Проверьте интернет и VPN"
    is SocketTimeoutException -> "Сервер не успел ответить. Повторите запрос позже"
    is HttpException -> if (error.code() == 401 || error.code() == 403)
        "Сервер отклонил доступ. Проверьте доступность развёртывания Apps Script"
        else "Ошибка HTTP ${error.code()}. Повторите запрос позже"
    else -> "Не удалось получить корректный отчёт. Повторите запрос позже"
}
