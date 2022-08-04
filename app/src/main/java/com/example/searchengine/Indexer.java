package com.example.searchengine;


import org.apache.commons.text.StringEscapeUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Indexer {
    private static HashMap<String, List<DocIdWithFrequency>> invertedindexHashMap = new HashMap<>();
    private static HashMap<Integer, Doc> documentsHashMap = new HashMap<>();

    private static String XMLsPath = "D:\\Projects\\" +
            "Android\\Me\\1398\\SearchEngine\\app\\src\\main\\assets\\WEBIR_S";
    private static final String DICTIONARY_FILE_PATH = "dictionary.txt";

    private static int uniqueId;

    private static int BAD_DOCUMENT_ID = 485;

    private static String invertedIndexFilePath = "D:\\Projects\\Android\\Me\\1398\\SearchEngine\\app\\src\\main\\assets\\inverted_index.txt";
    private static String documentsFilePath = "D:\\Projects\\Android\\Me\\1398\\SearchEngine\\app\\src\\main\\assets\\documents.txt";

    public static void main(String[] args) {
        try {
            bindingHashMapsFromFile();
        } catch (JDOMException | IOException e) {
            e.printStackTrace();
        }

        long startTime = System.currentTimeMillis();
        saveInvertedIndexHashMapToFile();
        System.out.println("saveInvertedIndexHashMapToFile time: " + (System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();
        saveDocumentsHashMapToFile();
        System.out.println("saveDocumentsHashMapToFile time: " + (System.currentTimeMillis() - startTime));
    }

    private static void bindingHashMapsFromFile() throws JDOMException, IOException {
        long startTime = System.currentTimeMillis();
        int documentsCount = 0;
        int wordsCount = 0;
        int lettersCount = 0;
        int size = Objects.requireNonNull(Objects.requireNonNull(new File(XMLsPath)).listFiles()).length;
        for (int i = 0; i < size; i++) {
            System.out.println("reading file " + i + " started");
            List<Element> children = getEachFilesDocs(XMLsPath, i);
            for (int j = 0; j < children.size(); j++) {
                documentsCount++;

                Doc doc = new Doc();
                Element docI = children.get(j);

                doc.setId(uniqueId++);
                doc.setUrl(docI.getChildText("URL"));
                String htmlText = docI.getChildText("HTML");
                String htmlTextWithoutTitle = setDocTitleAndReturnHtmlWithoutTitle(doc, htmlText);

                StringBuilder stringBuilder = new StringBuilder();
                String persianRegex = "(" + "[آ-ی]" + "|" + "[ي]" + "|" + "[ك]" + ")" + "+";
                Matcher matcher = Pattern.compile(persianRegex).matcher(htmlTextWithoutTitle);
                int bodyWeight = 1;
                if (doc.getId() != BAD_DOCUMENT_ID) {
                    while (matcher.find()) {
                        doc.setWordCount(doc.getWordCount() + 1);

                        String word = matcher.group();
                        stringBuilder.append(word).append(" ");

                        wordsCount++;
                        lettersCount += word.length();

                        if (invertedindexHashMap.get(word) == null) {
                            List<DocIdWithFrequency> docIdWithFrequencies = new ArrayList<>();
                            DocIdWithFrequency docIdWithFrequency = new DocIdWithFrequency(doc.getId(), bodyWeight);
                            docIdWithFrequencies.add(docIdWithFrequency);
                            invertedindexHashMap.put(word, docIdWithFrequencies);
                        } else {
                            List<DocIdWithFrequency> docIdWithFrequencies = invertedindexHashMap.get(word);
                            if (docIdWithFrequencies != null)
                                if (!isDocIdWithScoreExists(doc.getId(), docIdWithFrequencies)) {
                                    DocIdWithFrequency docIdWithFrequency = new DocIdWithFrequency(doc.getId(), bodyWeight);
                                    docIdWithFrequencies.add(docIdWithFrequency);
                                    invertedindexHashMap.put(word, docIdWithFrequencies);
                                } else {
                                    for (int k = 0; k < docIdWithFrequencies.size(); k++) {
                                        if (docIdWithFrequencies.get(k).getDocId() == doc.getId()) {
                                            docIdWithFrequencies.get(k)
                                                    .setFrequency(docIdWithFrequencies.get(k).getFrequency() + bodyWeight);
                                            break;
                                        }
                                    }
                                }
                        }
                    }

                    if (doc.getTitle() != null) {
                        Matcher titleMatcher = Pattern.compile(persianRegex).matcher(doc.getTitle());
                        int titleWeight = 60;
                        while (titleMatcher.find()) {
                            doc.setWordCount(doc.getWordCount() + 1);

                            String word = titleMatcher.group();

                            wordsCount++;
                            lettersCount += word.length();

                            if (invertedindexHashMap.get(word) == null) {
                                List<DocIdWithFrequency> docIdWithFrequencies = new ArrayList<>();
                                DocIdWithFrequency docIdWithFrequency = new DocIdWithFrequency(doc.getId(), titleWeight);
                                docIdWithFrequencies.add(docIdWithFrequency);
                                invertedindexHashMap.put(word, docIdWithFrequencies);
                            } else {
                                List<DocIdWithFrequency> docIdWithFrequencies = invertedindexHashMap.get(word);
                                if (docIdWithFrequencies != null)
                                    if (!isDocIdWithScoreExists(doc.getId(), docIdWithFrequencies)) {
                                        DocIdWithFrequency docIdWithFrequency = new DocIdWithFrequency(doc.getId(), titleWeight);
                                        docIdWithFrequencies.add(docIdWithFrequency);
                                        invertedindexHashMap.put(word, docIdWithFrequencies);
                                    } else {
                                        for (int k = 0; k < docIdWithFrequencies.size(); k++) {
                                            if (docIdWithFrequencies.get(k).getDocId() == doc.getId()) {
                                                docIdWithFrequencies.get(k)
                                                        .setFrequency(docIdWithFrequencies.get(k).getFrequency() + titleWeight);
                                                break;
                                            }
                                        }
                                    }
                            }
                        }
                    }

                    doc.setBody(stringBuilder.toString());
                    documentsHashMap.put(doc.getId(), doc);
                }
            }
        }
        System.out.println("bindingHashMapsFromFile time: " + (System.currentTimeMillis() - startTime));

        System.out.println("documentsCount= " + documentsCount);
        System.out.println("wordsCount= " + wordsCount);
        System.out.println("lettersCount= " + lettersCount);
        System.out.println("invertedindexHashMap.size(): " + invertedindexHashMap.size());

        startTime = System.currentTimeMillis();
        for (HashMap.Entry<String, List<DocIdWithFrequency>> entry : invertedindexHashMap.entrySet()) {
            List<DocIdWithFrequency> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                DocIdWithFrequency docIdWithFrequency = list.get(i);
                Doc doc = documentsHashMap.get(docIdWithFrequency.getDocId());
                if (doc != null) {
                    double score = (Math.log10(docIdWithFrequency.getFrequency() /*/ (double) doc.getWordCount()*/)
                            * Math.log10(documentsCount / (double) list.size()));
                    list.get(i).setFrequency(score);
                }
            }
        }
        System.out.println("index time: " + (System.currentTimeMillis() - startTime));
    }

    private static void saveDocumentsHashMapToFile() {
        System.out.println("start saveDocumentsHashMapToFile");
        File textFile = new File(documentsFilePath);
        try (FileOutputStream fileOutputStream = new FileOutputStream(textFile);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
            objectOutputStream.writeObject(documentsHashMap);
        } catch (IOException e) {
            System.out.println("catch");
            e.printStackTrace();
        }
        System.out.println("end saveDocumentsHashMapToFile");
    }

    private static void saveInvertedIndexHashMapToFile() {
        System.out.println("start saveInvertedindexHashMapToFile");
        File file = new File(invertedIndexFilePath);
        try (FileOutputStream fileOutputStream = new FileOutputStream(file);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
            objectOutputStream.writeObject(invertedindexHashMap);
            System.out.println("end saveInvertedindexHashMapToFile\n");
        } catch (IOException e) {
            System.out.println("catch");
            e.printStackTrace();
        }
    }

    private static List<Element> getEachFilesDocs(String pathname, int i) throws
            JDOMException, IOException {
        SAXBuilder saxBuilder = new SAXBuilder();
        File file = new File(pathname + "/" + "WebIR-" + formatNumber(i + 1) + ".xml");
        Document document = saxBuilder.build(file);
        Element rootElement = document.getRootElement();
        return rootElement.getChildren("DOC");
    }

    private static String formatNumber(int number) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 3 - String.valueOf(number).length(); i++) {
            stringBuilder.append("0");
        }
        return stringBuilder.append(number).toString();
    }

    private static String setDocTitleAndReturnHtmlWithoutTitle(Doc doc, String htmlText) {
        String findTitleRegex = "<title>.*</title>";
        Matcher matcher = Pattern.compile(findTitleRegex).matcher(htmlText);
        if (matcher.find()) {
            String scope = matcher.group();
            String greaterThanSign = ">";
            String lessThanSign = "<";
            doc.setTitle(StringEscapeUtils.unescapeHtml4(scope.substring(scope.indexOf(greaterThanSign) +
                    greaterThanSign.length(), scope.lastIndexOf(lessThanSign))));
            return htmlText.substring(scope.lastIndexOf(lessThanSign) + "</title>".length());
        }
        return htmlText;
    }

    private static boolean isDocIdWithScoreExists(int docId, List<DocIdWithFrequency> docIdWithFrequencies) {
        for (int i = 0; i < docIdWithFrequencies.size(); i++) {
            if (docIdWithFrequencies.get(i).getDocId() == docId)
                return true;
        }
        return false;
    }
}