package com.example.myapplication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.database.JournalEntry;

import java.util.List;

public class JournalEntryAdapter extends RecyclerView.Adapter<JournalEntryAdapter.ViewHolder> {
    private List<JournalEntry> journalEntries;
    private Context context;

    private OnButtonClickListener buttonClickListener;

    public interface OnButtonClickListener {
        void onButtonClicked(JournalEntry entry);
    }

    public void setOnButtonClickListener(OnButtonClickListener listener) {
        this.buttonClickListener = listener;
    }

    public JournalEntryAdapter(List<JournalEntry> journalEntries, Context context) {
        this.journalEntries = journalEntries;
        this.context = context;
    }

    public void setJournalEntries(List<JournalEntry> journalEntries) {
        this.journalEntries = journalEntries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_journal_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (journalEntries != null && !journalEntries.isEmpty()) {
            JournalEntry journalEntry = journalEntries.get(position);

            holder.titleTextView.setText(getTitleText(journalEntry.title));
            holder.dateTextView.setText(journalEntry.date);
            holder.openButton.setOnClickListener(v -> openJournalEntry(journalEntry));
        } else {
            // Handle the case when journalEntries is empty
            holder.titleTextView.setText(R.string.no_entries_found);
            holder.dateTextView.setText("");
            holder.openButton.setOnClickListener(null);
        }
    }

    private String getTitleText(String title) {
        return title.length() > 35 ? title.substring(0, 35) + "..." : title;
    }

    @Override
    public int getItemCount() {
        return journalEntries.size();
    }

    private void openJournalEntry(JournalEntry journalEntry) {
        // Handle opening the journal entry details or fragment
        // You can customize this part based on your requirements
//        Toast.makeText(context, "Opening journal entry: " + journalEntry.title, Toast.LENGTH_SHORT).show();

        if (buttonClickListener != null) {
            buttonClickListener.onButtonClicked(journalEntry);
        }

    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView dateTextView;
        Button openButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            openButton = itemView.findViewById(R.id.openButton);
        }
    }
}
