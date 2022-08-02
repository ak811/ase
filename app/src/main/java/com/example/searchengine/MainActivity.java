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

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiresApi(api = Build.VERSION_CODES.N)
@SuppressLint("UseSparseArrays")
public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();

    private EditText searchContentEditText;
    private LinearLayout searchButtonLinearLayout;
    private ImageView largeGoogleImageView;
    private ImageView searchContentImageView;
    private ProgressBar progressBar;
    private ImageView goBackImageView, goForwardImageView;
    private ImageView closeImageView;
    private View view;
    private ImageView homeButtonImageView;
    private IranYekanTextView infoTextView;
    private IranYekanTextView resultTextView;
    private View divider;

    private boolean onBackPressedFlag;
    private boolean vpnDialogFlag;

    private RecyclerView recyclerView;

    private HashMap<String, List<DocIdWithFrequency>> invertedIndexingHashMap = new HashMap<>();
    private HashMap<Integer, Doc> documentsHashMap = new HashMap<>();
    private HashMap<String, HashSet<String>> dictionaryHashMap = new HashMap<>();

    private ArrayList<Doc> resultDocs = new ArrayList<>();

    private String newCorrectedQuery;

    private List<Character> persianCharacters = Arrays.asList(
            'آ', 'ا', 'ب', 'پ', 'ت', 'ث', 'ج', 'چ', 'ح', 'خ', 'د', 'ذ', 'ر', 'ز', 'ژ', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ',
            'ع', 'غ', 'ف', 'ق', 'ک', 'گ', 'ل', 'م', 'ن', 'و', 'ه', 'ی', 'ك', 'ي'
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppThemeWithoutBackground);
        setContentView(R.layout.activity_main);

        bindViews();
        initListeners();

        long startTime = System.currentTimeMillis();
        invertedIndexingHashMap = extractMapFromInvertedIndexingFile();
        documentsHashMap = extractMapFromDocumentsFile();
        Toast.makeText(this, "loadHashMapsFromFile time: " + (System.currentTimeMillis() - startTime) + "ms", Toast.LENGTH_LONG).show();

        startTime = System.currentTimeMillis();
        try {
            fillDictionaryHashMap(invertedIndexingHashMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "fillDictionaryHashMap time: " + (System.currentTimeMillis() - startTime) + "ms", Toast.LENGTH_LONG).show();
        Toast.makeText(this, "I'm ready!", Toast.LENGTH_LONG).show();
    }

    private void bindViews() {
        searchContentEditText = findViewById(R.id.contentSearchEditText);
        searchButtonLinearLayout = findViewById(R.id.searchButtonLinearLayout);
        largeGoogleImageView = findViewById(R.id.googleLargeImageView);
        searchContentImageView = findViewById(R.id.searchImageView);
        progressBar = findViewById(R.id.progressBar);
        goBackImageView = findViewById(R.id.goBackImageView);
        goForwardImageView = findViewById(R.id.goForwardImageView);
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
        closeImageView.setOnClickListener(view -> searchContentEditText.setText(""));

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
            Toast.makeText(this, "فیلد خالی است.", Toast.LENGTH_LONG).show();
        }
    }

    private void search(String query) {

        newCorrectedQuery = null;

        System.out.println("query: " + query);

        long startTime = System.currentTimeMillis();
        query = correctQuerySpelling(query);
        Toast.makeText(this, "correctQuerySpelling() time: " + (System.currentTimeMillis() - startTime) + "ms", Toast.LENGTH_LONG).show();

        System.out.println("newCorrectedQuery: " + newCorrectedQuery);
        if (newCorrectedQuery != null) {
            infoTextView.setVisibility(View.VISIBLE);
            infoTextView.setText("آیا منظور شما این بود؟   " + "\n" + query);
        }

        String[] subQueries = query.split(" ");
        if (invertedIndexingHashMap != null && documentsHashMap != null &&
                subQueries.length != 0 && !subQueries[0].trim().equals("")) {
            List<DocIdWithFrequency> commonDocIdsWithFrequency = invertedIndexingHashMap.get(subQueries[0]);
            if (commonDocIdsWithFrequency != null) {
                for (int i = 1; i < subQueries.length; i++) {
                    List<DocIdWithFrequency> list2 = invertedIndexingHashMap.get(subQueries[i]);
                    commonDocIdsWithFrequency = extractCommonList(commonDocIdsWithFrequency, list2);
                }

                System.out.println("commonDocIds.size(): " + commonDocIdsWithFrequency.size());
                if (commonDocIdsWithFrequency.size() == 0) {
                    infoTextView.setVisibility(View.VISIBLE);
                    infoTextView.setText("نتیجه ای یافت نشد!");
                    Log.e(TAG, "HEllllllllllllllllllllllllllo");
                    resultTextView.setVisibility(View.INVISIBLE);
                } else {
                    commonDocIdsWithFrequency.sort((t1, t2) -> Double.compare(t2.getFrequency(), t1.getFrequency()));
                    for (int i = 0; i < commonDocIdsWithFrequency.size(); i++) {
                        Doc doc = documentsHashMap.get(commonDocIdsWithFrequency.get(i).getDocId());
                        if (doc != null) {
                            String body = doc.getBody();
                            if (body.contains(query)) {
                                int startIndex = body.indexOf(query) - 100 > 0 ?
                                        body.indexOf(query) - 100 : 0;
                                int endIndex = body.indexOf(query) + 100 < body.length() ?
                                        body.indexOf(query) + 100 : body.length();
                                String snippet = body.substring(startIndex, endIndex);
                                doc.setTitle(snippet.indexOf(query) + Constants.TITLE_SPLITTER
                                        + (snippet.indexOf(query) + query.length()) + Constants.TITLE_SPLITTER + doc.getTitle());
                                doc.setBody(snippet + " ...");
                            } else {
                                for (int j = 0; j < /*subQueries.length*/ 1; j++) {
                                    if (body.contains(subQueries[j])) {
                                        int startIndex = body.indexOf(subQueries[j]) - 100 > 0 ?
                                                body.indexOf(subQueries[j]) - 100 : 0;
                                        int endIndex = body.indexOf(subQueries[0]) + 100 < body.length() ?
                                                body.indexOf(subQueries[j]) + 100 : body.length();
                                        String snippet = body.substring(startIndex, endIndex);
                                        doc.setTitle(snippet.indexOf(query) + Constants.TITLE_SPLITTER
                                                + (snippet.indexOf(query) + query.length()) + Constants.TITLE_SPLITTER + doc.getTitle());
                                        doc.setBody(snippet + "...");
                                    } else {
                                        if (body.length() > 100)
                                            doc.setBody(body.substring(0, 100));
                                    }
                                }
                            }
                            resultDocs.add(doc);
                        } else {
                            System.out.println("cant find any document");
                        }
                    }

                    long searchTime = (System.currentTimeMillis() - startTime);
                    Toast.makeText(this, "search() time: " + searchTime + "ms", Toast.LENGTH_LONG).show();

                    String resultText = commonDocIdsWithFrequency.size() + " نتیجه در " + searchTime / 1000f + " ثانیه";
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
            System.out.println("maps are null");
        }
    }

    private void hideKeyboard() {
        view = this.getCurrentFocus();
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
        if (!searchContentEditText.getText().toString().trim().equals("")) {
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
        } /*else if (recyclerView.canGoBack()) {

        } */ else {
            if (recyclerView.getVisibility() == View.INVISIBLE) {
                if (!onBackPressedFlag) {
                    onBackPressedFlag = true;
                    Toast.makeText(this, "برای خروج دوباره کلیک کنید.", Toast.LENGTH_LONG).show();
                } else {
                    finish();
                }
                new Handler().postDelayed(() -> onBackPressedFlag = false, 5000);
            }
            hideWebViewAndShowOthers();
            progressBar.setProgress(0);
            progressBar.setVisibility(View.INVISIBLE);
            searchContentEditText.requestFocus();
            resultDocs.clear();
            infoTextView.setText("");
        }
    }

    /******************************************************************************************/

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
        String[] subQueries = query.split(" ");
        for (String subQuery : subQueries) {
            if (invertedIndexingHashMap.get(subQuery) == null) {
                String newStr = replaceSimilarLetters(subQuery);
                System.out.println("corrected query: " + newStr);
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


                    System.out.println("//////////////////////////////////////////////////////////////////////");

                    if (max.isPresent()) {
                        Map.Entry<String, Double> maxEntry = max.get();
                        correctedQuery.append(maxEntry.getKey()).append(" ");
                    }
                } else {
                    correctedQuery.append(newStr).append(" ");
                }
                isAnySubQueryCorrected = true;
            } else {
                correctedQuery.append(subQuery).append(" ");
            }
        }
        if (isAnySubQueryCorrected)
            newCorrectedQuery = correctedQuery.toString();
        return correctedQuery.toString();
    }

    private String replaceSimilarLetters(String hashSetStr) {
        HashMap<Character, Character> hashMap = new HashMap<>();
        hashMap.put('ک', 'گ');
        hashMap.put('د', 'ذ');
        hashMap.put('ر', 'ز');
        hashMap.put('ذ', 'ز');
        hashMap.put('ب', 'پ');
        hashMap.put('ح', 'خ');
        hashMap.put('ی', 'ي');
        hashMap.put('ك', 'ک');
        for (HashMap.Entry<Character, Character> hashEntry : hashMap.entrySet()) {
            if (invertedIndexingHashMap.get(hashSetStr.replace(hashEntry.getKey(),
                    hashEntry.getValue())) != null ||
                    invertedIndexingHashMap.get(hashSetStr.replace(hashEntry.getValue(),
                            hashEntry.getKey())) != null) {

                if (invertedIndexingHashMap.get(hashSetStr.replace(hashEntry.getKey(),
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

        // the smaller array must iterate first
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
//        System.out.println("word: " + word + " // " + "prob: " + prob);
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

    private void fillDictionaryHashMap(HashMap<String, List<DocIdWithFrequency>> invertedIndexingHashMap)
            throws IOException {
        for (int i = 0; i < persianCharacters.size(); i++) {
            for (int j = 0; j < persianCharacters.size(); j++) {
                dictionaryHashMap.put(
                        String.valueOf(persianCharacters.get(i)) +
                                persianCharacters.get(j), new HashSet<>());
            }
        }

        // fill dic from inverted indexing
        for (Map.Entry<String, List<DocIdWithFrequency>> mapEntry : invertedIndexingHashMap.entrySet()) {
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

        // fill dic from dictionary.txt
        String DICTIONARY_FILE_PATH = "dictionary.txt";
        InputStream inputStream = getAssets().open(DICTIONARY_FILE_PATH);
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

    private HashMap<String, List<DocIdWithFrequency>> extractMapFromInvertedIndexingFile() {
        System.out.println("start extractMapFromInvertedIndexingFile");
        try (InputStream inputStream = getAssets().open("inverted_indexing.txt");
             ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            HashMap<String, List<DocIdWithFrequency>> hashMap =
                    (HashMap<String, List<DocIdWithFrequency>>) objectInputStream.readObject();
            System.out.println("end extractMapFromInvertedIndexingFile");
            return hashMap;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private HashMap<Integer, Doc> extractMapFromDocumentsFile() {
        try (InputStream inputStream = getAssets().open("documents.txt");
             ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            HashMap<Integer, Doc> hashMap = (HashMap<Integer, Doc>) objectInputStream.readObject();
            System.out.println("end extractMapFromDocumentsFile");
            return hashMap;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String formatNumber(int number) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 3 - String.valueOf(number).length(); i++) {
            stringBuilder.append("0");
        }
        return stringBuilder.append(number).toString();
    }

    @Test
    public void testFormatNumberFunction() {
        Assert.assertEquals(formatNumber(1), "001");
        Assert.assertEquals(formatNumber(2), "002");

        Assert.assertEquals(formatNumber(10), "010");

        Assert.assertEquals(formatNumber(100), "100");

        Assert.assertNotEquals(formatNumber(10), "10");
    }
}
