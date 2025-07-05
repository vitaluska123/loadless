package dev.loadless.config;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import dev.loadless.core.Logger;
import java.security.SecureRandom;
import java.math.BigInteger;

public class ConfigManager {
    private static final String CONFIG_FILE = "config.xml";
    private Document configDoc;
    private File configFile;
    private Logger logger;

    public ConfigManager(Logger logger) throws Exception {
        this.logger = logger;
        configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        loadConfig();
        // Генерация ключа при первом запуске
        if (getAuthKey() == null || getAuthKey().isEmpty()) {
            String key = generateRandomKey(48);
            setAuthKey(key);
            logger.log("[Config] Сгенерирован новый authKey: " + key);
        }
        logger.log("[Config] Конфиг загружен из " + configFile.getAbsolutePath());
    }
    private String generateRandomKey(int length) {
        SecureRandom random = new SecureRandom();
        return new BigInteger(length * 5, random).toString(32);
    }

    private void createDefaultConfig() throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        configDoc = dBuilder.newDocument();
        Element rootElement = configDoc.createElement("loadless");
        configDoc.appendChild(rootElement);
        // <core>
        Element core = configDoc.createElement("core");
        Element host = configDoc.createElement("host");
        host.setTextContent("0.0.0.0");
        core.appendChild(host);
        Element port = configDoc.createElement("port");
        port.setTextContent("25566");
        core.appendChild(port);
        // <version>
        Element version = configDoc.createElement("version");
        Element versionName = configDoc.createElement("name");
        versionName.setTextContent("1.20.6");
        version.appendChild(versionName);
        Element versionProtocol = configDoc.createElement("protocol");
        versionProtocol.setTextContent("765");
        version.appendChild(versionProtocol);
        core.appendChild(version);
        // <motd>
        Element motd = configDoc.createElement("motd");
        motd.setTextContent("§aLoadless Proxy Server");
        core.appendChild(motd);
        // <authKey>
        Element authKey = configDoc.createElement("authKey");
        authKey.setTextContent(""); // будет сгенерирован при первом запуске
        core.appendChild(authKey);
        rootElement.appendChild(core);
        // <realServer>
        Element realServer = configDoc.createElement("realServer");
        Element realHost = configDoc.createElement("host");
        realHost.setTextContent("127.0.0.1");
        realServer.appendChild(realHost);
        Element realPort = configDoc.createElement("port");
        realPort.setTextContent("25565");
        realServer.appendChild(realPort);
        rootElement.appendChild(realServer);
        // <modules>
        Element modules = configDoc.createElement("modules");
        rootElement.appendChild(modules);
        // <localize>
        Element localize = configDoc.createElement("localize");
        Element offline = configDoc.createElement("offline");
        offline.setTextContent("offline");
        localize.appendChild(offline);
        rootElement.appendChild(localize);
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
        if (logger != null) logger.log("[Config] Конфиг сохранён: " + configFile.getAbsolutePath());
    }

    public Document getConfigDoc() {
        return configDoc;
    }

    // Новый способ получения параметров
    private Element getElementByTagChain(String... tags) {
        Element el = configDoc.getDocumentElement();
        for (String tag : tags) {
            NodeList nl = el.getElementsByTagName(tag);
            if (nl.getLength() == 0) return null;
            el = (Element) nl.item(0);
        }
        return el;
    }

    public String getCoreHost() {
        Element el = getElementByTagChain("core", "host");
        return el != null ? el.getTextContent() : "0.0.0.0";
    }
    public int getCorePort() {
        Element el = getElementByTagChain("core", "port");
        try { return el != null ? Integer.parseInt(el.getTextContent()) : 25565; } catch (Exception e) { return 25565; }
    }
    public String getVersionName() {
        Element el = getElementByTagChain("core", "version", "name");
        return el != null ? el.getTextContent() : "1.20.6";
    }
    public int getVersionProtocol() {
        Element el = getElementByTagChain("core", "version", "protocol");
        try { return el != null ? Integer.parseInt(el.getTextContent()) : 765; } catch (Exception e) { return 765; }
    }
    public int getPlayersMax() {
        Element el = getElementByTagChain("core", "players", "max");
        try { return el != null ? Integer.parseInt(el.getTextContent()) : 20; } catch (Exception e) { return 20; }
    }
    public int getPlayersOnline() {
        Element el = getElementByTagChain("core", "players", "online");
        try { return el != null ? Integer.parseInt(el.getTextContent()) : 1; } catch (Exception e) { return 1; }
    }
    public String getDefaultMotd() {
        Element el = getElementByTagChain("core", "motd");
        return el != null ? el.getTextContent() : "§aLoadless Proxy Server";
    }
    public String getRealServerHost() {
        Element el = getElementByTagChain("realServer", "host");
        return el != null ? el.getTextContent() : "127.0.0.1";
    }
    public int getRealServerPort() {
        Element el = getElementByTagChain("realServer", "port");
        try { return el != null ? Integer.parseInt(el.getTextContent()) : 25566; } catch (Exception e) { return 25566; }
    }
    public String getAuthKey() {
        Element el = getElementByTagChain("core", "authKey");
        return el != null ? el.getTextContent() : null;
    }
    public void setAuthKey(String value) throws TransformerException {
        Element core = getElementByTagChain("core");
        if (core != null) {
            NodeList list = core.getElementsByTagName("authKey");
            Element el;
            if (list.getLength() > 0) {
                el = (Element) list.item(0);
            } else {
                el = configDoc.createElement("authKey");
                core.appendChild(el);
            }
            el.setTextContent(value);
            saveConfig();
        }
    }

    // Модули теперь в <modules><module name="..."></module></modules>
    public Element getOrCreateModuleConfig(String moduleName) {
        NodeList modulesList = configDoc.getElementsByTagName("modules");
        Element modules;
        if (modulesList.getLength() > 0) {
            modules = (Element) modulesList.item(0);
        } else {
            modules = configDoc.createElement("modules");
            configDoc.getDocumentElement().appendChild(modules);
        }
        NodeList moduleNodes = modules.getElementsByTagName("module");
        for (int i = 0; i < moduleNodes.getLength(); i++) {
            Element module = (Element) moduleNodes.item(i);
            if (moduleName.equals(module.getAttribute("name"))) {
                return module;
            }
        }
        // create new module config
        Element module = configDoc.createElement("module");
        module.setAttribute("name", moduleName);
        modules.appendChild(module);
        return module;
    }

    public String getModuleParam(String moduleName, String key, String defaultValue) {
        Element module = getOrCreateModuleConfig(moduleName);
        NodeList params = module.getElementsByTagName(key);
        if (params.getLength() > 0) {
            return params.item(0).getTextContent();
        }
        return defaultValue;
    }

    public void setModuleParam(String moduleName, String key, String value) throws TransformerException {
        Element module = getOrCreateModuleConfig(moduleName);
        NodeList params = module.getElementsByTagName(key);
        Element param;
        if (params.getLength() > 0) {
            param = (Element) params.item(0);
        } else {
            param = configDoc.createElement(key);
            module.appendChild(param);
        }
        param.setTextContent(value);
        saveConfig();
        if (logger != null) logger.log("[Config] Параметр модуля '" + moduleName + "' -> " + key + " = " + value);
    }
    public String getOfflineFlag() {
        Element el = getElementByTagChain("localize", "offline");
        return el != null ? el.getTextContent() : "offline";
    }
    public void setOfflineFlag(String value) throws TransformerException {
        Element localize = getOrCreateLocalize();
        NodeList list = localize.getElementsByTagName("offline");
        Element el;
        if (list.getLength() > 0) {
            el = (Element) list.item(0);
        } else {
            el = configDoc.createElement("offline");
            localize.appendChild(el);
        }
        el.setTextContent(value);
        saveConfig();
    }
    private Element getOrCreateLocalize() {
        NodeList list = configDoc.getElementsByTagName("localize");
        Element localize;
        if (list.getLength() > 0) {
            localize = (Element) list.item(0);
        } else {
            localize = configDoc.createElement("localize");
            configDoc.getDocumentElement().appendChild(localize);
        }
        return localize;
    }
}
