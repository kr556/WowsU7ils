package org.w7ls.vmeditor;

import org.w3c.dom.*;
import org.w7ls.vmeditor.VMAudioFile;
import org.xml.sax.InputSource;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.io.StringReader;
import java.util.*;

public class AudioModification {

    // ExternalEventを表すデータクラス
    public static class ExternalEvent implements Serializable{
        @Serial
        private static final long serialVersionUID = -3174827957348456444L;

        public final String name;
        public final String externalId;
        public final String shortName;
        public final List<VMAudioFile> audios = new ArrayList<>();

        public ExternalEvent(String name, String externalId) {
            this.name = name;
            this.externalId = externalId;
            this.shortName = name.replaceFirst("^Play_VO_", "");
        }

        @Override
        public String toString() {
            return shortName;
        }
    }

    public static class Path implements Serializable {
        @Serial
        private static final long serialVersionUID = 792867465506371119L;

        public List<State> states = new ArrayList<>();
        public List<String> files = new ArrayList<>();
    }

    public static class State implements Serializable {
        @Serial
        private static final long serialVersionUID = -4957577501654186119L;

        public String name;
        public String value;

        public State(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    // ── フィールド ────────────────────────────

    File file;
    Document document;
    String modName;
    List<ExternalEvent> events = new ArrayList<>();

    // ── 読み込み ──────────────────────────────

    public void load(File file) throws Exception {
        this.file = file;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.parse(file);
        document.getDocumentElement().normalize();
        parse();
    }

    private void parse() {
        events.clear();

        // <AudioModification>
        Element root = (Element) document
                .getElementsByTagName("AudioModification").item(0);
        modName = getChildText(root, "Name");

        // <ExternalEvent>を全部取得
        NodeList eventNodes = root.getElementsByTagName("ExternalEvent");
        for (int i = 0; i < eventNodes.getLength(); i++) {
            Element eventEl = (Element) eventNodes.item(i);
            String name = getChildText(eventEl, "Name");

            Element container = (Element) eventEl
                    .getElementsByTagName("Container").item(0);
            String externalId = getChildText(container, "ExternalId");

            ExternalEvent event = new ExternalEvent(name, externalId);

            // <Path>を全部取得
            NodeList pathNodes = container.getElementsByTagName("Path");
            for (int j = 0; j < pathNodes.getLength(); j++) {
                Element pathEl = (Element) pathNodes.item(j);
                Path path = new Path();

                // StateListのStateを取得
                NodeList stateNodes = pathEl.getElementsByTagName("State");
                for (int k = 0; k < stateNodes.getLength(); k++) {
                    Element stateEl = (Element) stateNodes.item(k);
                    path.states.add(new State(
                            getChildText(stateEl, "Name"),
                            getChildText(stateEl, "Value")
                    ));
                }

                // FilesListのFileを取得
                NodeList fileNodes = pathEl.getElementsByTagName("File");
                for (int k = 0; k < fileNodes.getLength(); k++) {
                    Element fileEl = (Element) fileNodes.item(k);
                    path.files.add(getChildText(fileEl, "Name"));
                }
            }

            events.add(event);
        }
    }

    // ── 保存 ──────────────────────────────────

    public void save() throws Exception {
        saveAs(file);
    }

    public void saveAs(File dest) throws Exception {
        // 一時ファイルに書き出す
        File temp = File.createTempFile("mod_", ".xml", dest.getParentFile());
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(document), new StreamResult(temp));

            // 書き出し成功後にリネーム
            if (dest.exists()) dest.delete();
            temp.renameTo(dest);
        } catch (Exception e) {
            temp.delete(); // 失敗したら一時ファイルを削除
            throw e;
        }
    }

    // ── ゲッター ──────────────────────────────

    public List<ExternalEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public String getModName() {
        return modName;
    }

    public File getFile() {
        return file;
    }

    // ── ユーティリティ ────────────────────────

    private String getChildText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) return "";
        return nodes.item(0).getTextContent().trim();
    }

    public void removeEvent(int index) {
        events.remove(index);
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void loadFromString(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.parse(new InputSource(new StringReader(xmlContent)));
        document.getDocumentElement().normalize();
        parse();
    }
}