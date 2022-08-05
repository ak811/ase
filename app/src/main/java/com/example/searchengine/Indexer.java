package com.example.searchengine;


import android.util.Log;

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

import static com.example.searchengine.utils.Constants.BINDING_HASH_MAPS_FROM_FILE_TIME_MSG;
import static com.example.searchengine.utils.Constants.DOCS_EXTENSION;
import static com.example.searchengine.utils.Constants.DOCS_SUFFIX_FORMAT;
import static com.example.searchengine.utils.Constants.DOCUMENTS_FILE_PATH;
import static com.example.searchengine.utils.Constants.DOCUMENT_COUNT_MSG;
import static com.example.searchengine.utils.Constants.DOC_TAG_STR;
import static com.example.searchengine.utils.Constants.GREATER_THAN_SIGN;
import static com.example.searchengine.utils.Constants.HTML_TAG_STR;
import static com.example.searchengine.utils.Constants.INDEX_TIME_MAG;
import static com.example.searchengine.utils.Constants.INVERTED_INDEX_FILE_PATH;
import static com.example.searchengine.utils.Constants.LESS_THAN_SIGN;
import static com.example.searchengine.utils.Constants.LETTER_COUNT;
import static com.example.searchengine.utils.Constants.PERSIAN_REGEX;
import static com.example.searchengine.utils.Constants.SAVE_DOCUMENTS_HASH_MAP_IN_FILE_TIME_MSG;
import static com.example.searchengine.utils.Constants.SAVE_INVERTED_INDEX_HASH_MAP_IN_FILE_TIME_MSG;
import static com.example.searchengine.utils.Constants.SPACE_CHARACTER;
import static com.example.searchengine.utils.Constants.SUBSTANDARD_DOCUMENT_ID;
import static com.example.searchengine.utils.Constants.TITLE_END_TAG;
import static com.example.searchengine.utils.Constants.TITLE_REGEX;
import static com.example.searchengine.utils.Constants.TITLE_WEIGHT;
import static com.example.searchengine.utils.Constants.URL_TAG_STR;
import static com.example.searchengine.utils.Constants.WORD_COUNT;
import static com.example.searchengine.utils.Constants.XML_FILES_PATH;

public class Indexer {
    private static final String TAG = Indexer.class.getSimpleName();
    private static final HashMap<String, List<DocIdWithFrequency>> invertedIndexHashMap = new HashMap<>();
    private static final HashMap<Integer, Doc> documentsHashMap = new HashMap<>();
    private static int uniqueId;

    public static void main(String[] args) {
        try {
            bindingHashMapsFromFile();
        } catch (JDOMException | IOException e) {
            e.printStackTrace();
        }

        long startTime = System.currentTimeMillis();
        saveInvertedIndexHashMapInFile();
        Log.i(TAG, SAVE_INVERTED_INDEX_HASH_MAP_IN_FILE_TIME_MSG + (System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();
        saveDocumentsHashMapInFile();
        Log.i(TAG, SAVE_DOCUMENTS_HASH_MAP_IN_FILE_TIME_MSG + (System.currentTimeMillis() - startTime));
    }

    private static void bindingHashMapsFromFile() throws JDOMException, IOException {
        long startTime = System.currentTimeMillis();
        int documentsCount = 0;
        int wordsCount = 0;
        int lettersCount = 0;
        int size = Objects.requireNonNull(new File(XML_FILES_PATH).listFiles()).length;

        for (int i = 0; i < size; i++) {
            List<Element> children = getEachFilesDocs(i);
            for (int j = 0; j < children.size(); j++) {
                documentsCount++;

                Doc doc = new Doc();
                Element docI = children.get(j);

                doc.setId(uniqueId++);
                doc.setUrl(docI.getChildText(URL_TAG_STR));
                String htmlText = docI.getChildText(HTML_TAG_STR);
                String htmlTextWithoutTitle = setDocTitleAndReturnHtmlWithoutTitle(doc, htmlText);

                StringBuilder stringBuilder = new StringBuilder();
                Matcher matcher = Pattern.compile(PERSIAN_REGEX).matcher(htmlTextWithoutTitle);
                int bodyWeight = 1;
                if (doc.getId() != SUBSTANDARD_DOCUMENT_ID) {
                    while (matcher.find()) {
                        doc.setWordCount(doc.getWordCount() + 1);

                        String word = matcher.group();
                        stringBuilder.append(word).append(SPACE_CHARACTER);

                        wordsCount++;
                        lettersCount += word.length();

                        if (invertedIndexHashMap.get(word) == null) {
                            List<DocIdWithFrequency> docIdWithFrequencies = new ArrayList<>();
                            DocIdWithFrequency docIdWithFrequency = new DocIdWithFrequency(doc.getId(), bodyWeight);
                            docIdWithFrequencies.add(docIdWithFrequency);
                            invertedIndexHashMap.put(word, docIdWithFrequencies);
                        } else {
                            List<DocIdWithFrequency> docIdWithFrequencies = invertedIndexHashMap.get(word);
                            if (docIdWithFrequencies != null)
                                if (!isDocIdWithScoreExists(doc.getId(), docIdWithFrequencies)) {
                                    DocIdWithFrequency docIdWithFrequency = new DocIdWithFrequency(doc.getId(), bodyWeight);
                                    docIdWithFrequencies.add(docIdWithFrequency);
                                    invertedIndexHashMap.put(word, docIdWithFrequencies);
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
                        Matcher titleMatcher = Pattern.compile(PERSIAN_REGEX).matcher(doc.getTitle());
                        while (titleMatcher.find()) {
                            doc.setWordCount(doc.getWordCount() + 1);

                            String word = titleMatcher.group();

                            wordsCount++;
                            lettersCount += word.length();

                            if (invertedIndexHashMap.get(word) == null) {
                                List<DocIdWithFrequency> docIdWithFrequencies = new ArrayList<>();
                                DocIdWithFrequency docIdWithFrequency = new DocIdWithFrequency(doc.getId(), TITLE_WEIGHT);
                                docIdWithFrequencies.add(docIdWithFrequency);
                                invertedIndexHashMap.put(word, docIdWithFrequencies);
                            } else {
                                List<DocIdWithFrequency> docIdWithFrequencies = invertedIndexHashMap.get(word);
                                if (docIdWithFrequencies != null)
                                    if (!isDocIdWithScoreExists(doc.getId(), docIdWithFrequencies)) {
                                        DocIdWithFrequency docIdWithFrequency = new DocIdWithFrequency(doc.getId(), TITLE_WEIGHT);
                                        docIdWithFrequencies.add(docIdWithFrequency);
                                        invertedIndexHashMap.put(word, docIdWithFrequencies);
                                    } else {
                                        for (int k = 0; k < docIdWithFrequencies.size(); k++) {
                                            if (docIdWithFrequencies.get(k).getDocId() == doc.getId()) {
                                                docIdWithFrequencies.get(k)
                                                        .setFrequency(docIdWithFrequencies.get(k).getFrequency() + TITLE_WEIGHT);
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

        Log.i(TAG, BINDING_HASH_MAPS_FROM_FILE_TIME_MSG + (System.currentTimeMillis() - startTime));
        Log.i(TAG, DOCUMENT_COUNT_MSG + documentsCount);
        Log.i(TAG, WORD_COUNT + wordsCount);
        Log.i(TAG, LETTER_COUNT + lettersCount);

        startTime = System.currentTimeMillis();
        for (HashMap.Entry<String, List<DocIdWithFrequency>> entry : invertedIndexHashMap.entrySet()) {
            List<DocIdWithFrequency> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                DocIdWithFrequency docIdWithFrequency = list.get(i);
                Doc doc = documentsHashMap.get(docIdWithFrequency.getDocId());
                if (doc != null) {
                    double score = (Math.log10(docIdWithFrequency.getFrequency())
                            * Math.log10(documentsCount / (double) list.size()));
                    list.get(i).setFrequency(score);
                }
            }
        }
        Log.i(TAG, INDEX_TIME_MAG + (System.currentTimeMillis() - startTime));
    }

    private static void saveDocumentsHashMapInFile() {
        File textFile = new File(DOCUMENTS_FILE_PATH);
        try (FileOutputStream fileOutputStream = new FileOutputStream(textFile);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
            objectOutputStream.writeObject(documentsHashMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveInvertedIndexHashMapInFile() {
        File file = new File(INVERTED_INDEX_FILE_PATH);
        try (FileOutputStream fileOutputStream = new FileOutputStream(file);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
            objectOutputStream.writeObject(invertedIndexHashMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Element> getEachFilesDocs(int i) throws
            JDOMException, IOException {
        SAXBuilder saxBuilder = new SAXBuilder();
        File file = new File(XML_FILES_PATH + DOCS_SUFFIX_FORMAT + formatNumber(i + 1) + DOCS_EXTENSION);
        Document document = saxBuilder.build(file);
        Element rootElement = document.getRootElement();
        return rootElement.getChildren(DOC_TAG_STR);
    }

    private static String formatNumber(int number) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 3 - String.valueOf(number).length(); i++) {
            stringBuilder.append(0);
        }
        return stringBuilder.append(number).toString();
    }

    private static String setDocTitleAndReturnHtmlWithoutTitle(Doc doc, String htmlText) {
        Matcher matcher = Pattern.compile(TITLE_REGEX).matcher(htmlText);
        if (matcher.find()) {
            String scope = matcher.group();
            doc.setTitle(StringEscapeUtils.unescapeHtml4(scope.substring(scope.indexOf(GREATER_THAN_SIGN) +
                    GREATER_THAN_SIGN.length(), scope.lastIndexOf(LESS_THAN_SIGN))));
            return htmlText.substring(scope.lastIndexOf(LESS_THAN_SIGN) + TITLE_END_TAG.length());
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