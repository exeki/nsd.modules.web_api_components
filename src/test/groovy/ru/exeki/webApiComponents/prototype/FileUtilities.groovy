package ru.exeki.webApiComponents.prototype

import ru.kazantsev.nsd.modules.web_api_components.ProcessData
import org.springframework.web.multipart.MultipartFile;
import ru.naumen.core.server.script.spi.IScriptDtObject;
import ru.naumen.core.shared.dto.ISDtObject;

class FileUtilities {

    static ISDtObject attachMultipartFile(
            MultipartFile file,
            ISDtObject dtObject,
            String sourceAttr = null,
            String description = null
    ) {
        return utils.attachFile(
                dtObject as IScriptDtObject,
                sourceAttr,
                file.getOriginalFilename(),
                file.getContentType(),
                description,
                file.getBytes()
        )
    }

    static ISDtObject attachMultipartFile(
            MultipartFile file,
            String dtObjectUuid,
            String sourceAttr = null,
            String description = null
    ) {
        return attachMultipartFile(file, utils.get(dtObjectUuid) as IScriptDtObject, sourceAttr, description)
    }

    static List<ISDtObject> attachMultipartFiles(
            List<MultipartFile> files,
            ISDtObject dtObject,
            String sourceAttr = null,
            String description = null
    ) {
        files.collect {
            return attachMultipartFile(it, dtObject, sourceAttr, description)
        }
    }

    static List<ISDtObject> attachMultipartFiles(
            List<MultipartFile> files,
            String dtObjectUuid,
            String sourceAttr = null,
            String description = null
    ) {
        return attachMultipartFiles(files, utils.get(dtObjectUuid), sourceAttr, description)
    }

    static List<ISDtObject> attachMultipartFilesFromKey(
            ProcessData.MultipartBody processData,
            String formKey,
            ISDtObject dtObject,
            String sourceAttr = null,
            String description = null
    ) {
        return attachMultipartFiles(processData.getFiles(formKey), dtObject, sourceAttr, description)
    }

    static List<ISDtObject> attachMultipartFilesFromKey(
            ProcessData.MultipartBody processData,
            String formKey,
            String dtObjectUuid,
            String sourceAttr = null,
            String description = null
    ) {
        return attachMultipartFiles(
                processData.getFiles(formKey),
                utils.get(dtObjectUuid) as IScriptDtObject,
                sourceAttr,
                description
        )
    }

    static attachFileFromBinaryRequest(
            ProcessData.BinaryBody processData,
            ISDtObject dtObject,
            String contentType,
            String fileName,
            String sourceAttr = null,
            String description = null
    ) {
        return utils.attachFile(
                dtObject as IScriptDtObject,
                sourceAttr,
                fileName,
                contentType,
                description,
                processData.getRequest().getInputStream().getBytes()
        )
    }


}
