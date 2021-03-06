package com.taewon.shoppingmall.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

import com.google.firebase.storage.StorageReference;
import com.taewon.shoppingmall.R;
import com.taewon.shoppingmall.adapter.BoardRecyclerAdapter;
import com.taewon.shoppingmall.dialog.LottieLoadingDialog;
import com.taewon.shoppingmall.item.BoardItem;
import com.taewon.shoppingmall.util.BoardDateComparator;
import com.taewon.shoppingmall.util.BoardStarCountComparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MainActivity2 extends AppCompatActivity {
    BoardRecyclerAdapter searchedBoardRecyclerAdapter;
    String categoryString;
    FirebaseDatabase database;
    FirebaseAuth mAuth;
    RecyclerView rv_boardRecycler;
    ArrayList<BoardItem> searchItemList;
    Spinner sp_orderSpinner;
    FirebaseStorage storage;
    TextView tv_category;
    LottieLoadingDialog dialog;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        init();
        initViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getBoard();
    }

    private void init() {
        Intent intent = getIntent();
        categoryString = intent.getStringExtra("category");
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        searchItemList = new ArrayList<>();
        dialog = new LottieLoadingDialog(MainActivity2.this);
    }

    private void initViews() {
        tv_category = findViewById(R.id.tv_category);
        tv_category.setText(categoryString.toUpperCase());

        //?????????
        sp_orderSpinner = (Spinner) findViewById(R.id.sp_orderSpinner);
        ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.spinner_items, R.layout.support_simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        sp_orderSpinner.setAdapter(adapter);
        sp_orderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                switch (position) {
                    case 0:
                        Collections.sort(searchItemList, new BoardDateComparator());
                        break;
                    case 1:
                        Collections.sort(searchItemList, new BoardDateComparator(true));
                        break;
                    case 2:
                        Collections.sort(searchItemList, new BoardStarCountComparator());
                        break;
                }
                searchedBoardRecyclerAdapter.notifyDataSetChanged();
            }
            public void onNothingSelected(AdapterView<?> adapterView) {
                sp_orderSpinner.setSelection(0);
            }
        });

        //??????????????????
        rv_boardRecycler = (RecyclerView) findViewById(R.id.rv_boardRecycler);
        BoardRecyclerAdapter boardRecyclerAdapter = new BoardRecyclerAdapter(MainActivity2.this, searchItemList);
        searchedBoardRecyclerAdapter = boardRecyclerAdapter;
        LinearLayoutManager manager = new LinearLayoutManager(MainActivity2.this);
        manager.setOrientation(RecyclerView.VERTICAL);
        new PagerSnapHelper().attachToRecyclerView(rv_boardRecycler);
        rv_boardRecycler.setLayoutManager(manager);
        rv_boardRecycler.setAdapter(boardRecyclerAdapter);
    }

    private void getBoard(){
        dialog.show();
        rv_boardRecycler.setVisibility(View.VISIBLE);
        database.getReference("Board/").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
            public void onSuccess(DataSnapshot dataSnapshot) {
                // 1.???????????? ?????????. ????????????.
                searchItemList.clear();

                // 2. ????????? ???????????? ?????? ?????? ???????????? ????????????,
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    BoardItem item = (BoardItem) snapshot.getValue(BoardItem.class);
                    item.setBoardID(snapshot.getKey());
                    List<String> tags = item.getTags();

                    StringBuilder sb = new StringBuilder();
                    for(String tag : tags){
                        sb.append(tag);
                    }
                    //2021 12 10 ????????? ????????????
                    if(sb.toString().contains(categoryString) || item.getTitle().contains(categoryString)){
                        searchItemList.add(item);
                    }
                }

                // 3. ??? 2d, 3d ??????????????? ??????????????? ??????.
                Collections.sort(searchItemList, new BoardDateComparator());

                // 4. ???????????? ????????? ????????? ????????? ??????????????? ????????????.
                searchedBoardRecyclerAdapter.notifyDataSetChanged();

                if(searchItemList.size() < 1){
                    rv_boardRecycler.setVisibility(View.GONE);
                }
                dialog.dismiss();
            }
        }).addOnFailureListener(new OnFailureListener() {
            // ????????? ???????????? ???.
            public void onFailure(Exception e) {
                dialog.dismiss();
                new AlertDialog.Builder(MainActivity2.this).setTitle("???????????? ???????????? ???????????????.\n?????? ??????????????????.").setMessage("").setIcon(R.drawable.ic_baseline_back_hand_24).setPositiveButton("??????", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        getBoard();
                    }
                }).setNegativeButton("??????", (DialogInterface.OnClickListener) null).create().show();
            }
        });
    }
}