package ru.exeki.webApiComponents.prototype
import static ru.ekazantsev.nsd_empty_fake_api.EmptyNaumenApiPlaceholder.*


import ru.naumen.core.shared.dto.DtObject
import ru.naumen.core.server.script.api.metainfo.IStateWrapper
import ru.naumen.metainfo.shared.IClassFqn

/*
class java.lang.Boolean
class org.codehaus.groovy.runtime.NullObject
class ru.naumen.core.server.script.spi.ScriptDtOSet
class java.lang.Long
class java.lang.String
class ru.naumen.core.server.script.spi.ScriptDtObject
class ru.naumen.core.server.script.spi.LazyScriptDtObject
class ru.naumen.core.server.script.spi.ScriptDate
class ru.naumen.metainfo.shared.ClassFqn

class ru.naumen.common.shared.utils.DateTimeInterval
class ru.naumen.core.shared.timer.BackTimerDto
class ru.naumen.core.server.script.spi.ScriptDtOList
class ru.naumen.common.shared.utils.Hyperlink
class ru.naumen.core.server.script.spi.AggregateContainerWrapper
class ru.naumen.core.shared.timer.TimerDto
*/

final class Serializer {

    interface IBusinessObjectConverter {
        IBusinessObjectConverter setSerializer(Serializer serializer)

        Map convert(DtObject object)
    }

    class BasicBusinessObjectConverter implements IBusinessObjectConverter {
        private Serializer serializer

        IBusinessObjectConverter setSerializer(Serializer serializer) {
            this.serializer = serializer
            return this
        }

        Map convert(DtObject object) {
            if (object == null) {
                return object
            } else {
                return [
                        "title": object.title,
                        "UUID" : object.UUID
                ]
            }
        }
    }


    private String dateTimePattern = 'dd.MM.yyyy HH:hh:ss'
    private String datePattern = 'dd.MM.yyyy'

    Map<String, Class<? extends IBusinessObjectConverter>> serializerMap = [
            'abstractBO' : BasicBusinessObjectConverter,
            'catalogItem': BasicBusinessObjectConverter
    ]

    Serializer setDatePattern(String pattern) {
        datePattern = pattern
        return this
    }

    Serializer setDateTimePattern(String pattern) {
        dateTimePattern = pattern
        return this
    }

    String prepareObject(String str) {
        return str
    }

    Boolean prepareObject(Boolean value) {
        return value
    }

    Long prepareObject(Long num) {
        return num
    }

    Integer prepareObject(Integer num) {
        return num
    }

    String prepareObject(Date date) {
        if (date == null) {
            return null
        } else {
            return date.format(dateTimePattern)
        }
    }

    Map prepareObject(DtObject dtObject) {
        if (dtObject == null) {
            return null
        } else {
            String meta = dtObject.getMetaClass().toString()
            while (!serializerMap.hasProperty(meta)) {
                String oldMeta = meta
                meta = api.metainfo.getParentFqn(meta).toString()
                if (oldMeta == meta) {
                    throw new Exception("Не удается найти в serializerMap способ сериализовать объект метакласса ${dtObject.getMetaClass().toString()} или его наиболее высокий в иерархии родитель ${meta}")
                }
            }
            return serializerMap.get(meta)
                    .getDeclaredConstructor()
                    .newInstance()
                    .setSerializer(this)
                    .convert(dtObject)
        }
    }

    Map prepareObject(IClassFqn classFqn) {
        if (classFqn == null) {
            return null
        } else {
            return [
                    'id'  : classFqn.getId(),
                    'code': classFqn.getCode()
            ]
        }
    }

    Map prepareObject(IStateWrapper stateWrapper) {
        if (stateWrapper == null) {
            return null
        } else {
            return [
                    'code' : stateWrapper.getCode(),
                    'title': stateWrapper.getTitle(),
                    'color': stateWrapper.getColor()
            ]
        }
    }

    List<Map> prepareObject(List<DtObject> dtObjectsList) {
        if (dtObjectsList == null) {
            return null
        } else {
            return dtObjectsList.collect { prepareObject(it) }
        }
    }

}

