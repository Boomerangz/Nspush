package com.example.nspush;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private EditText urlInput;
    private EditText secretInput;
    private EditText intervalInput;
    private EditText lowInput;
    private EditText highInput;
    private CheckBox enabledInput;
    private CheckBox notifyEveryCheckInput;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationHelper.ensureChannel(this);
        requestNotificationPermissionIfNeeded();
        setContentView(buildContent());
        loadSettings();
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        root.setPadding(padding, padding, padding, padding);
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Nightscout Push");
        title.setTextSize(26);
        root.addView(title);

        TextView description = new TextView(this);
        description.setText("Проверяет Nightscout и создает обычное Android-уведомление. Если Huawei Health пересылает уведомления этого приложения, оно появится на Huawei GT6.");
        description.setPadding(0, dp(8), 0, dp(16));
        root.addView(description);

        urlInput = addTextInput(root, "Nightscout URL", "https://your-site.example", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        secretInput = addTextInput(root, "API secret или access token (необязательно)", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        intervalInput = addTextInput(root, "Интервал, минут (минимум " + AppConfig.getMinimumIntervalMinutes() + ")", String.valueOf(AppConfig.DEFAULT_INTERVAL_MINUTES), InputType.TYPE_CLASS_NUMBER);
        lowInput = addTextInput(root, "Нижний порог mg/dL", String.valueOf(AppConfig.DEFAULT_LOW), InputType.TYPE_CLASS_NUMBER);
        highInput = addTextInput(root, "Верхний порог mg/dL", String.valueOf(AppConfig.DEFAULT_HIGH), InputType.TYPE_CLASS_NUMBER);

        notifyEveryCheckInput = new CheckBox(this);
        notifyEveryCheckInput.setText("Уведомлять при каждой проверке (иначе только вне диапазона/устаревшие данные)");
        root.addView(notifyEveryCheckInput);

        enabledInput = new CheckBox(this);
        enabledInput.setText("Включить фоновую проверку");
        enabledInput.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                statusText.setText(isChecked ? "Проверка будет запланирована после сохранения." : "Проверка будет остановлена после сохранения.");
            }
        });
        root.addView(enabledInput);

        Button saveButton = new Button(this);
        saveButton.setText("Сохранить и запланировать");
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
        root.addView(saveButton);

        Button testButton = new Button(this);
        testButton.setText("Проверить сейчас");
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
                NightscoutWorker.runOnce(MainActivity.this);
                Toast.makeText(MainActivity.this, "Проверка запущена", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(testButton);

        statusText = new TextView(this);
        statusText.setPadding(0, dp(16), 0, 0);
        root.addView(statusText);
        return scrollView;
    }

    private EditText addTextInput(LinearLayout root, String label, String hint, int inputType) {
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setPadding(0, dp(12), 0, 0);
        root.addView(labelView);
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setSingleLine(true);
        editText.setInputType(inputType);
        root.addView(editText);
        return editText;
    }

    private void loadSettings() {
        AppConfig config = AppConfig.from(this);
        urlInput.setText(config.getBaseUrl());
        secretInput.setText(config.getSecret());
        intervalInput.setText(String.valueOf(config.getIntervalMinutes()));
        lowInput.setText(String.valueOf(config.getLowThreshold()));
        highInput.setText(String.valueOf(config.getHighThreshold()));
        notifyEveryCheckInput.setChecked(config.shouldNotifyEveryCheck());
        enabledInput.setChecked(config.isEnabled());
        statusText.setText(config.isEnabled() ? "Фоновая проверка включена." : "Фоновая проверка выключена.");
    }

    private void saveSettings() {
        String url = urlInput.getText().toString();
        if (!AppConfig.isValidUrl(url)) {
            Toast.makeText(this, "Введите корректный Nightscout URL", Toast.LENGTH_LONG).show();
            return;
        }
        int interval = parseInt(intervalInput, AppConfig.DEFAULT_INTERVAL_MINUTES);
        int low = parseInt(lowInput, AppConfig.DEFAULT_LOW);
        int high = parseInt(highInput, AppConfig.DEFAULT_HIGH);
        if (low >= high) {
            Toast.makeText(this, "Нижний порог должен быть меньше верхнего", Toast.LENGTH_LONG).show();
            return;
        }
        int safeInterval = Math.max(AppConfig.getMinimumIntervalMinutes(), interval);
        AppConfig.from(this).save(
                url,
                secretInput.getText().toString(),
                safeInterval,
                low,
                high,
                notifyEveryCheckInput.isChecked(),
                enabledInput.isChecked()
        );
        intervalInput.setText(String.valueOf(safeInterval));
        if (enabledInput.isChecked()) {
            NightscoutWorker.schedule(this, safeInterval);
            statusText.setText("Фоновая проверка запланирована каждые " + safeInterval + " мин.");
        } else {
            NightscoutWorker.cancel(this);
            statusText.setText("Фоновая проверка остановлена.");
        }
        Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show();
    }

    private int parseInt(EditText editText, int fallback) {
        String value = editText.getText().toString().trim();
        if (value.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
