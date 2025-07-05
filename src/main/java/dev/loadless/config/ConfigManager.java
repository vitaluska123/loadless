package dev.loadless.config;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

public class ConfigManager {
    private static final String CONFIG_FILE = "config.xml";
    private Document configDoc;
    private File configFile;

    public ConfigManager() throws Exception {
        configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        loadConfig();
    }

    private void createDefaultConfig() throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        configDoc = dBuilder.newDocument();
        Element rootElement = configDoc.createElement("loadless-config");
        configDoc.appendChild(rootElement);
        // Добавляем секцию ядра с настройками по умолчанию
        Element core = configDoc.createElement("core");
        core.setAttribute("host", "0.0.0.0");
        core.setAttribute("port", "25565");
        rootElement.appendChild(core);
        saveConfig();
    }

    private void loadConfig() throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        configDoc = dBuilder.parse(configFile);
        configDoc.getDocumentElement().normalize();
    }

    public void saveConfig() throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(configDoc);
        StreamResult result = new StreamResult(configFile);
        transformer.transform(source, result);
    }

    public Document getConfigDoc() {
        return configDoc;
    }

    public Element getOrCreateModuleConfig(String moduleName) {
        NodeList modules = configDoc.getElementsByTagName("module");
        for (int i = 0; i < modules.getLength(); i++) {
            Element module = (Element) modules.item(i);
            if (moduleName.equals(module.getAttribute("name"))) {
                return module;
            }
        }
        // create new module config
        Element module = configDoc.createElement("module");
        module.setAttribute("name", moduleName);
        configDoc.getDocumentElement().appendChild(module);
        return module;
    }

    public String getCoreHost() {
        NodeList coreList = configDoc.getElementsByTagName("core");
        if (coreList.getLength() > 0) {
            Element core = (Element) coreList.item(0);
            return core.getAttribute("host");
        }
        return "0.0.0.0";
    }

    public int getCorePort() {
        NodeList coreList = configDoc.getElementsByTagName("core");
        if (coreList.getLength() > 0) {
            Element core = (Element) coreList.item(0);
            try {
                return Integer.parseInt(core.getAttribute("port"));
            } catch (Exception ignored) {}
        }
        return 25565;
    }

    // Получить или создать секцию настроек только для этого модуля
    public Element getModuleConfig(String moduleName) {
        NodeList modules = configDoc.getElementsByTagName("module");
        for (int i = 0; i < modules.getLength(); i++) {
            Element module = (Element) modules.item(i);
            if (moduleName.equals(module.getAttribute("name"))) {
                return module;
            }
        }
        // create new module config
        Element module = configDoc.createElement("module");
        module.setAttribute("name", moduleName);
        configDoc.getDocumentElement().appendChild(module);
        return module;
    }

    // Получить значение параметра модуля
    public String getModuleParam(String moduleName, String key, String defaultValue) {
        Element module = getModuleConfig(moduleName);
        if (module.hasAttribute(key)) {
            return module.getAttribute(key);
        }
        return defaultValue;
    }

    // Установить значение параметра модуля
    public void setModuleParam(String moduleName, String key, String value) throws TransformerException {
        Element module = getModuleConfig(moduleName);
        module.setAttribute(key, value);
        saveConfig();
    }
}
