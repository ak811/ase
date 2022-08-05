package com.example.searchengine;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.searchengine.ui.IranYekanTextView;
import com.example.searchengine.utils.Constants;
import com.example.searchengine.utils.RecyclerViewAdapter;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.example.searchengine.utils.Constants.DOC_SNIPPET_LENGTH;
import static com.example.searchengine.utils.Constants.ELLIPSIS_STR;
import static com.example.searchengine.utils.Constants.EMPTY_STR;
import static com.example.searchengine.utils.Constants.MILLISECOND_FRACTION;
import static com.example.searchengine.utils.Constants.PERSIAN_CHARACTERS;
import static com.example.searchengine.utils.Constants.SECONDS_MSG;
import static com.example.searchengine.utils.Constants.SPACE_CHARACTER;

@RequiresApi(api = Build.VERSION_CODES.N)
@SuppressLint("UseSparseArrays")
public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();

    private EditText searchContentEditText;
    private LinearLayout searchButtonLinearLayout;
    private ImageView largeGoogleImageView;
    private ImageView searchContentImageView;
    private ProgressBar progressBar;
    private ImageView goBackImageView;
    private ImageView closeImageView;
    private ImageView homeButtonImageView;
    private IranYekanTextView infoTextView;
    private IranYekanTextView resultTextView;
    private View divider;

    private boolean onBackPressedFlag;
    private boolean vpnDialogFlag;

    private RecyclerView recyclerView;

    private HashMap<String, List<DocIdWithFrequency>> invertedindexHashMap = new HashMap<>();
    private HashMap<Integer, Doc> documentsHashMap = new HashMap<>();
    private final HashMap<String, HashSet<String>> dictionaryHashMap = new HashMap<>();

    private final ArrayList<Doc> resultDocs = new ArrayList<>();

    private String newCorrectedQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppThemeWithoutBackground);
        setContentView(R.layout.activity_main);

        bindViews();
        initListeners();

        long startTime = System.currentTimeMillis();
        invertedindexHashMap = extractMapFromInvertedIndexFile();
        documentsHashMap = extractMapFromDocumentsFile();
        Toast.makeText(this, Constants.LOAD_HASH_MAPS_FROM_FILE_TIME_MSG + (System.currentTimeMillis() - startTime), Toast.LENGTH_LONG).show();

        startTime = System.currentTimeMillis();
        try {
            fillDictionaryHashMap(invertedindexHashMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, Constants.FILL_DICTIONARY_HASH_MAP_TIME_MSG + (System.currentTimeMillis() - startTime), Toast.LENGTH_LONG).show();
        Toast.makeText(this, Constants.READY_MSG, Toast.LENGTH_LONG).show();
    }

    private void bindViews() {
        searchContentEditText = findViewById(R.id.contentSearchEditText);
        searchButtonLinearLayout = findViewById(R.id.searchButtonLinearLayout);
        largeGoogleImageView = findViewById(R.id.googleLargeImageView);
        searchContentImageView = findViewById(R.id.searchImageView);
        progressBar = findViewById(R.id.progressBar);
        goBackImageView = findViewById(R.id.goBackImageView);
        closeImageView = findViewById(R.id.closeImageView);
        homeButtonImageView = findViewById(R.id.homeImageView);
        infoTextView = findViewById(R.id.infoTextView);
        recyclerView = findViewById(R.id.recyclerView);
        resultTextView = findViewById(R.id.resultTextView);
        divider = findViewById(R.id.divider2);
    }

    private void initListeners() {
        searchContentEditText.requestFocus();
        searchContentEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String searchContent = searchContentEditText.getText().toString().trim();
                performSearch(searchContent);
                return true;
            }
            return false;
        });

        searchButtonLinearLayout.setOnClickListener(view -> {
            String searchContent = searchContentEditText.getText().toString().trim();
            performSearch(searchContent);
        });

        goBackImageView.setOnClickListener(view -> onBackPressed());
        closeImageView.setOnClickListener(view -> searchContentEditText.setText(Constants.EMPTY_STR));

        searchContentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                closeImageView.setVisibility(View.VISIBLE);

                if (searchContentEditText.getText().toString().trim().isEmpty()) {
                    closeImageView.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        homeButtonImageView.setOnClickListener(v -> {
            hideWebViewAndShowOthers();
            progressBar.setProgress(0);
            progressBar.setVisibility(View.INVISIBLE);
            openKeyboard();
        });
    }

    private void performSearch(String userInput) {
        if (!userInput.isEmpty()) {
            EditText editText = findViewById(R.id.contentSearchEditText);
            String input = editText.getText().toString().trim();
            search(input);
            progressBar.setVisibility(View.VISIBLE);
            hideKeyboard();
            showWebViewAndHideOthers();
        } else {
            Toast.makeText(this, Constants.FILED_IS_EMPTY_MSG, Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("SetTextI18n")
    private void search(String query) {

        newCorrectedQuery = null;

        long startTime = System.currentTimeMillis();
        query = correctQuerySpelling(query);
        Toast.makeText(this, Constants.CORRECT_QUERY_SPELLING_TIME_MSG + (System.currentTimeMillis() - startTime), Toast.LENGTH_LONG).show();

        if (newCorrectedQuery != null) {
            infoTextView.setVisibility(View.VISIBLE);
            infoTextView.setText(Constants.DID_YOU_MEAN_MSG + query);
        }

        String[] subQueries = query.split(SPACE_CHARACTER);
        if (invertedindexHashMap != null && documentsHashMap != null &&
                subQueries.length != 0 && !subQueries[0].trim().equals(EMPTY_STR)) {
            List<DocIdWithFrequency> commonDocIdsWithFrequency = invertedindexHashMap.get(subQueries[0]);
            if (commonDocIdsWithFrequency != null) {
                for (int i = 1; i < subQueries.length; i++) {
                    List<DocIdWithFrequency> list2 = invertedindexHashMap.get(subQueries[i]);
                    commonDocIdsWithFrequency = extractCommonList(commonDocIdsWithFrequency, list2);
                }

                if (commonDocIdsWithFrequency.size() == 0) {
                    infoTextView.setVisibility(View.VISIBLE);
                    infoTextView.setText(Constants.NO_RESULTS_MSG);
                    resultTextView.setVisibility(View.INVISIBLE);
                } else {
                    commonDocIdsWithFrequency.sort((t1, t2) -> Double.compare(t2.getFrequency(), t1.getFrequency()));
                    for (int i = 0; i < commonDocIdsWithFrequency.size(); i++) {
                        Doc doc = documentsHashMap.get(commonDocIdsWithFrequency.get(i).getDocId());
                        if (doc != null) {
                            String body = doc.getBody();
                            if (body.contains(query)) {
                                int startIndex = Math.max(body.indexOf(query) - DOC_SNIPPET_LENGTH, 0);
                                int endIndex = Math.min(body.indexOf(query) + DOC_SNIPPET_LENGTH, body.length());
                                String snippet = body.substring(startIndex, endIndex);
                                doc.setTitle(snippet.indexOf(query) + Constants.TITLE_SPLITTER
                                        + (snippet.indexOf(query) + query.length()) + Constants.TITLE_SPLITTER + doc.getTitle());
                                doc.setBody(snippet + ELLIPSIS_STR);
                            } else {
                                for (int j = 0; j < 1 /*subQueries.length*/; j++) {
                                    if (body.contains(subQueries[j])) {
                                        int startIndex = Math.max(body.indexOf(subQueries[j]) - DOC_SNIPPET_LENGTH, 0);
                                        int endIndex = body.indexOf(subQueries[0]) + DOC_SNIPPET_LENGTH < body.length() ?
                                                body.indexOf(subQueries[j]) + DOC_SNIPPET_LENGTH : body.length();
                                        String snippet = body.substring(startIndex, endIndex);
                                        doc.setTitle(snippet.indexOf(query) + Constants.TITLE_SPLITTER
                                                + (snippet.indexOf(query) + query.length()) + Constants.TITLE_SPLITTER + doc.getTitle());
                                        doc.setBody(snippet + ELLIPSIS_STR);
                                    } else {
                                        if (body.length() > DOC_SNIPPET_LENGTH)
                                            doc.setBody(body.substring(0, DOC_SNIPPET_LENGTH));
                                    }
                                }
                            }
                            resultDocs.add(doc);
                        } else {
                            Log.e(TAG, Constants.CAN_NOT_FIND_ANY_DOCUMENTS_ERR);
                        }
                    }

                    long searchTime = (System.currentTimeMillis() - startTime);
                    Toast.makeText(this, Constants.SEARCH_TIME_MSG + searchTime, Toast.LENGTH_LONG).show();

                    String resultText = commonDocIdsWithFrequency.size() + Constants.RESULTS_AT_MSG + searchTime / MILLISECOND_FRACTION + SECONDS_MSG;
                    resultTextView.setText(resultText);

                    //bind recyclerView
                    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
                    recyclerView.setLayoutManager(linearLayoutManager);

                    RecyclerViewAdapter adapter = new RecyclerViewAdapter(this, resultDocs);
                    recyclerView.setAdapter(adapter);
                    recyclerView.setHasFixedSize(true);
                    recyclerView.setDrawingCacheEnabled(true);
                    recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
                    adapter.notifyDataSetChanged();
                }
            }
        } else {
            Log.e(TAG, Constants.MAPS_ARE_NULL_ERR);
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null)
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void openKeyboard() {
        searchContentEditText.requestFocus();

        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.toggleSoftInputFromWindow(
                    searchContentEditText.getApplicationWindowToken(),
                    InputMethodManager.SHOW_FORCED, 0);
        }
    }

    private void showWebViewAndHideOthers() {
        recyclerView.setVisibility(View.VISIBLE);
        resultTextView.setVisibility(View.VISIBLE);
        divider.setVisibility(View.VISIBLE);
        largeGoogleImageView.setVisibility(View.INVISIBLE);
        searchContentImageView.setVisibility(View.INVISIBLE);
        searchContentEditText.setVisibility(View.INVISIBLE);
        searchButtonLinearLayout.setVisibility(View.INVISIBLE);
        closeImageView.setVisibility(View.INVISIBLE);
    }

    private void hideWebViewAndShowOthers() {
        largeGoogleImageView.setVisibility(View.VISIBLE);
        searchContentImageView.setVisibility(View.VISIBLE);
        searchContentEditText.setVisibility(View.VISIBLE);
        searchButtonLinearLayout.setVisibility(View.VISIBLE);
        if (!searchContentEditText.getText().toString().trim().equals(EMPTY_STR)) {
            closeImageView.setVisibility(View.VISIBLE);
        }
        recyclerView.setVisibility(View.INVISIBLE);
        resultTextView.setVisibility(View.INVISIBLE);
        divider.setVisibility(View.INVISIBLE);
        infoTextView.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (vpnDialogFlag) {
            vpnDialogFlag = false;
        } else {
            if (recyclerView.getVisibility() == View.INVISIBLE) {
                if (!onBackPressedFlag) {
                    onBackPressedFlag = true;
                    Toast.makeText(this, Constants.CLICK_AGAIN_TO_EXIT_MSG, Toast.LENGTH_LONG).show();
                } else {
                    finish();
                }
                new Handler().postDelayed(() -> onBackPressedFlag = false, Constants.ON_BACK_PRESSED_DELAY);
            }
            hideWebViewAndShowOthers();
            progressBar.setProgress(0);
            progressBar.setVisibility(View.INVISIBLE);
            searchContentEditText.requestFocus();
            resultDocs.clear();
            infoTextView.setText(EMPTY_STR);
        }
    }

    private List<DocIdWithFrequency> extractCommonList(
            List<DocIdWithFrequency> list, List<DocIdWithFrequency> list2) {
        List<DocIdWithFrequency> commonList = new ArrayList<>();
        if (list != null && list2 != null) {
            for (int i = 0, j = 0; i < list.size() && j < list2.size(); ) {
                if (list.get(i).getDocId() < list2.get(j).getDocId()) {
                    i++;
                } else if (list.get(i).getDocId() > list2.get(j).getDocId()) {
                    j++;
                } else {
                    commonList.add(list.get(i));
                    i++;
                    j++;
                }
            }
        }
        return commonList;
    }

    private String correctQuerySpelling(String query) {
        boolean isAnySubQueryCorrected = false;
        StringBuilder correctedQuery = new StringBuilder();
        String[] subQueries = query.split(SPACE_CHARACTER);
        for (String subQuery : subQueries) {
            if (invertedindexHashMap.get(subQuery) == null) {
                String newStr = replaceSimilarLetters(subQuery);
                Log.i(TAG, Constants.CORRECTED_QUERY_STR + newStr);
                newCorrectedQuery = newStr;
                if (newStr == null) {
                    HashMap<String, Double> probabilityHashMap = new HashMap<>();
                    for (int j = 0; j + 1 < subQuery.length(); j++) {
                        String subStr = subQuery.substring(j, j + 2);
                        HashSet<String> hashSet = dictionaryHashMap.get(subStr);
                        if (hashSet != null) {
                            for (String hashSetStr : hashSet) {
                                calculateProbability(probabilityHashMap, hashSetStr, query);
                            }
                        }
                    }

                    Optional<Map.Entry<String, Double>> max =
                            probabilityHashMap.entrySet().stream()
                                    .max((t1, t2) -> t1.getValue().compareTo(t2.getValue()));

                    probabilityHashMap.entrySet().stream()
                            .sorted((t1, t2) -> t2.getValue().compareTo(t1.getValue()))
                            .forEach(System.out::println);

                    if (max.isPresent()) {
                        Map.Entry<String, Double> maxEntry = max.get();
                        correctedQuery.append(maxEntry.getKey()).append(SPACE_CHARACTER);
                    }
                } else {
                    correctedQuery.append(newStr).append(SPACE_CHARACTER);
                }
                isAnySubQueryCorrected = true;
            } else {
                correctedQuery.append(subQuery).append(SPACE_CHARACTER);
            }
        }
        if (isAnySubQueryCorrected)
            newCorrectedQuery = correctedQuery.toString();
        return correctedQuery.toString();
    }

    private String replaceSimilarLetters(String hashSetStr) {
        for (HashMap.Entry<Character, Character> hashEntry : Constants.SIMILAR_PERSIAN_CHARACTERS.entrySet()) {
            if (invertedindexHashMap.get(hashSetStr.replace(hashEntry.getKey(),
                    hashEntry.getValue())) != null ||
                    invertedindexHashMap.get(hashSetStr.replace(hashEntry.getValue(),
                            hashEntry.getKey())) != null) {

                if (invertedindexHashMap.get(hashSetStr.replace(hashEntry.getKey(),
                        hashEntry.getValue())) != null) {
                    hashSetStr = hashSetStr.replace(hashEntry.getKey(), hashEntry.getValue());
                } else {
                    hashSetStr = hashSetStr.replace(hashEntry.getValue(), hashEntry.getKey());
                }
                return hashSetStr;
            }
        }
        return null;
    }

    private void calculateProbability(HashMap<String, Double> probabilityHashMap, String word, String query) {
        String[] wordBbiGramArray = new String[word.length() - 1];
        for (int i = 0; i + 1 < word.length(); i++) {
            String str = word.substring(i, i + 2);
            wordBbiGramArray[i] = str;
        }

        String[] queryBiGramArray = new String[query.length() - 1];
        for (int i = 0; i + 1 < query.length(); i++) {
            String str = query.substring(i, i + 2);
            queryBiGramArray[i] = str;
        }

        wordBbiGramArray = deleteDuplicates(wordBbiGramArray);
        queryBiGramArray = deleteDuplicates(queryBiGramArray);

        // >>> The smaller array must be iterated first.
        if (wordBbiGramArray.length > queryBiGramArray.length) {
            String[] temp = wordBbiGramArray;
            wordBbiGramArray = queryBiGramArray;
            queryBiGramArray = temp;
        }
        int commonCount = 0;
        for (String s : wordBbiGramArray) {
            for (String value : queryBiGramArray) {
                if (s.equals(value)) {
                    commonCount++;
                    break;
                }
            }
        }

        double prob = commonCount / (double) (wordBbiGramArray.length + queryBiGramArray.length - commonCount);
        probabilityHashMap.put(word, prob);
    }

    private String[] deleteDuplicates(String[] arr) {
        ArrayList<String> strings = new ArrayList<>();
        for (String s : arr)
            if (!isElementExistsInArray(strings, s)) strings.add(s);
        return strings.toArray(new String[]{});
    }

    private boolean isElementExistsInArray(ArrayList<String> strings, String element) {
        for (String s : strings)
            if (element.equals(s)) return true;
        return false;
    }

    private void fillDictionaryHashMap(HashMap<String, List<DocIdWithFrequency>> invertedindexHashMap)
            throws IOException {
        for (int i = 0; i < PERSIAN_CHARACTERS.size(); i++)
            for (int j = 0; j < PERSIAN_CHARACTERS.size(); j++)
                dictionaryHashMap.put(
                        String.valueOf(PERSIAN_CHARACTERS.get(i)) +
                                PERSIAN_CHARACTERS.get(j), new HashSet<>());

        // >>> Fill dictionary from inverted_index.txt*
        for (Map.Entry<String, List<DocIdWithFrequency>> mapEntry : invertedindexHashMap.entrySet()) {
            String key = mapEntry.getKey();
            for (int i = 0; i < key.length(); i++) {
                for (int j = i + 1; j < key.length(); j++) {
                    String dicKey = String.valueOf(key.charAt(i)) + key.charAt(j);
                    HashSet<String> values = dictionaryHashMap.get(dicKey);
                    if (values != null)
                        values.add(key);
                }
            }
        }

        // >>> Fill dictionary from dictionary.txt
        InputStream inputStream = getAssets().open(Constants.DICTIONARY_FILE_NAME);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            while (reader.ready()) {
                String word = reader.readLine();
                for (int i = 0; i < word.length(); i++) {
                    for (int j = i + 1; j < word.length(); j++) {
                        String dicKey = String.valueOf(word.charAt(i)) + word.charAt(j);
                        HashSet<String> values = dictionaryHashMap.get(dicKey);
                        if (values != null)
                            values.add(word);
                    }
                }
            }
        }
    }

    private HashMap<String, List<DocIdWithFrequency>> extractMapFromInvertedIndexFile() {
        try (InputStream inputStream = getAssets().open(Constants.INVERTED_INDEX_FILE_NAME);
             ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            return (HashMap<String, List<DocIdWithFrequency>>) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private HashMap<Integer, Doc> extractMapFromDocumentsFile() {
        try (InputStream inputStream = getAssets().open(Constants.DOCUMENTS_FILE_NAME);
             ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            return (HashMap<Integer, Doc>) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String formatNumber(int number) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 3 - String.valueOf(number).length(); i++) {
            stringBuilder.append(0);
        }
        return stringBuilder.append(number).toString();
    }

    @Test
    public void testFormatNumberFunction() {
        Assert.assertEquals(formatNumber(Constants.TEST_INPUT_1), Constants.TEST_ACTUAL_INPUT_1);
        Assert.assertEquals(formatNumber(Constants.TEST_INPUT_2), Constants.TEST_ACTUAL_INPUT_2);

        Assert.assertEquals(formatNumber(Constants.TEST_INPUT_3), Constants.TEST_ACTUAL_INPUT_3);

        Assert.assertEquals(formatNumber(Constants.TEST_INPUT_4), Constants.TEST_ACTUAL_INPUT_4);

        Assert.assertNotEquals(formatNumber(Constants.TEST_INPUT_5), Constants.TEST_ACTUAL_INPUT_5);
    }
}
