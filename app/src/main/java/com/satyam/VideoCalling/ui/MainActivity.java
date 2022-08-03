package com.satyam.VideoCalling.ui;

import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.satyam.VideoCalling.R;
import com.satyam.VideoCalling.application.MyApplication;
import com.satyam.VideoCalling.managers.ActivitySwitchManager;
import com.satyam.VideoCalling.managers.SharedPrefManager;
import com.satyam.VideoCalling.ui.auth.AuthActivity;
import com.satyam.VideoCalling.utils.Constants;
import com.satyam.VideoCalling.utils.Utils;

public class MainActivity extends AppCompatActivity {

    private MaterialButton online_counter;
    private MaterialButton startSearch;
    private MaterialButton btn_logout;
    private TextView tvUsername;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private SharedPrefManager sharedPrefManager;

    private static int flag = 0;
    private static int index = 0;
    public static long timestamp = 0;
    public static Handler handler;
    public static Dialog dialog;
    private static String channel_id = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUi();
        initConfig();

        setSearchingCounter();
    }

    private void initUi() {
        online_counter = findViewById(R.id.onlineCounter);
        btn_logout = findViewById(R.id.btn_logout);
        startSearch = findViewById(R.id.startSearch);
        tvUsername = findViewById(R.id.tv_username);
    }


    private void setSearchingCounter() {
        firestore.collection(Constants.COLLECTION_SEARCHING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e("ERROR: COUNT SEARCH", e.getMessage());
                            return;
                        }

                        if (snapshots != null) {
                            int count = snapshots.getDocuments().size();
                            Resources res = getResources();
                            String text = String.format(res.getString(R.string.online_ct), String.valueOf(count));
                            online_counter.setText(text);
                            online_counter.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }


    private void initConfig() {
        firebaseAuth = MyApplication.getInstance().getFirebaseAuth();
        firestore = MyApplication.getInstance().getFirestore();
        sharedPrefManager = MyApplication.getInstance().getSharedPrefManager();
        String[] name = sharedPrefManager.getUser().getUserName().split(" ");
        tvUsername.setText("Hello, " + name[0]);


        //Remove from Searching queue if exists
        firestore.collection(Constants.COLLECTION_SEARCHING).document(firebaseAuth.getCurrentUser().getUid())
                .delete();

        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth.signOut();
                sharedPrefManager.signOut();
                new ActivitySwitchManager(MainActivity.this, AuthActivity.class).openActivity();
            }
        });

        handler = new Handler();
        startSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Utils.isNetworkConnected(MainActivity.this)) {
                    timestamp = System.currentTimeMillis();
                    dialog = Utils.showSearching(MainActivity.this);
                    startSearching();
                    final TextView tv_time = dialog.findViewById(R.id.tv_time);
                    dialog.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            stopSearching();
                            dialog.dismiss();
                        }
                    });

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            long curr_time = System.currentTimeMillis();
                            long diff = (curr_time - timestamp) / 1000;
                            if (diff <= 15) {
                                handler.postDelayed(this, 1000);
                                if (diff < 10) {
                                    tv_time.setText("0:0" + diff);
                                } else {
                                    tv_time.setText("0:" + diff);
                                }
                            } else {
                                dialog.dismiss();
                                if (flag == 0) {
                                    stopSearching();
                                    Toast.makeText(MainActivity.this, "There are no users online for a video chat. Please try again after some time", Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    }, 1000);
                }
            }
        });
    }

    public static void stopSearching() {
        handler.removeCallbacksAndMessages(null);
        MyApplication.getInstance().getFirestore().collection(Constants.COLLECTION_SEARCHING).document(MyApplication.getInstance().getFirebaseAuth().getCurrentUser().getUid())
                .delete();
        if (!channel_id.equals("")) {
            MyApplication.getInstance().getFirestore().collection(Constants.COLLECTION_CHANNELS).document(channel_id)
                    .delete();
        }
    }

    public static void startSearching() {
        MyApplication.getInstance().getFirestore().collection(Constants.COLLECTION_SEARCHING)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snapshots) {
                        Log.e("OK", "OK");
                        if (snapshots != null && !snapshots.isEmpty()) {
                            List<DocumentSnapshot> documentSnapshots = snapshots.getDocuments();
                            final String userId_1 = documentSnapshots.get(0).getString("userId");
                            final String userName_1 = documentSnapshots.get(0).getString("userName_1");
                            final String channelId = documentSnapshots.get(0).getString("channel");

                            //update channel
                            HashMap<String, Object> updates = new HashMap<>();
                            long currentTime = System.currentTimeMillis();
                            String timeReal = setDate(currentTime);
                            updates.put("userId_2", MyApplication.getInstance().getFirebaseAuth().getCurrentUser().getUid());
                            updates.put("userName_2", MyApplication.getInstance().getSharedPrefManager().getUser().getUserName());
                            updates.put("joined_at", timeReal);
                            MyApplication.getInstance().getFirestore().collection(Constants.COLLECTION_CHANNELS).document(channelId)
                                    .update(updates)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.e("Joined channel: ", channelId);
                                            //TODO: START VIDEO CALL
                                            flag = 1;
                                            HashMap<String, String> params = new HashMap<>();
                                            params.put("channel_id", channelId);
                                            params.put("userId_1", userId_1);
                                            params.put("userId_2", userName_1);
                                            params.put("userName_1", MyApplication.getInstance().getFirebaseAuth().getCurrentUser().getUid());
                                            params.put("userName_2", MyApplication.getInstance().getSharedPrefManager().getUser().getUserName());
                                            new ActivitySwitchManager(MyApplication.getInstance().getActivity(), VideoChatViewActivity.class, params).openActivityWthoutFinish();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e("ERROR: JOIN CHANNEL", e.getMessage());
                                        }
                                    });

                        } else if (snapshots != null && snapshots.getDocuments().size() == 0) {
                            long currentTime = System.currentTimeMillis();
                            String timeReal = setDate(currentTime);
                            final HashMap<String, Object> channelData = new HashMap<>();
                            channelData.put("created_at", timeReal);
                            channelData.put("userId_1", MyApplication.getInstance().getFirebaseAuth().getCurrentUser().getUid());
                            channelData.put("userName_1", MyApplication.getInstance().getSharedPrefManager().getUser().getUserName());
                            channelData.put("userId_2", "");
                            channelData.put("userName_2", "");

                            MyApplication.getInstance().getFirestore().collection(Constants.COLLECTION_CHANNELS)
                                    .add(channelData)
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {
                                            //TODO : listen to channel updates
                                            channel_id = documentReference.getId();

                                            //Add to queue
                                            long currentTime = System.currentTimeMillis();
                                            String timeReal = setDate(currentTime);
                                            HashMap<String, Object> searchingData = new HashMap<>();
                                            searchingData.put("timestamp",timeReal);
                                            searchingData.put("userId", MyApplication.getInstance().getFirebaseAuth().getCurrentUser().getUid());
                                            searchingData.put("channel", channel_id);
                                            MyApplication.getInstance().getFirestore().collection(Constants.COLLECTION_SEARCHING).document(MyApplication.getInstance().getFirebaseAuth().getCurrentUser().getUid())
                                                    .set(searchingData);

                                            MyApplication.getInstance().getFirestore().collection(Constants.COLLECTION_CHANNELS).document(channel_id)
                                                    .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                                        @Override
                                                        public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                                                            if (e != null) {
                                                                Log.e("ERROR: CHANNEL", e.getMessage());
                                                                return;
                                                            }

                                                            if (snapshot != null) {
                                                                Log.e("snapshot", snapshot.getId() + " " + snapshot.getString("userId_2") + " " + snapshot.getString("userName_2"));
                                                                String participantId = snapshot.getString("userId_2");
                                                                if (participantId != null && !participantId.equals("")) {
                                                                    //TODO: START VIDEO CALL
                                                                    flag = 1;
                                                                    MyApplication.getInstance().getFirestore().collection(Constants.COLLECTION_SEARCHING).document(MyApplication.getInstance().getFirebaseAuth().getCurrentUser().getUid())
                                                                            .delete();
                                                                    HashMap<String, String> params = new HashMap<>();
                                                                    params.put("channel_id", channel_id);
                                                                    params.put("userId_1", snapshot.getString("userId_1"));
                                                                    params.put("userId_2", snapshot.getString("userId_2"));
                                                                    params.put("userName_1", snapshot.getString("userName_1"));
                                                                    params.put("userName_2", snapshot.getString("userName_2"));
                                                                    new ActivitySwitchManager(MyApplication.getInstance().getActivity(), VideoChatViewActivity.class, params).openActivityWthoutFinish();

                                                                }
                                                            }
                                                        }
                                                    });
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            //TODO: Channel Creating Failed
                                            Log.e("ERROR: ADD CHANNEL", e.getMessage());
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });

    }

    private static String setDate(long currentTime) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
        Date date = new Date(currentTime);
        String time = simpleDateFormat.format(date);
        return time;
    }

}
