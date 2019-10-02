package com.virun.customloading;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.jar.Attributes;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final CustomLoading customLoading = findViewById(R.id.loading);

        Button button = findViewById(R.id.button);
        Button button1 = findViewById(R.id.button2);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customLoading.startLoading();
            }
        });
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customLoading.stopLoading();
            }
        });
    }
}
