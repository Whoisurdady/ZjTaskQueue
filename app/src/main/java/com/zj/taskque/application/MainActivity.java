package com.zj.taskque.application;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zhaojian.taskqueueapplication.R;
import com.zj.taskque.application.sample.SampleTask1;
import com.zj.taskque.application.sample.SampleTask2;

import basic.TaskQueue;
import basic.TaskQueueBuilder;

public class MainActivity extends AppCompatActivity {
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            String s = (String)message.obj;
            textView.setText(s);
            return false;
        }
    });
    TextView textView;
    Button startBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.text);
        startBtn = (Button) findViewById(R.id.start);
        startBtn.setTextColor(Color.BLACK);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SampleTask1 task1 = new SampleTask1(handler);
                SampleTask2 task2 = new SampleTask2(handler);
                TaskQueue.getBuilder().add(task1).onSucceed(new Runnable() {
                    @Override
                    public void run() {
                        textView.post(new Runnable() {
                            @Override
                            public void run() {
                                textView.setTextColor(Color.RED);
                            }
                        });
                    }
                }).build().add(task2).onSucceed(new Runnable() {
                    @Override
                    public void run() {
                        textView.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Task Test Ok!", Toast.LENGTH_SHORT).show();
                                startBtn.setEnabled(true);
                                startBtn.setTextColor(Color.BLACK);
                            }
                        });

                    }
                }).build().execute();
                startBtn.setEnabled(false);
                startBtn.setTextColor(Color.GRAY);
            }
        });
    }
}
