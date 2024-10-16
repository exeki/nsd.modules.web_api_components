package ru.kazantsev.nsd.modules.web_api_components.old

import static ru.kazantsev.nsd.sdk.global_variables.ApiPlaceholder.*

import groovy.transform.Field

import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartFile

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import ru.naumen.core.server.script.api.injection.InjectApi
import ru.naumen.core.server.script.spi.IScriptDtObject
import ru.naumen.core.shared.dto.ISDtObject

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.Part
import java.text.SimpleDateFormat

@Field String MODULE_NAME = 'webApiComponents'

/** Конcтанты модуля */
class Constants {

    /** Паттерн для парсинга даты */
    static final String DEFAULT_PARSER_DATE_FORMAT_PATTERN = "dd.MM.yyyy HH:mm:ss"
    /** Часовой пояс для парсинга даты */
    static final String DEFAULT_PARSER_TIME_ZONE_ID = "UTC"
    static final String MODULE_NAME = 'webApiComponents'

    private Constants() {}

}

class ProcessUtilities {

    protected ProcessUtilities() {}

    static class Common {

        ProcessData processData

        Common(ProcessData processData) {
            this.processData = processData
        }

        ProcessData getProcessData() {
            return this.processData
        }

        static Boolean ifBlankString(String str) {
            return (str == null || (str != null && str.trim().size() == 0))
        }

        String getStringParameterElseThrow(String paramName) {
            String paramValue = processData.getParameter(paramName)
            if (ifBlankString(paramValue)) {
                throw new WebApiException.BadRequest("Не указан параметр $paramName")
            }
            return paramValue
        }

        Double getDoubleParameterElseThrow(String paramName) {
            Double value = getDoubleParameter(paramName)
            if (value == null) throw new WebApiException.BadRequest("Не указан параметр $paramName")
            return value
        }

        Double getDoubleParameter(String paramName) {
            String paramStrValue = processData.getParameter(paramName)
            if (ifBlankString(paramStrValue)) return null
            try {
                return paramStrValue.trim().toDouble()
            } catch (Exception e) {
                throw new WebApiException.BadRequest("Не удалось конвертировать параметр " +
                        "$paramName имеющий значение $paramStrValue в дробное" +
                        " число. Текст ошибки: ${e.getMessage()}")
            }
        }

        Long getLongParameterElseThrow(String paramName) {
            Long value = getLongParameter(paramName)
            if (value == null) throw new WebApiException.BadRequest("Не указан параметр $paramName")
            return value
        }

        Long getLongParameter(String paramName) {
            String paramStrValue = processData.getParameter(paramName)
            if (ifBlankString(paramStrValue)) return null
            try {
                return paramStrValue.trim().toLong()
            } catch (Exception e) {
                throw new WebApiException.BadRequest("Не удалось конвертировать параметр " +
                        "$paramName имеющий значение $paramStrValue в целое" +
                        " число. Текст ошибки: ${e.getMessage()}")
            }
        }

        Date getDateParameter(String paramName, String pattern = Constants.DEFAULT_PARSER_DATE_FORMAT_PATTERN) {
            String paramStrValue = processData.getParameter(paramName)
            if (ifBlankString(paramStrValue)) return null
            try {
                new SimpleDateFormat(pattern).parse(paramStrValue.trim())
            } catch (Exception e) {
                throw new WebApiException.BadRequest("Не удалось конвертировать параметр " +
                        "$paramName имеющий значение $paramStrValue в" +
                        " дату. Текст ошибки: ${e.getMessage()}")
            }
        }

        Date getDateParameterElseThrow(String paramName, String pattern = Constants.DEFAULT_PARSER_DATE_FORMAT_PATTERN) {
            Date dateParameter = getDateParameter(paramName, pattern)
            if (dateParameter == null) throw new WebApiException.BadRequest("Не указан параметр $paramName")
            return dateParameter
        }

        Boolean getBooleanParameter(String paramName, Map<String, Boolean> convertMap = null) {
            if (convertMap == null) map = [
                    'false': false,
                    'true' : true
            ]
            String paramStrValue = processData.getParameter(paramName)
            if (ifBlankString(paramStrValue)) return null
            Boolean converted = convertMap[(paramStrValue.trim())]
            if (converted == null) throw new WebApiException.BadRequest("Не удалось конвертировать параметр " +
                    "$paramName имеющий значение $paramStrValue в логическое.")
            return converted
        }

        Boolean getBooleanParameterElseThrow(String paramName, Map<String, Boolean> convertMap = null) {
            Boolean boo = getBooleanParameter(paramName, convertMap)
            if (boo == null) throw new WebApiException.BadRequest("Не указан параметр $paramName")
            return boo
        }

    }

}

/** Стандартные классы ошибок при работе скрипта */
abstract class WebApiException extends RuntimeException {

    final Integer status
    final String message

    /**
     * @param code - код ошибки
     * @param message - сообщении описывающее ошибку
     */
    WebApiException(Integer status, String message) {
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

    /** Класс для ошибки используемый при ошибке внутри сервера */
    static class InternalServerError extends WebApiException {
        InternalServerError(String message) {
            super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message)
        }
    }

    /** Класс для ошибки используемый когда пришедшие данные не корректны */
    static class BadRequest extends WebApiException {
        BadRequest(String message) {
            super(HttpServletResponse.SC_BAD_REQUEST, message)
        }
    }

    /** Класс для ошибки используемый когда у пользователя нет прав на операцию */
    static class Forbidden extends WebApiException {
        Forbidden(String message) {
            super(HttpServletResponse.SC_FORBIDDEN, message)
        }
    }

    /** Класс для ошибки используемый когда у пользователя нет прав на операцию */
    static class MethodNotAllowed extends WebApiException {
        MethodNotAllowed(String message) {
            super(HttpServletResponse.SC_METHOD_NOT_ALLOWED, message)
        }
    }

    /** Класс для ошибки используемый когда у пользователя нет прав на операцию */
    static class NotFound extends WebApiException {
        NotFound(String message) {
            super(HttpServletResponse.SC_NOT_FOUND, message)
        }
    }

}

/** Прототип ответа сервера */
@SuppressWarnings("unused")
@InjectApi
abstract class ResponsePrototype {

    Integer status = 200

    Map<String, String> headers = [:]

    /**
     * Записать контент текущего прототипа в ответ
     * @param response ответ, куда записать
     */
    protected abstract void writeBodyToResponse(HttpServletResponse response)

    /**
     * Записать хедеры текущего прототипа в ответ
     * @param response ответ, куда записать
     */
    protected void writeHeadersToResponse(HttpServletResponse response) {
        headers.each { key, value ->
            response.setHeader(key, value)
        }
    }

    /**
     * Записать статус текущего прототипа в ответ
     * @param response ответ, куда записать
     */
    protected void writeStatusToResponse(HttpServletResponse response) {
        response.setStatus(status)
    }

    /**
     * Записать все данные прототипа в ответ
     * @param response
     */
    protected void writeDataToResponse(HttpServletResponse response) {
        this.writeStatusToResponse(response)
        this.writeHeadersToResponse(response)
        this.writeBodyToResponse(response)
    }

    /**
     * Установить статус ответа
     * @param status статус
     */
    void setStatus(Integer status) {
        this.status = status
    }

    /**
     * Добавить хедер в ответ
     * @param key ключ хедера
     * @param value значение хедера
     */
    void addHeader(String key, String value) {
        this.headers.put(key, value)
    }

    /**
     * Добавить хедер в ответ
     * @param key ключ хедера
     * @param value значение хедера
     */
    void addHeader(String key, Integer value) {
        this.headers.put(key, value.toString())
    }

    /**
     * Добавить хедер в ответ
     * @param key ключ хедера
     * @param value значение хедера
     */
    void addHeader(String key, Long value) {
        this.headers.put(key, value.toString())
    }

    /**
     * Добавить хедер в ответ
     * @param key ключ хедера
     * @param value значение хедера
     */
    void addHeader(String key, List<String> value) {
        this.headers.put(key, value.join(','))
    }

    /**
     * Добавить хедер в ответ
     * @param key ключ хедера
     * @param value значение хедера
     */
    void addHeader(String key, Boolean value) {
        this.headers.put(key, value.toString())
    }

    /**
     * Установить все хедеры ответа
     * @param headers хедеры в виде Map<String, String>
     */
    void setHeaders(Map<String, String> headers) {
        this.headers = headers
    }

    /**
     * Ответ сервера без контента в боди
     */
    static class NoContent extends ResponsePrototype {

        abstract Integer status = 204

        NoContent() {}

        @Override
        protected void writeBodyToResponse(HttpServletResponse response) {}

    }

    /**
     * Ответ сервера с json в body
     */
    static class Json extends ResponsePrototype {

        protected def body

        protected ObjectMapper objectMapper

        protected Json() {}

        Json(def newBody) {
            this.body = newBody
        }

        void setObjectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper
        }

        ObjectMapper getObjectMapper() {
            if (this.objectMapper == null) {
                this.objectMapper = new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .setDateFormat(new SimpleDateFormat(Constants.DEFAULT_PARSER_DATE_FORMAT_PATTERN))
                        .setTimeZone(TimeZone.getTimeZone(Constants.DEFAULT_PARSER_TIME_ZONE_ID))
            }
            return this.objectMapper
        }

        @Override
        protected void writeBodyToResponse(HttpServletResponse response) {
            OutputStream os = response.getOutputStream()
            byte[] bytes
            if (this.body instanceof String) bytes = this.body.getBytes()
            else bytes = getObjectMapper().writeValueAsString(this.body).getBytes()
            os.write(bytes, 0, bytes.length)
            os.close()
        }

        @Override
        protected void writeHeadersToResponse(HttpServletResponse response) {
            this.addHeader('Content-Type', 'application/json')
            super.writeHeadersToResponse(response)
        }

        /** Ответ для передачи ошибки */
        static class Error extends Json {

            Error(String message, Integer status) {
                this.body = [
                        'message': message,
                        'status' : status
                ]
                this.setStatus(status)
            }

            Error(WebApiException exception) {
                this(exception.getMessage(), exception.getStatus())
            }

        }

        /** Ответ для передачи постраничных данных */
        static class Page extends Json {

            Page(List<Serializable> list, Long total, Integer page, Integer pageSize) {
                this.body = [
                        'list'           : list,
                        'total'          : total,
                        'page'           : page,
                        'pageSize'       : pageSize,
                        'currentPageSize': list.size()
                ]
            }
        }

        /** Отчет для перечня объектов */
        static class OffsetCollection extends Json {
            OffsetCollection(List<Serializable> collection, Long offset, Long limit) {
                this.body = [
                        'collection'    : collection,
                        'collectionSize': collection.size(),
                        'offset'        : offset,
                        'limit'         : limit
                ]
            }
        }

        /** Ответ для перечня объектов */
        static class Collection extends Json {
            Collection(List<Serializable> list) {
                this.body = [
                        'list'          : list,
                        'collectionSize': list.size()
                ]
            }
        }

        /** Ответ для одного объекта */
        static class Object extends Json {
            Object(Serializable object) {
                this.body = object
            }
        }
    }

    /**
     * Ответ сервера с json в body
     */
    static class StringBody extends ResponsePrototype {

        protected String body

        protected StringBody() {}

        StringBody(String newBody) {
            this.body = newBody
        }

        @Override
        protected void writeBodyToResponse(HttpServletResponse response) {
            OutputStream os = response.getOutputStream()
            byte[] bytes = this.body.getBytes()
            os.write(bytes, 0, bytes.length)
            os.close()
        }
    }

    /**
     * Прототип ответа с байтовым контентом
     */
    static class Bytes extends ResponsePrototype {

        String fileContentType
        String fileTitle
        byte[] fileBytes

        /**
         * Конструктор из dyObject файла системы
         * @param fileObject файл системы
         */
        Bytes(IScriptDtObject fileObject) {
            this.fileContentType = fileObject.mimeType
            this.fileTitle = fileObject.title
            this.fileBytes = utils.readFileContent(fileObject)
            this.addHeader('filename', fileTitle)
        }

        /**
         * Конструктор
         * @param bytes баты файла
         * @param fileName имя файла
         * @param fileContentType контент тайм файла
         */
        Bytes(byte[] bytes, String fileName, String fileContentType = null) {
            this.fileContentType = fileContentType
            this.fileTitle = fileName
            this.fileBytes = bytes
            this.addHeader('filename', fileTitle)
            if (fileContentType != null) {
                this.addHeader('fileMimeType', fileContentType)
            }
        }

        @Override
        protected void writeBodyToResponse(HttpServletResponse response) {
            OutputStream os = response.getOutputStream()
            os.write(fileBytes, 0, fileBytes.length)
            os.close()
        }
    }
}

//Стандартные контейнеры возвращаемых данных --------------------------------------------------------------------------

@SuppressWarnings("unused")
class RequestProcessor {

    protected HttpServletResponse response
    protected HttpServletRequest request
    protected ISDtObject user
    protected String methodName
    protected WebApiException preProcessException
    protected ProcessData processData

    protected RequestProcessor() {}

    static RequestProcessor create(ProcessData processDataInstance, HttpServletRequest request, HttpServletResponse response, ISDtObject user, String methodName) {
        RequestProcessor requestProcessor = new RequestProcessor()
        requestProcessor.request = request
        requestProcessor.response = response
        requestProcessor.user = user
        requestProcessor.methodName = methodName
        requestProcessor.processData = processDataInstance
        requestProcessor.processData.setRequestProcessor(requestProcessor)
        return requestProcessor
    }

    /**
     * Проверяет полученный content type на соответствие требуемому
     * если тип не соответствует, вызов метода process вызове ошибку
     * если в запросе не указан content type - проверка будет пропущена
     * @param contentType строка с кодом content type
     * @return текущий объект
     */
    RequestProcessor assertContentType(String contentType) {
        String getContentType = request.getContentType()
        if (getContentType && !getContentType.contains(contentType)) {
            this.preProcessException = new WebApiException.BadRequest("Требуемый content type - \"${contentType}\", полученный \"${getContentType}\".")
        }
        return this
    }

    /**
     * Проверяет пользователя, совершающего запрос, на соответствию списку пользователей, которым разрешен запрос
     * если в доступе отказано - при до начала обработки запроса будет возвращена ошибка
     * @param logins логины пользователей, которым разрешен доступ
     * @return текущий объект
     */
    RequestProcessor assertUserByLogin(List<String> assertedLogins) {
        if (this.preProcessException == null) {
            String currentUserLogin = this.user.login as String
            if (user != null && !(currentUserLogin in assertedLogins)) {
                this.preProcessException = new WebApiException.Forbidden("Эндпойнт не разрешен для пользователя ${currentUserLogin}.")
            }
        }
        return this
    }

    /**
     * Проверяет пользователя, совершающего запрос, на соответствию списку пользователей, которым разрешен запрос
     * если в доступе отказано - при до начала обработки запроса будет возвращена ошибка
     * @param assertedLogin логин пользователя, которому разрешен доступ
     * @return текущий объект
     */
    RequestProcessor assertUserByLogin(String assertedLogin) {
        return assertUserByLogin([assertedLogin])
    }

    /**
     * Проверяет, что обратившийся пользователь является суперпользователем
     * если в доступе отказано - при до начала обработки запроса будет возвращена ошибка
     * @param assertedLogin логин пользователя, которому разрешен доступ
     * @return текущий объект
     */
    RequestProcessor assertSuperuser() {
        if (this.preProcessException == null) {
            if (user != null) {
                this.preProcessException = new WebApiException.Forbidden("Эндпойнт не разрешен для пользователя ${currentUserLogin}.")
            }
        }
        return this
    }

    /**
     * Проверяет пользователя, совершающего запрос, на соответствию списку пользователей, которым разрешен запрос
     * если в доступе отказано - при до начала обработки запроса будет возвращена ошибка
     * @param assertedLogin логин пользователя, которому разрешен доступ
     * @return текущий объект
     */
    RequestProcessor assertUser(Map assertedUser) {
        return assertUserByLogin(assertedUser.login as String)
    }

    /**
     * Проверяет пользователя, совершающего запрос, на соответствию списку пользователей, которым разрешен запрос
     * если в доступе отказано - при до начала обработки запроса будет возвращена ошибка
     * @param assertedLogin логин пользователя, которому разрешен доступ
     * @return текущий объект
     */
    RequestProcessor assertUser(List<Map> assertedUsers) {
        return assertUserByLogin(assertedUsers.login as List<String>)
    }

    /**
     * Проверяет метод запроса на соответствие указанному
     * @param method требуемый метод запроса
     * @return текущий объект
     */
    RequestProcessor assertHttpMethod(String method) {
        if (this.preProcessException == null) {
            String currentMethod = this.request.getMethod()
            if (method.toLowerCase() != currentMethod.toLowerCase()) {
                this.preProcessException = new WebApiException.MethodNotAllowed("HTTP метод ${currentMethod} не разрешен для данного эндпойнта")
            }
        }
        return this
    }

    /**
     * Запуск процессинга запроса
     * Принимает на вход функцию, которая должна принимать на вход
     * объект указанного ранее подтипа ProcessData
     * а отдает на выходе ResponsePrototype
     */
    void process(Closure closure) {
        if (this.preProcessException) {
            new ResponsePrototype.Json.Error(this.preProcessException).writeDataToResponse(this.response)
        } else {
            try {
                ResponsePrototype responsePrototype = closure(this.processData) as ResponsePrototype
                if (responsePrototype == null) throw new WebApiException.InternalServerError("Функция обработки запроса вернула пустой результат.")
                responsePrototype.writeDataToResponse(response)
            } catch (WebApiException exception) {
                new ResponsePrototype.Json.Error(exception).writeDataToResponse(this.response)
            } catch (Exception e) {
                String errorMessage = 'modules.' + MODULE_NAME + '.' + methodName + ': ' + e.message
                new ResponsePrototype.Json.Error(new WebApiException.InternalServerError(errorMessage)).writeDataToResponse(this.response)
            }
        }
    }
}

@SuppressWarnings("unused")
@InjectApi
abstract class ProcessData {

    RequestProcessor requestProcessor

    void setRequestProcessor(RequestProcessor requestProcessor) {
        this.requestProcessor = requestProcessor
    }

    String getParameter(String formKey) {
        return this.getRequest().getParameter(formKey)
    }

    List<String> getParameterNames() {
        return this.getRequest().getParameterNames().toList()
    }

    Map<String, String> getParameterMap(String formKey) {
        Map<String, String> map = [:]
        this.getParameterNames().each {
            map.put(it, this.getParameter(it))
        }
        return map
    }

    List<String> getHeaderNames() {
        return this.getRequest().getHeaderNames().toList()
    }

    Map<String, String> getRequestHeadersMap() {
        Map<String, String> map = [:]
        this.getHeaderNames().each {
            map.put(it, request.getHeader(it))
        }
        return map
    }

    String getHeader(String headerKey) {
        return this.getRequest().getHeader(headerKey)
    }

    HttpServletRequest getRequest() {
        return this.requestProcessor.request
    }

    HttpServletResponse getResponse() {
        return this.requestProcessor.response
    }

    static class NoBody extends ProcessData {}

    static class JsonBody extends ProcessData {

        /** Паттерн для парсинга даты */
        final static String DEFAULT_PARSER_DATE_FORMAT_PATTERN = "dd.MM.yyyy HH:mm:ss"
        /** Часовой пояс для парсинга даты */
        final static String DEFAULT_PARSER_TIME_ZONE_ID = "UTC"

        private ObjectMapper objectMapper = null

        JsonBody setObjectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper
            return this
        }

        ObjectMapper getObjectMapper() {
            if (this.objectMapper == null) {
                this.objectMapper = new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .setDateFormat(new SimpleDateFormat(Constants.DEFAULT_PARSER_DATE_FORMAT_PATTERN))
                        .setTimeZone(TimeZone.getTimeZone(Constants.DEFAULT_PARSER_TIME_ZONE_ID))
            }
            return this.objectMapper
        }

        @Override
        void setRequestProcessor(RequestProcessor requestProcessor) {
            super.setRequestProcessor(requestProcessor)
        }

        def <T> T parseRequestBodyToObject(Class<T> clazz) {
            return getObjectMapper().readValue(this.getRequestBody(), clazz)
        }

        HashMap<String, Object> parseRequestBodyToMap() {
            return parseRequestBodyToObject(HashMap)
        }

        String getRequestBody() {
            return this.getRequest().getReader().getText()
        }

    }

    static class BinaryBody extends ProcessData {


        byte[] getRequestBody() {
            return this.getRequest().getInputStream().getBytes()
        }

        ISDtObject attachFile(IScriptDtObject dtObject, String contentType, String fileName, String sourceAttr = null, String description = null) {
            return utils.attachFile(
                    dtObject,
                    sourceAttr,
                    fileName,
                    contentType,
                    description,
                    this.getRequest().getInputStream().getBytes()
            )
        }

        ISDtObject attachFile(String dtObjectUuid, String contentType, String fileName, String sourceAttr = null, String description = null) {
            return utils.attachFile(
                    utils.get(dtObjectUuid) as IScriptDtObject,
                    sourceAttr,
                    fileName,
                    contentType,
                    description,
                    this.getRequest().getInputStream().getBytes()
            )
        }

    }

    /**
     * Данные процесса обработки запроса с mutlipart body
     */
    static class MultipartBody extends ProcessData {

        private MultipartHttpServletRequest multipartRequest

        @Override
        void setRequestProcessor(RequestProcessor requestProcessor) {
            super.setRequestProcessor(requestProcessor)
        }

        /**
         * Получить текущий запрос как mutlipart
         * @return mutlipart запрос
         */
        MultipartHttpServletRequest getMultipartRequest() {
            if (this.multipartRequest == null) {
                this.multipartRequest = (MultipartHttpServletRequest) this.getRequest()
            }
            return this.multipartRequest
        }

        /**
         * Получить часть mutlipart запроса
         * @param partName имя части
         * @return часть mutlipart запроса
         */
        Part getPart(String partName) {
            return this.getMultipartRequest().getPart(partName)
        }

        /**
         * Получить все части mutlipart запроса
         * @return все части mutlipart запроса
         */
        List<Part> getParts() {
            return this.getMultipartRequest().getParts()
        }

        /**
         * Получить файлы из части
         * @param formKey имя частм
         * @return список файлов
         */
        List<MultipartFile> getFiles(String formKey) {
            return this.getMultipartRequest().getFiles(formKey)
        }

        /**
         * Прикрепить к бизнес объекту все файлы из части
         * @param formKey имя ключа, откуда забирать файлы
         * @param dtObject бизнес объект, куда будет прикреплен файл
         * @param sourceAttr атрибут БО, куда будет прикреплен файл
         * @param description описание файла
         * @return
         */
        List<ISDtObject> attachFilesFromKey(String formKey, ISDtObject dtObject, String sourceAttr = null, String description = null) {
            return this.getFiles(formKey).collect {
                return utils.attachFile(
                        dtObject as IScriptDtObject,
                        sourceAttr,
                        it.getOriginalFilename(),
                        it.getContentType(),
                        description,
                        it.getBytes()
                )
            }
        }

        /**
         * Прикрепить к бизнес объекту все файлы из части
         * @param formKey имя ключа, откуда забирать файлы
         * @param dtObjectUuid UUID бизнес объекта, куда будет прикреплен файл
         * @param sourceAttr атрибут БО, куда будет прикреплен файл
         * @param description описание файла
         * @return
         */
        List<ISDtObject> attachFilesFromKey(String formKey, String dtObjectUuid, String sourceAttr = null, String description = null) {
            return attachFilesFromKey(formKey, utils.get(dtObjectUuid) as IScriptDtObject, sourceAttr, description)
        }

    }
}
