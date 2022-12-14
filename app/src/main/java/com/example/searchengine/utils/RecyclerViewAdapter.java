package com.example.searchengine.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.searchengine.Doc;
import com.example.searchengine.R;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import static com.example.searchengine.utils.Constants.DOC_TITLE_LENGTH_LIMIT;

@RequiresApi(api = Build.VERSION_CODES.N)
public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<Doc> docs;

    public RecyclerViewAdapter(Context context, ArrayList<Doc> docs) {
        this.context = context;
        this.docs = docs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.doc_item, parent, false);
        return new ViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Typeface tf = Typeface.createFromAsset(context.getAssets(),
                Constants.APP_FONT);

        int startIndex = 0;
        int endIndex = 0;
        String docTitle = docs.get(position).getTitle();
        String[] docTitleParts = docTitle.split(Constants.TITLE_SPLITTER);
        if (docTitleParts.length > 1) {
            startIndex = Integer.parseInt(docTitleParts[0]);
            endIndex = Integer.parseInt(docTitleParts[1]);
            docs.get(position).setTitle(docTitleParts[2]);
        }

        SpannableStringBuilder str = new SpannableStringBuilder(docs.get(position).getBody());
        str.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (docs.get(position).getTitle().length() > DOC_TITLE_LENGTH_LIMIT) {
            docs.get(position).setTitle(docs.get(position).getTitle().substring(0, DOC_TITLE_LENGTH_LIMIT) + Constants.ELLIPSIS_STR);
        }

        holder.title.setText(docs.get(position).getTitle());
        holder.url.setText(docs.get(position).getUrl());
        holder.body.setText(str);

        holder.title.setTypeface(tf, Typeface.BOLD);
        holder.url.setTypeface(tf, Typeface.BOLD);
    }

    @Override
    public int getItemCount() {
        return docs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView url;
        public TextView body;

        ViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            title = itemLayoutView.findViewById(R.id.docTitleTextView);
            url = itemLayoutView.findViewById(R.id.docUrlTextView);
            body = itemLayoutView.findViewById(R.id.docBodyTextView);
        }
    }
}
