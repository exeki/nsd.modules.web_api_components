package ru.kazantsev.nsd.modules.web_api_components.example

import com.fasterxml.jackson.databind.ObjectMapper
import ru.naumen.core.shared.dto.ISDtObject

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@SuppressWarnings('unused')
String exampleGet1(HttpServletRequest request, HttpServletResponse response, ISDtObject user) {
    String str = request.reader.text
    try {
        String param = request.getParameter("test")
        if(param == null) {
            throw new Exception("")
        }
    } catch (Exception e) {

    }

    try {
        Map map = new ObjectMapper().readValue(str, Map)
    } catch (Exception e) {
        response.setStatus(500)
        response.writer.write(new ObjectMapper().writeValueAsString(['message' : 'ytdfhrth', 'state' : 500]))
    }
    return new ObjectMapper().writeValueAsString(
            [
                    'getHeaders(test)'        : request.getHeaders("test").collect { it.toString() },
                    "getParameterMap"         : request.getParameterMap(),
                    'getParameterValues(test)': request.getParameterValues("test"),
                    'getHeader(test4)'        : request.getHeader('test4'),
                    'getIntHeader(test2)'     : request.getIntHeader('test2'),
                    'getDateHeader(test3)'    : request.getDateHeader('test3'),
                    'getContentType'          : request.getContentType(),
                    'getProtocol'             : request.getProtocol(),
                    'getServerName'           : request.getServerName(),
                    'getPathInfo'             : request.getPathInfo(),
                    'getPathTranslated'       : request.getPathTranslated(),
                    'getProperties'           : request.getProperties().collectEntries { key, value -> [key, value.toString()] },
                    'getAuthType'             : request.getAuthType(),
                    'getRemoteAddr'           : request.getRemoteAddr(),
                    'getRemoteHost'           : request.getRemoteHost(),
                    'getRemotePort'           : request.getRemotePort(),
                    'getRemoteUser'           : request.getRemoteUser(),
                    'getScheme'               : request.getScheme(),
                    'isSecure'                : request.isSecure(),
                    'getRequestURI'           : request.getRequestURI(),
                    'getRequestURL'           : request.getRequestURL(),
                    'getQueryString'          : request.getQueryString(),
                    'getUserPrincipal'        : request.getUserPrincipal()
            ]
    )
}
