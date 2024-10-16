package ru.kazantsev.nsd.modules.web_api_components

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.Field
import ru.naumen.core.server.script.api.injection.InjectApi
import ru.naumen.core.server.script.spi.AggregateContainerWrapper
import ru.naumen.core.server.script.spi.IScriptDtObject
import ru.naumen.core.server.script.spi.ScriptDate
import ru.naumen.core.shared.dto.ISDtObject

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.Part
import java.text.DateFormat
import java.text.SimpleDateFormat

import static ru.kazantsev.nsd.sdk.global_variables.ApiPlaceholder.utils

@SuppressWarnings("unused")
@Field String MODULE_NAME = 'webApiComponents'

/** Конcтанты модуля */
@SuppressWarnings("unused")
class Constants {
    /** Паттерн для парсинга даты по умолчанию */
    static final String DEFAULT_PARSER_DATE_FORMAT_PATTERN = "dd.MM.yyyy HH:mm:ss"
    /** Часовой пояс для парсинга даты по умолчанию */
    static final String DEFAULT_PARSER_TIME_ZONE_ID = "UTC"
    /** Коды атрибутов для сериализация вложенных объектов */
    static final List<String> DEFAULT_DT_OBJECT_ATTRS = ['title', 'UUID']
    /** Кодировка по умолчанию */
    static final String DEFAULT_CHARSET = 'UTF-8'
}

/**
 * Утилитарный класс, внедряется в closure обработки запроса в RequestProcessor.
 * Переменные запроса будут заранее внедрены.
 */
@InjectApi
@SuppressWarnings("unused")
class WebApiUtilities {

    RequestProcessor processor
    HttpServletRequest request
    HttpServletResponse response
    ISDtObject user
    Preferences prefs

    protected WebApiUtilities(RequestProcessor processor) {
        this.processor = processor
        this.request = processor.request
        this.response = processor.response
        this.user = processor.user
        this.prefs = processor.prefs
    }

    //Внутренние утилитарные методы:

    /**
     * Получить исключение о отсутствии body
     * @return исключение
     */
    protected static WebApiException.BadRequest getNoBodyException() {
        new WebApiException.BadRequest("Отсутствует тело запроса или не передан параметр 'raw' со значением 'true'.")
    }

    /**
     * Получить BadRequest исключение, причиной которого является отсутствие параметра
     * @param paramName имя параметра, где конвертируется параметр
     * @return BadRequest исключение
     */
    protected static WebApiException.BadRequest getNoParamException(String paramName) {
        return new WebApiException.BadRequest("Не указан параметр $paramName")
    }

    /**
     * Получить BadRequest исключение, причиной которого является исключение при попытке конвертации
     * @param paramName имя параметра, где конвертируется параметр
     * @param value значения параметра
     * @param targetClass целевой класс, в который конвертируется
     * @param e исключение
     * @return BadRequest исключение
     */
    protected static WebApiException.BadRequest getClassCastException(String paramName, String value, Class targetClass, Exception e) {
        String message = "Не удалось конвертировать параметр $paramName имеющий значение(я) $value в класс ${targetClass.getSimpleName()}."
        return new WebApiException.BadRequest(message, e)
    }

    /**
     * Преобразовать dtObject SD в пригодный для сериализация объект
     * @param dtObject объект SD
     * @param attrs добавляемые целевой объект поля. По умолчанию все
     * @return пригодный для сериализация объект
     */
    protected Map<String, Object> dtObjectToMap(ISDtObject dtObject, List<String> attrs = null) {
        if (dtObject == null) return null
        if (attrs == null) attrs = dtObject.keySet().toList()
        return attrs.collectEntries { attr ->
            def attrValue = dtObject[attr]
            switch (true) {
                case (attrValue instanceof IScriptDtObject):
                    return [attr, dtObjectToMap(attrValue as ISDtObject, Constants.DEFAULT_DT_OBJECT_ATTRS)]
                case (attrValue instanceof List):
                    return [attr, attrValue.collect { dtObjectToMap(it as ISDtObject, Constants.DEFAULT_DT_OBJECT_ATTRS) }]
                case (attrValue instanceof ScriptDate):
                    return [attr, prefs.getDateFormat().format(attrValue)]
                case (attrValue instanceof AggregateContainerWrapper):
                    attrValue = attrValue as AggregateContainerWrapper
                    Map<String, Object> map = [
                            'employee': dtObjectToMap(attrValue.employee),
                            'team'    : dtObjectToMap(attrValue.team),
                            'ou'      : dtObjectToMap(attrValue.ou)
                    ]
                    return [attr, map]
                default:
                    return [attr, attrValue]
            }
        }
    }

    //Методы для получения параметров

    /**
     * Получить значение параметра как строку
     * @param paramName имя параметра
     * @return значение параметры
     */
    Optional<String> getParamAsString(String paramName) {
        return Optional.ofNullable(request.getParameter(paramName))
    }

    /**
     * Получить значение параметра как строку или выкинуть исключение
     * @param paramName имя параметра
     * @return значение параметра
     * @throws WebApiException.BadRequest если параметр отсутствует
     */
    String getParamAsStringElseThrow(String paramName) throws WebApiException.BadRequest {
        return getParamAsString(paramName).orElseThrow(() -> getNoParamException(paramName))
    }

    /**
     * Получить значение параметра как список строк
     * @param paramName имя параметра
     * @return значение параметры
     */
    Optional<List<String>> getParamAsStringList(String paramName) {
        return Optional.ofNullable(request.getParameterValues(paramName)?.toList())
    }

    /**
     * Получить значение параметра как список строк или выкинуть исключение
     * @param paramName имя параметра
     * @return значение параметра
     * @throws WebApiException.BadRequest если параметр отсутствует
     */
    List<String> getParamAsStringListElseThrow(String paramName) {
        return getParamAsStringList(paramName).orElseThrow({ getNoParamException(paramName) })
    }

    /**
     * Получить значение параметра как дробное
     * @param paramName имя параметры
     * @return значение параметра
     * @throws WebApiException.BadRequest если не удалось конвертировать значение
     */
    Optional<Double> getParamAsDouble(String paramName) throws WebApiException.BadRequest {
        String value = getParamAsString(paramName).orElse(null)
        try {
            return Optional.ofNullable(value?.replace(',', '.')?.toDouble())
        } catch (Exception e) {
            throw getClassCastException(paramName, value, Double, e)
        }
    }

    /**
     * Получить значение параметра как дробное или выкинуть исключение
     * @param paramName имя параметры
     * @return значение параметра
     * @throws WebApiException.BadRequest если параметр отсутствует или не удалось конвертировать значение
     */
    Double getParamAsDoubleElseThrow(String paramName) throws WebApiException.BadRequest {
        return getParamAsDouble(paramName).orElseThrow(() -> getNoParamException(paramName))
    }

    /**
     * Получить значение параметра как список дробных
     * @param paramName имя параметры
     * @return значение параметра
     * @throws WebApiException.BadRequest если не удалось конвертировать значение
     */
    Optional<List<Double>> getParamAsDoubleList(String paramName) throws WebApiException.BadRequest {
        List<String> values = getParamAsStringList(paramName).orElse(null)
        try {
            return Optional.ofNullable(values?.collect { it.replace(',', '.')?.toDouble() })
        } catch (Exception e) {
            throw getClassCastException(paramName, values.join(', '), Double, e)
        }
    }

    /**
     * Получить значение параметра как список дробных или выкинуть исключение
     * @param paramName имя параметры
     * @return значение параметра
     * @throws WebApiException.BadRequest если параметр отсутствует или не удалось конвертировать значение
     */
    List<Double> getParamAsDoubleListElseThrow(String paramName) throws WebApiException.BadRequest {
        return getParamAsDoubleList(paramName).orElseThrow({ getNoParamException(paramName) })
    }

    /**
     * Получить значение параметра как целое число
     * @param paramName имя параметры
     * @return значение параметра
     * @throws WebApiException.BadRequest если не удалось конвертировать значение
     */
    Optional<Long> getParamAsLong(String paramName) throws WebApiException.BadRequest {
        String value = getParamAsString(paramName).orElse(null)
        try {
            return Optional.ofNullable(value?.toLong())
        } catch (Exception e) {
            throw getClassCastException(paramName, value, Long, e)
        }
    }

    /**
     * Получить значение параметра как дробное или выкинуть исключение
     * @param paramName имя параметры
     * @return значение параметра
     * @throws WebApiException.BadRequest если параметр отсутствует или не удалось конвертировать значение
     */
    Long getParamAsLongElseThrow(String paramName) throws WebApiException.BadRequest {
        return getParamAsLong(paramName).orElseThrow(() -> getNoParamException(paramName))
    }

    /**
     * Получить значение параметра как список целых чисел
     * @param paramName имя параметры
     * @return значение параметра
     * @throws WebApiException.BadRequest если не удалось конвертировать значение
     */
    Optional<List<Long>> getParamAsLongList(String paramName) throws WebApiException.BadRequest {
        List<String> values = getParamAsStringList(paramName).orElse(null)
        try {
            return Optional.ofNullable(values?.collect { it.toLong() })
        } catch (Exception e) {
            throw getClassCastException(paramName, values.join(','), Long, e)
        }
    }

    /**
     * Получить значение параметра как список целых чисел
     * @param paramName имя параметры
     * @return значение параметра
     * @throws WebApiException.BadRequest если не удалось конвертировать значение или значение отсутствует
     */
    List<Long> getParamAsLongListElseThrow(String paramName) throws WebApiException.BadRequest {
        return getParamAsLongList(paramName).orElseThrow({ getNoParamException(paramName) })
    }

    /**
     * Получить значение параметра как дату
     * @param paramName имя параметры
     * @param pattern паттерн для конвертации строки в дату
     * @return значение параметра
     * @throws WebApiException.BadRequest если не удалось конвертировать значение
     */
    Optional<Date> getParamAsDate(String paramName, String pattern = null) throws WebApiException.BadRequest {
        DateFormat dateFormat
        if (pattern == null) dateFormat = prefs.dateFormat
        else dateFormat = new SimpleDateFormat(pattern)
        String value = getParamAsString(paramName).orElse(null)
        try {
            return Optional.ofNullable(value ? dateFormat.parse(value) : null)
        } catch (Exception e) {
            throw getClassCastException(paramName, value, Date, e)
        }
    }

    /**
     * Получить значение параметра как дату или выкинуть исключение
     * @param paramName имя параметры
     * @param pattern паттерн для конвертации строки в дату
     * @return значение параметра
     * @throws WebApiException.BadRequest если параметр отсутствует или не удалось конвертировать значение
     */
    Date getParamAsDateElseThrow(String paramName, String pattern = null) throws WebApiException.BadRequest {
        return getParamAsDate(paramName, pattern).orElseThrow(() -> getNoParamException(paramName))
    }

    /**
     * Получить значение параметра как список дат
     * @param paramName имя параметры
     * @param pattern паттерн для конвертации строки в дату
     * @return значение параметра
     * @throws WebApiException.BadRequest если не удалось конвертировать значение
     */
    Optional<List<Date>> getParamAsDateList(String paramName, String pattern = null) throws WebApiException.BadRequest {
        DateFormat dateFormat
        if (pattern == null) dateFormat = prefs.dateFormat
        else dateFormat = new SimpleDateFormat(pattern)
        String values = getParamAsStringList(paramName).orElse(null)
        try {
            return Optional.ofNullable(values ? values.collect { dateFormat.parse(it) } : null)
        } catch (Exception e) {
            throw getClassCastException(paramName, values.join(','), Date, e)
        }
    }

    /**
     * Получить значение параметра как список дат
     * @param paramName имя параметры
     * @param pattern паттерн для конвертации строки в дату
     * @return значение параметра
     * @throws WebApiException.BadRequest если параметр отсутствует или не удалось конвертировать значение
     */
    List<Date> getParamAsDateListElseThrow(String paramName, String pattern = null) throws WebApiException.BadRequest {
        return getParamAsDateList(paramName).orElseThrow({ getNoParamException(paramName) })
    }

    /**
     * Получить значение параметра как булево
     * @param paramName имя параметра
     * @param convertMap мапа для конвертации, ключ - это строковое представление из параметры, значение - это булево соответствующее
     * @return значение параметра
     * @throws WebApiException.BadRequest если параметр отсутствует или не удалось конвертировать значение
     */
    Optional<Boolean> getParamAsBoolean(String paramName, Map<String, Boolean> convertMap = null) throws WebApiException.BadRequest {
        if (convertMap == null) convertMap = ['false': false, 'true': true]
        String value = getParamAsString(paramName).orElse(null)
        if (value == null) return Optional.empty()
        Boolean converted = convertMap[(value)]
        if (converted == null) throw getClassCastException(paramName, value, Boolean, new Exception("Не удалось сопоставить со значениями: ${convertMap.keySet().join(', ')}"))
        return Optional.of(converted)
    }

    /**
     * Получить значение параметра как булево или выкинуть исключение
     * @param paramName имя параметра
     * @param convertMap мапа для конвертации, ключ - это строковое представление из параметры, значение - это булево соответствующее
     * @return значение параметра
     * @throws WebApiException.BadRequest если параметр отсутствует или не удалось конвертировать значение
     */
    Boolean getParamAsBooleanElseThrow(String paramName, Map<String, Boolean> convertMap = null) throws WebApiException.BadRequest {
        return getParamAsBoolean(paramName, convertMap).orElseThrow(() -> getNoParamException(paramName))
    }

    //Методы для установки тела ответа:

    /**
     * Записать данные в тело ответа как строку
     * @param body тело ответа
     * @param contentType content type ответа БЕЗ указания кодировки. По умолчанию text/plain
     */
    void setBodyAsString(String body, String contentType = null) {
        response.addHeader('Content-Type', contentType)
        if (contentType == null) contentType = "text/plain"
        contentType += ";charset=${Constants.DEFAULT_CHARSET}"
        byte[] bytes = body.getBytes(Constants.DEFAULT_CHARSET)
        OutputStream os = response.getOutputStream()
        os.write(bytes, 0, bytes.length)
        os.close()
    }

    /**
     * Записать данные в тело ответа как JSON
     * @param body тело ответа, которое будет сериализовано и записано
     */
    void setBodyAsJson(Object body) {
        response.addHeader('Content-Type', "application/json;charset=${Constants.DEFAULT_CHARSET}")
        if (body instanceof ISDtObject) body = dtObjectToMap(body)
        byte[] bytes
        if (body instanceof String) bytes = body.getBytes()
        else bytes = prefs.getObjectMapper().writeValueAsString(body).getBytes()
        OutputStream os = response.getOutputStream()
        os.write(bytes, 0, bytes.length)
        os.close()
    }

    /**
     * Записать данные в тело ответа как байты
     * @param bytes байты для записи
     * @param contentType mime type файла
     */
    void setBodyAsBytes(byte[] bytes, String contentType) {
        response.addHeader('Content-Type', contentType)
        OutputStream os = response.getOutputStream()
        os.write(bytes, 0, bytes.length)
        os.close()
    }

    /**
     * Записать данные в тело ответа как JSON из файла системы
     * @param fileDtObject файл системы
     */
    void setBodyAsBytes(ISDtObject fileDtObject) {
        if (fileDtObject.getMetainfo().toString() != 'file') {
            String message = "Для записи файла ответ передан ISDtObject у которого метакласс не file."
            throw new WebApiException.InternalServerError(message)
        }
        byte[] fileBytes = utils.readFileContent(fileDtObject as IScriptDtObject)
        response.addHeader('File-Title', (String) fileDtObject.title)
        response.addHeader('File-Description', (String) fileDtObject.description)
        response.addHeader('File-Author-Title', (String) ((ISDtObject) fileDtObject.author)?.title)
        response.addHeader('File-Author-UUID', (String) ((ISDtObject) fileDtObject.author)?.UUID)
        response.addHeader('File-Source-UUID', (String) fileDtObject.source)
        response.addHeader('File-Creation-Date', prefs.getDateFormat().format((Date) fileDtObject.creationDate))
        setBodyAsBytes(fileBytes, (String) fileDtObject.mimeType)
    }

    //Методы для получения тела запроса:

    /**
     * Получить body запроса как текст
     * @return текст тела запроса
     */
    Optional<String> getBodyAsString() {
        String text = request.getReader().getText()
        if (text == null || text?.size() == 0) return Optional.empty()
        return Optional.of(text)
    }

    /**
     * Получить body запроса как текст, иначе выкинуть исключение
     * @return текст тела запроса
     * @throws WebApiException.BadRequest если тело запроса отсутствует
     */
    String getBodyAsStringElseThrow() throws WebApiException.BadRequest  {
        return getBodyAsString().orElseThrow({ getNoBodyException() })
    }

    /**
     * Получить тело запроса как объект
     * @param clazz тип объекта, в который нужно десериализовать тело запроса
     * @return десериализованный объект
     */
    public <T> Optional<T> getBodyAsJson(Class<T> clazz) {
        String text = getBodyAsString().orElse(null)
        if (text == null || text?.size() == 0) return Optional.empty()
        else return Optional.of(prefs.getObjectMapper().readValue(text, clazz))
    }

    /**
     * Получить тело запроса как объект, иначе выкинуть исключение
     * @param clazz тип объекта, в который нужно десериализовать тело запроса
     * @return десериализованный объект
     * @throws WebApiException.BadRequest если тело запроса отсутствует
     */
    public <T> T getBodyAsJsonElseThrow(Class<T> clazz) throws WebApiException.BadRequest  {
        return getBodyAsJson(clazz).orElseThrow({ getNoBodyException() })
    }

    /**
     * Получить тело запроса как объект
     * @return десериализованный объект
     */
    Optional<Object> getBodyAsJson() {
        String text = getBodyAsString()
        if (text == null || text?.size() == 0) return Optional.empty()
        return Optional.of(prefs.getObjectMapper().readValue(text, Object))
    }

    /**
     * Получить тело запроса как объект, иначе выкинуть исключение
     * @return десериализованный объект
     * @throws WebApiException.BadRequest если тело запроса отсутствует
     */
    Object getBodyAsJsonElseThrow() throws WebApiException.BadRequest  {
        return getBodyAsJson().orElseThrow({ getNoBodyException() })
    }

    /**
     * Получить тело запроса как набор байтов
     * @return массив байтов
     */
    Optional<byte[]> getBodyAsBinary() {
        byte[] bytes = this.getRequest().getInputStream().getBytes()
        if(bytes == null || bytes?.size() == 0) return Optional.empty()
        else return Optional.of(bytes)
    }

    /**
     * Получить тело запроса как набор байтов, иначе выкинуть исключение
     * @return массив байтов
     * @throws WebApiException.BadRequest если тело запроса отсутствует
     */
    byte[] getBodyAsBinaryElseThrow() throws WebApiException.BadRequest  {
        return  getBodyAsBinary().orElseThrow({getNoBodyException()})
    }

    /**
     * Получить части мультипарт запроса
     * @return массив байтов
     */
    Optional<List<Part>> getBodyAsMultipart() {
        //TODO не нравиться как оно сделано. надо как то оптимизировать процесс чтения значений
        List<Part> parts = request.getParts()
        if(parts == null || parts?.size() == 0) return Optional.empty()
        else return Optional.of(parts)
    }

    /**
     * Получить части мультипарт запроса
     * @return массив байтов
     * @throws WebApiException.BadRequest если тело запроса отсутствует
     */
    List<Part> getBodyAsMultipartElseThrow() throws WebApiException.BadRequest  {
        return getBodyAsMultipart().orElseThrow{getNoBodyException()}
    }

    //Прочие методы:

    /**
     * Создать файл из тела запроса
     * @param dtObject целевой объект SD
     * @param fileName имя файла
     * @param sourceAttr целевой атрибут
     * @param description описание создаваемого файла
     * @return dtObject созданного файла
     */
    ISDtObject attachBodyAsFile(ISDtObject dtObject, String fileName, String sourceAttr = null, String description = null) {
        byte[] bytes = getBodyAsBinary().orElseThrow({new WebApiException.InternalServerError("Попытка прикрепить файл из боди с пустым контентом.")})
        return utils.attachFile(dtObject as IScriptDtObject, sourceAttr, fileName, request.getContentType() ?: "unknown", description, bytes)
    }

    /**
     * Создать файл из тела запроса
     * @param UUID целевого объекта SD
     * @param fileName имя файла
     * @param sourceAttr целевой атрибут
     * @param description описание создаваемого файла
     * @return dtObject созданного файла
     */
    ISDtObject attachBodyAsFile(String dtObjectUuid, String fileName, String sourceAttr = null, String description = null) {
        return attachBodyAsFile(utils.get(dtObjectUuid), fileName, sourceAttr, description)
    }

    //TODO типизированное чтение чтение хедеров?
    //TODO Подумать про методы получения параметров с указанием класса получаемого параметра?
    //TODO Получение мапы параметров и мапы хедеров?
}

/** Класс для настроек */
@SuppressWarnings("unused")
class Preferences {

    protected String datePattern = Constants.DEFAULT_PARSER_DATE_FORMAT_PATTERN
    protected String timeZoneId = Constants.DEFAULT_PARSER_TIME_ZONE_ID
    protected ObjectMapper objectMapper
    protected DateFormat dateFormat
    protected String charset = Constants.DEFAULT_CHARSET

    protected List<String> assertUsers = []
    protected Boolean assertSuperuser = false
    protected String assertContentType = null
    protected String assertHttpMethod = null

    /**
     * Копирует настройки в новый объект.
     * Помогает, когда имеются шаблонные настройки на уровне модуля, часть
     * которых нужно переопределять на уровне методов.
     * @return скопированный экземпляр настроен
     */
    Preferences copy() {
        Preferences prefs = new Preferences()
        prefs.datePattern = this.datePattern
        prefs.timeZoneId = this.timeZoneId
        prefs.objectMapper = this.objectMapper
        prefs.dateFormat = this.dateFormat
        prefs.charset = this.charset
        prefs.assertUsers = this.assertUsers
        prefs.assertSuperuser = this.assertSuperuser
        return prefs
    }

    /**
     * Проверить соответствие отправившего запрос пользователя по списку логинов
     * @param logins список логинов
     * @return текущий объект
     */
    Preferences assertUserByLogin(List<String> logins) {
        assertUsers = logins
        return this
    }

    /**
     * Проверить соответствие отправившего запрос пользователя по логину
     * @param login логин
     * @return текущий объект
     */
    Preferences assertUserByLogin(String login) {
        return assertUserByLogin([login])
    }

    /**
     * Проверить соответствие отправившего запрос пользователя по списку объектов пользователей SD
     * @param users список объектов пользователей
     * @return текущий объект
     */
    Preferences assertUser(List<ISDtObject> users) {
        return assertUserByLogin(users.collect { it?.login } as List<String>)
    }

    /**
     * Проверить соответствие отправившего запрос пользователя по объекту пользователя SD
     * @param users объект пользователя
     * @return текущий объект
     */
    Preferences assertUser(ISDtObject user) {
        return assertUser([user])
    }

    /**
     * Проверить что отправивший запрос пользователь - суперпользователь
     * @param bool признак. Если tru - будет выполнена проверка
     * @return текущий объект
     */
    Preferences assertSuperuser(Boolean bool = true) {
        this.assertSuperuser = bool
        return this
    }

    /**
     * Проверяет полученный content type на соответствие требуемому
     * если тип не соответствует, вызов метода process вызове ошибку
     * если в запросе не указан content type - проверка будет пропущена
     * @param contentType строка с кодом content type
     * @return текущий объект
     */
    Preferences assertContentType(String contentType) {
        assertContentType = contentType
        return this
    }

    /**
     * Проверяет метод запроса на соответствие указанному
     * @param method требуемый метод запроса
     * @return текущий объект
     */
    Preferences assertHttpMethod(String method) {
        assertHttpMethod = method
        return this
    }

    String getCharset() {
        return charset
    }

    Preferences setCharset(String charset) {
        this.charset = charset
        return this
    }

    ObjectMapper getObjectMapper() {
        if (objectMapper == null) objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setDateFormat(getDateFormat())
                .setTimeZone(TimeZone.getTimeZone(getTimeZoneId()))
        return objectMapper
    }

    Preferences setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper
        return this
    }

    String getDatePatter() {
        return datePattern
    }

    Preferences setDatePatter(String datePattern) {
        this.datePattern = datePattern
        return this
    }

    protected DateFormat getDateFormat() {
        if (dateFormat == null) dateFormat = new SimpleDateFormat(getDatePatter())
        return dateFormat
    }

    String getTimeZoneId() {
        return timeZoneId
    }

    Preferences setTimeZone(String timeZone) {
        this.timeZoneId = timeZone
        return this
    }

}

/** Стандартные классы ошибок при работе скрипта */
@SuppressWarnings("unused")
class WebApiException extends RuntimeException {

    Integer status
    String message

    /**
     * @param code - код ошибки
     * @param message - сообщении описывающее ошибку
     */
    WebApiException(Integer status, String message, Throwable cause) {
        super(message, cause)
        this.status = status
        this.message = message
    }

    /**
     * Получить статус ошибки
     * @return статус ошибки
     */
    Integer getStatus() {
        return this.status
    }

    /**
     * Получить данные для записи в боди ответа
     * @return мапа с данными
     */
    Map<String, Object> getDataForResponse() {
        Map<String, Object> body = [
                'message': message,
                'status' : status
        ] as Map<String, Object>
        if (cause != null) body.put(
                'cause',
                [
                        'class'  : cause.getClass().getName(),
                        'message': cause.message
                ]
        )
        return body
    }

    /**
     * Записать данные в ответ
     * @param response ответ
     */
    void writeToResponse(HttpServletResponse response) {
        response.setStatus(getStatus())
        response.addHeader('Content-Type', 'application/json')
        byte[] bytes = new ObjectMapper().writeValueAsString(getDataForResponse()).getBytes()
        OutputStream os = response.getOutputStream()
        os.write(bytes, 0, bytes.length)
        os.close()
    }

    /** Класс для ошибки используемый при ошибке внутри сервера */
    static class InternalServerError extends WebApiException {
        InternalServerError(String message, Throwable cause = null) {
            super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message, cause)
        }
    }

    /** Класс для ошибки используемый когда пришедшие данные не корректны */
    static class BadRequest extends WebApiException {
        BadRequest(String message, Throwable cause = null) {
            super(HttpServletResponse.SC_BAD_REQUEST, message, cause)
        }
    }

    /** Класс для ошибки используемый когда у пользователя нет прав на операцию */
    static class Forbidden extends WebApiException {
        Forbidden(String message, Throwable cause = null) {
            super(HttpServletResponse.SC_FORBIDDEN, message, cause)
        }
    }

    /** Класс для ошибки используемый когда у пользователя нет прав на операцию */
    static class MethodNotAllowed extends WebApiException {
        MethodNotAllowed(String message, Throwable cause = null) {
            super(HttpServletResponse.SC_METHOD_NOT_ALLOWED, message, cause)
        }
    }

    /** Класс для ошибки используемый когда у пользователя нет прав на операцию */
    static class NotFound extends WebApiException {
        NotFound(String message, Throwable cause = null) {
            super(HttpServletResponse.SC_NOT_FOUND, message, cause)
        }
    }

}

/** Обработчик запросов */
@SuppressWarnings("unused")
class RequestProcessor {

    protected HttpServletResponse response
    protected HttpServletRequest request
    protected ISDtObject user
    protected Preferences prefs
    protected WebApiException preProcessException

    /**
     * Создать новый экземпляр
     * @param request обрабатываемый запрос
     * @param response обрабатываемый ответ
     * @param user обратившийся пользователь
     * @param preferences настройки
     * @return новый экземпляр
     */
    RequestProcessor(HttpServletRequest request, HttpServletResponse response, ISDtObject user, Preferences prefs = null) {
        this.request = request
        this.response = response
        this.user = user
        if (prefs != null) this.prefs = prefs
        else this.prefs = new Preferences()
    }

    /**
     * Создать новый экземпляр
     * @param request обрабатываемый запрос
     * @param response обрабатываемый ответ
     * @param user обратившийся пользователь
     * @param preferences настройки
     * @return новый экземпляр
     */
    static RequestProcessor create(HttpServletRequest request, HttpServletResponse response, ISDtObject user, Preferences preferences = null) {
        return new RequestProcessor(request, response, user, preferences)
    }

    protected void assertSuperuser() {
        if (user != null) {
            throw new WebApiException.Forbidden("Эндпойнт разрешено только для суперпользователя.")
        }
    }

    protected void assertUser(List<String> assertUsers) {
        if (user != null && (String) user?.login !in assertUsers) {
            throw new WebApiException.Forbidden("Эндпойнт не разрешен для пользователя ${user?.login}.")
        }
    }

    protected void assertHttpMethod(String method) {
        List<String> METHOD_ARR = ['GET', 'POST']
        String currentMethod = this.request.getMethod()
        if (method.toUpperCase() !in METHOD_ARR) {
            throw new WebApiException.InternalServerError("В метод assertHttpMethod() " +
                    "передан неизвестный HTTP метод. Допустимые значения: ${METHOD_ARR.join(', ')}")
        }
        if (method.toLowerCase() != currentMethod.toLowerCase()) {
            throw new WebApiException.MethodNotAllowed("HTTP " +
                    "метод ${currentMethod} не разрешен для данного эндпойнта.")
        }
    }

    protected void assertContentType(String contentType) {
        String getContentType = request.getContentType()
        if (getContentType && !getContentType.contains(contentType)) {
            throw new WebApiException.BadRequest("Требуемый content type - " +
                    "\"${contentType}\", полученный - \"${getContentType}\".")
        }
    }

    protected void preProcessAssert() {
        //Проверка что обращается суперпользователь
        if (prefs.assertSuperuser) assertSuperuser()
        //Проверка что пользователь входит в список разрешенных
        if (!prefs.assertUsers.isEmpty()) assertUser(prefs.assertUsers)
        //Проверка на HTTP метод
        if (prefs.assertHttpMethod != null) assertHttpMethod(prefs.assertHttpMethod)
        //Проверка на Content-Type
        if (prefs.assertContentType != null) assertContentType(prefs.assertContentType)
    }

    /**
     * Запуск процесса обработки запроса
     * @param action действие для обработки запроса
     */
    void process(Closure action) {
        try {
            preProcessAssert()
            WebApiUtilities webApiUtilities = new WebApiUtilities(this)
            action(webApiUtilities)
        } catch (WebApiException webApiException) {
            webApiException.writeToResponse(response)
        } catch (Exception exception) {
            String errorMessage = "Unexpected error"
            def e500 = new WebApiException.InternalServerError(errorMessage, exception)
            e500.writeToResponse(response)
        }
    }
}
