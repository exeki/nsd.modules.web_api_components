

import groovy.json.JsonBuilder
import org.springframework.web.multipart.MultipartHttpServletRequest

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @param response ответ для наполнения
 * @param message сообщение об ошибке
 * @param status статус ответа (не надо передавать сюда 200, это должно быть сообщение об ошибке)
 */
private void setErrorResponse(HttpServletResponse response, String message, Integer status = null) {
    Integer statusCode = status ?: HttpServletResponse.SC_BAD_REQUEST
    response.setStatus(statusCode)
    Map responseBody = [
            "error": [
                    "status" : statusCode,
                    "message": message
            ]
    ]
    PrintWriter writer = response.writer
    writer.write(new JsonBuilder(responseBody).toPrettyString())
    writer.flush()
    writer.close()
}

/**
 * записывает данные ответа в response
 * @param response ответ, наполяемый данными
 * @param responseMap body ответа
 */
private void setResponse(HttpServletResponse response, Map responseMap) {
    response.setStatus(HttpServletResponse.SC_OK)
    PrintWriter writer = response.writer
    writer.write(new JsonBuilder(responseMap).toPrettyString())
    writer.flush()
    writer.close()
}


void testContentType(HttpServletRequest request, HttpServletResponse response, Map user) {
    try {
        setResponse(
                response,
                [
                        'contentType' : request.getContentType(),
                        'method' : request.getMethod(),
                        'body' : request.getReader().getText()
                ]
        )
        logger.info("ответ сформирован")
    } catch (Exception e) {
        logInfo("ОШИБКА: ${e.message}")
        logger.info(response, "Произошла ошибка ${e.message}", HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
    }
}

void testFiles(HttpServletRequest request, HttpServletResponse response, Map user){
    def fileObject = utils.find('file', ['source' : 'serviceCall$39740902']).last()

    byte[] byteArray = utils.readFileContent(fileObject)
    response.setContentType("YOUR CONTENT TYPE HERE");
    response.setHeader("Content-Disposition", "filename=\"THE FILE NAME\"");
    response.setContentLength(byteArray.length);
    OutputStream os = response.getOutputStream();
    try {
        os.write(byteArray , 0, byteArray.length);
    } catch (Exception excp) {
        //handle error
    } finally {
        os.close();
    }
}

def testMutlipart(HttpServletRequest request, HttpServletResponse response, Map user){
    MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request

    return multipartRequest.getFile('files').getOriginalFilename()
}















