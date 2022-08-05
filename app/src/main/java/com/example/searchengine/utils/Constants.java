package com.example.searchengine.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Constants {
    public static final String APP_FONT = "fonts/IRANSansRegular.ttf";
    public static final String MY_PREFS_FILE_NAME_STR = "myPrefsFile";
    public static final String NOT_FOUND_FLAG_STR = "NOT-FOUND";
    public static final String TRUE_FLAG_STR = "true";
    public static final String TITLE_SPLITTER = "ADSFDSAacdsdffdvfdvfdvfvdfvfdfvfdvvfdvdf";
    public static final String EMPTY_STR = "";
    public static final String SPACE_CHARACTER = " ";
    public static final String ELLIPSIS_STR = " ...";
    public static final String FILED_IS_EMPTY_MSG = "Filed is empty!";
    public static final String CORRECTED_QUERY_STR = "Corrected query: ";
    public static final String URL_TAG_STR = "URL";
    public static final String HTML_TAG_STR = "HTML";
    public static final String DOC_TAG_STR = "DOC";
    public static final String DOCS_EXTENSION = ".xml";
    public static final String DOCS_SUFFIX_FORMAT = "/" + "WebIR-" ;


    public static final String LOAD_HASH_MAPS_FROM_FILE_TIME_MSG = "loadHashMapsFromFile time (ms): ";
    public static final String FILL_DICTIONARY_HASH_MAP_TIME_MSG = "fillDictionaryHashMap time (ms): ";
    public static final String CORRECT_QUERY_SPELLING_TIME_MSG = "correctQuerySpelling time (ms): ";
    public static final String SEARCH_TIME_MSG = "search time (ms): ";
    public static final String SAVE_INVERTED_INDEX_HASH_MAP_IN_FILE_TIME_MSG = "saveInvertedIndexHashMapInFile time (ms): ";
    public static final String SAVE_DOCUMENTS_HASH_MAP_IN_FILE_TIME_MSG = "saveDocumentsHashMapInFile time (ms): ";
    public static final String BINDING_HASH_MAPS_FROM_FILE_TIME_MSG = "bindingHashMapsFromFile time: (ms)";
    public static final String INDEX_TIME_MAG = "index time: (ms)";
    public static final String DOCUMENT_COUNT_MSG = "Document Count: ";
    public static final String WORD_COUNT = "Word Count: ";
    public static final String LETTER_COUNT = "Letter Count: ";

    public static final String READY_MSG = "I'm ready!";
    public static final String DID_YOU_MEAN_MSG = "آیا منظور شما این بود؟   " + "\n";
    public static final String NO_RESULTS_MSG = "نتیجه ای یافت نشد!";
    public static final String RESULTS_AT_MSG = " نتیجه در ";
    public static final String SECONDS_MSG = " ثانیه";
    public static final String CLICK_AGAIN_TO_EXIT_MSG = "برای خروج دوباره کلیک کنید.";

    public static final String CAN_NOT_FIND_ANY_DOCUMENTS_ERR = "Can not find any documents.";
    public static final String MAPS_ARE_NULL_ERR = "Maps are null.";

    public static final String INVERTED_INDEX_FILE_NAME = "inverted_index.txt";
    public static final String DOCUMENTS_FILE_NAME = "documents.txt";
    public static final String DICTIONARY_FILE_NAME = "dictionary.txt";
    public static final String INVERTED_INDEX_FILE_PATH = "D:\\Projects\\Android\\Me\\1398\\SearchEngine\\app\\src\\main\\assets\\inverted_index.txt";
    public static final String DOCUMENTS_FILE_PATH = "D:\\Projects\\Android\\Me\\1398\\SearchEngine\\app\\src\\main\\assets\\documents.txt";
    public static final String XML_FILES_PATH = "D:\\Projects\\" +
            "Android\\Me\\1398\\SearchEngine\\app\\src\\main\\assets\\WEBIR_S";


    public static final float MILLISECOND_FRACTION = 1000f;
    public static final int DOC_SNIPPET_LENGTH = 100;
    public static final int DOC_TITLE_LENGTH_LIMIT = 40;
    public static final int ON_BACK_PRESSED_DELAY = 5000;
    public static final int SUBSTANDARD_DOCUMENT_ID = 485;
    public static final int TITLE_WEIGHT = 60;

    public static final int TEST_INPUT_1 = 1;
    public static final int TEST_INPUT_2 = 2;
    public static final int TEST_INPUT_3 = 10;
    public static final int TEST_INPUT_4 = 100;
    public static final int TEST_INPUT_5 = 10;

    public static final String TEST_ACTUAL_INPUT_1 = "001";
    public static final String TEST_ACTUAL_INPUT_2 = "002";
    public static final String TEST_ACTUAL_INPUT_3 = "010";
    public static final String TEST_ACTUAL_INPUT_4 = "100";
    public static final String TEST_ACTUAL_INPUT_5 = "10";

    public static final String TITLE_REGEX = "<title>.*</title>";
    public static final String TITLE_END_TAG = "</title>";
    public static final String GREATER_THAN_SIGN = ">";
    public static final String LESS_THAN_SIGN = "<";
    public static final String PERSIAN_REGEX = "(" + "[آ-ی]" + "|" + "[ي]" + "|" + "[ك]" + ")" + "+";

    public static final List<Character> PERSIAN_CHARACTERS = Arrays.asList(
            'آ', 'ا', 'ب', 'پ', 'ت', 'ث', 'ج', 'چ', 'ح', 'خ', 'د', 'ذ', 'ر', 'ز', 'ژ', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ',
            'ع', 'غ', 'ف', 'ق', 'ک', 'گ', 'ل', 'م', 'ن', 'و', 'ه', 'ی', 'ك', 'ي'
    );

    public static final Map<Character, Character> SIMILAR_PERSIAN_CHARACTERS = new HashMap<Character, Character>() {{
        put('ک', 'گ');
        put('د', 'ذ');
        put('ر', 'ز');
        put('ذ', 'ز');
        put('ب', 'پ');
        put('ح', 'خ');
        put('ی', 'ي');
        put('ك', 'ک');
    }};
}
