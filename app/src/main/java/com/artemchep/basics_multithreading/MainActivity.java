package com.artemchep.basics_multithreading;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artemchep.basics_multithreading.cipher.CipherUtil;
import com.artemchep.basics_multithreading.domain.Message;
import com.artemchep.basics_multithreading.domain.WithMillis;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {

    private List<WithMillis<Message>> mList = new ArrayList<>();

    private MessageAdapter mAdapter = new MessageAdapter(mList);

    private Queue<WithMillis<Message>> myQueue;
    private Thread t;
    private boolean tempRun = true;
    private Handler hnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);

        myQueue = new LinkedList<>();

        myHandler();
        myThreed();

    }

    public void onPushBtnClick(View view) {
        Message message = Message.generate();
        insert(new WithMillis<>(message));
    }

    @UiThread
    public void insert(final WithMillis<Message> message) {
        mList.add(message);
        mAdapter.notifyItemInserted(mList.size() - 1);

        synchronized (myQueue) {
            myQueue.add(message);
            myQueue.notifyAll();
        }

        update(message);

    }

    @UiThread
    public void update(final WithMillis<Message> message) {
        for (int i = 0; i < mList.size(); i++) {
            if (mList.get(i).value.key.equals(message.value.key)) {
                mList.set(i, message);
                mAdapter.notifyItemChanged(i);
                return;
            }
        }

        throw new IllegalStateException();
    }

    public void myThreed(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(tempRun) {
                    List<WithMillis<Message>> messageArray;

                    synchronized (myQueue) {
                        messageArray = new ArrayList<>(myQueue);
                        myQueue.clear();
                    }

                    for(int i = 0; i < messageArray.size(); i++) {
                        android.os.Message message = android.os.Message.obtain();
                        WithMillis<Message> messageWithMillis = messageArray.get(i);
                        message.obj = new WithMillis<>(messageWithMillis.value.copy(CipherUtil.encrypt(messageWithMillis.value.plainText)), System.currentTimeMillis());
                        message.setTarget(hnd);
                        message.sendToTarget();
                    }

                    synchronized (myQueue) {
                        if(myQueue.isEmpty()) {
                            try {
                                myQueue.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }).start();
    }

    public void myHandler(){

        hnd = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {

                WithMillis<Message> messageWithMillis = (WithMillis<Message>) msg.obj;
                update(new WithMillis<>(messageWithMillis.value.copy(messageWithMillis.value.cipherText),(System.currentTimeMillis() - messageWithMillis.elapsedMillis)));
            }
        };
    }
}
