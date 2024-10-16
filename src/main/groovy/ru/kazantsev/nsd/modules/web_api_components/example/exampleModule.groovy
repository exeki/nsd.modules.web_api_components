package ru.kazantsev.nsd.modules.web_api_components.example

import groovy.transform.Field
import ru.kazantsev.nsd.modules.web_api_components.Preferences
import ru.kazantsev.nsd.modules.web_api_components.RequestProcessor
import ru.kazantsev.nsd.modules.web_api_components.WebApiException
import ru.kazantsev.nsd.modules.web_api_components.WebApiUtilities
import ru.naumen.core.shared.dto.ISDtObject

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.Part

import static ru.kazantsev.nsd.sdk.global_variables.ApiPlaceholder.*

@Field Preferences prefs = new Preferences().assertSuperuser()

//тест что эндпойнт доступ только суперпользователю
void assertSuperUserTest(HttpServletRequest request, HttpServletResponse response, ISDtObject user) {
    RequestProcessor.create(request, response, user, prefs.copy()).process {
        WebApiUtilities webUtils ->
            webUtils.setBodyAsJson(['message': 'Доступно только для суперпользователя'])
    }
}

//тест что эндпойнт доступен только пользователю с логином eadmintest и суперпользователю
void assertUserTest(HttpServletRequest request, HttpServletResponse response, ISDtObject user) {
    RequestProcessor.create(request, response, user, prefs.copy().assertSuperuser(false).assertUserByLogin('eadmintest')).process {
        WebApiUtilities webUtils ->
            webUtils.setBodyAsJson(['message': 'Доступно только для пользователя с логином eadmintest'])
    }
}

void assertHttpMethod(HttpServletRequest request, HttpServletResponse response, ISDtObject user) {
    RequestProcessor.create(request, response, user, prefs.copy().assertHttpMethod('GET')).process {
        WebApiUtilities webUtils ->
            webUtils.setBodyAsJson(['message': 'Доступно только по методы GET'])
    }
}

void assertContentType(HttpServletRequest request, HttpServletResponse response, ISDtObject user) {
    RequestProcessor.create(request, response, user, prefs.copy().assertContentType('application/json')).process {
        WebApiUtilities webUtils ->
            webUtils.setBodyAsJson(['message': 'Доступно только c Content-Type application/json'])
    }
}

void testParameters(HttpServletRequest request, HttpServletResponse response, ISDtObject user) {
    RequestProcessor.create(request, response, user, prefs.copy().assertContentType('application/json')).process {
        WebApiUtilities webUtils ->
            Map<String, Object> body = [
                    'string'    : webUtils.getParamAsString('string').orElse(null),
                    'stringList': webUtils.getParamAsStringList('stringList').orElse(null),
                    'double'    : webUtils.getParamAsDouble('double').orElse(null),
                    'doubleList': webUtils.getParamAsDoubleList('doubleList').orElse(null),
                    'long'      : webUtils.getParamAsLong('long').orElse(null),
                    'longList'  : webUtils.getParamAsLongList('longList').orElse(null),
                    'date'      : webUtils.getParamAsDate('date').orElse(null),
                    'dateList'  : webUtils.getParamAsDate('dateList').orElse(null),
                    'boolean'   : webUtils.getParamAsBoolean('boolean').orElse(null)
            ]
            webUtils.setBodyAsJson(body)
    }
}

void testParametersRequired(HttpServletRequest request, HttpServletResponse response, ISDtObject user) {
    RequestProcessor.create(request, response, user, prefs.copy().assertContentType('application/json')).process {
        WebApiUtilities webUtils ->
            Map<String, Object> body = [
                    'string'    : webUtils.getParamAsStringElseThrow('string'),
                    'stringList': webUtils.getParamAsStringListElseThrow('stringList'),
                    'double'    : webUtils.getParamAsDoubleElseThrow('double'),
                    'doubleList': webUtils.getParamAsDoubleListElseThrow('doubleList'),
                    'long'      : webUtils.getParamAsLongElseThrow('long'),
                    'longList'  : webUtils.getParamAsLongListElseThrow('longList'),
                    'date'      : webUtils.getParamAsDateElseThrow('date'),
                    'dateList'  : webUtils.getParamAsDateElseThrow('dateList'),
                    'boolean'   : webUtils.getParamAsBooleanElseThrow('boolean')
            ]
            webUtils.setBodyAsJson(body)
    }
}

class TestBody{
    String testField1
    Long testField2
}

void testJsonBody1(HttpServletRequest request, HttpServletResponse response, ISDtObject user) {
    RequestProcessor.create(request, response, user, prefs.copy().assertContentType('application/json')).process {
        WebApiUtilities webUtils ->
            Map requestBody = webUtils.getBodyAsJson().orElse(null) as Map
            webUtils.setBodyAsJson(['1' : requestBody.testField1, '2' : requestBody.testField2])
    }
}

void testJsonBody2(HttpServletRequest request, HttpServletResponse response, ISDtObject user) {
    RequestProcessor.create(request, response, user, prefs.copy().assertContentType('application/json')).process {
        WebApiUtilities webUtils ->
            TestBody requestBody = webUtils.getBodyAsJson(TestBody.class).orElse(null)
            webUtils.setBodyAsJson(requestBody)
    }
}

void testJsonBodyRequired1(HttpServletRequest request, HttpServletResponse response, ISDtObject user) {
    RequestProcessor.create(request, response, user, prefs.copy().assertContentType('application/json')).process {
        WebApiUtilities webUtils ->
            Map requestBody = webUtils.getBodyAsJsonElseThrow() as Map
            webUtils.setBodyAsJson(['1' : requestBody.testField1, '2' : requestBody.testField2])
    }
}

void testJsonBodyRequired2(HttpServletRequest request, HttpServletResponse response, ISDtObject user) {
    RequestProcessor.create(request, response, user, prefs.copy().assertContentType('application/json')).process {
        WebApiUtilities webUtils ->
            TestBody requestBody = webUtils.getBodyAsJsonElseThrow(TestBody)
            webUtils.setBodyAsJson(requestBody)
    }
}

void testStringBody(HttpServletRequest request, HttpServletResponse response, ISDtObject user){
    RequestProcessor.create(request, response, user, prefs.copy().assertContentType('application/json')).process {
        WebApiUtilities webUtils ->
            String requestBody = webUtils.getBodyAsStringElseThrow()
            webUtils.setBodyAsString(requestBody)
    }
}

void testBinaryBody(HttpServletRequest request, HttpServletResponse response, ISDtObject user){
    RequestProcessor.create(request, response, user, prefs.copy().assertContentType('application/json')).process {
        WebApiUtilities webUtils ->
            byte[] requestBody = webUtils.getBodyAsBinaryElseThrow()
            webUtils.setBodyAsBytes(requestBody, request.getContentType())
    }
}

void testMultipartBody(HttpServletRequest request, HttpServletResponse response, ISDtObject user){
    RequestProcessor.create(request, response, user, prefs.copy().assertContentType('application/json')).process {
        WebApiUtilities webUtils ->
            List<Part> requestBody = webUtils.getBodyAsMultipartElseThrow()
            Part textPart = requestBody.find{it.name == 'text'}
            webUtils.setBodyAsJson(['text' : textPart.get])
    }
}

class TestBody2 {
    String str
    Boolean bool
}

@Field Preferences prefs1 = new Preferences().assertSuperuser().setObjectMapper().setDatePatter("")

void exampleGet1(HttpServletRequest request, HttpServletResponse response, ISDtObject user) {
    RequestProcessor.create(request, response, user, prefs1.copy().assertSuperuser(false)assertUserByLogin("123")).process {
        WebApiUtilities webUtils ->
            String param1 = webUtils.getParamAsStringElseThrow("param1")
            Boolean param2 = webUtils.getParamAsBoolean('param2').orElseThrow({new WebApiException.BadRequest("Нет парама")})
            TestBody2 body = webUtils.getBodyAsJsonElseThrow(TestBody2)
            Map result = [:]
            result.put('param1' , param1)
            result.put('param2', param2)
            result.put('body', body)
            webUtils.setBodyAsJson(result)
    }
}


