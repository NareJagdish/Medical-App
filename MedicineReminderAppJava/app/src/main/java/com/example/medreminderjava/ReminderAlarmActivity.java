package com.example.medreminderjava;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.medreminderjava.data.DatabaseHelper;
import com.example.medreminderjava.notification.AlarmReceiver;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderAlarmActivity extends AppCompatActivity {

    private Ringtone ringtone;
    private Vibrator vibrator;
    private long medicineId;
    private String medicineName;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Advanced screen waking and lock screen bypass
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            android.app.KeyguardManager km = (android.app.KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) {
                km.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        
        // Full screen flags
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_reminder_alarm);

        dbHelper = DatabaseHelper.getInstance(this);
        medicineId = getIntent().getLongExtra(AlarmReceiver.EXTRA_MEDICINE_ID, -1);
        medicineName = getIntent().getStringExtra(AlarmReceiver.EXTRA_MEDICINE_NAME);
        String timing = getIntent().getStringExtra(AlarmReceiver.EXTRA_TIMING_INFO);

        TextView tvMedName = findViewById(R.id.tvMedName);
        TextView tvCurrentTime = findViewById(R.id.tvCurrentTime);
        TextView tvMedDosage = findViewById(R.id.tvMedDosage);

        tvMedName.setText(medicineName != null ? medicineName : "Medicine");
        tvMedDosage.setText(timing != null ? timing : "Reminder");

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        tvCurrentTime.setText(sdf.format(new Date()));

        startAlarm();

        findViewById(R.id.btnTakeNow).setOnClickListener(v -> {
            takeMedicine();
            stopAlarm();
            finish();
        });

        findViewById(R.id.btnSkipped).setOnClickListener(v -> {
            skipMedicine();
            stopAlarm();
            finish();
        });
    }

    private void startAlarm() {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (notification == null) {
            notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        if (ringtone != null) {
            ringtone.play();
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            long[] pattern = {0, 1000, 1000};
            vibrator.vibrate(pattern, 0);
        }
    }

    private void stopAlarm() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    private void takeMedicine() {
        if (medicineId != -1) {
            dbHelper.deductMedicineDose(medicineId);
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            dbHelper.logMedicineIntake(medicineId, today, "Taken");
            Toast.makeText(this, "Medicine Logged: Taken", Toast.LENGTH_SHORT).show();
        }
    }

    private void skipMedicine() {
        if (medicineId != -1) {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            dbHelper.logMedicineIntake(medicineId, today, "Missed");
            Toast.makeText(this, "Medicine Logged: Skipped", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarm();
    }
}
