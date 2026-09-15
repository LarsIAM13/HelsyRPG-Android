package com.bunbunka.programmernotebook;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(16, 19, 26);
    private static final int PANEL = Color.rgb(27, 32, 43);
    private static final int PANEL_ALT = Color.rgb(35, 41, 55);
    private static final int TEXT = Color.rgb(240, 243, 250);
    private static final int MUTED = Color.rgb(155, 164, 184);
    private static final int ACCENT = Color.rgb(124, 140, 255);
    private static final int DANGER = Color.rgb(244, 91, 105);

    private NotesRepository repository;
    private boolean editorMode;
    private long editingId = -1;
    private EditText titleEditor;
    private EditText contentEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(13, 15, 20));
        window.setNavigationBarColor(Color.rgb(13, 15, 20));
        repository = new NotesRepository(this);
        showList();
    }

    private void showList() {
        editorMode = false;
        editingId = -1;

        LinearLayout root = column();
        root.setPadding(dp(16), dp(18), dp(16), dp(16));

        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView heading = text("</>  Блокнот программиста", 23, TEXT, true);
        header.addView(heading, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button add = button("＋", ACCENT);
        add.setContentDescription("Создать заметку");
        header.addView(add, new LinearLayout.LayoutParams(dp(52), dp(48)));
        root.addView(header);

        TextView subtitle = text("Код, команды, идеи и решения — всегда под рукой", 13, MUTED, false);
        LinearLayout.LayoutParams subtitleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleLp.topMargin = dp(4);
        subtitleLp.bottomMargin = dp(14);
        root.addView(subtitle, subtitleLp);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Поиск по заметкам и коду…");
        search.setHintTextColor(MUTED);
        search.setTextColor(TEXT);
        search.setTextSize(15);
        search.setPadding(dp(14), 0, dp(14), 0);
        search.setBackground(roundRect(PANEL, 14));
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        searchLp.bottomMargin = dp(12);
        root.addView(search, searchLp);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout notesContainer = column();
        notesContainer.setPadding(0, dp(2), 0, dp(24));
        scroll.addView(notesContainer, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        renderNotes(notesContainer, "");
        search.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                renderNotes(notesContainer, s.toString());
            }
        });
        add.setOnClickListener(v -> openEditor(-1));
        setContentView(root);
    }

    private void renderNotes(LinearLayout container, String query) {
        container.removeAllViews();
        List<Note> notes = repository.list(query);
        if (notes.isEmpty()) {
            LinearLayout empty = column();
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(64), dp(20), dp(40));
            TextView icon = text("⌨", 44, ACCENT, false);
            icon.setGravity(Gravity.CENTER);
            empty.addView(icon);
            TextView title = text(query.trim().isEmpty() ? "Пока нет заметок" : "Ничего не найдено", 19, TEXT, true);
            title.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            titleLp.topMargin = dp(12);
            empty.addView(title, titleLp);
            TextView info = text(query.trim().isEmpty()
                    ? "Нажми ＋ и сохрани первую идею, сниппет или команду."
                    : "Попробуй другой запрос.", 14, MUTED, false);
            info.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            infoLp.topMargin = dp(8);
            empty.addView(info, infoLp);
            container.addView(empty);
            return;
        }

        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (Note note : notes) {
            LinearLayout card = column();
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            card.setBackground(roundRect(PANEL, 14));
            card.setClickable(true);
            card.setFocusable(true);

            TextView title = text(NoteLogic.displayTitle(note.getTitle()), 17, TEXT, true);
            card.addView(title);

            String previewText = NoteLogic.preview(note.getContent(), 110);
            if (!previewText.isEmpty()) {
                TextView preview = text(previewText, 14, MUTED, false);
                preview.setTypeface(Typeface.MONOSPACE);
                LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                previewLp.topMargin = dp(7);
                card.addView(preview, previewLp);
            }

            TextView time = text("Изменено: " + df.format(new Date(note.getUpdatedAt())), 11, MUTED, false);
            LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            timeLp.topMargin = dp(10);
            card.addView(time, timeLp);

            card.setOnClickListener(v -> openEditor(note.getId()));
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = dp(10);
            container.addView(card, cardLp);
        }
    }

    private void openEditor(long id) {
        editorMode = true;
        editingId = id;
        Note note = id > 0 ? repository.get(id) : null;
        if (id > 0 && note == null) {
            Toast.makeText(this, "Заметка не найдена", Toast.LENGTH_SHORT).show();
            showList();
            return;
        }

        LinearLayout root = column();
        root.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("←", PANEL_ALT);
        back.setContentDescription("Назад");
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(46)));

        TextView heading = text(id > 0 ? "Редактирование" : "Новая заметка", 19, TEXT, true);
        LinearLayout.LayoutParams headingLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        headingLp.leftMargin = dp(12);
        header.addView(heading, headingLp);

        Button save = button("Сохранить", ACCENT);
        header.addView(save, new LinearLayout.LayoutParams(dp(112), dp(46)));
        root.addView(header);

        titleEditor = new EditText(this);
        titleEditor.setSingleLine(true);
        titleEditor.setHint("Заголовок");
        titleEditor.setHintTextColor(MUTED);
        titleEditor.setTextColor(TEXT);
        titleEditor.setTextSize(19);
        titleEditor.setPadding(dp(14), 0, dp(14), 0);
        titleEditor.setBackground(roundRect(PANEL, 14));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        titleLp.topMargin = dp(14);
        titleLp.bottomMargin = dp(10);
        root.addView(titleEditor, titleLp);

        contentEditor = new EditText(this);
        contentEditor.setGravity(Gravity.TOP | Gravity.START);
        contentEditor.setHint("Код, команды, идеи, ошибки и решения…");
        contentEditor.setHintTextColor(MUTED);
        contentEditor.setTextColor(TEXT);
        contentEditor.setTextSize(15);
        contentEditor.setTypeface(Typeface.MONOSPACE);
        contentEditor.setPadding(dp(14), dp(14), dp(14), dp(14));
        contentEditor.setBackground(roundRect(PANEL, 14));
        contentEditor.setSingleLine(false);
        contentEditor.setHorizontallyScrolling(false);
        contentEditor.setGravity(Gravity.TOP | Gravity.START);
        root.addView(contentEditor, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout footer = row();
        footer.setGravity(Gravity.CENTER_VERTICAL);
        TextView hint = text("Автосохранение при выходе", 12, MUTED, false);
        footer.addView(hint, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (id > 0) {
            Button delete = button("Удалить", DANGER);
            footer.addView(delete, new LinearLayout.LayoutParams(dp(102), dp(44)));
            delete.setOnClickListener(v -> confirmDelete());
        }
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerLp.topMargin = dp(10);
        root.addView(footer, footerLp);

        if (note != null) {
            titleEditor.setText(note.getTitle());
            contentEditor.setText(note.getContent());
            contentEditor.setSelection(contentEditor.length());
        }

        back.setOnClickListener(v -> leaveEditor(true));
        save.setOnClickListener(v -> {
            saveCurrent();
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
            showList();
        });
        setContentView(root);
        if (id <= 0) titleEditor.requestFocus();
    }

    private void saveCurrent() {
        if (!editorMode || titleEditor == null || contentEditor == null) return;
        long savedId = repository.save(editingId, titleEditor.getText().toString(), contentEditor.getText().toString());
        if (savedId > 0) editingId = savedId;
    }

    private void leaveEditor(boolean save) {
        if (save) saveCurrent();
        showList();
    }

    private void confirmDelete() {
        if (editingId <= 0) return;
        new AlertDialog.Builder(this)
                .setTitle("Удалить заметку?")
                .setMessage("Это действие нельзя отменить.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить", (dialog, which) -> {
                    repository.delete(editingId);
                    Toast.makeText(this, "Заметка удалена", Toast.LENGTH_SHORT).show();
                    showList();
                })
                .show();
    }

    @Override
    protected void onPause() {
        if (editorMode) saveCurrent();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (editorMode) {
            leaveEditor(true);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (repository != null) repository.close();
        super.onDestroy();
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(BG);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setBackgroundColor(Color.TRANSPARENT);
        return layout;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(roundRect(color, 12));
        return button;
    }

    private GradientDrawable roundRect(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
