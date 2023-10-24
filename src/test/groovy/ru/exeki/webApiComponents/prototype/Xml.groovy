package ru.exeki.webApiComponents.prototype

import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.NodeChildren
import ru.kazantsev.nsd.modules.web_api_components.ProcessData

class Xml extends ProcessData {

    private XmlSlurper xmlSlurper = new XmlSlurper()

    Map convertGPathResultToMap(def node) {
        NodeChildren childrenNodes = node.children()
        return [
                'nodeName'      : node.name(),
                'nodeValue'     : childrenNodes.size() == 0 ? node.text() : null,
                'nodeAttributes': node.attributes(),
                'childNodes'    : childrenNodes.collect { this.convertGPathResultToMap(it) }
        ]
    }

    Xml setXmlSlurper(XmlSlurper xmlSlurper) {
        this.xmlSlurper = xmlSlurper
        return this
    }

    GPathResult parseRequestBodyToGPathResult() {
        return this.xmlSlurper.parseText(this.getRequestBody())
    }

    Map parseRequestBodyToMap() {
        return this.convertGPathResultToMap(this.parseRequestBodyToGPathResult())
    }

}